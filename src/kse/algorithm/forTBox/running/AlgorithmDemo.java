package kse.algorithm.forTBox.running;


import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.NAMEPROPERTY;

import java.util.List;
import java.util.Set;

import kse.algorithm.Diagnosis;
import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.MUPP;
import kse.misc.Timekeeping;
import kse.misc.Tools;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 *  算法综合测试
 * @author Xuefeng Fu
 *
 */
public class AlgorithmDemo {	
	
	GraphDatabaseService graphDB; 
	Diagnosis diag ;
	List<String> owls ;
	public static String gPathFormatter="neo4j-db/%s";
	public static String  oPathFormatter="owls/incoherent/%s.owl";
	
	public AlgorithmDemo(String gPath){
		owls = Tools.getTestOWLs();
		graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath ); 
		diag = Diagnosis.getDiagnosis(graphDB);
		
	}
	
	public AlgorithmDemo(int i){
		owls = Tools.getTestOWLs();
		String gPath =String.format(gPathFormatter, owls.get(i));
		System.out.println(gPath);
		graphDB= new  GraphDatabaseFactory().newEmbeddedDatabase(gPath  ); 
		diag = Diagnosis.getDiagnosis(graphDB);
	}
	
	public void getDisjointPairTest(){
		List<DisjointPair> pairs = GlobalFunct.getDisjointPair(graphDB);
		for(DisjointPair pair : pairs){
			System.out.println(pair.getFirst() +" <-> "+ pair.getSecond() );
		}
	}
	
	public void getUnsatNodeInTBoxText(){
		Set<String> unsatNodes = diag.getUnsatNodeByCypher();
		for(String node : unsatNodes){
			System.out.println(node);
		}
	}
	
	public void compMUPPsTest(){
		List<MUPP> mupps = diag.compMUPPs();
		for(MUPP mups: mupps){
			System.out.println(mups);			
		}
	}
	
	public void compMIPPsTest(){
		System.out.println("Test CompMIPPS ...");		
		List<MIPP> mipps = diag.compMIPPs();
		for(MIPP mips: mipps){
			System.out.println(mips);
			try(Transaction tx = diag.getGraphDB().beginTx()){
				for(Relationship r: mips.getDiagnosis()){
					System.out.print(r.getStartNode().getProperty(NAMEPROPERTY));
					System.out.print("-->");
					System.out.print(r.getEndNode().getProperty(NAMEPROPERTY)+"   ");
				}
				tx.success();
			}
		System.out.println();
		}
	}
	
	public  void createAllGraph(List<String> owls){
		for(String owl : owls){
			String owlPath = "owls/incoherent/"+ owl +".owl";
			String gPath = "neo4j-db/" + owl;
			System.out.println(gPath);
			Boolean isClear = true;
			GraphFromOWLbyCypher app = new  GraphFromOWLbyCypher(gPath, owlPath, COMEFROMFIRST);
			app.addTBoxToGraphDB(isClear);	
		}
		
	}
	
	//计算所有的不可满足概念
	public void printAllUN(){
		for(String owl:Tools.getTestOWLs()){
			//createAllGraph(owls);			
			//String owlPath = "owls/incoherent/"+ owl +".owl";
			String gPath = "neo4j-db/" + owl;
			System.out.print(owl+":");
			Timekeeping.begin();
			GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
			Diagnosis diag = Diagnosis.getDiagnosis(graphDB);	
			System.out.print(diag.getUnsatTripleByRelationship().size());			
			Timekeeping.end();		
			Timekeeping.showInfo(" Get UN by cypher ");
		}
	}
	
	//计算某个不可满足概念的mups
	public static void printUN(int i){
		//List<String> owls = GlobalParams.getTestOWLs();
		List<String> owls = Tools.getOWLsOfExperimentation();
		String owl = owls.get(i);
		//String owlPath = "owls/incoherent/"+ owl +".owl";
		String gPath = "neo4j-db/" + owl;
		System.out.print(owl+":");
		
		GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( gPath );
		Diagnosis diag = Diagnosis.getDiagnosis(graphDB);	
		Timekeeping.begin();
		System.out.println("Triple size is " + diag.getUnsatTripleByRelationship().size());			
		Timekeeping.end();		
		Timekeeping.showInfo(" Get UN by cypher ");
	}
	
	
	public static void main(String[] args){			
		/*String owlPath = "owls/DIY/DisjointTest.owl";
		String gPath = "neo4j-db/DisjointTest";*/				
		//String owlPath = "owls/incoherent/miniTambis.owl";
		//String gPath = "neo4j-db/DICE-A";		
		//String gPath = "neo4j-db/KernelTBox"; 		
		
		//***使用index来找unsatisfiable node 速度远快于Cypher		
		//Diagnosis diag = Diagnosis.getDiagnosis(graphDB);	
		
		//Timekeeping.begin();
		//System.out.println(diag.getUnsatTripleByRelationship().size());			
		//Timekeeping.end();		
		//Timekeeping.showInfo("Get UN by Relationship ");
		
		/*Timekeeping.begin();		
		System.out.println(diag.getUnsatNodeByCypher().size());			
		Timekeeping.end();		
		Timekeeping.showInfo("Get UN by cypher ");*/
		
		//printUN(18);		
	}	
	

}













