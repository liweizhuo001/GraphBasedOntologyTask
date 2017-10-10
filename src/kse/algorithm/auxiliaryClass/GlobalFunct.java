package kse.algorithm.auxiliaryClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.MUPP;
import kse.misc.Tools;
import kse.owl.OWLInfo;





//import org.neo4j.graphalgo.impl.util.PathImpl;
import org.neo4j.cypher.internal.PathImpl;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

import static kse.misc.GlobalParams.*;

/**
 * 基于图的本体算法中使用的全局函数[2014-5]
 * @author Xuefeng Fu
 */
public class GlobalFunct {
	
	/**
	 * 从某个图数据库中获取否定的节点
	 * @param ggo 图的全局操作类
	 * @return 图中的否定概念节点集合
	 */
	public static List<String> getNegativeNodes(GraphDatabaseService gDB){
		GlobalGraphOperations ggo = GlobalGraphOperations.at(gDB);
		List<String> negtiveConceptions = new ArrayList<>();
		for(Node node : ggo.getAllNodes()){
			String nodeName = node.getProperty(NAMEPROPERTY).toString();
			if(nodeName.startsWith(NEGATIVESIGN)){
				negtiveConceptions.add(nodeName);
			}
		}
		return negtiveConceptions;
	}
	//取出所有的节点
	public static List<Node> getAllNodes(GraphDatabaseService gDB){
		List<Node> nodes = new ArrayList<>();
		GlobalGraphOperations ggo = GlobalGraphOperations.at(gDB);
		for(Node node : ggo.getAllNodes()){
			nodes.add(node);
		}
		return nodes;
	}
	
	//取出所有的否定节点(negative_)
	public static List<Node> getAllNegNode(GraphDatabaseService gDB){
		List<Node> nodes = getAllNodes(gDB);
		List<Node> negNodes = new ArrayList<>();
		for(Node node: nodes){
			String nodeName = node.getProperty(NAMEPROPERTY).toString();
			if(nodeName.startsWith(NEGATIVESIGN))
				negNodes.add(node);
		}
		return negNodes;
	}
	
	//取出所有的NIs(其实就是disjointness公理)
	public static List<Relationship> getAllNIs(GraphDatabaseService gDB){
		List<Relationship> rels = new ArrayList<>();
		List<Node> negNodes = getAllNegNode(gDB);
		for(Node negNode:negNodes){
			for(Relationship rel :negNode.getRelationships()){
				rels.add(rel);
			}
		}
		return rels;
		
	}

	/**
	 * 从图中找不可满足概念对
	 * @param gDB
	 * @return
	 */
	public static List<DisjointPair> getDisjointPair(GraphDatabaseService gDB){
		List<DisjointPair> disjointPair = new ArrayList<>();
		try(Transaction tx = gDB.beginTx()){
			//System.out.println(gDB.index().existsForNodes(NODEINDEX));
			//Index<Node> nodeIndex = gDB.index().forNodes(NODEINDEX);
			GlobalGraphOperations ggo = GlobalGraphOperations.at(gDB);		
			List<String> allNodes = new ArrayList<>();
			List<String> negativeNodes = new ArrayList<>();
			for(Node node : ggo.getAllNodes())
			{
				String nodeName = node.getProperty(NAMEPROPERTY).toString();
				allNodes.add(nodeName);
				if(nodeName.startsWith(NEGATIVESIGN)){
					negativeNodes.add(nodeName);
				}
			}			
			for(String negNode : negativeNodes)
			{
				String posNode = Tools.removeNegativeToken(negNode);
				//System.out.println(posNode+"->"+negNode);
				if(allNodes.contains(posNode))
				{				
					disjointPair.add(new DisjointPair(posNode, negNode));
				}
			}
			tx.success();
		}		
		return disjointPair;		
	}
	
