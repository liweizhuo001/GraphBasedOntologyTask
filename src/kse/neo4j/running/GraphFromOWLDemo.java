package kse.neo4j.running;

import static kse.misc.GlobalParams.*;

import java.util.List;

import kse.misc.GlobalParams;
import kse.misc.Timekeeping;
import kse.misc.Tools;
import kse.neo4j.ver2_1.ExecCypher;
import kse.neo4j.ver2_1.GraphFromOWL;

@SuppressWarnings("unused")
public class GraphFromOWLDemo {
	public void createGraphFromTwo(String gDB, String firstPath, String secondPath){
		GraphFromOWL app = new  GraphFromOWL(gDB, firstPath, COMEFROMFIRST);
		app.addTBoxToGraphDB(true);		
		ExecCypher.listPaths(app.getGraphDB());
		app.shutdown();
		
		app = new GraphFromOWL(gDB, secondPath, COMEFROMSECOND);
		app.addTBoxToGraphDB(false);
		ExecCypher.listPaths(app.getGraphDB());
		app.shutdown();
	}
	
	public void createGraphFromSingle(String gDB,String firstPath){
		System.out.println("Creating graph database by function...");
		GraphFromOWL app = new  GraphFromOWL(gDB, firstPath, COMEFROMFIRST);
		app.addTBoxToGraphDB(true);		
		ExecCypher.listPaths(app.getGraphDB());
		app.shutdown();
	}
	
	public static void main(String[] args){	
		Timekeeping.begin();		
		GraphFromOWLDemo app = new  GraphFromOWLDemo();
		
		//public static String gPath = "neo4j-db/DIY2";
		//public static String owlPath = "owls/DIY/diyOnto.owl";			
		
		//List<String> owls = Tools.getTestOWLs();
		//List<String> owls = GlobalParams.getOWLsForGraph();
		List<String> owls = Tools.getOWLsOfExperimentation();
		String owl = owls.get(11);
		
		String owlPath = "owls/NewIncoherent/"+ owl+".owl";
		//String owlPath =  "owls/ForGraph/" + owl +".owl";
		String gPath = "neo4j-db/" + owl;
		
		/*String gDB = "neo4j-db/Economy-SDA" ;
		String owlPath = "owls/incoherent/Economy-SDA.owl";	*/
		app.createGraphFromSingle(gPath, owlPath);
		
		/*String gPath = "neo4j-db/DisjointTest" ;
		String owlPath = "owls/DIY/DisjointTest.owl";	
		app.createGraphFromSingle(gPath, owlPath);*/
		
		/*String gPath = "neo4j-db/KernelTBox" ;
		String o1 = "owls/DIY/Kernel_T1.owl";
		String o2 = "owls/DIY/Kernel_T2.owl";		
		app.createGraphFromTwo(gPath, o1, o2);*/
	
		
		Timekeeping.end();
		Timekeeping.showInfo("Creating Graph database ");
	}

}
