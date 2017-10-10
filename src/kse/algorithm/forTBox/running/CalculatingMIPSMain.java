package kse.algorithm.forTBox.running;

import static kse.misc.Tools.getOWLsOfExperimentation;

import java.io.File;
import java.util.List;

import kse.algorithm.Diagnosis;
import kse.algorithm.auxiliaryClass.UnsatTriple;
import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.MUPP;
import kse.misc.Timekeeping;
import kse.neo4j.ver2_1.ExecCypher;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.tooling.GlobalGraphOperations;

/**
 * TBox Diagnosis 主运行类
 * 计算图上的MIPP
 * @author Xuefeng fu
 *
 */
public class CalculatingMIPSMain {	
	GraphDatabaseService graphDB; 
	Diagnosis diag ;
	List<String> owls ;
	
	//public static String gPathFormatter="neo4j-TBox-Scoring/%s";
	public static String gPathFormatter="neo4j-db/%s";
	
	public CalculatingMIPSMain(int i){
		owls = getOWLsOfExperimentation();
		String gPath =String.format(gPathFormatter, owls.get(i));
		System.out.println(i+":"+gPath);
		graphDB= new  GraphDatabaseFactory().newEmbeddedDatabase(gPath  ); 
		diag = Diagnosis.getDiagnosis(graphDB);
	}
	
	public CalculatingMIPSMain(String path){
		graphDB= new  GraphDatabaseFactory().newEmbeddedDatabase(path); 
		diag = Diagnosis.getDiagnosis(graphDB);
	}
	
	public static  void outputDirectory(String path){
		File filesys = new File(path);
		if(filesys.isDirectory()){
			for(File file : filesys.listFiles()){
				System.out.println(file.getAbsolutePath());
			}
		}
	}	
	public List<UnsatTriple> getTriples(){		
		Timekeeping tk = Timekeeping.getTimekeeping();			
		List<UnsatTriple> triples = diag.getUnsatTripleByRelationship();				
		/*for(UnsatTriple triple : triples){
			System.out.println(triple.getUnsat());
		}*/				
		tk.finish();		
		System.out.println("The number of unsatisfiable triple is "+ triples.size()); //triple的数量大于或等于un的
		System.out.println("Get triples cost "+ tk.getRunningTime());
		//System.out.println("***********************************************");	
		return triples;
	}	
	
	public List<UnsatTriple> getTriplesByCypher(){
		List<UnsatTriple> triples = diag.getUnsatTripleByCypher();
		System.out.println("The number of unsatisfiable triple is "+ triples.size());
		return triples;
	}
	
	public List<MUPP> testMUPS(List<UnsatTriple> triples ){				
		Timekeeping time = Timekeeping.getTimekeeping();		
		//List<MUPS> MUPPS = diag.compMUPPsWithLabel(triples);
		List<MUPP> MUPPS = diag.compMUPPs(triples);
		//List<MUPS> MUPPS = diag.compMUPPs();		
		time.finish();		
		System.out.println("The number of MUPPS is "+ MUPPS.size());		
		System.out.println("Computing MUPS with triples cost "+ time.getRunningTime());		
		System.out.println("***********************************************");		
		return MUPPS;
	}
	
	public void testMIPS(List<UnsatTriple> triples){		
		Timekeeping time = Timekeeping.getTimekeeping();		
		//List<MIPS> MIPPS = diag.compMIPPsWithLabel(triples);
		List<MIPP> MIPPS = diag.compMIPPs(triples);
		//List<MIPS> MIPPS = diag.compMIPPs();		
		
		//输出每个MIPS的内容
		/*try(Transaction tx = graphDB.beginTx()){
			for(MIPS mips : MIPPS){
				mips.printMIPP();
				System.out.println("****");
			}
			tx.success();
		}*/
		
		try(Transaction tx = graphDB.beginTx()){
		for(MIPP mips : MIPPS)
		{
			mips.printMIPP();
			System.out.println("****");
		}
		tx.success();
		}
		
		time.finish("Calculating MIPP ");
		System.out.println("The number of MIPPS is "+ MIPPS.size());
		System.out.println("Computing MIPS with triples cost "+ time.getRunningTime());	
	}
	/**
	 * 测试直接通过cypher计算MIPP的函数,
	 * 当本体规模大的时候，性能远低于先求三元组的计算方法，因为先计算三元组降低了搜索空间
	 * 当本体规模小的时候，性能反而好
	 */
	public void testMIPS(){		
		Timekeeping time = Timekeeping.getTimekeeping();		
		List<MIPP> MIPPS = diag.compMIPPs();		
		time.finish();		
		System.out.println("The number of MIPPS is "+ MIPPS.size());
		System.out.println("Computing MIPS with triples cost "+ time.getRunningTime());				
	}
	
	/**
	 * 测试通过先求MUPP再求MIPP的函数
	 * @param MUPPS MUPP集合
	 */
	public void testMIPSbyMUPS(List<MUPP> MUPPS){
		Timekeeping time = Timekeeping.getTimekeeping();		
		List<MIPP> MIPPS = diag.compMIPPsbyMUPPs(MUPPS);		
		time.finish();
		System.out.println("The number of MIPPS is "+ MIPPS.size());
		System.out.println("Computing MIPS with triples cost "+ time.getRunningTime());	
	}
	
	public void printLabel(){
		try(Transaction tx = diag.getGraphDB().beginTx() ){
		    GlobalGraphOperations ggo = GlobalGraphOperations.at(diag.getGraphDB());
		    int numberOfLabel = 0;
		    for(Label lable: ggo.getAllLabels()){
		    	System.out.println(lable);
		    	numberOfLabel++;
		    }
		    if(numberOfLabel>1) {
		    	System.out.println(numberOfLabel);		    	
		    }
		}
	}
	
	public void printNodesWithLabel(String label){
		//MUPS_Airport
		String query = "MATCH (n:%s) RETURN n";
		try(Transaction tx = graphDB.beginTx()){
			ExecutionResult result = ExecCypher.simpleCypher(String.format(query, label), graphDB);
			System.out.println(result.dumpToString());
		}
	}	
	
	public static void main(String[] args){
		
		/*int index = 1;
		CalculatingMIPSMain app = new CalculatingMIPSMain(index);	*/		
		
			
		String gPath = "neo4j-test/Integrated_crs_cmt_test";
		//String gPath = "neo4j-TBox-Scoring/test";
		CalculatingMIPSMain app = new CalculatingMIPSMain(gPath);
		
		Timekeeping tk = Timekeeping.getTimekeeping();
			
		//List<UnsatTriple> triples = app.getTriplesByCypher(); //通过cypher直接找三元组的效率极低
		//app.printLabel();
		//app.printNodesWithLabel("MUPS_Machine");
		//Timekeeping.begin();				
		//app.testMUPS(triples);			
		//Timekeeping.end();
		//Timekeeping.showInfo("Runing MUPPS");			
		//Timekeeping.begin();		
		List<UnsatTriple> triples = app.getTriples();
		//app.testMUPS(triples);	
		app.testMIPS(triples);
		//app.testMIPS();//直接使用cypher查询，效率很低
		//app.testMUPS(triples);//直接使用cypher查询，效率很低
		//app.testMIPSbyMUPS(app.testMUPS(triples));
		tk.finish();
			
		System.out.println("\n \n");
	}
}




