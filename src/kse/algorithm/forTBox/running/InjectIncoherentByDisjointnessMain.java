package kse.algorithm.forTBox.running;

import java.util.List;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.algorithm.forTBox.preprocessing.InjectIncoherentByDisjointness;
import kse.misc.Timekeeping;
import static kse.misc.GlobalParams.*;
import static kse.misc.Tools.*;


public class InjectIncoherentByDisjointnessMain {
	
	public static void main(String[] args){
		String owlFormatter = "ClassicOntology/%s.owl";
		List<String> owls = getOWLsOfExperimentation();
		List<String> prefixs = getPrefixes();
		int index = 1;  //待注入不相交公理本体的索引
		int disjointNumber = 3;                                //不相交公理，不能太多，否则不可满足概念太多
		InjectIncoherentByDisjointness.MAXRECURSION = 1;   //父类递归的层数，越大生成的关系越复杂
		boolean isClear = true;
		
		String owl = owls.get(index);
		String owlPath = String.format(owlFormatter, owl);
		String dbPath = String.format("neo4j-db/%s", owl);  //对应的图数据库
		String prefix = prefixs.get(index);
		
		Timekeeping tk = Timekeeping.getTimekeeping();
		
		InjectIncoherentByDisjointness app = new InjectIncoherentByDisjointness(owlPath, dbPath, disjointNumber, isClear,prefix);
		app.injectingDisjoint();
		test(app);
		tk.finish();
	}

	public static void test(InjectIncoherentByDisjointness app){
		GraphDatabaseService graphDB = app.getGraphDB();
		try(Transaction tx = graphDB.beginTx()){
			List<Node> nodes = GlobalFunct.getAllNodes(app.getGraphDB());
			System.out.println(nodes.size());
			int index =20;
			Node n = nodes.get(index);
			System.out.println(n.getProperty(NAMEPROPERTY));
			for(Relationship rel : n.getRelationships(Direction.OUTGOING)){
				System.out.println(rel.getEndNode().getProperty(NAMEPROPERTY));
			}
		}
	}
}
