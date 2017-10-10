package kse.algorithm.forTBox.preprocessing;

import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.CONCEPTLABEL;
import static kse.misc.GlobalParams.CONCEPTTYPE;
import static kse.misc.GlobalParams.INCLUDEDREL;
import static kse.misc.GlobalParams.NAMEPROPERTY;
import static kse.misc.GlobalParams.NEGATIVESIGN;
import static kse.misc.GlobalParams.NODEINDEX;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

/**
 *  向一个协调的本体注入不可满足概念来引入不协调<br>
 *  找两个不相交的节点，在它们各自的子类中找节点对，新增一个节点为给节点对的子节点。
 *  实际上是添加新的subclassof公理
 *  [2015-5-21]
 * @author Xuefeng Fu
 */
public class InjectUnsatisfiableConcepts {
	String owlPath;
	String owlFileName;
	String dbPath;
	OWLOntology ontology;
	int ucNumber;                                         //要注入的不可满足概念的数目
	GraphDatabaseService graphDB;	
	GraphFromOWLbyCypher gTool; 
	boolean isClear;
	static final String newOntoFormat = "owls/NewIncoherence-3/%s";
	
	Index<Node> nodeIndex;
	OWLOntologyManager manager ;
	OWLDataFactory dataFactory ;             //本体公理工厂
	String PREFIX; 
	
	public static  int MAXRECURSION ;  //在找子节点的时候，最大的递归数

	/**
	 * @param owlPath 本体文件的路径
	 * @param dbPath   图数据库的路径
	 * @param ucNumber       不可以满足概念数目        
	 * @param isClear         是否清除原图数据库，默认不清除
	 * @param prefix           owl中概念的前缀
	 */
	public InjectUnsatisfiableConcepts(String owlPath, String dbPath, int ucNumber, String prefix){
		this.owlPath = owlPath;
		this.dbPath = dbPath;
		this.ucNumber = ucNumber;
		this.isClear = false;
		
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();		
		gTool = new GraphFromOWLbyCypher(dbPath, owlPath, COMEFROMFIRST	);
		this.PREFIX = prefix;
		init();
	}	
	/**
	 * 初始化系统，注入操作都是发生在已经存在的本体和图数据库中
	 */
	private void init(){		
		gTool.init(isClear);	
		graphDB = gTool.getGraphDB();
		try {
			File file = new File(owlPath);
			owlFileName = file.getName();
			ontology = manager.loadOntologyFromOntologyDocument(file);
		}catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}		
		try(Transaction tx = graphDB.beginTx()){
			nodeIndex = graphDB.index().forNodes(NODEINDEX);  
		}
	}
	
	public void generalUc(){		
		List<DisjointPair>pairs = GlobalFunct.getDisjointPair(graphDB);
		
		List<DisjointPair> subClassPairs = new ArrayList<>();
		int i=0;
		while(i<ucNumber){
			for(DisjointPair pair : pairs){
				//System.out.println((++i)+":"+pair);
				++i;
				List<DisjointPair> generalPairs = generalSubClassPair(pair,i);
				while(generalPairs.size() == 0){
					generalPairs = generalSubClassPair(pair,i);
				}				
				subClassPairs.addAll(generalPairs);
				if(i>=ucNumber)
					break;
			}
		}
		injectUcIntoGraph(subClassPairs);
		injectUcIntoOwl(subClassPairs);		
	}
	
	public void injectUcIntoGraph(List<DisjointPair> subClassPairs){
		System.out.println("Injecting UC into graph database....");
		try(Transaction tx = graphDB.beginTx()){
			for(DisjointPair pair : subClassPairs){
				String supNode = pair.getFirst();
				String subNewNode = pair.getSecond();
				gTool.createNode(subNewNode, CONCEPTLABEL);
				gTool.createRelationship(subNewNode, supNode, CONCEPTTYPE, INCLUDEDREL);
			}
			tx.success();
		}
	}
	
	public void injectUcIntoOwl(List<DisjointPair> subClassPairs){
		
		String newPath = String.format(newOntoFormat, owlFileName);
		System.out.println("Injecting UC into owl ontoloyg:" + newPath);
		
		try{
			OutputStream os = new FileOutputStream(new File(newPath));
			for(DisjointPair pair : subClassPairs){
				String supNode = pair.getFirst();
				String subNewNode = pair.getSecond();
				insertSubClasseOfAxioms(supNode, subNewNode);
			}
			manager.saveOntology(ontology, os);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void insertSubClasseOfAxioms(String sup, String sub){
		OWLClass supC = dataFactory.getOWLClass(IRI.create(PREFIX, sup));
		OWLClass subC = dataFactory.getOWLClass(IRI.create(PREFIX, sub));				
		
		OWLSubClassOfAxiom subClassAxiom = dataFactory.getOWLSubClassOfAxiom(subC, supC);
		System.out.println("Adding axiom ### "  + subClassAxiom);
		manager.addAxiom(ontology, subClassAxiom);			
	}
	
	public List<DisjointPair> generalSubClassPair(DisjointPair pair, int i){
		Random random = new Random();
		List<DisjointPair> subClassPairs = new ArrayList<>();
		String posName = pair.getFirst();
		String negName = pair.getSecond();		
	
		List<String> posDescendants,negDescendants;
		
		try(Transaction tx = graphDB.beginTx()){
			Node posNode = nodeIndex.get(NAMEPROPERTY, posName).getSingle();
			Node negNode = nodeIndex.get(NAMEPROPERTY, negName).getSingle();			
			
			posDescendants = GlobalFunct.getDescendantNodesInList(posNode);
			negDescendants = GlobalFunct.getDescendantNodesInList(negNode);
			tx.success();
		}
		//System.out.print(posDescendants.size());
		//System.out.print(negDescendants.size());
		int selectedIndexOfPos = (MAXRECURSION>posDescendants.size()) ? posDescendants.size():MAXRECURSION;
		int selectedIndexOfNeg = (MAXRECURSION>negDescendants.size()) ? negDescendants.size():MAXRECURSION;
		
		String supPos = posDescendants.get(random.nextInt(selectedIndexOfPos));
		String supNeg = negDescendants.get(random.nextInt(selectedIndexOfNeg));
		
		if(!(supNeg.startsWith(NEGATIVESIGN))){
			
			String newSubName = String.format("%s_%s_%d", supPos,supNeg,i);
			System.out.println(newSubName);
				
			subClassPairs.add(new DisjointPair(supPos, newSubName));
			subClassPairs.add(new DisjointPair(supNeg, newSubName));
		}
			
			
		
		return subClassPairs;		
	}
}
