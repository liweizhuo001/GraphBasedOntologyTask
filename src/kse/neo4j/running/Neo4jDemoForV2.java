package kse.neo4j.running;

import java.util.List;

import kse.algorithm.auxiliaryClass.GlobalFunct;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

import static kse.misc.GlobalParams.*;

/**
 * 2.0以后的版本，对图数据库的修改均需放置在Transaction中<br>
 * All database operations that access the graph, indexes, or the schema must be performed in a transaction.
 * @author Xuefeng Fu
 */

public class Neo4jDemoForV2 {
	
	public static void test1(){
		String gDB = "neo4j-db/test";
		GraphDatabaseService  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDB );
		 
		 try(Transaction transaction = graphDb.beginTx()){	//java1.7之后的语法,默认有内存的回收		 
			 Node node = graphDb.createNode();
			 node.setProperty("name", "node1");
			 transaction.success();
		 }
		 catch(Exception e){
			 e.printStackTrace();
		 }
	}
	
	public static void test2(){
		System.out.println("Read the nodes from graph database.");
		String gDB = "neo4j-db/DIY";
		GraphDatabaseService  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDB );
		try(Transaction tx = graphDb.beginTx()){
			GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDb);
			for(Node node : ggo.getAllNodes()){
				System.out.println(node.getProperty(NAMEPROPERTY));
			}
		}
		graphDb.shutdown();
	}
	
	public static void test3(){
		System.out.println("Read the negative concepts from graph database.");
		String gDB = "neo4j-db/DIY";
		GraphDatabaseService  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDB );
		try(Transaction tx = graphDb.beginTx()){
			List<String> negativeConcepts = GlobalFunct.getNegativeNodes(graphDb);
			System.out.println("The number of negative concept is " + negativeConcepts.size());
		}
		graphDb.shutdown();
	}
	
	public static void main(String[] args){		
		//test2();
		test3();
	}

}
