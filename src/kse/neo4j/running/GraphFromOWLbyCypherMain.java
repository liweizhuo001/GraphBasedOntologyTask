package kse.neo4j.running;

import java.util.List;

//import kse.misc.GlobalParams;
import kse.misc.Timekeeping;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;
import static kse.misc.GlobalParams.*;
import static kse.misc.Tools.*;

public class GraphFromOWLbyCypherMain {	
	
	public  void createGraphFromSingle(String gDB, String owlPath){
		System.out.println("Creating graph database by cypher...");
		Boolean isClear = true;
		GraphFromOWLbyCypher app = new  GraphFromOWLbyCypher(gDB, owlPath, COMEFROMFIRST);
		app.addTBoxToGraphDB(isClear);		
		//app.addABoxToGraphDB();
		//app.listPaths();		
	}
	
	public void createGraphFromTwo(String gDB, String firstPath, String secondPath){
		GraphFromOWLbyCypher app = new  GraphFromOWLbyCypher(gDB, firstPath, COMEFROMFIRST);
		app.addTBoxToGraphDB(true);		
		app.listPaths();
		app.shutDown();
		
		app = new GraphFromOWLbyCypher(gDB, secondPath, COMEFROMSECOND);
		app.addTBoxToGraphDB(false);
		app.listPaths();
		app.shutDown();
	}
	
	public static void main(String[] args){	
		
		List<String> owls = getOWLsOfExperimentation();
		String formatter = "owls/NewIncoherence-2/%s.owl";			

		
		Timekeeping.begin();		
		
		GraphFromOWLbyCypherMain app = new  GraphFromOWLbyCypherMain();		
		/*public  String gDB = "neo4j-db/DIY2";
		public  String owlPath = "owls/DIY/diyOnto.owl";*/
		
		String owl = owls.get(14);
		String gDB = "neo4j-db-2/" + owl;
		String owlPath = String.format(formatter, owl);
		
		
		/*String gDB = "neo4j-db/OntoTBoxDiag";
		String owlPath = "owls/DIY/OntoTBoxDiag.owl" ;*/
		
		app.createGraphFromSingle(gDB, owlPath);
		
		/*String gDB = "neo4j-db/KernelTBox" ;
		String o1 = "owls/DIY/Kernel_T1.owl";
		String o2 = "owls/DIY/Kernel_T2.owl";		
		app.createGraphFromTwo(gDB, o1, o2);*/
		
		//String gPath = "neo4j-db/CHEM-A" ;
		//String firstPath = "owls/incoherent/CHEM-A.owl";		
		//app.createGraphFromSingle(gPath, firstPath);
		
		Timekeeping.end();
		Timekeeping.showInfo("Handling Graph database ");
	}
}








