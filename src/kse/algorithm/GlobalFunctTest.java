package kse.algorithm;

import static kse.misc.Tools.getOWLsOfExperimentation;

import java.util.List;

import kse.algorithm.auxiliaryClass.GlobalFunct;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

public class GlobalFunctTest {
	//public static String gPathFormatter="neo4j-TBox-Scoring/%s";
	public static String gPathFormatter="neo4j-db/%s";	
	//public static String gPathFormatter="neo4j-test/%s";	
	public static void main(String[] args){
		List<String> owls = getOWLsOfExperimentation();
		for(int i=0; i<owls.size(); i++){
			//int index=1;
			String gPath =String.format(gPathFormatter, owls.get(i));
			System.out.println(gPath);
			GraphDatabaseService graphDB= new  GraphDatabaseFactory().newEmbeddedDatabase(gPath); 
			try(Transaction tx = graphDB.beginTx()){
				List<Relationship> rels = GlobalFunct.getAllNIs(graphDB);//仅仅只是测试有多少含否定的结点
				System.out.println(owls.get(i) + ":" + rels.size());
				tx.success();
			}
		}		
	}
}
