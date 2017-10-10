package kse.algorithm.forMapping.running;

import java.io.IOException;
import java.util.ArrayList;

import com.hp.hpl.jena.graph.query.regexptrees.Nothing;

import kse.algorithm.auxiliaryClass.Evaluation;
import kse.algorithm.forMapping.revision.ScoringMappingRevision;
import kse.misc.Timekeeping;
import kse.neo4j.ver1_8.DoulbeOWLMappingToGraphDB2;
import kse.owl.MappingInfo;


public class MappingRevisionMainFrame {
	static ArrayList<String> revisedMappings= new ArrayList<String>();
	static ArrayList<String> removedMappings= new ArrayList<String>();
	static ArrayList<String> candidateMappings= new ArrayList<String>();
	
	public static void createGraphDB(String gDB, String oFile1, String oFile2, MappingInfo MappingInformation) throws IOException{
		//从单个本体中创建图数据库
		DoulbeOWLMappingToGraphDB2 owlToGraph = new DoulbeOWLMappingToGraphDB2(gDB, oFile1, oFile2, MappingInformation,true);			
		owlToGraph.createDbFromOwl();
		owlToGraph.shutdown();		
	}
	
	public static void revisingMappings(String gPath,ArrayList<String> mappings)
	{
		System.out.println("Loading the constructed graph.");
		ScoringMappingRevision revisor = new ScoringMappingRevision(gPath,mappings);		
		System.out.println("End of init graph.");
		revisor.goRevising();
		revisedMappings=revisor.getMappings();
		removedMappings=revisor.getRemoveMappings();
		candidateMappings=revisor.getCandidateMappings();
		revisor.shutdown();
	}
	
	public static void main(String[] args) throws IOException
	{
		Timekeeping.begin();
		
		String oFile1 = "OAEIOntology/cmt.owl";
	    String oFile2 = "OAEIOntology/edas.owl";
		String gPath = "neo4j-test/Integrated_cmt_edas_test";
		
		//String mappingsPath = "alignments/crs-cmt_test.txt";
		String mappingsPath = "alignments/GMap/GMap-cmt-edas.rdf";
		String referencePath = "alignments/ReferenceAlignment/cmt-edas.rdf";
		
/*		String mappingsPath = "alignments/FCA_Map/FCA_Map-mouse-human.rdf";
		String referencePath = "alignments/ReferenceAlignment/reference_2015.rdf";*/
		
		//获取mappings的信息
		MappingInfo MappingInformation=new MappingInfo(mappingsPath);	
		ArrayList<String> mappings= new ArrayList<String>();
		mappings=MappingInformation.getMappings();
		
		ArrayList<String> referenceMappings= new ArrayList<String>();
		MappingInfo ReferenceInformation=new MappingInfo(referencePath);
		referenceMappings=ReferenceInformation.getMappings();
				
		//构建图数据库
		//createGraphDB(gPath, oFile1, oFile2, mappingsPath);
		createGraphDB(gPath, oFile1, oFile2, MappingInformation);
	
		//借助图数据库来修复mappings
		revisingMappings(gPath,mappings);
		System.out.println("--------------------------------------------------------");
		System.out.println("mapping reduced from " + mappings.size() + " to " + revisedMappings.size() + " correspondences");
		System.out.println("Removed the following "+removedMappings.size()+" correspondences:" );
		if(removedMappings.isEmpty())
			System.out.println("No mappings will be removed!");
		for(String s:removedMappings)
		{
			System.out.println(s);
		}
		System.out.println("--------------------------------------------------------");
		System.out.println("Added the following "+candidateMappings.size()+" correspondences:" );
		if(candidateMappings.isEmpty())
			System.out.println("No mappings will be added!");
		for(String s:candidateMappings)
		{
			System.out.println(s);
		}
		
		// compare against reference alignment
		Evaluation cBefore = new Evaluation(mappings, referenceMappings);
		Evaluation cAfter = new Evaluation(revisedMappings, referenceMappings);
		
		System.out.println("--------------------------------------------------------");
		System.out.println("before debugging (pre, rec, f): " + cBefore.toShortDesc());
		System.out.println("after debugging (pre, rec, f):  " + cAfter.toShortDesc());
		
		
		Timekeeping.end();
		Timekeeping.showInfo("running ");
	}
	

	
	
	
}
