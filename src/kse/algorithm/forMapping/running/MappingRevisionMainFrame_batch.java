package kse.algorithm.forMapping.running;

import java.io.IOException;
import java.util.ArrayList;

import com.hp.hpl.jena.graph.query.regexptrees.Nothing;

import Tools.OAEIAlignmentSave;
import kse.algorithm.auxiliaryClass.Evaluation;
import kse.algorithm.forMapping.revision.ScoringMappingRevision;
import kse.misc.Timekeeping;
import kse.neo4j.ver1_8.DoulbeOWLMappingToGraphDB2;
import kse.owl.MappingInfo;


public class MappingRevisionMainFrame_batch {
	static ArrayList<String> revisedMappings= new ArrayList<String>();
	static ArrayList<String> removedMappings= new ArrayList<String>();
	static ArrayList<String> candidateMappings= new ArrayList<String>();
	static String URI1="",URI2="";
	
	public static void createGraphDB(String gDB, String oFile1, String oFile2, MappingInfo MappingInformation) throws IOException{
		//从单个本体中创建图数据库
		DoulbeOWLMappingToGraphDB2 owlToGraph = new DoulbeOWLMappingToGraphDB2(gDB, oFile1, oFile2, MappingInformation,true);	
		URI1=owlToGraph.getOwlInfo1().URI.replace(".owl", "").replace(".rdf", "");
		URI2=owlToGraph.getOwlInfo2().URI.replace(".owl", "").replace(".rdf", "");
		owlToGraph.createDbFromOwl();
		owlToGraph.shutdown();		
	}
	
	public static void revisingMappings(String gPath,ArrayList<String> mappings)
	{
		System.out.println("Loading the constructed graph.");
		ScoringMappingRevision revisor = new ScoringMappingRevision(gPath,mappings);		
		System.out.println("End of init graph.");
		revisor.goRevising();
		//revisor.goRevisingbyImpactor();
		//revisor.goRevisingbyClosure();		
		//revisor.goRevisingbyWeight();
		//revisor.goRevisingbyWeightPlusImpactor();
		//revisor.goRevisingbyWeightPlusClosure();
		//revisor.goRevisingbyImpactorPlusClosure();
		
		//revisor.goRevisingP1P3P2();
		//revisor.goRevisingP2P1P3();
		//revisor.goRevisingP2P3P1();
		//revisor.goRevisingP3P1P2();
		//revisor.goRevisingP3P2P1();
		//revisor.goRevisingBagging();
		
		revisedMappings=revisor.getMappings();
		removedMappings=revisor.getRemoveMappings();
		candidateMappings=revisor.getCandidateMappings();
		revisor.shutdown();
	}
	
