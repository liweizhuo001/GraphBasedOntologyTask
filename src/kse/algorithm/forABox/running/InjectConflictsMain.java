package kse.algorithm.forABox.running;

import kse.algorithm.forABox.preprocessing.InjectConflicts;
import kse.misc.Timekeeping;

public class InjectConflictsMain {	
	
	public static void main(String[] args){
		
		Timekeeping.begin();		

		int percent = 40;
		int disjointNum = 1000;
		int[] univNum = new int[]{2, 4, 6, 8, 10};
		boolean ifHandlingGraph = true;
		InjectConflicts app = new InjectConflicts(percent, disjointNum);
		for(int i=0; i<univNum.length; i++){
			//只有第一次才处理TBox
			if(i!=0){
				ifHandlingGraph = false;
			}			
			app.go(univNum[i], ifHandlingGraph);
		}
		Timekeeping.end();
		Timekeeping.showInfo("Running ");
	}
}