	/**
	 * 从图中找不可满足概念对
	 * @param gDB
	 * @return
	 */
	public static List<DisjointPairForMappings> getDisjointPairForMappings(GraphDatabaseService gDB){
		//List<DisjointPair> disjointPair = new ArrayList<>();
		List<DisjointPairForMappings> disjointPairForMappings = new ArrayList<>();
		try(Transaction tx = gDB.beginTx()){
			//System.out.println(gDB.index().existsForNodes(NODEINDEX));
			//Index<Node> nodeIndex = gDB.index().forNodes(NODEINDEX);
			GlobalGraphOperations ggo = GlobalGraphOperations.at(gDB);		
			List<String> allNodes = new ArrayList<>();
			//List<String> negativeNodes = new ArrayList<>();
			//Map<String,String> negativeNodes = new HashMap<String,String>();
			//Map<String,String> negativeNodes = new HashMap<String,String>();
			ArrayList<String> negativeNodes = new ArrayList<String>();
			ArrayList<String> comefroms = new ArrayList<String>();
			
			for(Node node : ggo.getAllNodes())
			{
				String nodeName = node.getProperty(NAMEPROPERTY).toString();
				String comefrome = node.getProperty(COMEFROMPROPERTY).toString();
				allNodes.add(nodeName);
				if(nodeName.startsWith(NEGATIVESIGN))
				{
					//negativeNodes.add(nodeName);
					negativeNodes.add(nodeName);
					comefroms.add(comefrome);
				}
			}		
			for(int i=0; i<negativeNodes.size();i++)
			{
				String posNode = Tools.removeNegativeToken(negativeNodes.get(i));
				//System.out.println(posNode+"->"+negNode);
				if(allNodes.contains(posNode))
				{				
					disjointPairForMappings.add(new DisjointPairForMappings(posNode, negativeNodes.get(i),comefroms.get(i)));
				}
			}
			/*for(String negNode : negativeNodes.keySet())
			{
				String posNode = Tools.removeNegativeToken(negNode);
				//System.out.println(posNode+"->"+negNode);
				if(allNodes.contains(posNode))
				{				
					disjointPairForMappings.add(new DisjointPairForMappings(posNode, negNode,negativeNodes.get(negNode)));
				}
			}*/
			tx.success();
		}		
		return disjointPairForMappings;		
	}
	
	/**
	 * 从本体中直接找不可满足概念对
	 * @param owlPath owl文件路径
	 * @return disjointness concept pair
	 */
	public static List<DisjointPair> getDisjointPair(String owlPath){
		List<DisjointPair> disjointPair = new ArrayList<>();
		Set<OWLDisjointClassesAxiom> axioms= OWLInfo.getDisjointness(owlPath);
		//System.out.println(axioms.size());
		for(OWLDisjointClassesAxiom axiom : axioms){
			List<OWLClassExpression> classes = axiom.getClassExpressionsAsList();		
			OWLClassExpression c1 = classes.get(0);
			OWLClassExpression c2 = classes.get(1);
			if((c1 instanceof OWLClass) && (c2 instanceof OWLClass)){
				String key = ((OWLClass)c1).getIRI().getFragment();
				String value = ((OWLClass)c2).getIRI().getFragment();
				disjointPair.add(new DisjointPair(key,value));
			}
		}
		return disjointPair;		
	}	
	
	/**
	 * 使用递归来获取某个节点的所有子节点,包括自己
	 * @param node 目标节点
	 * @return
	 */
	public static Set<String> getDescendantNodesWithRecursion(Node node){
		Set<String> nodesInName = new HashSet<String>();		
		//System.out.println(node.getProperty(NAMEPROPERTY).toString());
		//Tools.saveToFile(node.getProperty(NAMEPROPERTY).toString(), "output",true);
		nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		
		Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);
		
