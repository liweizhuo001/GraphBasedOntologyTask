package kse.algorithm.forTBox.running;

import static kse.misc.Tools.getOWLsOfExperimentation;

import java.util.ArrayList;
import java.util.List;

//import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.neo4j.ver1_8.Tools4Graph;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.tooling.GlobalGraphOperations;

import static kse.misc.GlobalParams.*;

public class GlobalFunctDemo {
	
	GraphDatabaseService graphDb ; 
	Index<Node> indexForNode;
	
	public GlobalFunctDemo(int index){
		List<String> owls = getOWLsOfExperimentation();
		
		//String graphFormatter = "neo4j-db-2/%s";
		String graphFormatter = "neo4j-db/%s";
		String owl = owls.get(index);
		String gPath =  String.format(graphFormatter, owl);
		
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( gPath ); 
		Tools4Graph.registerShutdownHook(graphDb);  
	}
	
	public void hasSameNode(){
		//判断是否存在相同的节点
		List<String> nodesInName = new ArrayList<>();
		List<String> namesClone = new ArrayList<>();
		try(Transaction tx = graphDb.beginTx()){
			GlobalGraphOperations ggo = GlobalGraphOperations.at(graphDb);			
			for(Node node : ggo.getAllNodes()){
				String name = node.getProperty(NAMEPROPERTY).toString();
				namesClone.add(name);
				nodesInName.add(name);				
			}
		}
		for(int i=0; i<nodesInName.size()-1; i++){
			String currentName = nodesInName.get(i);
			System.out.println("#"+i);
			for(int j=i+1;j<nodesInName.size(); j++){
				String temp = nodesInName.get(j);
				if(currentName.equalsIgnoreCase(temp)){
					System.out.println(":"+currentName);
				}
			}
		}
	}
	
	public void testSomeNodeRelation(String nodeName){
		try(Transaction tx = graphDb.beginTx()){
			indexForNode = graphDb.index().forNodes(NODEINDEX);
			Node node = indexForNode.get(NAMEPROPERTY, nodeName).getSingle();
			if(node!=null){
			    Iterable<Relationship> rels = node.getRelationships(Direction.INCOMING);
			    for(Relationship rel : rels){
			    	System.out.println(rel.getStartNode().getProperty(NAMEPROPERTY));
			    }
			}
		}
	}
	
	public static void main(String[] args){
		
		int index = 1;
		GlobalFunctDemo app = new GlobalFunctDemo(index);
		
		
		String nodeName = "ChemicalElement";
		app.testSomeNodeRelation(nodeName);
	}
}



