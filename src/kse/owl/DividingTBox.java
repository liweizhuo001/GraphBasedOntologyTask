package kse.owl;

import java.util.Random;

import kse.misc.Tools;

import org.semanticweb.owlapi.model.OWLAxiom;

/**
 * @2015-5-29
 * 将一个TBox分拆成两个TBox
 * @author Xuefeng Fu
  */

public class DividingTBox {
	OWLInfo owl;
	String targetPath;
	String sourcePath;
	OntoCreator creator1;
	OntoCreator creator2;
	
	public DividingTBox(String owlName, String tPath, String sPath){
		targetPath = tPath;
		sourcePath = sPath;
		owl = new OWLInfo(String.format(sourcePath,owlName));
		creator1 = new OntoCreator(String.format(targetPath, owlName+"_1"));		
		creator2 = new OntoCreator(String.format(targetPath, owlName+"_2"));
	}
	
	public void go(){
		Random random = new Random();
		for( OWLAxiom axiom : owl.getAllAxioms()){
			if(random.nextFloat()>0.5){
				creator1.insertAxiom(axiom);			
			}
			else{
				creator2.insertAxiom(axiom);
			}
		}
		creator1.saveOnto();
		creator2.saveOnto();
	}

	public static void main(String[] args){
		String sourcePath = "ForTBoxRevi/transform/%s.owl";
		String targetPath = "ForTBoxRevi/divided/%s.owl";
		for(String owlName : Tools.getOWLsOfExperimentation()){
			System.out.println("Dividing "+owlName +".owl");
			DividingTBox dividing = new DividingTBox(owlName, targetPath, sourcePath);
			dividing.go();			
		}		
	}
}
