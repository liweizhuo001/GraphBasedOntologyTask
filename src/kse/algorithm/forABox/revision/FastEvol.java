package kse.algorithm.forABox.revision;

import static kse.algorithm.auxiliaryClass.GlobalFunct.getDisjointPair;

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
import static kse.misc.GlobalParams.*;

import org.neo4j.cypher.internal.PathImpl;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


/**
 * FastEvol算法在图上的实现，先对整个图求闭包，再找MIS，直接删除导致不一致的实例断言
 * @author Xuefeng fu
 *
 */
public class FastEvol {
	String owlPath;
	String gPath;
	GraphDatabaseService graphDB;	
	//Set<IndividualConceptPair> forAdd; //要添加的边
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

	
	public FastEvol(GraphDatabaseService graphDB){
		this.graphDB = graphDB;
		this.engine = new ExecutionEngine(graphDB);
	}
	
	public FastEvol(String gPath){		
		this.gPath = gPath;
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩函数，程序结束自动调用来释放图数据库		
		this. engine = new ExecutionEngine( graphDB );
	}
	
	public FastEvol(String owlPath,String gPath){
		this.owlPath = owlPath;
		this.gPath = gPath;
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩函数，程序结束自动调用来释放图数据库		
		this. engine = new ExecutionEngine( graphDB );
	}
	
	public static FastEvol getFastEvol(GraphDatabaseService graphDB){
		return new FastEvol(graphDB);
	}	
	public static FastEvol getFastEvol(String owlPath,String gPath){
		return new FastEvol(owlPath, gPath);
	}	
	public static FastEvol getFastEvol(String gPath){
		return new FastEvol(gPath);
	}
	/**
	 * 计算ABox关于TBox的闭包
	 * @return 实例断言到概念集合的映射
	 */
	public Map<String, Set<String>> computeClosure(){
		StringBuilder query = new StringBuilder();
		query.append("MATCH p = (i:Individual)-[:MEMBEROF]->(c:Concept) "); 					
		query.append("RETURN i, c");		
		System.out.println(query.toString());
		Map<String, Set<String>> newAssertions = new HashMap<String, Set<String>>(); //要增加的
		Map<String, Set<String>> existAssertions = new HashMap<String, Set<String>>(); //已存在的
		
		try(Transaction tx = graphDB.beginTx()){
			ExecutionResult result =  engine.execute(query.toString());
			ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
			while(resultInIterator.hasNext()){
				Map<String,Object> unsatMap = resultInIterator.next();		
				Node nodeI = (Node)unsatMap.get("i");;					
				Node nodeC = (Node)unsatMap.get("c");
				String iName = nodeI.getProperty(NAMEPROPERTY).toString();
				String cName = nodeC.getProperty(NAMEPROPERTY).toString();
				
				Set<String> newNodes = newAssertions.get(iName);
				Set<String> existNodes = existAssertions.get(iName);
				
				if(newNodes == null){
					newNodes = new HashSet<String>();
					newAssertions.put(iName, newNodes);
				}
				
				if(existNodes == null){
					existNodes = new HashSet<String>();
					existAssertions.put(iName, existNodes);
				}
				newNodes.addAll(GlobalFunct.getAncestorNode(nodeC));
				existNodes.add(cName);
			}
			tx.success();
		}
		for(String iName : newAssertions.keySet()){
			Set<String> newNodes = newAssertions.get(iName);
			Set<String> existNodes = existAssertions.get(iName);
			if(newNodes.size()>0){
				newNodes.removeAll(existNodes);
			}
		}
		return newAssertions;
	}
	/**
	 * 将闭包写到图中
	 * @param closure
	 */
	public void insertClosure(Map<String, Set<String>> closure){
		List<IndividualConceptPair> forAddList = new ArrayList<>();
		for(String iName : closure.keySet()){
			for(String cName : closure.get(iName)){
				forAddList.add(new IndividualConceptPair(iName, cName));
			}
		}
		StringBuilder query = new StringBuilder();
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
						//query.append("RETURN r ");
							
						engine.execute(query.toString(),params);
						++iCount;	
					}
					tx.success();
					//System.out.println(iCount);
					if(iCount==forAddList.size()-1)  //可能会出错
						break;
				}
			}		
			System.out.println("The number of new added edge:"+forAddList.size());
		}
	}
	
	/**
	 * 计算图数据库中的Minimal Inconsistent subset
	 */
	public List<MIS> getMIS(){
		System.out.println("Computing MIS ... ");		
		
		List<MIS> miss = new ArrayList<MIS>();	
		List<DisjointPair> pairs = getDisjointPair(graphDB);	
		StringBuilder query = new StringBuilder();
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : pairs){
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("pos", pair.getFirst());
				params.put("neg", pair.getSecond());	
				Tools.clear(query);
				
				query.append("MATCH pp = (uc:Individual)-[*]->(n:Concept), "); 
				query.append("np= (uc:Individual)-[*]->(_n:Concept) ");
				query.append("WHERE n.Name={pos} and _n.Name={neg} "  );				
				query.append("RETURN pp,np");
				
				//System.out.println(query.toString());
				ExecutionResult result =  engine.execute(query.toString(),params);
				//System.out.println(result.dumpToString());
				
				ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
				while(resultInIterator.hasNext()){
					Map<String,Object> unsatMap = resultInIterator.next();		
					PathImpl path = null;					
					PathImpl pPath = (PathImpl)unsatMap.get("pp");
					PathImpl nPath = (PathImpl)unsatMap.get("np");
					path = GlobalFunct.getIndividualForDel(pPath, nPath);
								
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
	public Set<IndividualConceptPair>  processingMIS(List<MIS> MISs ){
		System.out.println("In processing MIS...");
		Set<IndividualConceptPair> forDelete = new HashSet<>();
		
		System.out.println("MIS:"+MISs.size());
		try(Transaction tx = graphDB.beginTx()){
			for(MIS mis : MISs){							
				forDelete.add(mis.getBeDeletedPair());				
			}		
			tx.success();			
		}
		System.out.println("delete:"+forDelete.size());
		return forDelete;
	}
	
	/**
	 * 完成修正操作，即删边和添边。先删后添
	 */
	public void deleting(Set<IndividualConceptPair> forDelete){
		System.out.println("deleting...");
		StringBuilder query = new StringBuilder();
		//删边
		if(forDelete.size()>0){
			System.out.println("Deleting inconsistent edge...");
			try(Transaction tx = graphDB.beginTx()){
				for(IndividualConceptPair pair : forDelete){
					if(pair == null){
						continue;
					}
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
	}
		
	public void doRevising(){
		Map<String, Set<String>> closure = computeClosure(); //计算闭包
		insertClosure(closure); //插入闭包
		List<MIS> MISs =  getMIS();  //计算MIS
		Set<IndividualConceptPair> deletingPair = processingMIS(MISs); //从MIS中获取待删除的实例断言
		deleting(deletingPair); //删除
		
	}
	
	public void shutdown(){
		graphDB.shutdown();
	}

}
