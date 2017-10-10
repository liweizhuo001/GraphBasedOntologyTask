package kse.algorithm.forTBox.preprocessing;

import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.CONCEPTLABEL;
import static kse.misc.GlobalParams.CONCEPTTYPE;
import static kse.misc.GlobalParams.INCLUDEDREL;
import static kse.misc.GlobalParams.NAMEPROPERTY;
import static kse.misc.GlobalParams.NEGATIVESIGN;
import static kse.misc.Tools.getNegativeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * 使用随机方式生成不协调本体<br>
 * 通过将一个概念的不同父类设置为disjointness来引入incoherence.<br>
 * 将TBox转换成图，再根据图中概念的关系，添加不相交公理，产生TBox的不协调，再将公理写入TBox和图中
 * 过程：先取出图中所有的节点，随机找出一个节点，取出它不同分支上的所有父类，随机取两个父类，添加disjointness axiom
 * [2014-4]
 * @author Xuefeng fu
 */
public class InjectIncoherentByDisjointness {	
	String owlPath;
	String owlFileName;
	String dbPath;
	OWLOntology ontology;
	GraphFromOWLbyCypher gTool; 
	int disjointness;                                         //要注入的不相交公理数目
	GraphDatabaseService graphDB;	
	OWLOntologyManager manager ;
	OWLDataFactory dataFactory ;             //本体公理工厂
	String PREFIX; 
	public static  int MAXRECURSION ;
	
