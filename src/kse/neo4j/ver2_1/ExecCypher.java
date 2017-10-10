package kse.neo4j.ver2_1;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kse.misc.Tools;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;

import static kse.misc.GlobalParams.*;

/**
 * 执行Cypher，有返回值
 * @author Xuefeng Fu
 */
public class ExecCypher {
	
	/**
	 * 在图数据库上执行简单的查询
	 * @param query
	 * @param graphDb
	 */
	public static  ExecutionResult simpleCypher(StringBuilder query, GraphDatabaseService graphDB){		
			return simpleCypher(query.toString(), graphDB);
	} 	
	
	public static  ExecutionResult simpleCypher(String query, GraphDatabaseService graphDB){
		//System.out.println("Cypher:" + query);
		ExecutionResult result =null;
		//try(Transaction tx = graphDB.beginTx()){		
			ExecutionEngine engine = new ExecutionEngine(graphDB);
			result =  engine.execute(query);
			//tx.success();
			//System.out.println(result.dumpToString());
		//}	
		return result;
	}
	
	public static Set<Node> getParentNodes(Set<Node> subs,GraphDatabaseService graphDB){
		Set<Node> parents = new HashSet<>();
		StringBuilder query = new StringBuilder();
		StringBuilder params = new StringBuilder();
		params.append("[");
		String formatter = "'%s',";
		for(Node n : subs){
			params.append(String.format(formatter, n.getProperty(NAMEPROPERTY)));
		}
		params.append("]");
		query.append("MATCH n-->p ");
		query.append("WHERE n.Name IN "+ params.toString().replaceAll(",]", "]"));
		query.append(" RETURN p");		
		
		try(Transaction tx = graphDB.beginTx()){
			ExecutionResult result = simpleCypher(query.toString(), graphDB);
			ResourceIterator<Node> iterator = result.columnAs("p");
			while(iterator.hasNext() ){
				Node parent = iterator.next();
				parents.add(parent);
				//System.out.println(parent.getProperty(NAMEPROPERTY));
			}
			tx.success();
		}		
		return parents;
	}
	
	/**
	 * String matchFormatter = "MATCH (sub),(sup) " ;
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		从起始节点添加一系列的边到目标数据库
	 * @param start ：起始节点
	 * @param end ：目标节点
	 * @param graphDB ：目标图数据库
	 */
	
	public static void createRelationBetweenNodes(String start,Node end,GraphDatabaseService graphDB){
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		//String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{Type:'%s',ComeFrom:'%s' }]->(sup) ";
		Tools.clear(query);
		String sup = end.getProperty(NAMEPROPERTY).toString();
		query.append("MATCH (sub),(sup) ");
		query.append(String.format(whereFormatter, start,sup));
		query.append(String.format(createFormatter, INCLUDEDREL,CONCEPTTYPE,"Revision"));			
		simpleCypher(query.toString(), graphDB);			
	}
	
	public static void createRelationBetweenNodesNameWithWeight(String start,Node end,Double weight,GraphDatabaseService graphDB){
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{Type:'%s',ComeFrom:'%s',Weight:'%s' }]->(sup) ";
		Tools.clear(query);
		String sup = end.getProperty(NAMEPROPERTY).toString();
		query.append("MATCH (sub),(sup) ");
		query.append(String.format(whereFormatter, start,sup));
		query.append(String.format(createFormatter, INCLUDEDREL,CONCEPTTYPE,"Revision",weight));			
		simpleCypher(query.toString(), graphDB);			
	}
	
	public static void createRelationBetweenNodesWithWeight(Node start,Node end,Double weight,GraphDatabaseService graphDB){
		StringBuilder query = new StringBuilder();
		String sub=start.getProperty(NAMEPROPERTY).toString();
		String sup=end.getProperty(NAMEPROPERTY).toString();
		String source=start.getProperty(COMEFROMPROPERTY).toString();
		String target=end.getProperty(COMEFROMPROPERTY).toString();
		//String Type=type; //考虑角色可能会增加大量的复杂度，暂时不这样考虑。而且添加角色的可能性很低。
		
		
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' and sub.ComeFrom='%s' and sup.ComeFrom='%s'";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{Type:'%s',ComeFrom:'%s',Weight:'%s' }]->(sup) ";
		Tools.clear(query);
		query.append("MATCH (sub),(sup) ");
		query.append(String.format(whereFormatter, sub,sup,source,target));
		query.append(String.format(createFormatter, INCLUDEDREL,CONCEPTTYPE,"Revision",weight));			
		simpleCypher(query.toString(), graphDB);			
	}
	
	/**
	 * String matchFormatter = "MATCH (sub),(sup) " ;
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		从起始节点添加一系列的边到目标数据库
	 * @param start ：起始节点
	 * @param sups ：目标节点的集合
	 * @param graphDB ：目标图数据库
	 */
	
	public static void createRelationBetweenNodes(String start,Set<Node> sups,GraphDatabaseService graphDB){
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		for(Node n : sups){
			Tools.clear(query);
			String sup = n.getProperty(NAMEPROPERTY).toString();
			query.append("MATCH (sub),(sup) ");
			query.append(String.format(whereFormatter, start,sup));
			query.append(String.format(createFormatter, INCLUDEDREL,CONCEPTTYPE,"Revision"));			
			simpleCypher(query.toString(), graphDB);
		}			
	}
	
	/**
	 * String matchFormatter = "MATCH (sub),(sup) " ;
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		从起始节点添加一系列的边到目标数据库
	 * @param start ：起始节点
	 * @param sups ：目标节点
	 * @param graphDB ：目标图数据库
	 */
	
	public static void createRelationBetweenNodes(String start,List<Node> sups,GraphDatabaseService graphDB){
		StringBuilder query = new StringBuilder();
		String whereFormatter = "WHERE sub.Name='%s' and sup.Name='%s' ";
		String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";
		for(Node n : sups){
			Tools.clear(query);
			String sup = n.getProperty(NAMEPROPERTY).toString();
			query.append("MATCH (sub),(sup) ");
			query.append(String.format(whereFormatter, start,sup));
			query.append(String.format(createFormatter, INCLUDEDREL,CONCEPTTYPE,"Revision"));			
			simpleCypher(query.toString(), graphDB);
		}			
	}
	
	/**
	 * 输出所有的直接路径
	 */
	public static void listPaths(GraphDatabaseService graphDB){
		ExecutionEngine engine = new ExecutionEngine(graphDB);
		//String _query = "MATCH p=(a-[*1..3]->b) RETURN p ";
		String _query = "MATCH p=(a-[*]->b) RETURN p ";
		//String _query = "MATCH p=(a-[r:MEMBEROF{ComeFrom:'NewAdd'}]->b) RETURN p ";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			System.out.println(result.dumpToString());
		}		
	}
	
	/**
	 * 输出所有的节点
	 */
	
	public static void listNodes(GraphDatabaseService graphDB){
		ExecutionEngine engine = new ExecutionEngine(graphDB);
		String _query = "MATCH (_node) RETURN _node ";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			System.out.println(result.dumpToString());
		}	
	}
}
