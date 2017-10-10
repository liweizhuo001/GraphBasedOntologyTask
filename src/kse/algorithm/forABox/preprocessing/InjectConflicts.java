package kse.algorithm.forABox.preprocessing;

import static kse.misc.GlobalParams.CONCEPTLABEL;
import static kse.misc.GlobalParams.CONJUNCTION;
import static kse.misc.GlobalParams.EXISTENCESIGN;
import static kse.misc.GlobalParams.NAMEPROPERTY;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;
/**
 * 向BenchMark TBox中注入冲突
 * 
 * 1.Function冲突，如果有Funct(R),又有R(a,b),R(a,c),则本体不一致
 * 2.向TBox中添加不交类，再向ABox中添加不交类的实例
 * 向一个本体中按概念数的百分比添加不交类，这些不交类之间没有关系（表现在图中就是不存在路径），这可以保证本体是一致的
 * 使用UOBM生成ABox本体对，根据不交类，如A,B不交，分别向两个本体添加A(a)和B(a),由于A，B不交，这样当两个ABox本体
 * 合并时，会产生不一致。
 * <br>
 *  From experiment of ISWC'13 [2013-5]
 * @author Xuefeng Fu
 */

public class InjectConflicts {
	public  String prefix = "http://uob.iodt.ibm.com/univ-bench-lite.owl#";                           //UOBM 本体的前缀
	//public static String prefix = "http://swat.cse.lehigh.edu/onto/univ-bench.owl#";                      //LUBM 本体前缀
	//原始的benchmark TBox 本体 【univ-bench-大学数量-百分比】
	public  String benchmarkFormat = "ForABoxRevi/P%s/univ-bench-lite-%s.owl";             
	public String benchmarkOnto;
	public String graphDBPath = "neo4j-ABox/UOBM_benchmark";                                                  //图数据库存储路径      
	private GraphDatabaseService graphDB;	
	public ExecutionEngine engine ;	
	
	int percent;               //不相交概念在TBox中的百分比
	int disjointnessIndiNum;    //向ABox中注入的实例断言的数量
	
	private List<String> conceptNames;	
	private Map<String, List<String>> relaitonOfNode;
	private Map<String, String> disjointClassPair;                                                                    //用以生成不相交公理的概念对
	public Set<OWLDisjointClassesAxiom> disjointClasses ;
	public List<OWLClassImpl> classes1;
	public List<OWLClassImpl> classes2;
	
	public List<OWLObjectPropertyImpl> roles;	
	
	private List<String> roleNames;
	public Set<OWLFunctionalObjectPropertyAxiom> functAxioms;
	public List<String> functAxiomNames;
	
	/**
	 * @param pNum 不相交概念的百分比
	 * @param iNum  ABox中不相交实例断言的数量
	 */
	public InjectConflicts(int pNum, int iNum){
		this.percent = pNum;
		this.disjointnessIndiNum = iNum;
		
		benchmarkOnto = String.format(benchmarkFormat, pNum, pNum);
		System.out.println(benchmarkOnto);
		//从benchmark TBox本体中创建图
		GraphFromOWLbyCypher app =  new  GraphFromOWLbyCypher(graphDBPath, benchmarkOnto, CONJUNCTION);;
		
		app.addTBoxToGraphDB(true);			
		//app.listPaths();
		
		//graphDb =new GraphDatabaseFactory().newEmbeddedDatabase( graphDBPath );
		//Tools4Graph.registerShutdownHook(graphDb);  //挂钩函数，程序结束自动调用来释放图数据库			
		//engine = new ExecutionEngine( graphDb );
		
		graphDB = app.getGraphDB();
		engine = app.getEngine();		
		
		conceptNames = new ArrayList<String>();
		relaitonOfNode = new HashMap<String, List<String>>();
		disjointClassPair = new HashMap<String, String>();
		disjointClasses = new HashSet<OWLDisjointClassesAxiom>();
		classes1 = new ArrayList<OWLClassImpl>();
		classes2 = new ArrayList<OWLClassImpl>();
		
		roleNames = new ArrayList<String>();
		roles = new ArrayList<OWLObjectPropertyImpl>();
		functAxiomNames = new ArrayList<String>();
		functAxioms = new HashSet<OWLFunctionalObjectPropertyAxiom> ();
	}
	