	//owl:owl文件的路径
	public InjectIncoherentByDisjointness(String owlPath, String dbPath, int n, boolean isClear, String prefix){
		this.owlPath = owlPath;
		this.dbPath = dbPath;
		this.disjointness = n;
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		gTool = new GraphFromOWLbyCypher(dbPath, owlPath, COMEFROMFIRST	);
		this.PREFIX = prefix;
		initGraph(isClear);
	}
	
	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}

	/**
	 * 初始化化图数据库，即从本体中构建图数据库
	 * @param isClear 是否清除原有的图数据库
	 */
	private void initGraph(boolean isClear){
		if(isClear){
			gTool.addTBoxToGraphDB(isClear);			
		}
		else{
			gTool.init(false);			
		}		
		graphDB = gTool.getGraphDB();
		//ontology = gTool.getOwlInfo().getOntology();
		try {
			File file = new File(owlPath);
			owlFileName = file.getName();
			ontology = manager.loadOntologyFromOntologyDocument(file);
		}catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}		
		//ExecCypher.listPaths(graphDB);
	}
	
	public void injectingDisjoint(){
		System.out.println("Injecting Disjoint Axioms...");
		Map<String, String> pairs = null;
		try(Transaction tx = graphDB.beginTx()){
			pairs = getDisjointnessPairs();
			System.out.println("disjointness pairs size is " + pairs.size());
			/*int index=0;
			for(String key : pairs.keySet()){
				System.out.println((++index)+":"+key+"###"+pairs.get(key));
			}*/
			tx.success();
		}
		insertDisjointnessToGraph(pairs);
		insertDisjointnessToOWL(pairs);
	}
	
	/**
	 * 将生成的不相交概念添加到owl文件中
	 */
	public void insertDisjointnessToOWL(Map<String, String> pairs){
		String newPath = String.format("IncoherenceOntology/%s", owlFileName);
		System.out.println("New file saved to " + newPath);		
		OutputStream os;
		try {
			os = new FileOutputStream(new File(newPath));
			for(String key : pairs.keySet()){
				String supkey = pairs.get(key);
				insertDisjointClassesAxiom(key, supkey);
			}
			//OWLOntologyFormat ontoFormat =new DLSyntaxOntologyFormat();
			manager.saveOntology(ontology, os);
		} catch (FileNotFoundException | OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 将生成的不相交公理添加到图中
	 */
	public void insertDisjointnessToGraph(Map<String, String> pairs){
		System.out.println("Insert disjointness to graph...");
		try(Transaction tx = graphDB.beginTx()){
			for(String key : pairs.keySet()){
				String supkey = getNegativeToken(pairs.get(key));
				gTool.createNode(supkey, CONCEPTLABEL);
				gTool.createRelationship(key, supkey, CONCEPTTYPE, INCLUDEDREL);
			}	
			tx.success();
		}
	}
	
	/**
	 * 取出待生成的不相交公理中的节点队
	 * @return
	 */
	private Map<String, String>	getDisjointnessPairs(){
		Map<String, String> pairs = new HashMap<>();
		List<Node> nodes = GlobalFunct.getAllNodes(graphDB);
		Random random = new Random();
		Set<Integer> selectIndex = new HashSet<>();
		//随机选定100个节点
		int i=0;
		while(i < disjointness ){			
			int index = random.nextInt(nodes.size());
			//System.out.print("\nIndex:"+index+"#"+i);
			if(selectIndex.contains(new Integer(index))){
				//System.out.println("\ncontinue");
				continue;
			}
			else if(generatingDisjointnessPair(pairs, nodes, index, random)){
				selectIndex.add(new Integer(index));
				++i;
				System.out.println("insert disjointAxiom:"+i);
			}
		}		
		return pairs;
	}
	//从第index个节点中选两个不同的父类，让他们不交
	private boolean generatingDisjointnessPair(Map<String, String> pairs, List<Node> nodes, int index,Random random){
		boolean isSuccess = false;
		Node node = nodes.get(index);
		//找选定点的父方向的关系，如果有两个以上，则可以开始选择
		List<Relationship> rels = new ArrayList<>();
		for(Relationship rel : node.getRelationships(Direction.OUTGOING)){
			rels.add(rel);
			if(rels.size()>0)  //原来的值为大于1
				break;
		}
		//System.out.print("#"+node.getProperty(NAMEPROPERTY)+"#relaitonship size:"+rels.size());
		if(rels.size() >= 2){  //有的本体中找不到两条关系的路径
			Relationship r1 = rels.get(0);
			Relationship r2 = rels.get(1);
			isSuccess = getPair(r1, r2, pairs, random);
			//isSuccess = true;
		}
		else if(rels.size() == 1 ){ //如果没有两个父类会出错
			Relationship r1 = rels.get(0);			
			isSuccess = getPair(r1, r1, pairs, random);
			//return false;
		}
		return isSuccess;
	}
	//private int iCount;
	private boolean getPair(Relationship r1, Relationship r2, Map<String, String> pairs, Random random){
		int maxTime =10;  //最多比较的次
		int iCount = 0;
		recursions = 0;
		List<Node> ns1 = getNodesFromRelationship(r1);
		recursions = 0;
		List<Node> ns2 = getNodesFromRelationship(r2);
		Node n1=null,n2=null;
		
		while(n1==null || n2==null || GlobalFunct.isNodeEquals(n1, n2)){ //可能死循环
			n1 = ns1.get(random.nextInt(ns1.size()));
			n2 = ns2.get(random.nextInt(ns2.size()));
			//System.out.println(n1.getProperty(NAMEPROPERTY)+"**"+n2.getProperty(NAMEPROPERTY));
			++iCount;
			if(iCount>maxTime)
				//break;
				return false;
		}
		
		if(GlobalFunct.isExistenceNode(n1)||GlobalFunct.isExistenceNode(n2)){
			return false;
		}		
		else {
			String key = n1.getProperty(NAMEPROPERTY).toString();
			String value = n2.getProperty(NAMEPROPERTY).toString();
			if(pairs.keySet().contains(key))
				return false;
			else if(key.startsWith(NEGATIVESIGN) || value.startsWith(NEGATIVESIGN)){
				return false;
			}
			else{
				pairs.put(key, value );
				return true;
			}
		}		
	}
	//使用递归得到一个关系链上所有的 end node
	private static int recursions = 0;  //递归的层次，太深了会导致结构太复杂
	private List<Node> getNodesFromRelationship(Relationship r){		
		List<Node> ns = new ArrayList<>();
		Node n = r.getEndNode();
		if(n!=null){
			ns.add(n);
		}		
		if(recursions++ > MAXRECURSION)
			return ns;
		Iterable<Relationship> rels = n.getRelationships(Direction.OUTGOING);
		if(rels.iterator().hasNext()){
			for(Relationship rr : rels){
				ns.addAll(getNodesFromRelationship(rr));
			}
		}
		return ns;
	}	
	
	/**
	 * 插入不相交的类
	 * @param c1 概念1
	 * @param c2 概念2
	 */
	public void insertDisjointClassesAxiom(String c1, String c2){
		OWLClass ocC1 = dataFactory.getOWLClass(IRI.create(PREFIX, c1));
		OWLClass ocC2 = dataFactory.getOWLClass(IRI.create(PREFIX, c2));				
		Set<OWLClass> classSet = new HashSet<OWLClass>();
		classSet.add(ocC1);
		classSet.add(ocC2);
		OWLDisjointClassesAxiom odcAxiom = dataFactory.getOWLDisjointClassesAxiom(classSet);
		System.out.println("Adding axiom ### " +odcAxiom);
		manager.addAxiom(ontology, odcAxiom);			
	}
}






