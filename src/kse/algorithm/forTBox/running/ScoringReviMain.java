package kse.algorithm.forTBox.running;

//import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.scoring.RelFreqInMIPPs;
import kse.algorithm.forTBox.debugging.scoring.RevisionSpace;
import kse.algorithm.forTBox.debugging.scoring.ScoringRevision;
import kse.misc.Timekeeping;
import kse.misc.Tools;
import static kse.misc.GlobalParams.*;
import static kse.algorithm.auxiliaryClass.GlobalFunct.*;

/**
 * 基于评分函数的修正方法主函数 
 * 2015-5-29
 * @author Xuefeng Fu
 *
 */

@SuppressWarnings("unused")
public class ScoringReviMain {		
	public static void printRS(Set<RevisionSpace> ros ){
		for(RevisionSpace ro : ros){
			System.out.print(ro.getTimes()+"#[");
			System.out.print(relToStr(ro.getR())+"]#");
			for(Node n  :ro.getBeConnectedNodes()){
				System.out.print(n.getProperty(NAMEPROPERTY)+"      ");
			}			
			System.out.println();
		}
	}
	
	public static void printList(List<Relationship> rels){
		for(Relationship rel:rels){
			System.out.print(relToStr(rel)+"*");
		}
		System.out.println("\n");
	}
	
	public static void revisingSingle(String gPath){
		System.out.println(gPath);			
		ScoringRevision revisor = new ScoringRevision(gPath);		
		System.out.println("End of init graph.");
		revisor.goRevising();
		revisor.shutdown();
	}
	
	public static void revisingSingle(String gPath, int index){
		List<String> owls = Tools.getOWLsOfExperimentation();		
    	String gDB = String.format(gPath, owls.get(index));
		System.out.println(gDB);		
		
		ScoringRevision revisor = new ScoringRevision(gDB);		
		System.out.println("End of init graph.");
		revisor.goRevising();
		revisor.shutdown();
	}
	
	public static void revisingAll(String gPath){		
		List<String> owls = Tools.getOWLsOfExperimentation();		
		for(int index=0;index<owls.size(); index++){
			Timekeeping tk = Timekeeping.getTimekeeping();		
			String gDB = String.format(gPath, owls.get(index));
			System.out.println(gDB);					
			ScoringRevision revisor = new ScoringRevision(gDB);		
			System.out.println("End of init graph.");
			revisor.goRevising();
			revisor.shutdown();
			tk.finish(owls.get(index) +" revising ");
		}		
	}
	
	public static void main(String[] args){
		Timekeeping.begin();
		//String gPath = "neo4j-TBox-Scoring/%s"; 	
		//String gPath = "neo4j-test/%s";
		
		/*String gPath = "neo4j-db/%s";   //只修复单一图数据库中的矛盾，其中index是本体列表中的索引
		int index = 1;
		revisingSingle(gPath, index);*/
		
		//String gPath = "neo4j-test/Integrated_crs_cmt_mappings";
		String gPath = "neo4j-test/Integrated_crs_cmt_test";
		revisingSingle(gPath);
		
		
		//revisingAll(gPath);
		Timekeeping.end();
		Timekeeping.showInfo("running ");
		
		
		/*Set<RevisionSpace> reviSpaces = revisor.getReviSpace();
		for(RevisionSpace reviSpace : reviSpaces){
			System.out.println(reviSpace);
		}*/
		
		/*Map<Relationship, Set<MIPP>> relMappingMIPP = revisor.getRelMappingMIPP();		
		//输出关系到MIPP集合的映射，即一个relationship出现哪些MIPP中
		for(Relationship r : relMappingMIPP.keySet()){
			System.out.println(r + ":"+relMappingMIPP.get(r).size());
		}*/
		
		
		//对原来方法的测试
		/*try(Transaction tx = revisor.getGraphDB().beginTx()){
			Map<Relationship,Set<MIPP>> map = revisor.getRelMappingMIPP(); //与relationship(待修正)关联的MIPS
			for(Relationship r : map.keySet()){
				System.out.println(relToStr(r)+"#"+map.get(r).size());
				for(MIPP mi : map.get(r)){
					System.out.println(mi);
					printList(mi.getDiagnosis());
					printList(mi.getPathToP());
					printList(mi.getPathToN());
					System.out.println("\n**************");
				}
			}	*/	
			
			//Set<RevisionOperator> ros = revision.compRevisionOperator();
			//printRS(ros);					
			//revision.reviseTBox();
		
			//tx.success();
		//}
		

	}
}