		if(inRelationships.iterator().hasNext()){ //是否有入度
			for(Relationship inRel : inRelationships){
				System.out.println(inRel.toString());
				System.out.println(inRel.getStartNode().getProperty(NAMEPROPERTY)+" "+inRel.getEndNode().getProperty(NAMEPROPERTY));
				if(inRel.getStartNode().getProperty(NAMEPROPERTY).equals(inRel.getEndNode().getProperty(NAMEPROPERTY))){
					continue;  //自己指向自己
				}
				Set<String> descendantNodes = getDescendantNodes(inRel.getStartNode());
				for(String descendantNode : descendantNodes){
					if(!(nodesInName.contains(descendantNode))){
						nodesInName.add(descendantNode);
					}
				}
				//nodesInName.addAll(getDescendantNodesWithRecursion(inRel.getStartNode()));
			}
		}		
		return nodesInName;
	}
	
	/**
	 * 不使用递归的方式来取所有的子类
	 * @param node
	 * @return
	 */
	public static Set<String> getDescendantNodes(Node node){
		Set<String> nodesInName = new HashSet<String>();		
		nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		
		Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);
		List<Relationship> undone = new ArrayList<>();
		List<Long> done = new ArrayList<>();		
		if(inRelationships.iterator().hasNext()){ //是否有入度
			for(Relationship inRel : inRelationships){
				undone.add(inRel);
			}
		}		
		while(undone.size()>0){
			Relationship rel = undone.get(0);
			undone.remove(0);
			
			done.add(rel.getId());
			
			Node start = rel.getStartNode();
			nodesInName.add(start.getProperty(NAMEPROPERTY).toString());
			
			Iterable<Relationship> inStartRelationships = start.getRelationships(Direction.INCOMING);
			for(Relationship inRel : inStartRelationships){
				if(!(done.contains(inRel.getId()))){
					undone.add(inRel);
				}
			}
		}		
		return nodesInName;
	}
	
	public static Set<Node> getDescendantNodesWithRecursionForMappings(Node node){
		Set<Node> nodesInName = new HashSet<Node>();		
		//System.out.println(node.getProperty(NAMEPROPERTY).toString());
		//Tools.saveToFile(node.getProperty(NAMEPROPERTY).toString(), "output",true);
		nodesInName.add(node);
		
		Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);
		
		if(inRelationships.iterator().hasNext()){ //是否有入度
			for(Relationship inRel : inRelationships){
				/*if(inRel.getStartNode().getProperty(NAMEPROPERTY).equals(inRel.getEndNode().getProperty(NAMEPROPERTY))){
					continue;  //自己指向自己
				}*/
				if(inRel.getStartNode().getProperty(NAMEPROPERTY).equals(inRel.getEndNode().getProperty(NAMEPROPERTY))
						&&inRel.getStartNode().getProperty(COMEFROMPROPERTY).equals(inRel.getEndNode().getProperty(COMEFROMPROPERTY)))
				{
					continue;  //自己指向自己
				}
				Set<Node> descendantNodes = getDescendantNodesForMappings(inRel.getStartNode());
				for(Node descendantNode : descendantNodes){
					if(!nodesInName.contains(descendantNode))
					{
						nodesInName.add(descendantNode);
					}
				}
				//nodesInName.addAll(getDescendantNodesWithRecursion(inRel.getStartNode()));
			}
		}		
		return nodesInName;
	}
	
	/**
	 * 不使用递归的方式来取所有的子类
	 * @param node
	 * @return
	 */
	public static Set<Node> getDescendantNodesForMappings(Node node){
		Set<Node> nodesInName = new HashSet<Node>();		
		nodesInName.add(node);		
		Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);
		List<Relationship> undone = new ArrayList<>();
		List<Long> done = new ArrayList<>();		
		if(inRelationships.iterator().hasNext()){ //是否有入度
			for(Relationship inRel : inRelationships){
				undone.add(inRel);
			}
		}		
		while(undone.size()>0){
			Relationship rel = undone.get(0);
			undone.remove(0);
			
			done.add(rel.getId());
			
			Node start = rel.getStartNode();
			nodesInName.add(start);
			
			Iterable<Relationship> inStartRelationships = start.getRelationships(Direction.INCOMING);
			for(Relationship inRel : inStartRelationships){
				if(!(done.contains(inRel.getId()))){
					undone.add(inRel);
				}
			}
		}		
		return nodesInName;
	}
	
	/**
	 * 使用递归来获取某个节点的所有子节点,包括自己
	 * @param node 目标节点
	 * @return
	 */
	public static List<String> getDescendantNodesInList(Node node){
		List<String> nodesInName = new ArrayList<String>();		
		if(node !=null){
			nodesInName.add(node.getProperty(NAMEPROPERTY).toString());		
			Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);		
			if(inRelationships.iterator().hasNext()){ //是否有入度
				for(Relationship inRel : inRelationships){				
					nodesInName.addAll(getDescendantNodesInList(inRel.getStartNode()));
				}
			}		
		}
		return nodesInName;
	}

	
	/**
	 * 使用递归来获取某个节点的所有祖先节点
	 * @param node 目标节点
	 * @return
	 */
	public static Set<String> getAncestorNode(Node node){
		Set<String> nodesInName = new HashSet<String>();		
		nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels){
				nodesInName.addAll(getAncestorNode(outRel.getEndNode()));
			}
		}		
		return nodesInName;
	}
	
	/**
	 * 使用递归来获取某个节点的所有祖先节点(保证其是从子到父的一种递增状态)
	 * @param node 目标节点
	 * @return
	 */
	public static ArrayList<String> getOrderedAncestorNode(Node node)
	{
		ArrayList<String> nodesInName = new ArrayList<String>();		
		nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels){
				nodesInName.addAll(getAncestorNode(outRel.getEndNode()));
			}
		}		
		return nodesInName;
	}
	
	/**
	 * 使用递归来获取某个节点的所有父亲节点
	 * @param node 目标节点
	 * @return
	 */
	public static ArrayList<String> getOrderedFatherNode(Node node){
		ArrayList<String> nodesInName = new ArrayList<String>();		
		nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels){   //如果是图的话父亲结点可能不止一个
				nodesInName.add(outRel.getEndNode().getProperty(NAMEPROPERTY).toString());
			}
		}		
		return nodesInName;
	}
	
	public static Set<String> getFatherNodeName(Node node){
		Set<String> nodesInName = new HashSet<String>();		
		//nodesInName.add(node.getProperty(NAMEPROPERTY).toString());
		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels){   //如果是图的话父亲结点可能不止一个
				nodesInName.add(outRel.getEndNode().getProperty(NAMEPROPERTY).toString());
			}
		}		
		return nodesInName;
	}
	
	public static List<Node> getFatherNode(Node node){
		List<Node> nodesInName = new ArrayList<Node>();		

		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels)   //如果是图的话父亲结点可能不止一个
			{
					nodesInName.add(outRel.getEndNode());
			}
		}		
		return nodesInName;
	}
	
	public static List<Node> getFatherNodeSignleSource(Node node){
		List<Node> nodesInName = new ArrayList<Node>();		
		//nodesInName.add(node);
		String comefrom=node.getProperty(COMEFROMPROPERTY).toString();
		Iterable<Relationship> outRels = node.getRelationships(Direction.OUTGOING);
		if(outRels.iterator().hasNext()){ //是否有入度
			for(Relationship outRel : outRels)   //如果是图的话父亲结点可能不止一个
			{
				String source=outRel.getEndNode().getProperty(COMEFROMPROPERTY).toString();
				if(comefrom.equals(source)) //保证来源相同
					nodesInName.add(outRel.getEndNode());
			}
		}		
		return nodesInName;
	}
	
	
	
	/**
	 * Relationship转为字符串：StartNode.Name->EndNode.Name
	 * @param rel
	 * @return
	 */
	public static String relToStr(Relationship rel){
		return rel.getStartNode().getProperty(NAMEPROPERTY)+"->" +
					   rel.getEndNode().getProperty(NAMEPROPERTY) + "[" +
					   rel.getProperty(COMEFROMPROPERTY) +"]";
	}
	
