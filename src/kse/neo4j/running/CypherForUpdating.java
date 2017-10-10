package kse.neo4j.running;

import static kse.misc.GlobalParams.*;
import kse.neo4j.ver1_8.Tools4Graph;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Using cypher updating graph database
 * @author Xuefeng fu
 *
 */
@SuppressWarnings("unused")
public class CypherForUpdating {
	public static  String gDB = "neo4j-db/CypherDemo";
	//public static String gDB = "neo4j-db/DIY";
	public  GraphDatabaseService  graphDb ;
	public  ExecutionEngine engine;		
	
	public CypherForUpdating(){
		  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDB );
		  engine = new ExecutionEngine( graphDb );	
	}
	
	/**
	 * 简单的查询测试
	 * @param query 查询字符串
	 */
	public  void simpleCypher(StringBuilder query){		
		String _query = query.toString();
		System.out.println(_query);
		try(Transaction tx = graphDb.beginTx()){
			//ExecutionEngine engine = new ExecutionEngine( graphDb );						
			ExecutionResult result =  engine.execute(_query);
			System.out.println(result.dumpToString());
			tx.success();
		}		
	}
	
	public  boolean isNodeExistence(StringBuilder query,  String colName){
		String _query = query.toString();
		System.out.println(_query);
		try(Transaction tx = graphDb.beginTx()){
			//ExecutionEngine engine = new ExecutionEngine( graphDb );						
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			if(result.columnAs(colName).hasNext()){
				return true;
			}
			else{
				return false;
			}						
		}	
	}
	/**
	 * 清空StringBuilder
	 * @param query 要清空的查询
	 */
	public static  void clear(StringBuilder query){
		query.delete(0, query.length());
	}

	
	public static  void main(String[] args){				
		Tools4Graph.clearDb(gDB);  //清空數據庫		
		StringBuilder query = new StringBuilder();
		
		CypherForUpdating app = new CypherForUpdating();
		
		//添加索引
		query.append("CREATE INDEX ON: Concept(name); ");
		app.simpleCypher(query); 
		clear(query);
		
		// 创建一个节点		
		query.append("CREATE (A:Concept {name: 'A', comefrom:'first'})  ");
		query.append("RETURN A; "); 		
		app.simpleCypher(query );
		clear(query);
		
		query.append("CREATE (B:Concept {name: 'B', comefrom:'second'})  ");
		query.append("RETURN B; "); 		
		app.simpleCypher(query );
		clear(query);
		
		query.append("CREATE (C:Concept {name: 'C', comefrom:'second'})  ");
		query.append("RETURN C; "); 		
		app.simpleCypher(query );
		clear(query);
		
		//创建唯一的Relationship
		query.append("MATCH (A:Concept{name:'A'}), (B:Concept{name:'B'}) ");		
		query.append("CREATE UNIQUE   (A)-[:ACTED_IN {name:'A->B'}]->(B) ");
		app.simpleCypher(query );
		clear(query);		
		
		query.append("MATCH (A:Concept),(B:Concept)" );
		query.append("WHERE A.name='A' and B.name='B' ");
		query.append("CREATE UNIQUE  (A)-[:INCLUDEDBY {name:'A->B'}]->(B) ");		
		app.simpleCypher(query );
		clear(query);
				
		query.append("MATCH (A),(C) ");
		query.append("WHERE A.name='A' and C.name='C' ");
		query.append("CREATE UNIQUE  (A)-[:INCLUDEDBY {name:'A->C'}]->(C) ");		
		app.simpleCypher(query );
		clear(query);
		
		query.append("MATCH (A),(C) ");
		query.append("WHERE A.name='A' and C.name='D' ");
		query.append("CREATE UNIQUE  (A)-[:INCLUDEDBY {name:'A->C'}]->(C) ");		
		app.simpleCypher(query );
		clear(query);
		
		//创建前判断是否存在
		/*query.append("MATCH   (A:concept {name: 'A'})  ");
		query.append("RETURN A; "); 		
		if(app.isNodeExistence(query,  "A" )){
			System.out.println("The node is already existence.");
		}
		else{
			clear(query);
			query.append("CREATE   (A:concept {name: 'A'})  ");
			query.append("RETURN A; "); 		
			app.simpleCypher(query );
		}	
		clear(query);*/		
		
		//新增一个节点并且建立关系
		/*query.append("CREATE (B:concept {name: 'B', comefrom:'first'})  ");
		query.append("RETURN B; "); 		
		simpleCypher(query, graphDb );
		clear(query);*/

		//建立节点之间的关系
		/*query.append("MATCH (sub:concept {name:'A'}), " );
		query.append("(sup:concept {name:'B'})  ");
		query.append("CREATE(sub-[:INCUDEDDBY{name: (sub.name+'->'+sup.name) }]->sup) ");
		simpleCypher(query, graphDb );	
		clear(query);*/
		
		//对所有的节点添加一新属性
		/*query.append("MATCH (n) ");
		query.append("SET n.type = 'concept' ");
		query.append("RETURN n ");
		simpleCypher(query, graphDb );			
		clear(query);*/
		
		/*query.append("MATCH p=(a)-->(b) " );
		query.append("RETURN p ");
		simpleCypher(query, graphDb );	
		clear(query);*/
		
		query.append("MATCH p=(a-[r]->b) RETURN p" );
		app.simpleCypher(query );			
		clear(query);
		app.graphDb.shutdown();
	}
}