	public static void main(String[] args) throws Exception
	{
		Timekeeping.begin();
		
		/*String oFile1 = "OAEIOntology/confOf.owl";
	    String oFile2 = "OAEIOntology/edas.owl";
		String gPath = "neo4j-test/Integrated-confof-edas-test";
		String mappingsPath = "alignments/editdistance/confOf-edas-edit_batch_1.0.rdf";
		String referencePath = "alignments/ReferenceAlignment/confOf-edas.rdf";
		String Object="Edit-";*/
		
		/*String oFile1 = "OAEIOntology/cmt.owl";
	    String oFile2 = "OAEIOntology/confOf.owl";
		String gPath = "neo4j-test/Integrated-cmt-confof-test";
		String mappingsPath = "alignments/editdistance/cmt-confOf-edit_batch_1.0.rdf";
		String referencePath = "alignments/ReferenceAlignment/cmt-confOf.rdf";
		String Object="Edit-";*/
		
		
		/*String oFile1 = "OAEIOntology/cmt.owl";
	    String oFile2 = "OAEIOntology/Conference.owl";
		String gPath = "neo4j-test/Integrated-cmt-conference-test";
		String mappingsPath = "alignments/GMap/GMap-cmt-conference.rdf";
		String referencePath = "alignments/ReferenceAlignment/cmt-conference.rdf";
		String Object="GMap-";*/
		
		/*String oFile1 = "OAEIOntology/cmt.owl";
	    String oFile2 = "OAEIOntology/edas.owl";
		String gPath = "neo4j-test/Integrated-cmt-edas-test";
		String mappingsPath = "alignments/GMap/GMap-cmt-edas.rdf";
		String referencePath = "alignments/ReferenceAlignment/cmt-edas.rdf";
		String Object="GMap-";*/
		
		/*String oFile1 = "OAEIOntology/confOf.owl";
	    String oFile2 = "OAEIOntology/edas.owl";
		String gPath = "neo4j-test/Integrated-confof-edas-test";
		String mappingsPath = "alignments/GMap/GMap-confof-edas.rdf";
		String referencePath = "alignments/ReferenceAlignment/confOf-edas.rdf";
		String Object="GMap-";*/

		/*String oFile1 = "OAEIOntology/Conference.owl";
	    String oFile2 = "OAEIOntology/confOf.owl";
		String gPath = "neo4j-test/Integrated-conference-confof-test";
		String mappingsPath = "alignments/GMap/GMap-conference-confof.rdf";
		String referencePath = "alignments/ReferenceAlignment/conference-confOf.rdf";
		String Object="GMap-";*/
		
		/*String oFile1 = "OAEIOntology/Conference.owl";
	    String oFile2 = "OAEIOntology/edas.owl";
		String gPath = "neo4j-test/Integrated-conference-edas-test";
		String mappingsPath = "alignments/FCA_Map/FCA_Map-Conference-edas.rdf";
		String referencePath = "alignments/ReferenceAlignment/conference-edas.rdf";
		String Object="FCA_Map-";*/
		

		
	
		/*String oFile1 = "OAEIOntology/mouse.owl";
	    String oFile2 = "OAEIOntology/human.owl";
		String gPath = "neo4j-test/Integrated_mouse_human_test";
		String mappingsPath = "alignments/editdistance/mouse-human-edit_batch_1.0.rdf";
		String referencePath = "alignments/ReferenceAlignment/reference_2015.rdf";
		String Object="Edit-";*/
		
		String oFile1 = "OAEIOntology/mouse.owl";
	    String oFile2 = "OAEIOntology/human.owl";
		String gPath = "neo4j-test/Integrated_mouse_human_test";
		String mappingsPath = "alignments/GMap/GMap-mouse-human.rdf";
		String referencePath = "alignments/ReferenceAlignment/reference_2015.rdf";
		String Object="GMap-";
		
		/*String oFile1 = "OAEIOntology/mouse.owl";
	    String oFile2 = "OAEIOntology/human.owl";
		String gPath = "neo4j-test/Integrated_mouse_human_test";
		String mappingsPath = "alignments/FCA_Map/FCA_Map-mouse-human.rdf";
		String referencePath = "alignments/ReferenceAlignment/reference_2015.rdf";
		String Object="FCA_Map-";*/
		

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
		System.out.println("--------------------------------------------------------");
		/*System.out.println("Revised mappings "+revisedMappings.size()+" correspondences:" );
		for(String s:revisedMappings)
		{
			System.out.println(s);
		}*/
		
		// compare against reference alignment
		Evaluation cBefore = new Evaluation(mappings, referenceMappings);
		Evaluation cAfter = new Evaluation(revisedMappings, referenceMappings);
		
		System.out.println("--------------------------------------------------------");
		System.out.println("before debugging (pre, rec, f): " + cBefore.toShortDesc());
		System.out.println("The number of total wrong mappings in alignment:  " + (cBefore.getMatcherAlignment()-cBefore.getCorrectAlignment()));
		System.out.println("after debugging (pre, rec, f):  " + cAfter.toShortDesc());
		System.out.println("The number of wrong removed mappings:  " + (cBefore.getCorrectAlignment()-cAfter.getCorrectAlignment()));
		
		//可以考虑先存储为Txt
		
		
		String alignmentPath="RepairedResults/"+Object+URI1.replace("http://", "")+"-"+URI2.replace("http://", "")+"-repaired_testPrincipleOrder";
		OAEIAlignmentSave out=new OAEIAlignmentSave(alignmentPath,URI1,URI2);
		for(int i=0;i<revisedMappings.size();i++)
		{
			String parts[]=revisedMappings.get(i).split(",");
				//System.out.println(Alignments.get(i));
			out.addMapping2Output(parts[0],parts[1],parts[2],parts[3]);
		}
		out.saveOutputFile();
		System.out.println("The file is saved in "+alignmentPath);
		
		
		Timekeeping.end();
		Timekeeping.showInfo("running ");
	}
	

	
	
	
}