/*	public static void printRelationship(Relationship r, boolean isEnter){
		String rFormatter = "%s-->%s";
		String pStr = String.format(rFormatter, r.getStartNode().getProperty(NAMEPROPERTY),r.getEndNode().getProperty(NAMEPROPERTY));
		if(isEnter)
				System.out.println(pStr);
		else
			System.out.println(pStr);
	}*/
	
	/**
	 * 判断两个关系是否相同，只要两个关系起点和终点Name属性值相同，就一样
	 * @param r1
	 * @param r2
	 * @return
	 */
	public static boolean isRelEquals(Relationship r1, Relationship r2){
		String r1_s = r1.getStartNode().getProperty(NAMEPROPERTY).toString();
		String r1_e = r1.getEndNode().getProperty(NAMEPROPERTY).toString();
		String r2_s = r2.getStartNode().getProperty(NAMEPROPERTY).toString();
		String r2_e = r2.getEndNode().getProperty(NAMEPROPERTY).toString();
		if((r1_s.equals(r2_s))&&(r1_e.equals(r2_e))){
			return true;
		}
		else{ 
			return false;
		}
	}
	
	public static boolean isNodeEquals(Node n1, Node n2){
		if(n1==null || n2 == null)
			return false;
		String n1Name = n1.getProperty(NAMEPROPERTY).toString();
		String n2Name = n2.getProperty(NAMEPROPERTY).toString();		
		if(n1Name.equals(n2Name)){
			return true;
		}
		else{ 
			return false;
		}
	}
	
	/**
	 * 判断一个Relationship set中是否含有某个关系
	 * @param rels Relationship set
	 * @param r target Relationship
	 * @return 
	 */
	public static boolean isContain(List<Relationship> rels, Relationship r){
		for(Relationship rel:rels){
			if(isRelEquals(rel,r)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 标注一个概念的所有父类
	 * 将一个概念和它的所有父节点(祖先节点)归为一个Label.
	 */
	public static void labelAncestors(Node node, Label label){				
		if(node!=null && !(node.hasLabel(label))){
			node.addLabel(label);			
			Iterable<Relationship> outRelationships = node.getRelationships(Direction.OUTGOING);
			if(outRelationships.iterator().hasNext()){
				for(Relationship out:outRelationships){
					Node supNode = out.getEndNode();
					labelAncestors(supNode,label);
				}
			}
		}
	}
	
	/**
	 * 标注一个节点的所有后代节点
	 */
	
	public static void labelDescendants(Node node, Label label){
		if(node!=null && !(node.hasLabel(label))){
			node.addLabel(label);			
			Iterable<Relationship> inRelationships = node.getRelationships(Direction.INCOMING);
			if(inRelationships.iterator().hasNext()){
				for(Relationship in : inRelationships){
					Node subNode = in.getStartNode();
					labelDescendants(subNode,label);
				}
			}
		}
	}
	
	/**
	 * 判断一个节点的Name属性是否是 "existence_" 类型
	 * @param node
	 * @return
	 */
	public static boolean isExistenceNode(Node node){
		String nodeName = node.getProperty(NAMEPROPERTY).toString();
		if(nodeName.startsWith(EXISTENCESIGN)){
			return true;
		}
		else
			return false;
	}
	
	/**
	 * 取出MUPS中的公共边就是mips
	 * mups中没有公共边的就是mips，这个可以用在从mups集合中找mips
	 * @param mups 
	 * @return mips
	 */
	public static MIPP uToI(MUPP mups){
		Set<String> nodes = new HashSet<>();
		List<Relationship> pp = new ArrayList<>();
		List<Relationship> np = new ArrayList<>();
		List<Relationship> ppInMUPS = mups.getPathToP();
		List<Relationship> npInMUPS = mups.getPathToN();
		boolean notJointness;
		Relationship inNp = null;
		Relationship inPp = null;
		for(Relationship r1: ppInMUPS){
			notJointness = true;
			inPp = r1;
			for(Relationship r2:npInMUPS){
				inNp = r2;
				if(isRelEquals(r1, r2)){
					notJointness = false;
					break;
				}
			}
			if(notJointness){
				pp.add(inPp);
				np.add(inNp);
				nodes.add(inPp.getStartNode().getProperty(NAMEPROPERTY).toString());
				nodes.add(inPp.getEndNode().getProperty(NAMEPROPERTY).toString());
				nodes.add(inNp.getStartNode().getProperty(NAMEPROPERTY).toString());
				nodes.add(inNp.getEndNode().getProperty(NAMEPROPERTY).toString());
			}
		}		
		return new MIPP(pp,np,nodes);
	}
	
	/**
	 * 取出路径中的关系Relationship序列
	 * @param path 目标路径
	 * @return 关系列表
	 */
	public static List<Relationship> pathToList(PathImpl path){
		List<Relationship> relInPath = new ArrayList<>();
		for(Relationship rel : path.relationships()){
			//System.out.print(GlobalFunct.relToStr(rel)+"->");
			relInPath.add(rel);
		}
		//System.out.println("\n");
		return relInPath;
	}
	
	/**
	 * 输出Cypher查询的路径
	 * @param path Cypher查询的路径
	 */
	public static void printPath(PathImpl path) {
		System.out.println(path);
		for(Node n:path.nodes()){
			System.out.print(n.getProperty(NAMEPROPERTY)+"*");
		}
		System.out.println();
		for(Relationship rel : path.relationships()){
			Node start = rel.getStartNode();
			Node end = rel.getEndNode();
			String comefrom = rel.getProperty(COMEFROMPROPERTY).toString();
			System.out.println(start.getProperty(NAMEPROPERTY)+"->"+end.getProperty(NAMEPROPERTY)+"*"+comefrom);
		}
		System.out.println();
	}
	
	/**
	 * 获取MUPP中的节点集合
	 * @param path1 positive path
	 * @param path2 negative path
	 * @return 节点集合
	 */
	public static Set<String> nodesToList(PathImpl path1,PathImpl path2){
		Set<String> nodes = new HashSet<>();
		for(Node n:path1.nodes()){			
			nodes.add(n.getProperty(NAMEPROPERTY).toString());
		}
		for(Node n:path2.nodes()){			
			nodes.add(n.getProperty(NAMEPROPERTY).toString());
		}
		return nodes;
	}
	
	public static Set<String> nodesToList(PathImpl path){
		Set<String> nodes = new HashSet<>();
		for(Node n:path.nodes()){			
			nodes.add(n.getProperty(NAMEPROPERTY).toString());
		}		
		return nodes;
	}
	
	/**
	 * 判断一个路径中是否有来源为First的实例断言
	 * @param path : 路径
	 * @return 
	 */
	public static boolean hasFirstIndividual(PathImpl path){
		for(Relationship rel : path.relationships()){
			if(rel.getType().name().equalsIgnoreCase(MEMBEROFREL) 
					&& rel.getProperty(COMEFROMPROPERTY).toString().equalsIgnoreCase(COMEFROMFIRST)){
				//System.out.println(rel.getStartNode().getProperty(NAMEPROPERTY).toString());
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 取出要删除的路径，在求闭包的环境下，可能出现Second和NewAdd
	 * @param path
	 * @return
	 */
	public static PathImpl getIndividualForDel(PathImpl path1, PathImpl path2){
		
		for(Relationship rel : path1.relationships()){//path1是否来源于first
			if(rel.getType().name().equalsIgnoreCase(MEMBEROFREL) 
					&& rel.getProperty(COMEFROMPROPERTY).toString().equalsIgnoreCase(COMEFROMFIRST)){
				return path1;
			}
		}
		for(Relationship rel : path2.relationships()){//path2是否来源于first
			if(rel.getType().name().equalsIgnoreCase(MEMBEROFREL) 
					&& rel.getProperty(COMEFROMPROPERTY).toString().equalsIgnoreCase(COMEFROMFIRST)){
				return path2;
			}
		}
		for(Relationship rel : path1.relationships()){//path1是否来源于first
			if(rel.getType().name().equalsIgnoreCase(MEMBEROFREL) 
					&& rel.getProperty(COMEFROMPROPERTY).toString().equalsIgnoreCase(COMEFROMNEW)){
				return path1;
			}
		}
		return path2;
	}
	
	public static String getMUPSLabel(String conceptName){
		String label = "MUPS_"+conceptName;
		return label.replace("-", "_");
	}	
	
	public static String getMUPSLabel(String ucName, String name){
		String label = "MUPS_"+"ucName"+"_"+name;
		return label.replaceAll("-", "_");
	}	
	
	/**
	 * 将MIPP集合中的所有Relationship取出放到List中，不要重复添加
	 * @param mipps
	 * @return
	 */
	public static List<Relationship> transMIPPstoRels(List<MIPP> mipps){
		Set<Relationship> _rels = new HashSet<>();
		List<Relationship> rels = new ArrayList<>();
		for(MIPP mi : mipps){
			_rels.addAll(mi.getDiagnosis());
		}
		while( _rels.iterator().hasNext()){ 
			rels.add(_rels.iterator().next());
		}
		return rels;
	}
	
	/**
	 * 将mipp中的关系（公理）转换成对应与关系列表中的集合
	 * @param mipp
	 * @param rels
	 * @return
	 */
	public static List<Integer> tranRelationshipToInteger(MIPP mipp, List<Relationship>  rels){
		List<Integer> mippInInt = new ArrayList<>();
		for(Relationship relInMIPP : mipp.getDiagnosis()){
			for(int index=0; index<rels.size(); index++){
				if(isRelEquals(relInMIPP, rels.get(index))){
					mippInInt.add(index);
					break;
				}
			}			
		}		
		return mippInInt;
	}
	
	/**
	 * 获取目标关系在集合中的位置
	 * @param rels 关系集合
	 * @param r      目标关系
	 * @return         目标关系在集合中的index
	 */
	
	public static  int indexOf(List<Relationship> rels, Relationship r){
		int index=-1; //如果返回-1，则在list中没有该relationship
		boolean isExist = false;
		for(Relationship rel:rels){
			++index;
			if(GlobalFunct.isRelEquals(rel,r)){
				isExist = true;
				break;
			}
		}
		if(isExist){
			return index;
		}
		else{
			return -1;
		}
	}
	
	
}






