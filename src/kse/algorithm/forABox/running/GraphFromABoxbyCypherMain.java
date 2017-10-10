package kse.algorithm.forABox.running;

import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.COMEFROMSECOND;
import static kse.misc.GlobalParams.CONJUNCTION;
import kse.misc.Timekeeping;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

/**
 * 从本体中构建图，应用的场景一个TBox，第一个ABox，第二个ABox
 * @author Xuefeng fu
 *
 */

public class GraphFromABoxbyCypherMain {
	
	public static void main(String[] args){
		
		Timekeeping.begin();	
		GraphFromOWLbyCypher app;
		
		String ontoFormat = "ForABoxRevi/P%s/univ_%s_%s.owl";	
		String gDBFormat = "neo4j-ABox/UOBM_%s_%s";		

		int[] univNum = new int[]{2, 4, 6, 8, 10};
		//int[] univNum = new int[]{8};
		int percent = 40;     //不交概念占比
		for(int i=0; i<univNum.length; i++){
			String gDB = String.format(gDBFormat, percent, univNum[i]);   //disjoint 20%, 一个大学		
			String pathOfTFormat = "ForABoxRevi/P%s/univ-bench-lite.owl";		
			String pathOfT = String.format(pathOfTFormat, percent);
			System.out.println("Create Graph from TBox:"+ pathOfT);
			Boolean isClear = true;
			
			app = new  GraphFromOWLbyCypher(gDB, pathOfT, CONJUNCTION);
			app.addTBoxToGraphDB(isClear);	
			//app.listPaths();
			app.shutDown();
			
			String pathOfFirstA = String.format(ontoFormat,percent, univNum[i], "1");
			System.out.println("Insert "+ pathOfFirstA +" into graph");
			app = new  GraphFromOWLbyCypher(gDB, pathOfFirstA, COMEFROMFIRST);
			app.addABoxToGraphDB(true, false);	
			//app.listPaths();
			app.shutDown();
			
			String pathOfSecondA = String.format(ontoFormat,percent, univNum[i], "2");
			System.out.println("Insert "+ pathOfSecondA +" into graph");
			app = new  GraphFromOWLbyCypher(gDB, pathOfSecondA, COMEFROMSECOND);
			app.addABoxToGraphDB(true, false);	
			//app.listPaths();
			app.shutDown();
		}
		
		//输出图中的节点信息
		/*app = new  GraphFromOWLbyCypher(gDB, pathOfT, CONJUNCTION);
		app.init(false);		
		//app.listNodes(INDIVIDUALLABEL);		
		//app.listNodes(CONCEPTLABEL);		
		app.listPaths();		
		app.shutDown();*/
		
		Timekeeping.end();
		Timekeeping.showInfo("Handling Graph database ");		
		
	}
}
