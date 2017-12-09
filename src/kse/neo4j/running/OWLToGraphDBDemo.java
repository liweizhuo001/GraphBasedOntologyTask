package kse.neo4j.running;

import static kse.misc.GlobalParams.*;
import kse.misc.Timekeeping;
import kse.neo4j.ver1_8.InsertABoxToGraph;
import kse.neo4j.ver1_8.InsertTBoxToGraph;
import kse.neo4j.ver1_8.SingleOWLToGraphDB;

@SuppressWarnings("unused")
public class OWLToGraphDBDemo {
	
	public static void createGraphDB(String gDB, String oFile1){
		//从单个本体中创建图数据库
		SingleOWLToGraphDB owlToGraph = new SingleOWLToGraphDB(gDB, oFile1, COMEFROMFIRST,true);
		owlToGraph.createDbFromOwl();
		owlToGraph.shutdown();		
	}
	
	public static String gDB1 = "neo4j-db/UOBM";
	//public static String oFile1 = "owls/Test1.owl";	 
	//public static String oFile2 = "owls/Test2.owl";
	//public static String oFile1 = "owls/UOBM/univ-bench.owl";
	//public static String aFile1 = "owls/UOBM/University0_0.owl";
	
	//public static String gDB2 = "neo4j-db/DIY";
	//public static String oFile2 = "owls/DIY/diyOnto.owl";
	
	public static String gDB2 = "neo4j-test/chemical";
	//public static String oFile2 = "ClassicOntology/not-galen.owl";
	public static String oFile2 = "ClassicOntology/chemical.owl";
	
	public static void main(String[] args) {
		
		Timekeeping.begin();		
		
		/*createGraphDB(gDB1,oFile1);
		//插入一个本体到图数据库中
		InsertTBoxToGraph iTBox = new InsertTBoxToGraph(gDB1, oFile2);
		iTBox.InsertOwlIntoGraph();
		iTBox.shutdown();		
		//插入ABox到图数据库中
		InsertABoxToGraph aBox = new InsertABoxToGraph(gDB1, aFile1, COMEFROMFIRST);
		aBox.InsertABoxIntoGraph();*/
		

		createGraphDB(gDB2, oFile2);		
		//InsertABoxToGraph aBox = new InsertABoxToGraph(gDB2, oFile2, COMEFROMFIRST);
		//aBox.InsertABoxIntoGraph();
		
		Timekeeping.end();
		Timekeeping.showInfo("Create single graph db");
	}
}