	/**
	 * 随机方式生成不相交的概念对
	 * @param percent 不一致概念的百分比
	 */	
	public void createDisjointClassPair(int percent){
		int size = conceptNames.size();		
		int number = size*percent/100;	
		System.out.println(String.format("create disjoint pair number %d in size %d. ",number,size));
		List<Integer> nodeIndexs = new ArrayList<Integer>();
		Random random = new Random();
		int index = 0;
		//随机生成number个整数
		while(index<number){
			int randomNum = random.nextInt(size); //生成0-size之间的一个随机数，包括0，不包括指定值
			if(nodeIndexs.contains(randomNum))
				continue;
			else{
				nodeIndexs.add(randomNum);				
				index++;
			}
		}
		for(Integer i : nodeIndexs){	
			String oneClass = conceptNames.get(i);
			String anotherClass = null;
			List<String> relationOfOne = relaitonOfNode.get(oneClass);
			if(relationOfOne == null){  //表示这个概念不和其他概念关联
				int tempIndex = -1;
				do{
					tempIndex = random.nextInt(size);
					anotherClass = conceptNames.get(tempIndex);	
				}while(nodeIndexs.contains(tempIndex));   //如果一个概念没有关联的类就随机生成一个，但不要是已选出的类，否则会可能重复
			}
			else{				
				while(true){
					int tempIndex = random.nextInt(size);
					anotherClass = conceptNames.get(tempIndex);		
					if(relationOfOne.contains(anotherClass)){ //在和他关联的类中，重新取过
						System.out.println(anotherClass + " has relation with " + oneClass);
					}
					else{///不在他关联的类中，获取成功
						break;
					}
				}				
			}
			disjointClassPair.put(oneClass, anotherClass);
		}
		index=1;
		for(String oneClass : disjointClassPair.keySet()){
			System.out.println(index++ + ":" + oneClass + "-->" + disjointClassPair.get(oneClass));
		}
	}
	
	/**
	 * 随机方式生成functionAxiom
	 */
	public void createFunctionAxiom(int percent){		
		int size = roleNames.size();		
		int number = size*percent/100;		
		System.out.println(String.format("create  function axiom number %d in size %d. ",number,size));
		List<Integer> roleIndexs = new ArrayList<Integer>();
		Random random = new Random();
		int index = 0;
		//随机生成number个整数
		while(index<number){
			int randomNum = random.nextInt(size);  //生成0-size之间的一个随机数，包括0，不包括指定值
			if(roleIndexs.contains(randomNum))
				continue;
			else{
				roleIndexs.add(randomNum);
				//System.out.println(randomNum); 
				index++;
			}
		}
		for(Integer i : roleIndexs){			
			functAxiomNames.add(roleNames.get(i));
		}
		index=1;
		for(String functAxiomName : functAxiomNames){
			System.out.println(index++ + ":"  + functAxiomName);
		}
	}
	
	/**
	 * 取出图中所有的节点的名字，根据类型(Concept, Role), 保存在相应的集合中<br>
	 * 图中这两类节点是通过Label来区别的
	 */
	public void getNodeNames(){		
		Iterator<Node> nodes =   GlobalGraphOperations.at(graphDB).getAllNodes().iterator();
		
		while(nodes.hasNext()){
			boolean isRole = true;
			Node node = nodes.next();
			String name = node.getProperty(NAMEPROPERTY).toString();
			if(name.startsWith(EXISTENCESIGN)){ //只取简单概念
				continue;
			}
			//保证取出的节点中是概念节点，而不是关系节点
			for(Label label : node.getLabels()){
				if(label.name().equals(CONCEPTLABEL)){					
					conceptNames.add(name);
					isRole = false;
					break;
				}
			}
			//取出role节点
			if(isRole){
				roleNames.add(name);
			}
		}	
		System.out.println(String.format("roles:%d,concepts:%d",roleNames.size(),conceptNames.size()));
	}
	
