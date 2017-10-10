package kse.owl.running;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;

import kse.misc.GlobalParams;
import kse.misc.Timekeeping;
import kse.misc.Tools;
import kse.neo4j.ver1_8.PreOWL4Graph;
import kse.owl.*;
import static kse.misc.Tools.*;

@SuppressWarnings("unused")
public class OWLInfoMain {
	
	public static OWLInfo owlInfo;

	public static void printInfo() {
		System.out.println("TBox	ABox	SubClassOf	Function	SubProperty	DisjointClass	DisjointProperty	InverseProperty");
		String format = "%d	%d	%d		%d		%d		%d		 %d			%d";
		System.out.println(String.format(format, owlInfo.getAxiomsInTBox()
				.size(), owlInfo.getAxiomsInABox().size(), owlInfo
				.getAxiomsOfSubClass().size(), owlInfo.getAxiomsOfFunct()
				.size(), owlInfo.getAxiomsOfSubProperty().size(), owlInfo
				.getAxiomsOfDisjointClass().size(), owlInfo
				.getAxiomsOfDisjointObjProperty().size(), owlInfo
				.getAxiomsOfInverseProperty().size()));
	}

	public static void printAllDisjoint() {
		int i = 0;
		System.out.println("All Disjoint Classes ----------------------------------");
		for (OWLDisjointClassesAxiom oc : owlInfo.getAxiomsOfDisjointClass()) {
			System.out.println("[" + i++ + "]" + oc.toString());
		}

		System.out.println("All Disjoint properties ----------------------------------");
		i = 0;
		for (OWLDisjointObjectPropertiesAxiom oc : owlInfo	.getAxiomsOfDisjointObjProperty()) {
			System.out.println("[" + i++ + "]" + oc.toString());
		}
	}

	public static void printTokensSize() {
		System.out.println(String.format("Concept:%s,Roles:%s", owlInfo.getConceptTokens().size(), owlInfo.getObjPropertyTokens().size()));
	}

	public static String getName(int i, int j) {
		String owlNameFormat = "Owls/UOBM2/percent10/univ_%d_%d.owl";
		return String.format(owlNameFormat, i, j);
	}
	
	/**
	 * 输出本体中所有的TBox公理
	 */
	public static void printAxiomsInTBox() {
		System.out.println("**** In Print TBox ****");
		for (OWLAxiom axiom : owlInfo.getAxiomsInTBox()) {
			System.out.println(axiom);
		}
	}

	/**
	 * 输出本体中所有的符号，类和属性
	 * @param  type "concept" "role"
	 */
	public static void printTokens(String type) {
		System.out.println("**** In Print " + type + " Token ****");
		Set<String> tokens = new HashSet<String>();
		if (type.equals("concept"))
			tokens = owlInfo.getConceptTokens();
		else if(type.equals("role"))
			tokens = owlInfo.getObjPropertyTokens();

		for (String token : tokens) {
			System.out.println(token);
		}
	}

	/**
	 * 输出本体中的类别包含公理
	 */
	public static void printAxiomOfSubClass() {
		System.out.println("**** In Print SubClassOf Axiom ****");
		for (OWLAxiom axiom : owlInfo.getAxiomsOfSubClass()) {
			System.out.println(axiom);
		}
	}

	/**
	 * 输出本体中的函数属性公理
	 */
	
	public static void printAxiomOfFunct() {
		System.out.println("**** In Print Functional Axiom ****");
		for (OWLAxiom axiom : owlInfo.getAxiomsOfFunct()) {
			System.out.println(axiom);
		}
	}

	/**
	 * 输出本体中的逆属性公理
	 */
	public static void printAxiomOfInverseProperty() {
		System.out.println("**** In Print Inverse Property Axiom ****");
		for (OWLAxiom axiom : owlInfo.getAxiomsOfInverseProperty()) {
			System.out.println(axiom);
		}
	}

	/**
	 * 输出特定的公理集
	 * 
	 * @param axioms :公理集
	 */
	@SuppressWarnings("rawtypes")
	public static void printAxioms(Set axioms) {
		System.out.println(String.format("Size:%d", axioms.size()));
		for (Object axiom : axioms) {
			System.out.println(axiom);
		}
	}
	/**
	 * 输出本体中各类公理的数目
	 */
	public static void printNumberOfAxioms(){
		System.out.println("概念数(class/concept)：" + owlInfo.getConceptTokens().size());
		System.out.println("对象属性(object property)数：" + owlInfo.getObjPropertyTokens().size());
		System.out.println("数据属性(data property)数：" + owlInfo.getDataPropertyTokens().size());
		System.out.println("概念包含(subclassof)公理："+ owlInfo.getAxiomsOfSubClass().size());
		System.out.println("角色包含(subObjectProperty)公理："+ owlInfo.getAxiomsOfSubProperty().size());
		System.out.println("所有的公理数：" + owlInfo.getAllAxioms().size());
		System.out.println("TBox公理数:" + owlInfo.getAxiomsInTBox().size());
		
		System.out.println("ABox公理数:" + owlInfo.getAxiomsInABox().size());
		System.out.println("Role membership :"+ owlInfo.getAxiomsOfObjProAssertion().size());
		System.out.println("函数公理:" + owlInfo.getAxiomsOfFunct().size());
		System.out.println("不相交(否定包含)公理数(概念):" + owlInfo.getAxiomsOfDisjointClass().size());
		System.out.println("不相交(否定包含)公理数(属性):" + owlInfo.getAxiomsOfDisjointObjProperty().size());
	}

