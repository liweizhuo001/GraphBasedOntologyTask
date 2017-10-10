package kse.neo4j.running;

import java.util.List;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.misc.Timekeeping;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.tooling.GlobalGraphOperations;

import static kse.misc.GlobalParams.*;


public class Neo4jDemo {	
	
	//图数据库节点的测试
	public static  void outputNodes(GlobalGraphOperations ggo){
		int i=0;
		for (Node node : ggo.getAllNodes()) {
			++i;
			
			if (node.hasProperty(NAMEPROPERTY)){
				for(Label label : node.getLabels()){
					System.out.print(label+"  :  ");
				}
				System.out.println(node);
				
				System.out.println(node.getProperty(NAMEPROPERTY) +"  "+node.getProperty(COMEFROMPROPERTY));
				//输出实例节点
				/*if(node.hasProperty(TYPEPROPERTY) && node.getProperty(TYPEPROPERTY).equals(INDIVIDUALTYPE)){
					System.out.println(node.getProperty(NAMEPROPERTY));
					for(Relationship relation :node.getRelationships()){
						System.out.println(node.getProperty(NAMEPROPERTY)+"-->"+relation.getEndNode().getProperty(NAMEPROPERTY));
					}
				}*/
			}			
		}
		System.out.println(String.format("Number of nodes is %d", i));
	}
	
	//图数据库关系的测试
	public static void outputRelationships(GlobalGraphOperations ggo){
		int i=0;
		for(Relationship rel :  ggo.getAllRelationships()){
			++i;
			System.out.println(rel);
			System.out.println(rel.getStartNode().getProperty(NAMEPROPERTY)+" "+rel.getEndNode().getProperty(NAMEPROPERTY));
			System.out.println(rel.getProperty(TYPEPROPERTY).toString()+ "  "+rel.getProperty(COMEFROMPROPERTY)+"  "+rel.getType());
			//System.out.println(rel.getType());
			if(rel.hasProperty(WEIGHTEDPROPERTY))  //判断是否有这个属性
			{
				System.out.println(rel.getProperty(WEIGHTEDPROPERTY).toString());
			}
		}
		System.out.println(String.format("Number of relationship is %d", i));
	}

	//索引测试
	public static void indexTest(GraphDatabaseService  graphDb, String nodeName){		
		System.out.println("In index testing...");
		Index<Node> nodeIndex = graphDb.index().forNodes(NODEINDEX);
		Node node = nodeIndex.query(NAMEPROPERTY, nodeName).getSingle();
		if(node!=null){
			//System.out.println(node.getId());
			System.out.println(node.getProperty(COMEFROMPROPERTY));
			System.out.println(node.getProperty(NAMEPROPERTY));
			System.out.println(node.getProperty(TYPEPROPERTY));
		}
		else{
			System.out.println("no node found.");
		}
	}
	
	//输出所有的否定节点
	public static void outputNegativeNodes(GraphDatabaseService ggo){
		List<String> negativeNodes = GlobalFunct.getNegativeNodes(ggo);
		for(String negativeNode : negativeNodes){
			System.out.println(negativeNode);
		}
		
		List<DisjointPair> negMap = GlobalFunct.getDisjointPair(ggo);
		for(DisjointPair posNode : negMap){
			System.out.println(posNode.getFirst() + " --> " + posNode.getSecond()); 
		}
	}
	
	public static void main(String[] args) {
		Timekeeping.begin();
		//String gDBPath = "neo4j-db/OntoTBoxDiag";
		//String gDBPath = "neo4j-TBox-Scoring/aeo";
		//String gDBPath = "neo4j-TBox-Scoring/chemical2";
		//String gDBPath = "neo4j-TBox-Scoring/test";
		//String gDBPath = "neo4j-test/Integrated_crs_cmt_test";
		//String gDBPath = "neo4j-test/Integrated_crs_cmt_mappings";
		//String gDBPath = "neo4j-test/Integrated_crs_cmt_Moremappings";
		//String gDBPath = "neo4j-test/Integrated_cmt_confof_mappings";
		String gDBPath = "neo4j-test/Integrated-cmt-confof-test";
		//String gDBPath = "neo4j-db/UOBM";
		GraphDatabaseService  graphDb =  new GraphDatabaseFactory().newEmbeddedDatabase( gDBPath );
		GlobalGraphOperations ggo= GlobalGraphOperations.at(graphDb);
		try(Transaction tx = graphDb.beginTx()){			
			outputNodes(ggo);  									//输出所有的节点		
			outputRelationships(ggo); 					    //输出所有的关系		
			//indexTest(graphDb,"chair");
			//outputNegativeNodes(graphDb);
		 }
		
		graphDb.shutdown();		
		Timekeeping.end();
		Timekeeping.showInfo("Graph database test in ");
	}
}
