package kse.algorithm;

import static kse.misc.GlobalParams.*;
import static kse.algorithm.auxiliaryClass.GlobalFunct.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.UnsatTriple;
import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.MUPP;
import kse.misc.Timekeeping;
import kse.misc.Tools;

import org.apache.commons.collections.CollectionUtils;
import org.neo4j.cypher.internal.PathImpl;
//import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * 
 * 本体诊断与修正类
 * 1. 提供 MIPP MUPP的计算函数
 * 2. 提供ABox不一致修正函数
 * 最近修改时间：2015-5-30 ！important
 * @author Xuefeng Fu
 **/

public class Diagnosis {
	String owlPath;
	String gPath;
	GraphDatabaseService graphDB;
	ExecutionEngine engine;
	Set<String> unsatNodes;
	boolean hasDisjointLabel;
	
	public static Diagnosis getDiagnosis(GraphDatabaseService graphDB){
		return new Diagnosis(graphDB);
	}
	
	public static Diagnosis getDiagnosis(String owlPath,String gPath){
		return new Diagnosis(owlPath,gPath);
	}
	
	public Diagnosis(String owlPath,String gPath){
		this.owlPath = owlPath;
		this.gPath = gPath;
		graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		engine = new ExecutionEngine(graphDB);
		unsatNodes = new HashSet<>();
		hasDisjointLabel = false;
	}
	
	public Diagnosis(GraphDatabaseService graphDB){
		this.graphDB = graphDB;
		engine = new ExecutionEngine(graphDB);
		unsatNodes = new HashSet<>();
		hasDisjointLabel = false;
	}
	
	public String getOwlPath() {
		return owlPath;
	}

