package kse.algorithm.forABox.preprocessing;

import static kse.misc.GlobalParams.CONCEPTLABEL;
import static kse.misc.GlobalParams.CONJUNCTION;
import static kse.misc.GlobalParams.EXISTENCESIGN;
import static kse.misc.GlobalParams.INVERSESIGN;
import static kse.misc.GlobalParams.NAMEPROPERTY;
import static kse.misc.GlobalParams.NODEINDEX;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.neo4j.ver2_1.GraphFromOWLbyCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.tooling.GlobalGraphOperations;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
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
 *  [2014-9]修订，原来的方法过于简单
 *  新的思路，将概念节点分块，分块的原则：每个块中的节点都有关联
 * @author Xuefeng Fu
 */

public class NewInjectConflicts {
	private  String prefix = "http://uob.iodt.ibm.com/univ-bench-lite.owl#";                           //UOBM 本体的前缀
	//public static String prefix = "http://swat.cse.lehigh.edu/onto/univ-bench.owl#";                      //LUBM 本体前缀
	//原始的benchmark TBox 本体 【univ-bench-lite.owl】
	
	private String benchmarkOnto;
	private String graphDBPath = "neo4j-ABox/UOBM_benchmark";                                                  //图数据库存储路径      
	private GraphDatabaseService graphDB;	
	//private ExecutionEngine engine ;	
	
	int percent;               //不相交概念在TBox中的百分比
	int disjointnessIndiNum;    //向ABox中注入的实例断言的数量
	
	private List<String> conceptNames;	
	private List<String> roleNames;
	
	/**
	 * @param pNum 不相交概念的百分比
	 * @param iNum  ABox中不相交实例断言的数量
	 */
	public NewInjectConflicts(int pNum, int iNum){
		this.percent = pNum;
		this.disjointnessIndiNum = iNum;
		
		benchmarkOnto = String.format("ForABoxRevi/P%s/univ-bench-lite.owl", pNum);
		System.out.println(benchmarkOnto);
		//从benchmark TBox本体中创建图
		GraphFromOWLbyCypher app =  new  GraphFromOWLbyCypher(graphDBPath, benchmarkOnto, CONJUNCTION);;
		
		app.addTBoxToGraphDB(true);	
		
		graphDB = app.getGraphDB();
		//engine = app.getEngine();		
		
		conceptNames = new ArrayList<String>();
		roleNames = new ArrayList<String>();		
		
	}
	
	/**
	 * 将概念分块，只要概念间存在关系，就属于同一块
	 * 这部分要访问图
	 */
	public List<Set<String>> blockingConcept(){
		List<Set<String>> blockSet = new ArrayList<Set<String>>();		
		try(Transaction tx = graphDB.beginTx()){
			List<Set<String>> blockList = new ArrayList<Set<String>>();					
			//先取出所有节点的关联节点，之后合并
			Iterator<Node> nodes =   GlobalGraphOperations.at(graphDB).getAllNodes().iterator();
			while(nodes.hasNext()){
				boolean isRole = true;
				Node node = nodes.next();
				String nName = node.getProperty(NAMEPROPERTY).toString();
				if(nName.startsWith(EXISTENCESIGN) || nName.startsWith(INVERSESIGN)){
					continue;
				}
				for(Label label : node.getLabels()){
					if(label.name().equals(CONCEPTLABEL)){					
						isRole = false;
						break;
					}
				}
				if(isRole){
					continue;
				}				
				Set<String> nodeBlock = GlobalFunct.getAncestorNode(node);
				nodeBlock.addAll(GlobalFunct.getDescendantNodes(node));
				blockList.add(nodeBlock);
			}
			//合并有共同节点的block
			Set<Integer> mergedIndex = new HashSet<Integer>();
			
			for(int i=0; i<blockList.size()-1; i++){
				if(mergedIndex.contains(i)){  //已经做了合并操作了
					continue;
				}
				Set<String> currentBlock = blockList.get(i);
				for(int j=i+1; j<blockList.size(); j++){
					if(mergedIndex.contains(j)){  //已经做了合并操作了
						continue;
					}					
					Set<String> tempBlock = cloneStrSet(blockList.get(j));
					tempBlock.retainAll(currentBlock);  //是否有共同节点
														
					if(tempBlock.size()>0){  //有共同节点
						currentBlock.addAll(blockList.get(j));
						mergedIndex.add(j);
						/*printStringSet(blockList.get(i));
						printStringSet(blockList.get(j));
						printStringSet(tempBlock);
						System.out.println();*/
					}
				}
				blockSet.add(currentBlock);
			}			
			tx.success();
		}		
		handleBlockSet(blockSet); //进一步处理，去掉只有一个的集合和集合中的非原子概念		
		return blockSet;
	}
	
