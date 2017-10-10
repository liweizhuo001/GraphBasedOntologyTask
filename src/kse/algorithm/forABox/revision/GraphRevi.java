package kse.algorithm.forABox.revision;

import static kse.algorithm.auxiliaryClass.GlobalFunct.getDisjointPair;
import static kse.algorithm.auxiliaryClass.GlobalFunct.hasFirstIndividual;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.algorithm.auxiliaryClass.IndividualConceptPair;
import kse.misc.Tools;
import kse.neo4j.ver1_8.Tools4Graph;

import org.neo4j.cypher.internal.PathImpl;
//import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * 基于图的ABox修正<br>
 * 应用场景：一个TBox，两个ABox，单独的一个TBox和ABox都是一致的
 * 当加入一个新的ABox后，本体出现 inconsistency，删除comefrom=first的边
 * 1. 先在图上找出所有的不一致路径
 * 2. 记录要删除的边，已经可能要添加的边（这条边可能存在，也可能在后面出现在要删除的地方）
 * 3. 修正<br>
 * From paper "A new operator for ABox revision in DL-Lite"
 * For paper "Approximating Model-based ABox Revision in DL-Lite: Theory and Practice"
 * [2014-8]
 * @author Xuefeng Fu 
 *@since JDK1.7
 */
public class GraphRevi {
	String owlPath;
	String gPath;
	GraphDatabaseService graphDB;
	Set<IndividualConceptPair> forDelete;
	Set<IndividualConceptPair> forAdd; //要添加的边
	ExecutionEngine engine;  //Cypher引擎
	
	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}

	public String getOwlPath() {
		return owlPath;
	}

	public String getgPath() {
		return gPath;
	}

	public ExecutionEngine getEngine() {
		return engine;
	}

	
	public GraphRevi(GraphDatabaseService graphDB){
		this.graphDB = graphDB;
		this.engine = new ExecutionEngine(graphDB);
	}
	
	public GraphRevi(String gPath){		
		this.gPath = gPath;
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩函数，程序结束自动调用来释放图数据库		
		this. engine = new ExecutionEngine( graphDB );
	}
	
	public GraphRevi(String owlPath,String gPath){
		this.owlPath = owlPath;
		this.gPath = gPath;
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩函数，程序结束自动调用来释放图数据库		
		this. engine = new ExecutionEngine( graphDB );
	}
	
	public static GraphRevi getGraphRevi(GraphDatabaseService graphDB){
		return new GraphRevi(graphDB);
	}	
	public static GraphRevi getGraphRevi(String owlPath,String gPath){
		return new GraphRevi(owlPath, gPath);
	}	
	public static GraphRevi getGraphRevi(String gPath){
		return new GraphRevi(gPath);
	}
	
	/**
	 * 计算图数据库中的Minimal Inconsistent subset
	 */
	public List<MIS> getMIS(){
		System.out.println("Computing MIS ... ");		
		
		List<MIS> miss = new ArrayList<MIS>();	
		List<DisjointPair> pairs = getDisjointPair(graphDB);	
		StringBuilder query = new StringBuilder();
		int i=0;
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : pairs){
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("pos", pair.getFirst());
				params.put("neg", pair.getSecond());	
				Tools.clear(query);
				
				/*query.append("MATCH pp = (uc:Individual)-[:MEMBEROF*1]->(x:Concept)-[r1:INCLUDEDBY*]->(n:Concept), "); 
				query.append("np= (uc:Individual)-[:MEMBEROF*1]->(_x:Concept)-[r2:INCLUDEDBY*]->(_n:Concept) ");
				query.append("WHERE n.Name={pos} and _n.Name={neg} "  );				
				query.append("RETURN pp,np");	*/			
				
				//查询语句，从实例到不相交概念只要有一个就可以了,所以在这里用上“最短路径函数”（不一致只在ABox端导致）
				//上面的分析有问题，在删除的时候没有影响，但添加的时候会丢失一些边
				/*query.append("MATCH pp =shortestPath( (uc:Individual)-[*]->(n:Concept)), "); 
				query.append("np= shortestPath((uc:Individual)-[*]->(_n:Concept)) ");
				query.append("WHERE n.Name={pos} and _n.Name={neg} "  );				
				query.append("RETURN pp,np");*/
				
				query.append("MATCH pp = (uc:Individual)-[*]->(n:Concept), "); 
				query.append("np= (uc:Individual)-[*]->(_n:Concept) ");
				query.append("WHERE n.Name={pos} and _n.Name={neg} "  );				
				query.append("RETURN pp,np");
				
				System.out.println(query.toString());
				ExecutionResult result =  engine.execute(query.toString(),params);
				System.out.println(result.dumpToString());
				
				if(++i>3) break;
				
				ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
				while(resultInIterator.hasNext()){
					Map<String,Object> unsatMap = resultInIterator.next();		
					PathImpl path = null;					
					PathImpl pPath = (PathImpl)unsatMap.get("pp");
					PathImpl nPath = (PathImpl)unsatMap.get("np");
					if(hasFirstIndividual(pPath)){
						path = pPath;
					}
					else if(hasFirstIndividual(nPath)){
						path = nPath;
					}				
					if(path!=null){
						MIS mis = new MIS(path);
						miss.add(mis);	
					}
				}
				//System.out.println(miss.size());
			}
			tx.success();
		}			
		return miss;		
	}

	/**
	 * 处理MIS，从MIS中提取要删除的边和要添加的边
	 * 如果是从实例节点a(设a是概念B的实例)到概念节点A导致不一致，且符合要处理的标准（实例断言来自First）
	 * 则要删除实例断言B(a)，添加a到Parent(B)/child(A)交Parent(B)【这要去除的其实就是改路径上的其他节点】,再去掉A
	 */
	public void  processingMIS(List<MIS> MISs ){
		System.out.println("In processing MIS...");
		forDelete = new HashSet<>();
		forAdd = new HashSet<>();
		
		System.out.println("MIS:"+MISs.size());
		//int index = 0;
		try(Transaction tx = graphDB.beginTx()){
			for(MIS mis : MISs){	
				//System.out.println(MISs.size()+"-->"+(++index));
				Set<Node> tempNodes = new HashSet<>(); //记录所有要计算闭包（找父类）的节点
				Set<String> candidateNodes = new HashSet<>();				
				Set<String> tempNodeNames = new HashSet<>();
				
				forDelete.add(mis.getBeDeletedPair());
				
				String individualNodeName = mis.getIndividualNodeName();
				Node endNode = mis.getEndNode();
				Set<Node> nodes = mis.getMidNodes();
				nodes.add(endNode);
				tempNodes.addAll(nodes);
				tempNodeNames.add(mis.getEndNodeName());
				tempNodeNames.addAll(mis.getMidNodeNames());				
				
				//计算路径上的概念节点的所有父节点名（只有一个实例节点，它的所有的父节点都是概念节点）
				for(Node node : tempNodes){
					candidateNodes.addAll(GlobalFunct.getAncestorNode(node));
				}
				//去掉路径上的所有概念节点
				candidateNodes.removeAll(tempNodeNames);
				//添加要增加的实例断言对
				
				for(String candidateNode : candidateNodes){
					
					if(candidateNode != null){
						IndividualConceptPair pair = new IndividualConceptPair(individualNodeName, candidateNode);
						//System.out.println(pair);
						forAdd.add(pair);
					}
				}
				//取出可能要删除的，以免重复处理
				//	
				
			}		
			tx.success();			
		}
		//去除要删除的，这时要用hashset，查询与合并的速度为list快很多
		for(IndividualConceptPair pair : forDelete){
			forAdd.remove(pair);
		}		
		System.out.println("delete:"+forDelete.size()+"#add:"+forAdd.size());
	}
	
	/**
	 * 完成修正操作，即删边和添边。先删后添
	 */
	public void revising(){
		StringBuilder query = new StringBuilder();
		//删边
		if(forDelete.size()>0){
			System.out.println("Deleting inconsistent edge...");
			try(Transaction tx = graphDB.beginTx()){
				for(IndividualConceptPair pair : forDelete){
					Map<String, Object> params = new HashMap<String, Object>();
					params.put("individual", pair.getIndividual());
					params.put("concept", pair.getConcept());	
					
					Tools.clear(query);
					query.append("MATCH  (i:Individual)-[r:MEMBEROF]->(c:Concept) "); 
					query.append("WHERE i.Name={individual} and c.Name={concept} "  );				
					query.append("DELETE r");
					//ExecutionResult result =  engine.execute(query.toString(),params);
					engine.execute(query.toString(),params);
				}
				tx.success();
			}			
		}
		System.out.println("The number of deleted edge:"+forDelete.size());
		List<IndividualConceptPair> forAddList = new ArrayList<>();
		for(IndividualConceptPair i : forAdd){
			forAddList.add(i);
		}
		//添边
		if(forAddList.size() > 0){
			System.out.println("Adding edge to graph...");
			//为了避免Transaction过大，每2000个提交一次
			int ONCE_REL_NUMBER = 2000;
			int iCount = 0;
			int iEnd = 0;
			while(iCount<forAddList.size()){
				try(Transaction tx = graphDB.beginTx()){
					iEnd = iCount + ONCE_REL_NUMBER;
					if(iEnd > forAddList.size()){
						iEnd = forAddList.size();
					}
					while(iCount < iEnd){
						IndividualConceptPair pair = forAddList.get(iCount);
						Map<String, Object> params = new HashMap<String, Object>();
						params.put("individual", pair.getIndividual());
						params.put("concept", pair.getConcept());	
							
						Tools.clear(query);
						query.append("MATCH (i:Individual), (c:Concept) "); 
						query.append("WHERE i.Name={individual} and c.Name={concept} "  );				
						query.append("CREATE UNIQUE (i)-[r:MEMBEROF{RelNodeType:'Individual', ComeFrom:'NewAdd'}]->(c) ");
						query.append("RETURN r ");
							
						/*String matchFormatter = "MATCH (sub:%s{Name:'%s'}),(sup:%s{Name:'%s'}) " ;			
						String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";			
						Tools.clear(query);*/		
							
						engine.execute(query.toString(),params);
						++iCount;	
					}
					tx.success();
					//System.out.println(iCount);
					if(iCount==forAdd.size()-1)  //可能会出错
						break;
				}
			}		
			System.out.println("The number of new added edge:"+forAdd.size());
		}
	}
		
	public void doRevising(){
		List<MIS> MISs =  getMIS();
		processingMIS(MISs);
		revising();				
	}
	
	public void shutdown(){
		graphDB.shutdown();
	}
}





