package kse.algorithm.forTBox.running;

import java.util.List;

import kse.algorithm.forTBox.debugging.hittingset.HSTRevision;
import kse.misc.Timekeeping;
import kse.misc.Tools;

public class HSTRevisionMain {
	
	public static void revisingAll(String gPath){
		List<String> owls = Tools.getOWLsOfExperimentation();		
		for(int index=0;index<owls.size(); index++){
			Timekeeping tk = Timekeeping.getTimekeeping();		
			String gDB = String.format(gPath, owls.get(index));
			System.out.println(gDB);					
			HSTRevision revisor = new HSTRevision(gDB);		
			System.out.println("End of init graph.");
			revisor.goRevising();
			revisor.shutdown();
			tk.finish(owls.get(index) +" revising ");
			System.out.println("###########################################");
		}	
	}
	
	public static void revisingSingle(String gPath, int index){
		List<String> owls = Tools.getOWLsOfExperimentation();		
		Timekeeping tk = Timekeeping.getTimekeeping();		
		String gDB = String.format(gPath, owls.get(index));
		System.out.println(gDB);					
		HSTRevision revisor = new HSTRevision(gDB);		
		System.out.println("End of init graph.");
		revisor.goRevising();
		revisor.shutdown();
		tk.finish(owls.get(index) +" revising ");
		System.out.println("###########################################");
	}

	//@SuppressWarnings("unused")
	public static void main(String[] args) {
		Timekeeping.begin();
		//String gPath = "neo4j-TBox-HST/%s"; 
		String gPath = "neo4j-db/%s"; 
		int index = 1;
		revisingSingle(gPath, index);
		//revisingAll(gPath);
		Timekeeping.end();
		Timekeeping.showInfo("running ");
	}
}