	/**
	 * 找出有关联的概念对，如果概念A和概念B有关联，则A是B的子类。
	 * 这样如果添加A和B不相交，会导致TBox不一致
	 */
	public void getConceptPair(){				
		//String query = "start a=node(*) MATCH p=a-[*]->m WHERE a.type! = 'concept' RETURN a.name,m.name";//type! 表示该变量不能为空
		//String query = "MATCH p = (a:Concept)-[:INCLUDEDBY]->(m:Concept) RETURN a.Name, m.Name "; //这只能取出Concept
		String query = "MATCH p = (A)-[:INCLUDEDBY]->(B) RETURN A.Name, B.Name "; 
		System.out.println(query);
		ExecutionResult result =engine.execute(query);
		//System.out.println(result.dumpToString()); 
		
		for (Map<String, Object> it : IteratorUtil.asIterable(result.iterator())) {
			Object[] values = it.values().toArray();
			if(values[0].toString().startsWith(EXISTENCESIGN)
					|| values[1].toString().startsWith(EXISTENCESIGN)){ //非原子概念不要
				continue;
			}
			List<String> relationNodeName0 = relaitonOfNode.get(values[0]);
			if(relationNodeName0 == null){
				relationNodeName0 = new ArrayList<String>();
				relaitonOfNode.put(values[0].toString(), relationNodeName0);
			}
			if(!relationNodeName0.contains(values[1].toString())){ //避免重复添加
				relationNodeName0.add(values[1].toString());
			}
			
			List<String> relationNodeName1 = relaitonOfNode.get(values[1]);
			if(relationNodeName1 == null){
				relationNodeName1 = new ArrayList<String>();
				relaitonOfNode.put(values[1].toString(), relationNodeName1);
			}	
			if(!relationNodeName1.contains(values[0].toString())){ //避免重复添加
				relationNodeName1.add(values[0].toString());
			}
		}
		graphDB.shutdown();		
	}
	/**
	 * 添加函数公理到TBox中
	 * @param benchmarkOnto 本体文件路径
	 */
	public void addFunctAxiomToTBox(){
		System.out.println("Add Functional axiom class to TBox.");
		String prefixFormat = prefix + "%s";
		File ontologyFile = new File(benchmarkOnto);		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLOntology ontology = null;
		try {
			ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);		
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology = null;
		}
		for(String role : functAxiomNames){  //functAxiomNames是前面使用随机函数生成的
			IRI iri = IRI.create(String.format(prefixFormat, role));						
			OWLObjectPropertyImpl objProperty = new OWLObjectPropertyImpl(iri);		
			roles.add(objProperty);       //保存生成的Role，为后面添加实例做准备
			OWLFunctionalObjectPropertyAxiom functAxiom = dataFactory.getOWLFunctionalObjectPropertyAxiom(objProperty);
			functAxioms.add(functAxiom);	//函数角色集合添加公理	
		}
		if(ontology!=null){
			System.out.println("functional axiom size:"+functAxioms.size());
			manager.addAxioms(ontology, functAxioms);
			try {
				manager.saveOntology(ontology);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 向TBox中注入不相交公理
	 * @param benchmarkOnto 本体文件路径
	 */
	public void addDisjointClassToTBox(){
		System.out.println("Add disjoint class to TBox.");
		String prefixFormat = prefix + "%s";
		File ontologyFile = new File(benchmarkOnto);		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLOntology ontology = null;
		try {
			ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);		
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology = null;
		}
		//dataFactory.getOWLDisjointClassesAxiom(arg0);
		for(String key : disjointClassPair.keySet()){
			IRI iri1 = IRI.create(String.format(prefixFormat, key));
			IRI iri2 = IRI.create(String.format(prefixFormat,disjointClassPair.get(key)));
			//System.out.println(iri1.toString()+"--"+iri2.toString());
			OWLClassImpl class1 = new OWLClassImpl(iri1);
			OWLClassImpl class2 = new OWLClassImpl(iri2);
		
			Set<OWLClassImpl> classSet = new HashSet<OWLClassImpl>();
			classSet.add(class1);
			classSet.add(class2);
			OWLDisjointClassesAxiom disjointAxiom = dataFactory.getOWLDisjointClassesAxiom(classSet);
			disjointClasses.add(disjointAxiom);	//不相交类集合添加公理	
			classes1.add(class1);
			classes2.add(class2);
		}
		if(ontology!=null){
			System.out.println("disjointClasses size:"+disjointClasses.size());
			manager.addAxioms(ontology, disjointClasses);
			try {
				manager.saveOntology(ontology);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * 分别向两个ABox添加不相交概念的实例断言
	 * @param abox1 
	 * @param abox2
	 */
	public void addDisjointClassIndividualToABox(String abox1, String abox2){
		//A,B 不交,则添加实例A(a)到abox1 B(a) 到abox2
		System.out.println("Add disjoint class individual to ABox.");
		System.out.println(abox1+"***"+abox2);
		File ontologyFile1 = new File(abox1);		
		File ontologyFile2 = new File(abox2);		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLOntology ontology1 = null;
		OWLOntology ontology2 = null;
		try {
			ontology1 = manager.loadOntologyFromOntologyDocument(ontologyFile1);		
			ontology2 = manager.loadOntologyFromOntologyDocument(ontologyFile2);	
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology1 = null;
			ontology2 = null;
		}
		if(ontology1!=null && ontology2!=null){
			//dataFactory.getOWLNamedIndividual(arg0)
			 PrefixManager pm = new DefaultPrefixManager(prefix);
			 Set<OWLClassAssertionAxiom> classAssertion1 = new HashSet<OWLClassAssertionAxiom>();
			 Set<OWLClassAssertionAxiom> classAssertion2 = new HashSet<OWLClassAssertionAxiom>();
			for(int i =0 ;i< classes1.size(); i++){
				individual_i++;
				for(int j=0; j<this.disjointnessIndiNum; j++){
					OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual("c_"+individual_i+"_"+j, pm);
					
					//System.out.println(individual.getIRI().toString());
					
					OWLClassAssertionAxiom axiom1 = dataFactory.getOWLClassAssertionAxiom(classes1.get(i), individual);
					OWLClassAssertionAxiom axiom2 = dataFactory.getOWLClassAssertionAxiom(classes2.get(i), individual);
					classAssertion1.add(axiom1);
					classAssertion2.add(axiom2);
				}
				//System.out.println(axiom1.toString());
				//System.out.println(axiom2.toString());
			}
			manager.addAxioms(ontology1, classAssertion1);
			manager.addAxioms(ontology2, classAssertion2);
			try {
				manager.saveOntology(ontology1);
				manager.saveOntology(ontology2);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static int individual_i=0;
	/**
	 * 向ABox中添加函数约束断言实例
	 * @param abox1
	 * @param abox2
	 */
	public void addFunctRoleIndividualToABox(String abox1, String abox2){		
		System.out.println("Add functional assertion individual to ABox.");
		System.out.println(abox1+"***"+abox2);
		File ontologyFile1 = new File(abox1);		
		File ontologyFile2 = new File(abox2);		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLDataFactory dataFactory = manager.getOWLDataFactory();
		OWLOntology ontology1 = null;
		OWLOntology ontology2 = null;
		try {
			ontology1 = manager.loadOntologyFromOntologyDocument(ontologyFile1);		
			ontology2 = manager.loadOntologyFromOntologyDocument(ontologyFile2);	
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology1 = null;
			ontology2 = null;
		}
		if(ontology1!=null && ontology2!=null){
			//dataFactory.getOWLNamedIndividual(arg0)
			 PrefixManager pm = new DefaultPrefixManager(prefix);
			 Set<OWLObjectPropertyAssertionAxiom> objProAssertion1 = new HashSet<OWLObjectPropertyAssertionAxiom>();
			 Set<OWLObjectPropertyAssertionAxiom> objProAssertion2 = new HashSet<OWLObjectPropertyAssertionAxiom>();
			
			for(int i =0 ;i< roles.size(); i++){
				individual_i++;
				OWLNamedIndividual individual = dataFactory.getOWLNamedIndividual("r"+individual_i,pm);
				OWLNamedIndividual individual_1 = dataFactory.getOWLNamedIndividual("r"+i+"_1",pm);
				OWLNamedIndividual individual_2 = dataFactory.getOWLNamedIndividual("r"+i+"_2",pm);
				
				OWLObjectPropertyAssertionAxiom axiom1 = dataFactory.getOWLObjectPropertyAssertionAxiom(roles.get(i),individual,individual_1);
				OWLObjectPropertyAssertionAxiom axiom2 = dataFactory.getOWLObjectPropertyAssertionAxiom(roles.get(i),individual,individual_2);
			
				objProAssertion1.add(axiom1);
				objProAssertion2.add(axiom2);
				//System.out.println(axiom1.toString());
				//System.out.println(axiom2.toString());
			}
			manager.addAxioms(ontology1, objProAssertion1);
			manager.addAxioms(ontology2, objProAssertion2);
			try {
				manager.saveOntology(ontology1);
				manager.saveOntology(ontology2);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 是否处理图数据库
	 * @param ifHandling
	 */
	public void handleGraph(){
		try(Transaction tx =  graphDB.beginTx()){
			getNodeNames();
			getConceptPair();		
			createDisjointClassPair(percent);
			//createFunctionAxiom(percent);
		
			//处理TBox，向TBox添加不相交公理和函数约束公理
			addDisjointClassToTBox();		
			//addFunctAxiomToTBox();
			tx.success();
		}
	}
	
	/**
	 * 向TBox中添加disjoint公理，再分别向两个ABox添加不相交类的实例
	 */
	
	public void go(int univNum, boolean ifHandling){					
			String abox1 = String.format("ForABoxRevi/P%s/univ_%s_1.owl", percent, univNum);
			String abox2 = String.format("ForABoxRevi/P%s/univ_%s_2.owl", percent, univNum);
			System.out.println("add disjoint indivdual into:"+abox1+"###"+abox2);		
			
			if(ifHandling){
				handleGraph();
			}			
			//处理ABox
			addDisjointClassIndividualToABox(abox1, abox2);
			//addFunctRoleIndividualToABox(abox1, abox2);					
	}	
	
	//向precent文件夹下的所有相关的owl对添加实例,渐进式实验，针对LUBM
/*	public void goForLUBM(){
		getNodeNames();
		getConceptPair();			
		
		//String file = ABoxFileHandle.getBenchmarkName(percent);  //获取benchmark文件
		//String file =String.format(benchmarkFormat, percent, percent);   //获取benchmark文件
		
		createDisjointClassPair(percent); 
		createFunctionAxiom(percent);
		//按百分百给benchmark文件增加不相交类和函数型公理
		addDisjointClassToTBox();	
		addFunctAxiomToTBox();
		
		//再向Abox注入冲突信息，0为基准
		String abox_iFormat ="Owls/LUBM/University%d_%d.owl";
		for(int i=0; i<=4; i++){
			String abox0 = String.format("Owls/LUBM/University%d_0.owl", i);
			for(int j=1; j<=9; j++){
				String abox_i = String.format(abox_iFormat,i,j);
				//System.out.println(abox0+"***"+abox_i);
				addDisjointClassIndividualToABox(abox0, abox_i);
				addFunctRoleIndividualToABox(abox0, abox_i);
			}
		}
	}*/
}