	//删除分块中大小为一的块
	private void handleBlockSet(List<Set<String>> sets){
		List<Set<String>> forDelete = new ArrayList<Set<String>>();
		for(Set<String> set : sets){
			if(set.size()<=1)
				forDelete.add(set);
		}
		for(Set<String> set : forDelete){
			sets.remove(set);
		}
	}
	
	public void printStrSet(Set<String> strs){
		System.out.print(strs.size()+"#");
		for(String str: strs){
			System.out.print(str+"*");
		}
		System.out.println();
	}
	
	public Set<String> cloneStrSet(Set<String> strs){
		Set<String> clone = new HashSet<>();
		for(String str : strs){
			clone.add(str);
		}
		return clone;
	}	
	
	/**
	 * 固定的不一致对，添加对应的disjoint公理到TBox中，同时保证TBox一致.
	 * 共20对
	 * @return
	 */
	public List<DisjointPair> getDisjointConcepts(){
		List<DisjointPair> pairs = new ArrayList<>();
		pairs.add(new DisjointPair("existence_isHeadOf", "Organization"));
		pairs.add(new DisjointPair("Employee", "Publication"));
		pairs.add(new DisjointPair("TechnicalReport", "AssistantProfessor"));
		pairs.add(new DisjointPair("Person", "TechnicalReport"));
		pairs.add(new DisjointPair("Specification", "SupportingStaff"));
		
		pairs.add(new DisjointPair("existence_like", "Specification"));
		pairs.add(new DisjointPair("Faculty", "Publication"));
		pairs.add(new DisjointPair("Publication", "SportsLover"));
		pairs.add(new DisjointPair("existence_worksFor", "TechnicalReport"));
		pairs.add(new DisjointPair("existence_isStudentOf", "Article"));
		
		pairs.add(new DisjointPair("existence_isCrazyAbout", "Work"));
		pairs.add(new DisjointPair("SportsFan", "Publication"));
		pairs.add(new DisjointPair("existence_isHeadOf", "Organization"));
		pairs.add(new DisjointPair("Organization", "AssistantProfessor"));
		pairs.add(new DisjointPair("existence_teachingAssistantOf", "Software"));
		
		pairs.add(new DisjointPair("ConferencePaper", "Student"));
		pairs.add(new DisjointPair("Sports", "TechnicalReport"));
		pairs.add(new DisjointPair("Organization", "man"));
		pairs.add(new DisjointPair("UnofficialPublication", "student"));
		pairs.add(new DisjointPair("existence_worksFor", "Publication"));
		
		return pairs;
	}
	/**
	 * 取出固定不一致对的对应子类，用以插入不一致的实例断言
	 * @return
	 */
	public List<DisjointPair> getDesDisjontConcepts(){
		List<DisjointPair> pairs = new ArrayList<>();
		pairs.add(new DisjointPair("Dean", "University"));
		pairs.add(new DisjointPair("Faculty", "ClericalStaff"));
		pairs.add(new DisjointPair("TechnicalReport", "AssistantProfessor"));
		pairs.add(new DisjointPair("Professor ", "TechnicalReport"));
		pairs.add(new DisjointPair("Specification", "SystemsStaff"));
		
		pairs.add(new DisjointPair("SportsLover", "Specification"));
		pairs.add(new DisjointPair("FullProfessor", "JournalArticle"));
		pairs.add(new DisjointPair("JournalArticle", "SportsLover"));
		pairs.add(new DisjointPair("Dean", "Lecturer"));
		pairs.add(new DisjointPair("Student", "ConferencePaper"));
		
		pairs.add(new DisjointPair("SportsFan", "Work"));
		pairs.add(new DisjointPair("SportsFan", "Publication"));
		pairs.add(new DisjointPair("Director", "College"));
		pairs.add(new DisjointPair("College", "AssistantProfessor"));
		pairs.add(new DisjointPair("TeachingAssistant", "Software"));

		pairs.add(new DisjointPair("ConferencePaper", "Student"));
		pairs.add(new DisjointPair("Sports", "TechnicalReport"));
		pairs.add(new DisjointPair("Organization", "man"));
		pairs.add(new DisjointPair("UnofficialPublication", "student"));
		pairs.add(new DisjointPair("Lecturer", "Publication"));
		
		return pairs;
	}
	
	
	/**
	 * 从块中取得不相交的概念，由于不同块中的概念没有关系
	 * @param blockSet
	 */
	public List<DisjointPair> getDisjointConceptsFromBlock(List<Set<String>> blockSet){
		//percent为要生成的不交概念对，一共有52个基本的概念，故生成的不交对是52*percent/100
		//blockset的大小分布是 10,7,31,4,3，为了复杂起见，从31选开始的节点
		List<DisjointPair> pairs = new ArrayList<>();
		List<String> size10 = setToList(blockSet.get(0));
		List<String> size7 = setToList(blockSet.get(1));
		List<String> size31 = setToList(blockSet.get(2));
		List<String> size4 = setToList(blockSet.get(3));
		List<Integer> addedIndex = new ArrayList<Integer>();
		int flag=1;
		Random random = new Random();
		int pairsNum = (52*percent)/100;
		int firstIndex;
		int secondIndex;
		String first;
		String second;
		while(pairsNum > 0){
			firstIndex = random.nextInt(size31.size());
			first = size31.get(firstIndex);
			if(first.startsWith(EXISTENCESIGN))
				continue;
			if(addedIndex.contains(firstIndex)){
				continue;				
			}
			else{
				//分别从其他块中取概念
				addedIndex.add(new Integer(firstIndex));
				if(flag % 1==0){
					secondIndex = random.nextInt(size10.size());
					second = size10.get(secondIndex);
					pairs.add(new DisjointPair(first, second));
					if(--pairsNum <= 0) break;
				}
				else if(flag % 2 == 0){
					secondIndex = random.nextInt(size7.size());
					second = size7.get(secondIndex);
					pairs.add(new DisjointPair(first, second));
					if(--pairsNum <= 0) break;
				}
				else if(flag % 3 == 0){
					secondIndex = random.nextInt(size4.size());
					second = size4.get(secondIndex);
					pairs.add(new DisjointPair(first, second));
					if(--pairsNum <= 0) break;
				}
				flag++;
			}
		}
		System.out.println(pairs.size());
		for(DisjointPair pair : pairs){
			System.out.println(pair);
		}
		return pairs;
	}
	
