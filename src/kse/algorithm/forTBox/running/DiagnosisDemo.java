package kse.algorithm.forTBox.running;

import static kse.misc.Tools.getOWLsOfExperimentation;

import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import kse.algorithm.Diagnosis;

public class DiagnosisDemo {	
	
	public DiagnosisDemo(){
		
	}
	public static void main(String[] args){
		
		List<String> owls = getOWLsOfExperimentation();
		//String graphFormatter = "neo4j-db-2/%s";
		/*String graphFormatter = "neo4j-db/%s";
		
		int index = 1;
		String owl = owls.get(index);
		System.out.println(owl);
		String gPath = String.format(graphFormatter, owl);*/
		
		String gPath = "neo4j-test/Integrated_crs_cmt_test";
		
		GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Diagnosis diagnosis = new Diagnosis(graphDB);
		diagnosis.getUnsatNodes();
	}
}