	public static void printTBoxAxiomMapping(){
		List<AxiomMapping> aMappings =  owlInfo.getTBoxAxiomMappings();
		for(AxiomMapping aMapping : aMappings){
			System.out.println(aMapping);
		}
		System.out.println("Size of mapping is "+ aMappings.size());
	}
	
	public static void printABoxAxiomMapping(){
		List<AxiomMapping> aMappings =  owlInfo.getABoxAxiomMapping();
		for(AxiomMapping aMapping : aMappings){
			System.out.println(aMapping);
		}
		System.out.println("Size of mapping is "+ aMappings.size());
	}
	
	public static void getOWLsInfo(){
		String targetPath = "ForTBoxRevi/divided/%s_%s.owl";
		for(String owlName : Tools.getOWLsOfExperimentation()){
			for(int i=1;i<=2;i++){
				String _owl = String.format(targetPath, owlName, i);
				System.out.println(_owl);
				writeOWLInfoIntoTxt(_owl);
			}			
		}	
	}
	//将owl信息写入文件
	public static void writeOWLInfoIntoTxt(String owlPath){
		OWLInfo owl = new OWLInfo(owlPath);
		FileWriter writer;
        try {        	
            writer = new FileWriter("owlInfo.txt", true);            
            writer.write(owlPath);
            writer.write("\r\n");
            
            writer.write("ALL: " + owl.getAllAxioms().size());	writer.write("  ");    	
    		
    		writer.write("TBox: " + owl.getAxiomsInTBox().size());writer.write("  ");    		
    		
    		writer.write("NIs: " + owl.getAxiomsOfDisjointClass().size());writer.write("  ");
    		writer.write("_NIs: " + owl.getAxiomsOfDisjointObjProperty().size());writer.write("  ");
            
            writer.write("Concepts: " + owl.getConceptTokens().size()); writer.write("  ");
            
            writer.write("Roles: " + owl.getObjPropertyTokens().size());  writer.write("  ");
    		
    		writer.write("概念包含: "+ owl.getAxiomsOfSubClass().size());  writer.write("  ");    		
    		
    		writer.write("角色包含: "+ owl.getAxiomsOfSubProperty().size()); writer.write("  ");
    		
    		writer.write("函数公理: " + owl.getAxiomsOfFunct().size());writer.write("  ");
    		
    
    		writer.write("\r\n");
    		writer.write("\r\n");
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}

	public static void main(String[] args) {			
		Timekeeping.begin();
		
		//For TBox Debugging
		/*//List<String> owls = GlobalParams.getTestOWLs();
		List<String> owls = getOWLsOfExperimentation();
		String owl = owls.get(12);		
		//String owlPath = "owls/NewIncoherence-2/"+owl+".owl";		
		String owlPath = "owls/Original-2/"+owl+".owl";		
		owlPath = "D:\\J2EE\\apache-tomcat-7.0.27\\webapps\\GBDS\\owls\\AEO.owl";
		owlInfo = new OWLInfo(owlPath);		
		System.out.println(owlInfo.getOwlFileName());
		//printNumberOfAxioms();
		//printAllDisjoint();
*/		
		
		//输出一个本体的详细信息
		//String ontoFormat = "ForTBoxRevi/transform/%s";		
		/*String ontoFormat = "ForTBoxRevi/original/%s";
		String onto = null;
		onto = String.format(ontoFormat, "fly-anatomy.owl");
		owlInfo = new OWLInfo(onto);			
		printNumberOfAxioms();
		System.out.println("################################");*/
		
		//将一系列本体的详细信息写到文本文件中
		getOWLsInfo();
		
		//printAllDisjoint();
		/*onto = String.format(ontoFormat, "univ_4_1.owl");
		owlInfo = new OWLInfo(onto);		
		printNumberOfAxioms();*/		
		
		//printTBoxAxiomMapping();
		//printABoxAxiomMapping();
		//System.out.println(owlInfo.getAxiomsInABox().size());
		
		Timekeeping.end();
		Timekeeping.showInfo("Running program");
	}
}