	private List<String> setToList(Set<String> strs){
		List<String> strList = new ArrayList<>();
		for(String str : strs ){
			strList.add(str);
		}
		return strList;
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
	 * 向TBox中注入不相交公理
	 * @param benchmarkOnto 本体文件路径
	 */
	public void addDisjointClassToTBox(List<DisjointPair> disjointClassPair){
		Random random = new Random();
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
		
		Set<OWLDisjointClassesAxiom> disjointClasses = new HashSet<>() ;
		
		//dataFactory.getOWLDisjointClassesAxiom(arg0);
		for(DisjointPair pair : disjointClassPair){
			IRI iri1 = IRI.create(String.format(prefixFormat, pair.getFirst()));
			IRI iri2 = IRI.create(String.format(prefixFormat, pair.getSecond()));
			//System.out.println(iri1.toString()+"--"+iri2.toString());
			OWLClassImpl class1 = new OWLClassImpl(iri1);
			OWLClassImpl class2 = new OWLClassImpl(iri2);
		
			Set<OWLClassImpl> classSet = new HashSet<OWLClassImpl>();
			if(random.nextInt(10)>=5){
				classSet.add(class1);
				classSet.add(class2);
			}
			else{
				classSet.add(class2);
				classSet.add(class1);
			}
			OWLDisjointClassesAxiom disjointAxiom = dataFactory.getOWLDisjointClassesAxiom(classSet);
			disjointClasses.add(disjointAxiom);	//不相交类集合添加公理					
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
	public static int individual_i=0;
	public void addDisjointClassIndividualToABox(String abox1, String abox2, List<DisjointPair> descenantPairs){
		String prefixFormat = prefix + "%s";
		List<OWLClassImpl> classes1 = new ArrayList<OWLClassImpl>();		
		List<OWLClassImpl> classes2 = new ArrayList<OWLClassImpl>();
		
		//添加不相交类的子类的实例断言
		//List<DisjointPair> descenantPairs = getDescenantClass(disjointClassPair); //到这来处理，每次的descenant都不同
		for(DisjointPair pair : descenantPairs){
			IRI iri1 = IRI.create(String.format(prefixFormat, pair.getFirst()));
			IRI iri2 = IRI.create(String.format(prefixFormat, pair.getSecond()));
			//System.out.println(iri1.toString()+"--"+iri2.toString());
			OWLClassImpl class1 = new OWLClassImpl(iri1);
			OWLClassImpl class2 = new OWLClassImpl(iri2);		
			
			classes1.add(class1);
			classes2.add(class2);
			
			//System.out.println(pair.getFirst()+"***"+pair.getSecond());
		}
		
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
	
	public List<DisjointPair> getDescenantClass(List<DisjointPair> disjointClassPair){
		Random random = new Random();
		List<DisjointPair> descenantPairs = new ArrayList<>();
		try(Transaction tx =  graphDB.beginTx()){
			/*Iterable<Node> nodes =   GlobalGraphOperations.at(graphDB).getAllNodes();
			List<Node> nodeList = new ArrayList<>();
			for(Node node : nodes){
				nodeList.add(node);				
			}*/
			Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);
			String firstDes;
			String secondDes;
			for(DisjointPair pair : disjointClassPair){
				String first = pair.getFirst();
				String second = pair.getSecond();
				Node firstNode = nodeIndex.query(NAMEPROPERTY, first).getSingle();
				Node secondNode = nodeIndex.query(NAMEPROPERTY, second).getSingle();
				if(firstNode!=null && secondNode!=null){ 
					List<String> firstDescenants = setToList(GlobalFunct.getDescendantNodes(firstNode));
					List<String> secondDescenats = setToList(GlobalFunct.getDescendantNodes(secondNode));
					//从后代节点中随机取一个,如果没有后代节点就选自身
					if(firstDescenants.size() == 0){
						firstDes=first;
					}
					else{
						firstDes = firstDescenants.get(random.nextInt(firstDescenants.size()));
					}
					if(secondDescenats.size() == 0){
						secondDes = second;
					}
					else{
						secondDes = secondDescenats.get(random.nextInt(secondDescenats.size()));
					}
					descenantPairs.add(new DisjointPair(firstDes, secondDes));
				}
			}
		}
		return descenantPairs;
	}
	
/*	private OWLClassImpl getADescentClass(OWLClassImpl ancestor){
		OWLClassImpl descendant = null;
		try(Transaction tx =  graphDB.beginTx()){
			String nodeName = ancestor.getIRI().getFragment();
		}
		return descendant;
	}*/

	/**
	 * 是否处理图数据库
	 * @param ifHandling
	 */
	public void handleGraph(List<DisjointPair> disjointClassPair){
		try(Transaction tx =  graphDB.beginTx()){
			getNodeNames();		
			//处理TBox，向TBox添加不相交公理和函数约束公理
			addDisjointClassToTBox(disjointClassPair);		
			tx.success();
		}
	}
	
	/**
	 * 向TBox中添加disjoint公理，再分别向两个ABox添加不相交类的实例
	 * @param univNum 大学数目
	 * @param ifHandling 是否在图上做处理
	 */	
	public void go(int univNum, boolean ifHandling){			
		List<Set<String>> blockSet = blockingConcept();					
		List<DisjointPair> disjointPairs = getDisjointConceptsFromBlock(blockSet);			
		List<DisjointPair> descenantPair = getDescenantClass(disjointPairs);
		for(DisjointPair pair : descenantPair){
			System.out.println(pair.getFirst()+"***"+pair.getSecond());
		}
		if(ifHandling){
			handleGraph(disjointPairs);
		}			
		String abox1 = String.format("ForABoxRevi/P%s/univ_%s_1.owl", percent, univNum);
		String abox2 = String.format("ForABoxRevi/P%s/univ_%s_2.owl", percent, univNum);
		System.out.println("add disjoint indivdual into:"+abox1+"###"+abox2);				
		//处理ABox
		addDisjointClassIndividualToABox(abox1, abox2,descenantPair);
	}		
	
	public void go(int[] univs){
		List<Set<String>> blockSet = blockingConcept();		
		List<DisjointPair> disjointPairs = getDisjointConceptsFromBlock(blockSet);	
		List<DisjointPair> descenantPair = getDescenantClass(disjointPairs);
		for(DisjointPair pair : descenantPair){
			System.out.println(pair.getFirst()+"***"+pair.getSecond());
		}
		for(int i=0; i<univs.length; i++){
			if(i ==0){
				handleGraph(disjointPairs);
			}
			String abox1 = String.format("ForABoxRevi/P%s/univ_%s_1.owl", percent, univs[i]);
			String abox2 = String.format("ForABoxRevi/P%s/univ_%s_2.owl", percent, univs[i]);
			System.out.println("add disjoint indivdual into:"+abox1+"###"+abox2);				
			//处理ABox
			addDisjointClassIndividualToABox(abox1, abox2,descenantPair);
		}
	}
	
	/**
	 * 用固定的不相交类来注入不一致信息
	 * @param univs
	 */
	public void staticGo(int[] univs){
		
		List<DisjointPair> disjointPairs = getDisjointConcepts().subList(0, percent*52/100);
		List<DisjointPair> descenantPair = getDesDisjontConcepts().subList(0, 52*percent/100);
		
		/*for(DisjointPair pair : descenantPair){
			System.out.println(pair.getFirst()+"***"+pair.getSecond());
		}*/
		
		addDisjointClassToTBox(disjointPairs);	
		
		for(int i=0; i<univs.length; i++){			
			String abox1 = String.format("ForABoxRevi/P%s/univ_%s_1.owl", percent, univs[i]);
			String abox2 = String.format("ForABoxRevi/P%s/univ_%s_2.owl", percent, univs[i]);
			System.out.println("add disjoint indivdual into:"+abox1+"###"+abox2);				
			//处理ABox
			addDisjointClassIndividualToABox(abox1, abox2,descenantPair);
		}
	}
	
	public void shutdown(){
		graphDB.shutdown();
	}
}
