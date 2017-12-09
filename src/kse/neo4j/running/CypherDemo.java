package kse.neo4j.running;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.misc.Timekeeping;

import static kse.algorithm.auxiliaryClass.GlobalFunct.pathToList;
import static kse.misc.GlobalParams.*;

import org.neo4j.cypher.internal.PathImpl;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;
import org.neo4j.graphdb.index.Index;
/**
 * The query based on cypher 
 * @author Xuefeng fu
 *
 */
@SuppressWarnings("unused")
public class CypherDemo {
	//public static String gDB = "neo4j-db/DIY";
	//public static String gDB = "neo4j-ABox/UOBM_benchmark/";
	//public static String gDB = "neo4j-TBox-Scoring/test";
	//public static String gDB = "neo4j-TBox-Scoring/test";
	public static String gDB = "neo4j-test/Integrated-cmt-edas-test";		
	public static GraphDatabaseService  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDB );
	
	/**
	 * 测试带参数的Cypher查询
	 * @param query 查询字符串
	 * @param params 参数
	 */
	public static void paramCypher(StringBuilder query,Map<String,Object> params){
		String _query = query.toString();
		System.out.println(_query);
		ExecutionEngine engine = new ExecutionEngine( graphDb);
		try(Transaction tx = graphDb.beginTx()){			
			ExecutionResult result =  engine.execute(_query, params);
			System.out.println(result.dumpToString());
			tx.success();
		}
	}
	
	/**
	 * 简单的查询测试
	 * @param query 查询字符串
	 */
	public static ExecutionResult simpleCypher(StringBuilder query){		
		String _query = query.toString();
		System.out.println(_query);
		ExecutionEngine engine = new ExecutionEngine( graphDb );			
		ExecutionResult result =  engine.execute(_query);
		return result;
		//System.out.println(result.dumpToString());
	}
	
	/**
	 * 清空StringBuilder
	 * @param query 要清空的查询
	 */
	public static void clear(StringBuilder query){
		query.delete(0, query.length());
	}
	
	public static void main(String[] args){		
		try(Transaction tx = graphDb.beginTx()){
			/*GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDb);
			Index<Node> nodeIndex = graphDb.index().forNodes(NODEINDEX);		
			Index<Relationship> nodeRel = graphDb.index().forRelationships(RELATIONSHIPINDEX);
			List<String> negativeConcepts = GlobalFunct.getNegativeNodes(graphDb);	*/				
			/*StringBuilder query = new  StringBuilder();
			query.append("MATCH p=( n<-[*]-uc-[*]->_n) ");
			query.append("WHERE n.Name='Fodder' and _n.Name='negative_Fodder' and uc.Name='MulberryLeaf'  ");
			query.append("RETURN p");
			
			simpleCypher(query);*/
			
			
			/*//取出全部的节点，Ver1.8的方法
			query.append("start a=node(*) return a");		
			simpleCypher(query);
			clear(query);*/				
			
			
			//***************V2.1 Cypher Test*********************
			
			//PART-1 简单查询，基于Cypher				
			
			//1. Get all node in the graph 
			/*query.append("MATCH (n)  RETURN n;");
			simpleCypher(query);
			clear(query);*/
			
			//2. 取出所有的直接关联节点
			/*query.append("MATCH (n)-->(m)  RETURN n, m;");
			simpleCypher(query);
			clear(query);*/
			
			//3. 取出具有关系(出度-outgoing)的节点
			/*query.append("MATCH (n)-->() RETURN n;");
			simpleCypher(query);
			clear(query);*/
			
			//4. 返回节点指定的属性值
			/*query.append("MATCH (node)-->() RETURN node.name;");
			simpleCypher(query);
			clear(query);*/
			
			//5. 访问关系 relationship，由关系连接节点时使用 "->",特别注意是一个"-"			
			/*query.append("MATCH (node1)-[rel]->() RETURN rel , type(rel);");
			simpleCypher(query);
			clear(query);*/
			
			//指定关系的类型
			/*query.append("MATCH(node1)-[:INCLUDEDBY]->(node2) ");
			query.append("RETURN node1,node2");
			simpleCypher(query);
			clear(query);*/
			
			//6. 通过节点的label来匹配，如果节点设置了label
			//MATCH (node:LabelName)-[:Relationship]->(_node)	RETURN node,_node;
			
			//7. 通过节点的属性获取节点
			/*query.append("MATCH (n) ");
			query.append("WHERE n.name='D' ");
			query.append("RETURN n");
			simpleCypher(query);
			clear(query);
			
			query.append("MATCH (n { name: 'B' }) RETURN n");
			simpleCypher(query);
			clear(query);
			
			query.append("MATCH (n{comefrom:'first'}) RETURN n");
			simpleCypher(query);
			clear(query);*/
			
			//*************** Path *************
			/*query.append("MATCH p= (a)-[r{type:'role'}]->(b)  RETURN p");
			simpleCypher(query);
			clear(query);*/
			
			//****************** Advanced Cypher *************************			
		}

		
		/*Timekeeping.begin();
		try(Transaction tx = graphDb.beginTx()){						
			StringBuilder query = new  StringBuilder();
			query.append("MATCH p=(s:Concept)-[*]->(a:Concept) ");
			//query.append("WHERE a.Name='negative_Fodder'   ");
			query.append("RETURN p");
			
			System.out.println(simpleCypher(query).dumpToString());
		}
		Timekeeping.end();
		Timekeeping.showInfo("Computing sub nodes ");*/
		
		Timekeeping.begin();
/*		try(Transaction tx = graphDb.beginTx()){						
			StringBuilder query = new  StringBuilder();
			query.append("MATCH p=(s-[:INCLUDEDBY*]->a) ");
			query.append("WHERE a.Name='negative_testC'   ");
			query.append("RETURN p");
			
			System.out.println(simpleCypher(query).dumpToString());
		}
		Timekeeping.end();
		Timekeeping.showInfo("Computing sub nodes ");*/
		
		try(Transaction tx = graphDb.beginTx()){						
			StringBuilder query = new  StringBuilder();
			/*query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n),");
			query.append("np=(uc-[r2:INCLUDEDBY*]->t) ");
			//query.append("MATCH p=(s-[:INCLUDEDBY*]->a) ");
			query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) ");
			//query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.ComeFrom='First' and n.ComeFrom='First' and t.ComeFrom='First' ");
			query.append("RETURN pp,np");*/
			
			query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n),");
			query.append("np=(uc-[r2:INCLUDEDBY*]->t) ");
			query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) ");
			query.append(" and all(x in nodes(pp) WHERE 1=size(filter(y in nodes(pp) where x=y))) "); //路径上的点时唯一的
			query.append(" and all(x in nodes(np) WHERE 1=size(filter(y in nodes(np) where x=y))) ");
			//query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.ComeFrom='First' and n.ComeFrom='First' and t.ComeFrom='First' ");
			query.append("RETURN pp,np");
			
			/*query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n1-[r2:INCLUDEDBY*]->n), ");
			query.append("np=(uc-[r3:INCLUDEDBY*]->t1-[r4:INCLUDEDBY*]->t ) ");
			//query.append("MATCH p=(s-[:INCLUDEDBY*]->a) ");
			query.append("WHERE all(r in r1 WHERE all(g in r3 WHERE not(g=r))) ");
			query.append("and all(r in r2 WHERE all(g in r3 WHERE not(g=r))) ");
			query.append("and all(r in r1 WHERE all(g in r4 WHERE not(g=r))) ");
			query.append("and all(r in r2 WHERE all(g in r4 WHERE not(g=r))) ");
			//query.append("WHERE all(r in r1 WHERE all(g in r3 WHERE not(g=r))) ");
			//query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.ComeFrom='First' and n.ComeFrom='First' and t.ComeFrom='First' ");
			query.append("RETURN pp, np");*/
			
			/*query.append("MATCH pp= allShortestPaths((uc-[:INCLUDEDBY*]->n)),");
			query.append("np= allShortestPaths((uc-[:INCLUDEDBY*]->t)) ");
			//query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r))) ");
			query.append(" WHERE uc.Name='Reviewer' and n.Name='Person' and t.Name='negative_Person' ");
			query.append(" and uc.ComeFrom='First' and n.ComeFrom='First' and t.ComeFrom='First' ");
			query.append("RETURN pp,np");*/
			
			System.out.println(simpleCypher(query).dumpToString());
			long ticStart=System.currentTimeMillis();
			ExecutionEngine engine = new ExecutionEngine(graphDb);
			
			ExecutionResult result = engine.execute(query.toString());					
			//考虑pos=unsatNode的情况
			ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
			while(resultInIterator.hasNext())
			{
				long tic=System.currentTimeMillis();
				Map<String, Object> unsatMap = resultInIterator.next();				
				//System.out.println("cyhpe语句消耗的时间为："+(toc-tic)+"ms");
				PathImpl pPath = (PathImpl) unsatMap.get("pp");
				PathImpl nPath = (PathImpl) unsatMap.get("np");
				long toc=System.currentTimeMillis();	
				System.out.println(toc-tic+" ms");
			}
			long tocEnd=System.currentTimeMillis();
			System.out.println("查询路径消耗的时间为："+ (tocEnd-ticStart)/1000+" s");
			
			
		}
			
		Timekeeping.end();
		Timekeeping.showInfo("Computing sub nodes ");
		
		/*String ppFormatter="MATCH pp=((uc:%s )-[r1:INCLUDEDBY*]->(n:%s )), ";
		String npFormatter="np=((uc:%s )-[r2:INCLUDEDBY*]->(_n:%s )) ";	
		query.append(String.format(ppFormatter, conceptLabel, conceptLabel));
		query.append(String.format(npFormatter, conceptLabel, conceptLabel));
		query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
		query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "  );		//r1,r2是关系的集合，查询要求r1和r2没有公共的关系
		query.append("RETURN pp,np");*/
		
		/*Timekeeping.begin();
		try(Transaction tx = graphDb.beginTx()){						
			StringBuilder query = new  StringBuilder();
			query.append("MATCH p=( s-[*]->a) ");
			query.append("WHERE a.Name='negative_Fodder'   ");
			query.append("RETURN p");
			
			System.out.println(simpleCypher(query).dumpToString());
		}
		Timekeeping.end();
		Timekeeping.showInfo("Computing path ");	*/	
		
		graphDb.shutdown();
	}
}