	public String getGPath() {
		return gPath;
	}

	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}
	
	/**
	 * 取出TBox中的不可满足节点(概念)<br>
	 * 算法不太高效，查找了MUPS，而实际只要计算不相交概念子类即可
	 */
	public  Set<String> getUnsatNodeByCypher(){
		System.out.println("Getting unsatisfactable nodes in TBox");	
		
		List<DisjointPair> disjointPairs = getDisjointPair(graphDB);
		System.out.println("DisjointPairs size is "+disjointPairs.size());
		
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE n.Name='%s' and _n.Name='%s' ";		
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : disjointPairs){
				Tools.clear(query);		
				query.append("MATCH p=(n<-[*]-(unsat)-[ *]->_n) ");
				query.append(String.format(whereFormatter,pair.getFirst(), pair.getSecond()));
				query.append("RETURN  unsat.Name as UC ");	
				//System.out.println(query.toString());
				ExecutionResult result = engine.execute(query.toString());
				ResourceIterator<String> unsats = result.columnAs("UC");
				while(unsats.hasNext()){
					String unsat = unsats.next();
					if(!unsatNodes.contains(unsat)){
						unsatNodes.add(unsat);
						System.out.println(unsat);
					}
				}
			}
			tx.success();
		}	
		return unsatNodes;
	}
	
	/**
	 * 计算图中不可满足节点，从不相交的节点队出发，分别计算他们所有的子节点
	 * 注意：由于使用递归的方法，故图上不能有环，否则递归栈会溢出。
	 * 最后不相交节点的子节点集合的交就是不可满足的概念
	 */
	public void getUnsatNodes(){
		System.out.println("Getting unsatisfactable nodes in TBox by relationship");
		unsatNodes.clear();
		
		List<DisjointPair> disjointPairs = getDisjointPair(graphDB);	
		System.out.println("disjoint pairs:"+disjointPairs.size());
		try(Transaction tx = graphDB.beginTx()){
			Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);			
			for(DisjointPair pair : disjointPairs){
				
				Node first = nodeIndex.get(NAMEPROPERTY, pair.getFirst()).getSingle();
				Node second = nodeIndex.get(NAMEPROPERTY, pair.getSecond()).getSingle();
				
				//使用递归的方式来获取所有的子类
				Set<String> firstDescendant = getDescendantNodesWithRecursion(first);
				Set<String> secondDescendant =  getDescendantNodesWithRecursion(second);
				
				firstDescendant.retainAll(secondDescendant);//两集合的交
				unsatNodes.addAll(firstDescendant);
			}
			tx.success();
		}	
		System.out.println("Number of UN  is "+unsatNodes.size());		
	}
	
	public  List<UnsatTriple> getUnsatTripleByCypher(){
		System.out.println("Getting unsatisfactable nodes in TBox");			
		List<UnsatTriple> unSatTriples = new ArrayList<>();		
		List<DisjointPair> disjointPairs = getDisjointPair(graphDB);
		System.out.println("DisjointPairs size is "+disjointPairs.size());		
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE n.Name='%s' and _n.Name='%s' ";		
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : disjointPairs){
				Tools.clear(query);		
				query.append("MATCH p=( (n:Concept)<-[*]-(unsat:Concept)-[ *]->(_n:Concept) ) ");
				query.append(String.format(whereFormatter,pair.getFirst(), pair.getSecond()));
				query.append("RETURN  unsat.Name as UC ");	
				//System.out.println(query.toString());
				ExecutionResult result = engine.execute(query.toString());
				ResourceIterator<String> unsats = result.columnAs("UC");
				while(unsats.hasNext()){
					String unsat = unsats.next();
					UnsatTriple triple = new UnsatTriple(unsat, pair.getFirst(), pair.getSecond());
					unSatTriples.add(triple);
				}
			}
			tx.success();
		}	
		return unSatTriples;
	}
	
	/**
	 * 通过找不相交概念，取出TBox中的不可满足节点(概念)<br>
	 * 这个值不是不可满足概念数，是不可满足概念与不交概念的三元组
	 */
	public  List<UnsatTriple> getUnsatTripleByRelationship(){
		System.out.println("Getting unsatisfactable nodes in TBox by relationship");
		List<UnsatTriple> unSatTriples = new ArrayList<>();				
		List<DisjointPair> disjointPairs = getDisjointPair(graphDB);	
		System.out.println("disjoint pairs:"+disjointPairs.size());
		try(Transaction tx = graphDB.beginTx()){
			Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);			
			for(DisjointPair pair : disjointPairs){
				
				Node first = nodeIndex.get(NAMEPROPERTY, pair.getFirst()).getSingle();
				Node second = nodeIndex.get(NAMEPROPERTY, pair.getSecond()).getSingle();
				
				System.out.println(first.getProperty(NAMEPROPERTY));
				System.out.println(second.getProperty(NAMEPROPERTY));
				
				//使用递归的方式来获取所有的子类
				Set<String> firstDescendant = getDescendantNodesWithRecursion(first);
				Set<String> secondDescendant =  getDescendantNodesWithRecursion(second);
				
				//不使用递归的方式来获取所有的子类
				//Set<String> firstDescendant = getDescendantNodes(first);
				//Set<String> secondDescendant =  getDescendantNodes(second);
				
				firstDescendant.retainAll(secondDescendant);//两集合的交
				unsatNodes.addAll(firstDescendant);
				for(String nodeName : firstDescendant){
					unSatTriples.add(new UnsatTriple(nodeName, pair.getFirst(),pair.getSecond()));
				}
			}
			tx.success();
		}	
		/*for(String un:unsatNodes){
			System.out.println(un);
		}*/
		System.out.println("Number of UN  is "+unsatNodes.size());		
		return unSatTriples;
	}
	
	//
	/**
	 * 获取不可满足概念三元组，并进行标示
	 * @return
	 */
	public  List<UnsatTriple> getUnsatTripleWithLabelByRelationship(){
		Timekeeping.begin();
		List<UnsatTriple> triples = getUnsatTripleByRelationship();
		Timekeeping.end();
		Timekeeping.showInfo("Computer unsatisfiable concept ");
		//labelUnsats();
		return triples;
	}
	/**
	 * 通过节点的名字检索节点
	 * @param nodeName：节点名
	 * @return Node in graph
	 */
	public Node getNodeByName(String nodeName){
		GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDB);
		for(Node node : ggo.getAllNodes()){
			if(node.getProperty(NAMEPROPERTY).equals(nodeName))
				return node;
		}
		return null;
	}
	
	/**
	 * 计算图数据库中的MUPP
	 * @param graphDB 目标图数据库
	 */
	public List<MUPP> compMUPPs(){
		List<MUPP> mupps = new ArrayList<>();
		System.out.println("Compute MUPPS in TBox of Graph database.");
		List<DisjointPair> disjointPairs = getDisjointPair(graphDB);
		String whereFormatter = "WHERE n.Name='%s' and _n.Name='%s' ";		
		StringBuilder query = new StringBuilder();
		for(DisjointPair pair : disjointPairs){
			Tools.clear(query);		
			//query.append("MATCH p=((n:Concept)<-[:INCLUDEDBY *]-(unsat:Concept)-[:INCLUDEDBY *]->(_n:Concept)) ");
			//query.append("MATCH p=((n)<-[:INCLUDEDBY *]-(unsat)-[:INCLUDEDBY *]->(_n)) "); //不使用node标签，竟然运行速度要快
			//分成两段来执行
			/*query.append("MATCH pp=((unsat)-[:INCLUDEDBY *]->n), ");
			query.append(" np=((unsat)-[:INCLUDEDBY *]->_n) ");	*/	
			query.append("MATCH pp=(uc:Concept)-[:INCLUDEDBY *]->(n:Concept), ");
			query.append(" np=(uc:Concept)-[:INCLUDEDBY *]->(_n:Concept) ");	
			query.append(String.format(whereFormatter,pair.getFirst(), pair.getSecond()));		
			query.append(" RETURN pp,np,uc");			
			
			try(Transaction tx = graphDB.beginTx()){			
				ExecutionResult result = engine.execute(query.toString());
				ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
				while(resultInIterator.hasNext()){
					Map<String,Object> unsatMap = resultInIterator.next();					
					Node unsat = (Node)unsatMap.get("uc");
					PathImpl pPath = (PathImpl)unsatMap.get("pp");
					PathImpl nPath = (PathImpl)unsatMap.get("np");				
					
					String unsatNode = unsat.getProperty(NAMEPROPERTY).toString();
					Set<String> nodes =nodesToList(pPath,nPath);
					List<Relationship> pp = pathToList(pPath);
					List<Relationship> pn = pathToList(nPath);
					
					MUPP mups = new MUPP(unsatNode,pp,pn,nodes);
					mupps.add(mups);
				}	
				tx.success();
			}
		}	
		return mupps;
	}
	
	/**
	 * 计算一个不可满足概念的MUPS
	 * @return
	 */
	public List<MUPP> compMUPS(UnsatTriple triple){
		List<MUPP> mupps = new ArrayList<>();
		System.out.println("Compute MUPS : "+ triple.getUnsat());		
		
		String unsatNode = triple.getUnsat();
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("pos", triple.getFirst());
		params.put("neg", triple.getSecond());	
		params.put("uc", unsatNode);
		
		StringBuilder query = new StringBuilder();			
		query.append("MATCH pp=((uc:Concept)-[:INCLUDEDBY *]->n:Concept) ");
		query.append("MATCH np=((uc:Concept)-[:INCLUDEDBY *]->_n:Concept) ");	
		query.append("WHERE n.Name={pos} and _n.Name={neg} and uc.Name={uc}");
		query.append(" RETURN pp,np,uc");	
		
		try(Transaction tx = graphDB.beginTx()){			
			ExecutionResult result = engine.execute(query.toString(),params);
			ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
			while(resultInIterator.hasNext()){
				Map<String,Object> unsatMap = resultInIterator.next();		
				
				PathImpl pPath = (PathImpl)unsatMap.get("pp");
				PathImpl nPath = (PathImpl)unsatMap.get("np");				
				
				Set<String> nodes =nodesToList(pPath,nPath);
				List<Relationship> pp = pathToList(pPath);
				List<Relationship> pn = pathToList(nPath);
				
				MUPP mups = new MUPP(unsatNode,pp,pn,nodes);
				mupps.add(mups);
			}
			tx.success();
		}		
		return mupps;
	}
	
	/**
	 * 在添加标签的环境下，计算一个不可满足概念的MUPS
	 * @return
	 */
	public List<MUPP> compMUPSwithLabel(UnsatTriple triple){
		List<MUPP> mupps = new ArrayList<>();
		System.out.println("Compute MUPS : "+ triple.getUnsat());
		
		String unsatNode = triple.getUnsat();		
		//labelUnsat(unsatNode); //添加标签
		//labelTriple(triple);
		
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("pos", triple.getFirst());
		params.put("neg", triple.getSecond());	
		params.put("uc", unsatNode);		
		
		//String whereFormatter = "WHERE n.Name='%s' and _n.Name='%s' and uc.Name='%s' ";
		String whereFormatter = "WHERE n.Name={pos} and _n.Name={neg} and uc.Name={uc}";
		StringBuilder query = new StringBuilder();			
		query.append("MATCH pp=((uc)-[:INCLUDEDBY *]->n) ");
		query.append("MATCH np=((uc)-[:INCLUDEDBY *]->_n) ");	
		//query.append(String.format(whereFormatter,triple.getFirst(), triple.getSecond(), triple.getUnsat() ));		
		query.append(whereFormatter);
		query.append(" RETURN pp,np,uc");			
		
		try(Transaction tx = graphDB.beginTx()){			
			//ExecutionResult result = engine.execute(query.toString());
			ExecutionResult result = engine.execute(query.toString(),params);
			ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
			while(resultInIterator.hasNext()){
				Map<String,Object> unsatMap = resultInIterator.next();		
				
				PathImpl pPath = (PathImpl)unsatMap.get("pp");
				PathImpl nPath = (PathImpl)unsatMap.get("np");				
				
				Set<String> nodes =nodesToList(pPath,nPath);
				List<Relationship> pp = pathToList(pPath);
				List<Relationship> pn = pathToList(nPath);
				
				MUPP mups = new MUPP(unsatNode,pp,pn,nodes);
				mupps.add(mups);
			}
			tx.success();
		}			
		//删除标签
		//removeLabel(unsatNode);
		return mupps;
	}
	
	/**
	 * 删除Label, 找出该label下的所有的node，去除node上的label.
	 * @param uc
	 */
	public void removeLabel(String uc){
		if(uc!=null){		
			try(Transaction tx = graphDB.beginTx()){
				Label uLabel = DynamicLabel.label(getMUPSLabel(uc));
				GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDB);
				for(Node labeledNode : ggo.getAllNodesWithLabel(uLabel)){
					labeledNode.removeLabel(uLabel);
				}
				tx.success();
			}
		}
		else{
			System.out.println("Error!  The uc is null.");
		}
	}
	/**
	 * 计算所有的mupps
	 * @param triples
	 * @return
	 */
	public List<MUPP> compMUPPs(List<UnsatTriple> triples){
		List<MUPP> mupps = new ArrayList<>();
		System.out.println("Compute MUPPS In Graph Database " );		
		
		//String whereFormatter = "WHERE n.Name='%s' and _n.Name='%s' and uc.Name='%s' ";
		String whereFormatter = "WHERE n.Name={pos} and _n.Name={neg} and uc.Name={uc} ";
		StringBuilder query = new StringBuilder();			
		
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每MAXMUPS个提交一次	
		while(iCount < triples.size()){			
			iEnd = iCount + MAXMUPS;
			if(iEnd >= triples.size()){
				iEnd = triples.size();
			}
			try(Transaction tx = graphDB.beginTx()){			
				while(iCount < iEnd){	
					UnsatTriple triple = triples.get(iCount);		
					Tools.clear(query);				
					String uc = triple.getUnsat();
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("pos", triple.getFirst());
					params.put("neg", triple.getSecond());	
					params.put("uc", uc);
					
					query.append("MATCH pp=((uc:Concept)-[:INCLUDEDBY *]->(n:Concept) ) ,");
					query.append(" np=((uc:Concept)-[:INCLUDEDBY *]->(_n:Concept)) ");	
					//query.append("MATCH pp=((uc)-[:INCLUDEDBY *]->(n) ),  ");
					//query.append(" np=((uc)-[:INCLUDEDBY *]->(_n)) ");
					query.append(whereFormatter);		
					query.append(" RETURN pp,np");					
					//System.out.println(query.toString());
					ExecutionResult result = engine.execute(query.toString(),params);
					ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
					while(resultInIterator.hasNext()){
						Map<String,Object> unsatMap = resultInIterator.next();					
						PathImpl pPath = (PathImpl)unsatMap.get("pp");
						PathImpl nPath = (PathImpl)unsatMap.get("np");					
						
						Set<String> nodes =nodesToList(pPath,nPath);
						List<Relationship> pp = pathToList(pPath);
						List<Relationship> pn = pathToList(nPath);
						
						MUPP mups = new MUPP(uc,pp,pn,nodes);
						mupps.add(mups);
					}	
					++iCount;						
				}
				tx.success();
				System.out.print(iCount+"   ");
				if(iCount >= triples.size()-1)  
					break;
			}
		}
		return mupps;
	}	
	//给不相交类添加label
	public void addDisjointLabel(){
		System.out.println("Adding disjoint label...");
		List<DisjointPair> pairs = getDisjointPair(graphDB);
		if(pairs.size()>120){  //如果不相交概念太大，效率会很低
			System.out.println("The disjointness is too big");
		}
		else{
			StringBuilder query = new StringBuilder();
			try(Transaction tx = graphDB.beginTx()){
				for(DisjointPair pair : pairs){				
					Tools.clear(query);					
					query.append(String.format("CREATE INDEX ON :%s(%s) ",getMUPSLabel(pair.getFirst()), NAMEPROPERTY));
					engine.execute(query.toString());
					Tools.clear(query);					
					query.append(String.format("CREATE INDEX ON :%s(%s) ",getMUPSLabel(pair.getSecond()), NAMEPROPERTY));
					engine.execute(query.toString());
				}
				tx.success();
			}
			
			try(Transaction tx = graphDB.beginTx()){							
				for(DisjointPair pair : pairs){
					String pos = pair.getFirst();
					String neg = pair.getSecond();
					Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);	
					
					Node posNode = nodeIndex.get(NAMEPROPERTY, pos).getSingle();
					Node negNode = nodeIndex.get(NAMEPROPERTY, neg).getSingle();
					Label posLabel = DynamicLabel.label(getMUPSLabel(pos));
					Label negLabel = DynamicLabel.label(getMUPSLabel(neg));	
					
					//Schema schema = graphDB.schema();				
					//IndexDefinition posIndex = schema.indexFor( posLabel ).on( NAMEPROPERTY).create();
					//schema.awaitIndexOnline( posIndex, 10, TimeUnit.SECONDS );  //给时间等待索引的建立					
					//IndexDefinition negIndex = schema.indexFor( negLabel ).on( NAMEPROPERTY).create();
					//schema.awaitIndexOnline( negIndex, 10, TimeUnit.SECONDS );  //给时间等待索引的建立					
					
					labelDescendants(posNode, posLabel);
					labelDescendants(negNode,negLabel);							
					tx.success();
				}					
			}
			hasDisjointLabel = true;
		}
	}
	
	/**
	 * 从三元组中构建label，是uc的父节点与disjoint节点的并的交
	 * @param triple
	 */
	@SuppressWarnings("unchecked")
	public void labelTriple(UnsatTriple triple){
		String uc = triple.getUnsat();
		String pos = triple.getFirst();
		String neg = triple.getSecond();
		String labelName = getMUPSLabel(uc+"_"+pos);
		try(Transaction tx = graphDB.beginTx()){
			Label label = DynamicLabel.label(labelName);
			Label conceptLabel = DynamicLabel.label(CONCEPTLABEL);			
			//建立基于新标签的索引
			Schema schema = graphDB.schema();
			IndexDefinition indexDefinition = schema.indexFor( label ).on( NAMEPROPERTY).create();
			schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );  //给时间等待索引的建立
			
			Node ucNode = graphDB. findNodesByLabelAndProperty( conceptLabel, NAMEPROPERTY, uc).iterator().next();
			Node posNode = graphDB. findNodesByLabelAndProperty( conceptLabel, NAMEPROPERTY, pos).iterator().next();
			Node negNode = graphDB. findNodesByLabelAndProperty( conceptLabel, NAMEPROPERTY, neg).iterator().next();
			
			if(ucNode !=null && posNode!=null && negNode != null){
				Set<String> ancestorOfUc= getAncestorNode(ucNode);				
				
				Set<String> descentantsOfPos = getDescendantNodesWithRecursion(posNode);
				Set<String> descentantsOfNeg = getDescendantNodesWithRecursion(negNode);
				
				Collection<String> descentantUnion = CollectionUtils.union(descentantsOfPos, descentantsOfNeg);
				Collection<String> targetNodeNames = CollectionUtils.intersection(ancestorOfUc, descentantUnion);
				for(String targetName : targetNodeNames){
					Node targetNode = graphDB. findNodesByLabelAndProperty( DynamicLabel.label(CONCEPTLABEL), NAMEPROPERTY, targetName).iterator().next();
					if(targetNode !=null && !(targetNode.hasLabel(label))){
						targetNode.addLabel(label);
					}
				}
			}
			tx.success();
		}
	}	
	//给一个节点的所有父类增加label
	public void labelUnsat(String uc){
		if(uc!=null){		
			try(Transaction tx = graphDB.beginTx()){				
				Schema schema = graphDB.schema();
				Label uLabel = DynamicLabel.label(getMUPSLabel(uc));
				IndexDefinition indexDefinition = schema.indexFor(uLabel).on(NAMEPROPERTY ).create();
				schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS );  //等待索引的创建					
				Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);	
				Node uNode = nodeIndex.get(NAMEPROPERTY, uc).getSingle();
				labelAncestors(uNode,uLabel);					
				/*String indexQuery = String.format("CREATE INDEX ON :%s(%s)", getMUPSLabel(uc),NAMEPROPERTY);
				engine.execute(indexQuery);*/
				tx.success();
			}
		}
		else{
			System.out.println("Error, The uc is null.");
		}
	}
	
	/**
	 * 给不可满足概念添加标签和索引
	 */
	public void labelUnsats(){
		if(unsatNodes.size()!=0){		
			try(Transaction tx = graphDB.beginTx()){
				for(String uc : unsatNodes){					
					Label uLabel = DynamicLabel.label(getMUPSLabel(uc));
					Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);	
					Node uNode = nodeIndex.get(NAMEPROPERTY, uc).getSingle();
					labelAncestors(uNode,uLabel);					
				}
				tx.success();
			}		
			//建立索引
			try(Transaction tx = graphDB.beginTx()){
				for(String uc : unsatNodes){
					String indexQuery = String.format("CREATE INDEX ON :%s(%s)", getMUPSLabel(uc),NAMEPROPERTY);
					engine.execute(indexQuery);
				}
				tx.success();
			}
		}
		else{
			System.out.println("Please compute the set of unsatisfiable nodes in graph.");
		}
	}	
	/**
	 * 应用标签和不一致三元组计算MUPP
	 * @param triples
	 * @return
	 */
	public List<MUPP> compMUPPsWithLabel(List<UnsatTriple> triples){
		List<MUPP> mupps = new ArrayList<>();
		System.out.println("Compute MUPPS In Graph Database " );	
		
		String ppFormatter="MATCH pp=((uc:%s)-[:INCLUDEDBY*]->(n:%s)), ";
		String npFormatter="np=((uc:%s)-[:INCLUDEDBY*]->(_n:%s)) ";
		//****使用参数
		String whereFormatter = "WHERE n.Name={pos} and _n.Name={neg} and uc.Name={uc} "; 		
		StringBuilder query = new StringBuilder();				
		
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每MAXMUPS个提交一次		
		//System.out.println("Triples size is " + triples.size());		
		while(iCount < triples.size()){
			try(Transaction tx = graphDB.beginTx()){
				iEnd = iCount + MAXMUPS;
				if(iEnd >= triples.size()){
					iEnd = triples.size();
				}
				while(iCount < iEnd){
					UnsatTriple triple = triples.get(iCount);					
					
					Map<String, Object> params = new HashMap<String, Object>();
					String pos = triple.getFirst();
					String neg = triple.getSecond();
					String unsatNode = triple.getUnsat();
					params.put("pos", pos);
					params.put("neg", neg);	
					params.put("uc", unsatNode);
					
					String posLabel = null, negLabel = null, conceptLabel = "Concept";
					if(hasDisjointLabel){
						posLabel = getMUPSLabel(pos);
						negLabel = getMUPSLabel(neg);
					}
					else{
						posLabel = "Concept";		
						negLabel = "Concept";
					}
					
					Tools.clear(query);
					query.append(String.format(ppFormatter, conceptLabel, posLabel));
					query.append(String.format(npFormatter, conceptLabel, negLabel));
					query.append(whereFormatter);					
					//query.append(" RETURN pp,np,uc");	
					query.append("RETURN pp,np ");
					
					ExecutionResult result = engine.execute(query.toString(),params);					
					ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
					while(resultInIterator.hasNext()){
						Map<String,Object> unsatMap = resultInIterator.next();					
						//Node unsat = (Node)unsatMap.get("uc");
						PathImpl pPath = (PathImpl)unsatMap.get("pp");
						PathImpl nPath = (PathImpl)unsatMap.get("np");				
						
						//String unsatNode = unsat.getProperty(NAMEPROPERTY).toString();
						Set<String> nodes =nodesToList(pPath,nPath);
						List<Relationship> pp = pathToList(pPath);
						List<Relationship> pn = pathToList(nPath);
						
						MUPP mups = new MUPP(unsatNode,pp,pn,nodes);
						mupps.add(mups);
					}							
					++iCount;					
				}	
				tx.success();
				System.out.println(iCount);	
				if(iCount >= triples.size()-1)  //可能会出错
					break;
							
			}
		}	
		return mupps;
	}
	
	/**
	 * 取出某个不可满足概念的MUPP中的重复边来计算图数据库中的MIPP<br>
	 * 在TBox上展开，并不涉及ABox
	 * @param graphDB 目标图数据库
	 */
	public  List<MIPP> compMIPPsbyMUPPs(List<MUPP> mupps){
		List<MIPP> mipps = new ArrayList<>();
		System.out.println("Compute MIPPS in TBox of Graph database.");
		//List<MUPS> mupps = compMUPPs();
		try(Transaction tx = graphDB.beginTx()){
			for(MUPP mupp : mupps){
				MIPP mi = uToI(mupp);
				if(!(mipps.contains(mi))){
					mipps.add(mi);
				}				
			}
			tx.success();
		}
		return mipps;
	}
	
	/**
	 * 通过cypher直接计算MIPS，
	 * 函数先计算所有的不相交的概念对，在生成cypher语句
	 * @param graphDB
	 */
	public List<MIPP> compMIPPs(){
		List<MIPP> mipps = new ArrayList<>();
		List<DisjointPair> pairs = getDisjointPair(graphDB);		
		StringBuilder query = new StringBuilder();
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : pairs){
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("pos", pair.getFirst());
				params.put("neg", pair.getSecond());	
				Tools.clear(query);
				query.append("MATCH pp = (uc:Concept)-[r1:INCLUDEDBY*]->(n:Concept), "); //r1,r2是关系的集合，查询要求r1和r2没有公共的关系
				query.append("np= (uc:Concept)-[r2:INCLUDEDBY*]->(_n:Concept) ");
				query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) and ");   //这条语句保证两条路径没有公共边
				query.append("n.Name={pos} and _n.Name={neg} "  );				
				query.append("RETURN pp,np");				
				//System.out.println(query.toString());
				ExecutionResult result =  engine.execute(query.toString(),params);
				ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
				while(resultInIterator.hasNext()){
					Map<String,Object> unsatMap = resultInIterator.next();					
					PathImpl pPath = (PathImpl)unsatMap.get("pp");
					PathImpl nPath = (PathImpl)unsatMap.get("np");
					Set<String> nodes =nodesToList(pPath,nPath);
					List<Relationship> pp = pathToList(pPath);
					List<Relationship> pn = pathToList(nPath);					
					MIPP mips = new MIPP(pp,pn,nodes);
					mipps.add(mips);
				}				
			}
			tx.success();
		}	
		//System.out.println("Number of mips is " + mipps.size());
		return mipps;
	}	
	
	
	/**
	 * 通过cypher直接计算MIPS。!obsolete.
	 * @param graphDB
	 */
	public List<MIPP> _compMIPPs(){
		List<MIPP> mipps = new ArrayList<>();
		List<DisjointPair> pairs = getDisjointPair(graphDB);		
		StringBuilder query = new StringBuilder();
		String posFormatter = "MATCH pp =(uc:%s)-[r1:INCLUDEDBY*]->(n:%s), ";
		String negFormatter = "np= (uc:%s)-[r2:INCLUDEDBY*]-> (_n:%s) ";
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : pairs){
				Map<String, Object> params = new HashMap<String, Object>();				
				String pos = pair.getFirst();
				String neg = pair.getSecond();
				//System.out.println(pos+"#"+neg);
				//String posLabel = null, negLabel = null;
				String conceptLabel = "Concept";
				/*if(hasDisjointLabel){
					posLabel = getMUPSLabel(pos);
					negLabel = getMUPSLabel(neg);
				}
				else{
					posLabel = "Concept";		
					 negLabel = "Concept";
				}	*/				
				params.put("pos", pos);
				params.put("neg", neg);	
				Tools.clear(query);
				
				query.append(String.format(posFormatter, conceptLabel, conceptLabel)); //r1,r2是关系的集合，查询要求r1和r2没有公共的关系
				query.append(String.format(negFormatter, conceptLabel, conceptLabel));
				query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) and ");
				//query.append("n.Name={pos} and _n.Name={neg} "  );	
				query.append(String.format("n.Name='%s' and _n.Name='%s' ",pos,neg));
				query.append("RETURN pp,np");	
					
				//System.out.println(query.toString());
				
				ExecutionResult result =  engine.execute(query.toString(),params);
				ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
				while(resultInIterator.hasNext()){
					Map<String,Object> unsatMap = resultInIterator.next();					
					PathImpl pPath = (PathImpl)unsatMap.get("pp");
					PathImpl nPath = (PathImpl)unsatMap.get("np");
					Set<String> nodes =nodesToList(pPath,nPath);
					List<Relationship> pp = pathToList(pPath);
					List<Relationship> pn = pathToList(nPath);					
					MIPP mips = new MIPP(pp,pn,nodes);
					mipps.add(mips);
				}				
			}
			tx.success();
		}	
		//System.out.println("Number of mips is " + mipps.size());
		return mipps;
	}	
	
	/**
	 * 通过cypher计算MIPS，使用label和不交三元组。
	 * 先获取不相交概念对，以及概念对共同的子节点，
	 * 将子节点与不交的概念对组合成三元组
	 * 如果图数据库比较简单，使用过多标签反而降低了速度
	 * @param graphDB
	 */
	public List<MIPP> compMIPPsWithLabel(List<UnsatTriple> triples){
		List<MIPP> mipps = new ArrayList<>();			
		StringBuilder query = new StringBuilder();
		
		//使用参数的字符串模式
		String ppFormatter="MATCH pp=((uc:%s )-[r1:INCLUDEDBY*]->(n:%s )), ";
		String npFormatter="np=((uc:%s )-[r2:INCLUDEDBY*]->(_n:%s )) ";		
		/*String ppFormatter="MATCH pp=((uc:%s {Name:'%s'})-[r1:INCLUDEDBY*]->(n:%s {Name:'%s'})), ";
		String npFormatter="np=((uc:%s {Name:'%s'})-[r2:INCLUDEDBY*]->(_n:%s {Name:'%s'})) ";*/
		
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每MAXMUPS个提交一次		
		while(iCount < triples.size()){
			try(Transaction tx = graphDB.beginTx()){
				System.out.print(iCount+"   ");
				iEnd = iCount + MAXMUPS;
				if(iEnd >= triples.size()){
					iEnd = triples.size();
					//System.out.println("iEnd is " +iEnd);
				}
				while(iCount < iEnd){					
					//*******使用参数，手册上认为参数的模式会提供查询的效率
					UnsatTriple triple = triples.get(iCount);					
					Map<String, Object> params = new HashMap<String, Object>();
					String pos = triple.getFirst();
					String neg = triple.getSecond();
					String unsatNode = triple.getUnsat();
					params.put("pos", pos);
					params.put("neg", neg);	
					params.put("uc", unsatNode);
					
					String posLabel = null, negLabel = null, conceptLabel = "Concept";
					if(hasDisjointLabel){
						posLabel = getMUPSLabel(pos);
						negLabel = getMUPSLabel(neg);
					}
					else{
						posLabel = "Concept";		
						 negLabel = "Concept";
					}						
					
					Tools.clear(query);
					query.append(String.format(ppFormatter, conceptLabel, posLabel));
					query.append(String.format(npFormatter, conceptLabel, negLabel));
					query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
					query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "  );		//r1,r2是关系的集合，查询要求r1和r2没有公共的关系
					query.append("RETURN pp,np");	
					
					ExecutionResult result = engine.execute(query.toString(),params);					
					ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
					while(resultInIterator.hasNext()){
						Map<String,Object> unsatMap = resultInIterator.next();					
						PathImpl pPath = (PathImpl)unsatMap.get("pp");
						PathImpl nPath = (PathImpl)unsatMap.get("np");
						Set<String> nodes =nodesToList(pPath,nPath);
						List<Relationship> pp = pathToList(pPath);
						List<Relationship> pn = pathToList(nPath);					
						MIPP mips = new MIPP(pp,pn,nodes);
						mipps.add(mips);
					}							
					++iCount;					
				}	
				tx.success();
				System.out.println(iCount);	
				if(iCount >= triples.size()-1)  //可能会出错
					break;							
			}
		}	
		return mipps;		
	}	
	
	/**
	 * 不使用特定标签的mips计算方法,
	 * 只使用了默认的Concept标签和概念不可满足概念三元组<br>
	 * 注意如果标签过多，性能其实是下降的
	 * @param triples
	 * @return
	 */
	public List<MIPP> compMIPPs(List<UnsatTriple> triples){
		List<MIPP> mipps = new ArrayList<>();			
		StringBuilder query = new StringBuilder();
		
		//使用参数的字符串模式
		/*String ppFormatter="MATCH pp=((uc:%s )-[r1:INCLUDEDBY*]->(n:%s )), ";
		String npFormatter="np=((uc:%s )-[r2:INCLUDEDBY*]->(_n:%s )) ";	*/	
		
		/*String ppFormatter="MATCH pp=((uc:%s )-[:INCLUDEDBY*]->(n:%s )), ";
		String npFormatter="np=((uc:%s )-[:INCLUDEDBY*]->(_n:%s )) ";	*/
		
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每MAXMUPS个提交一次		
		while(iCount < triples.size()){
			try(Transaction tx = graphDB.beginTx()){
				//System.out.print(iCount+"   ");
				iEnd = iCount + MAXMUPS;
				if(iEnd >= triples.size()){
					iEnd = triples.size();					
				}
				//System.out.print(iEnd+" "); //输出最后一个三元组的序号
				while(iCount < iEnd){					
					//*******使用参数，手册上认为参数的模式会提供查询的效率
					UnsatTriple triple = triples.get(iCount);					
					Map<String, Object> params = new HashMap<String, Object>();
					String pos = triple.getFirst();
					String neg = triple.getSecond();
					String unsatNode = triple.getUnsat();
					params.put("pos", pos);
					params.put("neg", neg);	
					params.put("uc", unsatNode);
					
											
					
					Tools.clear(query);
					/*String conceptLabel = "Concept";	
					query.append(String.format(ppFormatter, conceptLabel, conceptLabel));
					query.append(String.format(npFormatter, conceptLabel, conceptLabel));
					query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
					query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "  );		//r1,r2是关系的集合，查询要求r1和r2没有公共的关系
					query.append("RETURN pp,np");
					System.out.println(query.toString());
					ExecutionResult result = engine.execute(query.toString(),params);	*/
					
					
					/*query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n),");
					query.append("np=(uc-[r2:INCLUDEDBY*]->t) ");
					query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) ");
					query.append(" and uc.Name='testA' and n.Name='testC' and t.Name='negative_testC' ");
					query.append("RETURN pp,np");
					System.out.println(query.toString());
					ExecutionResult result = engine.execute(query.toString(),params);*/
					
					query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n), ");
					query.append("np=(uc-[r2:INCLUDEDBY*]->_n) ");
					query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
					query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "  );		//r1,r2是关系的集合，查询要求r1和r2没有公共的关系
					query.append("RETURN pp,np");
					System.out.println(query.toString());
					ExecutionResult result = engine.execute(query.toString(),params);
					
					/*query.append(String.format(ppFormatter, conceptLabel, conceptLabel));
					query.append("WHERE n.Name={pos} and uc.Name={uc} ");		
					query.append("RETURN pp");				
					System.out.println(query.toString());
					ExecutionResult result = engine.execute(query.toString(),params);*/
					
					/*query.append("MATCH (ee:Concept)-[:INCLUDEDBY]-(bb:Concept)");
					//query.append("WHERE ee.name = "Emil"");
					query.append("RETURN ee, bb");
					ExecutionResult result = engine.execute(query.toString());*/
					
									
					ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
					while(resultInIterator.hasNext()){
						Map<String,Object> unsatMap = resultInIterator.next();					
						PathImpl pPath = (PathImpl)unsatMap.get("pp");
						PathImpl nPath = (PathImpl)unsatMap.get("np");
						Set<String> nodes =nodesToList(pPath,nPath);
						List<Relationship> pp = pathToList(pPath);
						List<Relationship> pn = pathToList(nPath);					
						MIPP mips = new MIPP(pp,pn,nodes);
						mipps.add(mips);
					}							
					++iCount;					
				}	
				tx.success();
				//System.out.println(iCount);	
				if(iCount >= triples.size()-1)  //可能会出错
					break;							
			}
		}	
		System.out.println("Number of MIPP is "+mipps.size());
		return mipps;		
	}	
	
	/**
	 * 在图数据库中找出ABox中不一致节点(实例)
	 * 原查询有点问题，如果Individual是节点的直接子类，有的不一致会丢失，
	 * 如果TBox是一致的话，直接用getAll...就可以了
	 * 增加INDIVIDUAL标签就可以了
	 * @param graphDB 目标数据库
	 */
	public  List<String>  getUnsatNodeInABox(GraphDatabaseService graphDB){
		System.out.println("Getting unsatisfactable nodes in ABox");
		List<String> nodes = new ArrayList<>();
		List<DisjointPair> pairs = getDisjointPair(graphDB);		
		String whereFormatter = "WHERE  n.Name='%s' and _n.Name='%s' ";
		StringBuilder query = new StringBuilder();
		for(DisjointPair pair : pairs){
			Tools.clear(query);				
			query.append(String.format("MATCH p=(n<-[*]-(uc:%s)-[*]->_n)  ", INDIVIDUALLABEL));			
			query.append(String.format(whereFormatter,pair.getFirst(), pair.getSecond()));
			query.append("RETURN uc.Name as ucName");
			
			ExecutionResult  result = engine.execute(query.toString());
		    ResourceIterator<String> iterator = result.columnAs("ucName");
		    while(iterator.hasNext()){
		    	nodes.add(iterator.next());
		    }
		}
		return nodes;
	}
}