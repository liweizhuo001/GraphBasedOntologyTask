package kse.owl.running;

import kse.owl.OntoCreator;

public class OntoCreatorMain {
	
	//Create Example from "A New Operator for ABox Revision in DL-Lite"
	public static void example_1(){
		System.out.println("In creating ontology...");
		OntoCreator ontoCreator = new OntoCreator("owls/DIY/diyOnto.owl");
		//TBox
		ontoCreator.insertFunctionalAxiom("R");
		ontoCreator.insertSubsumptionOfRole("R", "R1");
		ontoCreator.insertSubsumptionOfConcept("existence_inverse_R", "B");
		ontoCreator.insertSubsumptionOfConcept("A", "C");
		
		ontoCreator.insertDisjointClassesAxiom("C", "D");		
		ontoCreator.insertDisjointClassesAxiom("B", "C");
		
		//Add incoherence
		ontoCreator.insertSubsumptionOfConcept("A", "D");
		
		//ABox
		ontoCreator.insertMembershipAssertion("C", "d");
		ontoCreator.insertMembershipAssertion("A", "e");
		ontoCreator.insertMembershipAssertion("A", "b");
		ontoCreator.insertMembershipAssertion("D", "d");
		ontoCreator.insertMembershipAssertion("A", "d");
		
		ontoCreator.insertMembershipAssertion("R", "a", "b");
		ontoCreator.insertMembershipAssertion("R", "a", "f");	
		ontoCreator.insertMembershipAssertion("R", "x", "f");
		ontoCreator.insertMembershipAssertion("R", "x", "y");
		
		ontoCreator.saveOnto();
		
	}
	
	/**
	 * 创建论文A Kernel Revision Operator for Terminologies中的例子
	 */
	
	public static void example_2(){
		
		//First TBox
		System.out.println("In creating ontology T1...");
		OntoCreator ontoCreator = new OntoCreator("owls/DIY/Kernel_T1.owl");		
		//ontoCreator.insertSubsumptionOfConcept("E", "B");
		ontoCreator.insertSubsumptionOfConcept("E", "M");  //替换上面公理
		
		
		ontoCreator.insertSubsumptionOfConcept("F", "B");
		ontoCreator.insertSubsumptionOfConcept("F", "C");
		//ontoCreator.insertSubsumptionOfConcept("D", "negative_B");			
		ontoCreator.saveOnto();
		
		//Second TBox
		System.out.println("In creating ontology T2...");
		ontoCreator = new OntoCreator("owls/DIY/Kernel_T2.owl");		
		ontoCreator.insertSubsumptionOfConcept("D", "E");
		ontoCreator.insertSubsumptionOfConcept("G", "D");
		ontoCreator.insertSubsumptionOfConcept("F", "D");
		ontoCreator.insertSubsumptionOfConcept("H", "A");		
		
		ontoCreator.insertSubsumptionOfConcept("D", "negative_B");//原属于t1			
		
		//not in example
		ontoCreator.insertSubsumptionOfConcept("H", "negative_B"); 
		//为添边增加的公理
		ontoCreator.insertSubsumptionOfConcept("B", "Q");
		ontoCreator.insertSubsumptionOfConcept("D", "P");
		ontoCreator.insertSubsumptionOfConcept("M", "B");
		ontoCreator.insertSubsumptionOfConcept("M", "N");
		
		ontoCreator.saveOnto();
	}
	
	//计算不可满足概念
	public static void example_3(){
		System.out.println("In creating ontology...");
		OntoCreator ontoCreator = new OntoCreator("owls/DIY/DisjointTest.owl");	
		ontoCreator.insertSubsumptionOfConcept("M", "Q");
		ontoCreator.insertSubsumptionOfConcept("P", "M");
		ontoCreator.insertSubsumptionOfConcept("A", "P");
		ontoCreator.insertSubsumptionOfConcept("B", "A");
		ontoCreator.insertSubsumptionOfConcept("D", "B");
		
		ontoCreator.insertDisjointClassesAxiom("P", "Q");
		ontoCreator.insertDisjointClassesAxiom("A", "Q");
		ontoCreator.insertDisjointClassesAxiom("B", "Q");
		ontoCreator.insertDisjointClassesAxiom("D", "Q");
		ontoCreator.saveOnto();
	}
	
	public static void main(String[] args){
		//example_1();
		example_3();
	}
}
