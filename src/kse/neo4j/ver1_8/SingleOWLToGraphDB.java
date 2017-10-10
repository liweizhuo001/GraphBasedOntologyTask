package kse.neo4j.ver1_8;

import static kse.misc.GlobalParams.*;

import java.util.List;
import java.util.Set;
import kse.owl.AxiomMapping;
import kse.owl.OWLInfo;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;

/**
 * 将OWLInfo转换成图数据库，图中有两类节点，分别是概念节点和角色节点。<br>
 * 概念节点：有三个属性分别是节点名，节点类型，节点来源。<br>
 * 角色节点：属性分别是，名称：便于索引，类型：是从concept的关系还是role的关系
 * @author Xuefeng Fu
 */
public class SingleOWLToGraphDB {		
	private String comefrom;                                               //该本体是第一个本体，还是第二个本体
	private GraphDatabaseService graphDb;	
	private OWLInfo owlInfo;
	private  Index<Node> nodeIndex;                               //节点索引，方便查找节点	
	private Index<Relationship> relationshipIndex;        //关系索引，方便查找节点
	
	/**
	 * 构造函数
	 * @param dbPath           图数据库存储路径
	 * @param owlPath                  要转换的本体文件
	 * @param comefrom      如果要处理两个OWL的合并，comefrom表示来第几个本体
	 * @param isClear            是否清除已存在的同名图数据库
	 */
	public SingleOWLToGraphDB(String dbPath, String owlPath, String comefrom,boolean isClear){		
		//是否清除已经存在的数据库
		if(isClear){
			Tools4Graph.clearDb(dbPath);
		}		
		this.comefrom = comefrom;	
		//创建一个新的数据库或者打开一个已经存在的数据库，实例可以在多个线程中共享，但不能创建多个实例来指向同一个数据库
		//graphDb = new EmbeddedGraphDatabase(dbPath);		这是1.8版本中的表达方式，已经过期
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
		Tools4Graph.registerShutdownHook(graphDb);  //挂钩，程序结束自动调用
		try(Transaction tx = graphDb.beginTx()){
			IndexManager indexM = graphDb.index();
			nodeIndex = indexM.forNodes(NODEINDEX);  //节点索引，参数是索引的名字			
			relationshipIndex = indexM.forRelationships(RELATIONSHIPINDEX); //关系索引
		}
		owlInfo = new OWLInfo(owlPath);		
	}
	
	public OWLInfo getOwlInfo() {
		return owlInfo;
	}

	/**
	 * 从OWL中创建图数据库(静态方法)
	 * @param dbPath         图数据库的存储路径
	 * @param owl                owl文件路径
	 * @param comefrom    是第几个本体
	 * @param isClear          是否清除原来图数据库数据
	 */
	/*public static void createGraphDb(String dbPath, String owl, String comefrom,boolean isClear){
		SingleOWLToGraphDB app = new SingleOWLToGraphDB(dbPath, owl, comefrom, isClear);
		//对owl信息作一次转换前的预处理
		System.out.println("preprocessing owlInfo for graph ... ...");
		PreOWL4Graph.handle(app.getOwlInfo());
		app.createDbFromOwl();
		app.shutdown();
	}*/
	
	public void createDbFromOwl(){		
		System.out.println("preprocessing owlInfo for graph ... ...");
		PreOWL4Graph.handle(getOwlInfo());
		System.out.println("Create graph database from owlInfo ... ...");
		createNodes(owlInfo, comefrom);		
		createRelationshipsFromOwlInfo(owlInfo, comefrom);		
		//this.shutdown();     //无须显式调用，已经设置了挂钩函数，程序结束会调用图的shutdown
	}	
	
	/**
	 * 从本体信息(OWL Info)中创建节点，并记录节点的来源
	 * @param owl               已经解析的本体信息
	 * @param comefrom   本体来源
	 */	
	private void createNodes(OWLInfo owl, String comefrom){
		System.out.println(String.format("    #Creating class nodes from %s...", owl.getOwlFileName()));
		//取出所有的谓词，主要是概念和对象属性，这时已经做了图转换前的预处理
		Set<String> concepts = owl.getConceptTokens();
		Set<String> roles = owl.getObjPropertyTokens();			
		Transaction tx = graphDb.beginTx();   //开始事务		
		try{
			//创建概念节点
			for(String concept : concepts){				
				createNode(concept,CONCEPTLABEL,comefrom);				
			}	
			//创建 属性/角色 节点
			for(String role : roles){				
			    createNode(role,ROLETYPE,comefrom);
			}
			tx.success(); //提交事务，节点添加完成
		}finally{
			tx.close();
		}
	}	
	
