package kse.neo4j.running;

import static kse.misc.GlobalParams.*;

import java.io.IOException;
import java.util.ArrayList;

import kse.misc.Timekeeping;
import kse.neo4j.ver1_8.DoulbeOWLMappingToGraphDB;
import kse.neo4j.ver1_8.DoulbeOWLMappingToGraphDB;
import kse.neo4j.ver1_8.InsertABoxToGraph;
import kse.neo4j.ver1_8.InsertTBoxToGraph;
import kse.neo4j.ver1_8.SingleOWLToGraphDB;
import kse.owl.MappingInfo;

@SuppressWarnings("unused")
public class OWLMappingToGraphDBDemo {
	
	public static void createGraphDB(String gDB, String oFile1, String oFile2, String mappingPaths) throws IOException{
		//从单个本体中创建图数据库
		//SingleOWLToGraphDB owlToGraph = new SingleOWLToGraphDB(gDB, oFile1, COMEFROMFIRST,true);
		//DoulbeOWLMappingToGraphDB owlToGraph = new DoulbeOWLMappingToGraphDB(gDB, oFile1, oFile2,COMEFROMFIRST,mappingPaths,true);
		DoulbeOWLMappingToGraphDB owlToGraph = new DoulbeOWLMappingToGraphDB(gDB, oFile1, oFile2,mappingPaths,true);
		
		
		MappingInfo MappingInformation=new MappingInfo(mappingPaths);
		ArrayList<String> mappings=MappingInformation.getMappings();
		for(String a: mappings)
		{
			System.out.println(a);
		}
		
		owlToGraph.createDbFromOwl();
		owlToGraph.shutdown();		
	}
	

/*	public static String oFile1 = "OAEIOntology/crs_test.owl";
	public static String oFile2 = "OAEIOntology/cmt_test.owl";*/
	
	
/*	public static String oFile1 = "OAEIOntology/crs.owl";
	public static String oFile2 = "OAEIOntology/cmt.owl";*/
	
	/*public static String oFile1 = "OAEIOntology/cmt_test.owl";
	//public static String oFile2 = "OAEIOntology/confof_test.owl";
	public static String oFile2 = "OAEIOntology/confof_test2.owl";*/

	
	public static String oFile1 = "OAEIOntology/cmt.owl";
	public static String oFile2 = "OAEIOntology/edas.owl";
	
	
	//public static String mappingsPath = "alignments/crs-cmt_test.txt";
	//public static String mappingsPath = "alignments/cmt-confof.rdf";
	//public static String mappingsPath = "alignments/cmt-confof_test.txt";
	public static String mappingsPath = "alignments/GMap-cmt-edas.rdf";
	//public static String mappingsPath = "alignments/GMap-cmt-confof_test.rdf";
	
	public static String gDB = "neo4j-test/Integrated_cmt_edas_test";
	//public static String gDB = "neo4j-test/Integrated_crs_cmt_test";
	
	public static void main(String[] args) throws IOException {
		
		Timekeeping.begin();		
		
		
		/*createGraphDB(gDB1,oFile1);
		//插入一个本体到图数据库中
		InsertTBoxToGraph iTBox = new InsertTBoxToGraph(gDB1, oFile2);
		iTBox.InsertOwlIntoGraph();
		iTBox.shutdown();		
		//插入ABox到图数据库中
		InsertABoxToGraph aBox = new InsertABoxToGraph(gDB1, aFile1, COMEFROMFIRST);
		aBox.InsertABoxIntoGraph();*/
		

		createGraphDB(gDB, oFile1, oFile2, mappingsPath);
		
		//InsertABoxToGraph aBox = new InsertABoxToGraph(gDB2, oFile2, COMEFROMFIRST);
		//aBox.InsertABoxIntoGraph();
		
		Timekeeping.end();
		Timekeeping.showInfo("Create a complex graph db");
	}
}
