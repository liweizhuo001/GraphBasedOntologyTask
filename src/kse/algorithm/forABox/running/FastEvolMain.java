package kse.algorithm.forABox.running;

import kse.algorithm.forABox.revision.FastEvol;
import kse.misc.Timekeeping;
import kse.misc.Tools;

public class FastEvolMain {

	public static String gPathFormat = "neo4j-ABox-FastEval/UOBM_%s_%s";
	
	public static void main(String[] args){
		//int univNum = 6;
		int percent = 40;
		int[] univNum = new int[]{2, 4, 6, 8, 10};
		//int[] univNum = new int[]{2};
		
		for(int i=0; i<univNum.length; i++){
			String gPath = String.format(gPathFormat, percent, univNum[i]);
			Timekeeping.begin();
	
			System.out.println("Loading graph database..."+gPath);
			FastEvol app = FastEvol.getFastEvol (gPath);
			
			//app.computeClosure();
			//List<MIS> MISs =  app.getMIS();			
			/*for(MIS mis : MISs){
				System.out.println(mis);
			}		*/
			//System.out.println(MISs.size());
			
			app.doRevising();
			//ExecCypher.listPaths(app.getGraphDB());
			app.shutdown();
			
			Timekeeping.end();
			Timekeeping.showInfo("FastEvolRevising");
			String info = String.format("[Percent:%s,University:%s]",percent, univNum[i]);
			Tools.saveToFile(Timekeeping.infomation+info, "result_fastEvol.txt", true);
		}
	}
}