	/**
	 * 创建图数据库单个节点并添加到index中
	 * @param name            节点的名称
	 * @param type              节点的类别
	 * @param comefrom    节点来源本体(first or second)
	 * @return 已创建或者存在的节点
	 */
	private Node createNode(String name, String type, String comefrom){		
		//查询图中是否存在指定的概念节点，query的参数(节点属性名，节点属性值),索引的时候是对某个节点的某个属性来索引
		Node node = nodeIndex.query(NAMEPROPERTY, name).getSingle();
		//创建一个节点，有三个属性(节点名，节点类型，节点来源)
		if(node == null){			
			node = graphDb.createNode();
			node.setProperty(NAMEPROPERTY, name);
			node.setProperty(TYPEPROPERTY, type);			
			node.setProperty(COMEFROMPROPERTY, comefrom);			
			nodeIndex.add(node, NAMEPROPERTY,name);
		}				
		return node;
	}	
	
	/**
	 * 从本体中构建关系
	 * @param owlInfo 本体.
	 * @param comefrom 本体来源，是第一个本体，还是第二个本体，此处是处理一个本体的构造，故值为first.
	 */
	private void createRelationshipsFromOwlInfo(OWLInfo owlInfo, String comefrom){
		System.out.println("    #Create relationship on graph of ontology "+owlInfo.getOwlFileName());
		//Set<OWLAxiom> Axioms = owlInfo.getAxiomsInTBox();
		List<AxiomMapping> aMappings = owlInfo.getTBoxAxiomMappings();
		Transaction tx = graphDb.beginTx(); 
		try{
			for(AxiomMapping aMapping : aMappings){
				//处理概念包含
				String subKey = aMapping.getSubKey();
				String supKey = aMapping.getSupKey();
				String relType = aMapping.getAxiomType();
				if(relType.equals("Concept"))
					createRelationship(subKey,supKey,COMEFROMFIRST,relType);
				else
					createRelationship(subKey,supKey,COMEFROMFIRST,relType);
			}
			tx.success();
		}
		finally{
			tx.close();
		}
	}	
	
	/**
	 * 创建一个图上的关系来表示包含公理，子类指向父类<br>
	 * 一个关系有三个属性：关系来源(first|second)，关系的名称(sub***sup)，关系的类型(INCLUDEDBY|MEMBEROF)
	 * @param subKey           子类标识符 
	 * @param supKey           父类表示符
	 * @param comefrom       关系来源本体
	 * @param relType           关系的类型，是role的关系还是concept的关系
	 */
	private void createRelationship(String subKey, String supKey, String comefrom, String relType){
		Relationship relationship = null;				
		Node subNode=null, supNode=null;		
		//找出节点来，图中是先把概念节点和role节点添加进去了
		try{
			 subNode = nodeIndex.query(NAMEPROPERTY, subKey).getSingle();		
			 supNode = nodeIndex.query(NAMEPROPERTY, supKey).getSingle();		
		}catch(Exception e){
			System.out.println("Exception: "+subKey + " * " + supKey);
		}			
		if(subNode!=null && supNode!=null){
			String relationshipName = Tools4Graph.getRelationshipName(subKey, supKey);			
			IndexHits<Relationship> hits = relationshipIndex.query(NAMEPROPERTY,relationshipName); //查找关系是否存在
			//如果关系不存在，添加关系，并将新的关系添加到关系索引中
			if(hits.size()==0){
				relationship = subNode.createRelationshipTo(supNode,	GlobalAttribute.RelTypes.INCLUDEDBY);
				relationship.setProperty(COMEFROMPROPERTY, comefrom);
				relationship.setProperty(NAMEPROPERTY, relationshipName);
				relationship.setProperty(TYPEPROPERTY, relType);
				relationshipIndex.add(relationship, NAMEPROPERTY, relationshipName);
			}
		}
	}
	
	/**
	 * 正常关闭图数据库
	 */
	public void shutdown(){		
		this.graphDb.shutdown();
		System.out.println("Shut down graph database.\n");
	}
}
	

	

