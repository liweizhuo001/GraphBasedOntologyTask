package kse.algorithm.forABox.running;

import java.util.List;

import kse.algorithm.forABox.revision.GraphRevi;
import kse.algorithm.forABox.revision.MIS;
import kse.misc.Timekeeping;
import kse.misc.Tools;
/**
 * ABox修正主程序[2014-8]
 * @author Xuefeng Fu
 */
public class GraphReviMain {
	public static String gPathFormat = "neo4j-ABox/UOBM_%s_%s";
	
	public static void main(String[] args){
		//int univNum = 6;
		int percent = 40;
		//int[] univNum = new int[]{2, 4, 6, 8, 10};
		int[] univNum = new int[]{10};
		
		for(int i=0; i<univNum.length; i++){
			String gPath = String.format(gPathFormat, percent, univNum[i]);
			Timekeeping.begin();
	
			System.out.println("Loading graph database..."+gPath);
			GraphRevi app = GraphRevi.getGraphRevi (gPath);
			
			List<MIS> MISs =  app.getMIS();			
			for(MIS mis : MISs){
				System.out.println(mis);
			}		
			//System.out.println(MISs.size());
			
			//app.doRevising();
			//ExecCypher.listPaths(app.getGraphDB());
			app.shutdown();
			
			Timekeeping.end();
			Timekeeping.showInfo("Revising");
			String info = String.format("[Percent:%s,University:%s]",percent, univNum[i]);
			Tools.saveToFile(Timekeeping.infomation+info, "result.txt", true);
		}
	}

}
