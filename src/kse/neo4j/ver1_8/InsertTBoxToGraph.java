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

/**
 * 将一个新的TBox本体插入到图数据库中.<br>
 * 图数据库中已经有一个TBox本体，COMEFROM=first.<br>
 * 新的TBox中的节点和关系的COMEFROM=second.
 * @author Xuefeng fu
 *
 */

public class InsertTBoxToGraph {
	private static final String comefrom="second";     //comefrom第二个本体
	private GraphDatabaseService graphDb;	
	private OWLInfo owlInfo;
	private  Index<Node> nodeIndex;                               //节点索引，方便查找节点	
	private Index<Relationship> relationshipIndex;        //关系索引，方便查找节点
	
	/**
	 * 构造函数，不需要清除原来的数据库，因为是将当前的Tbox本体添加到图中
	 * @param dbPath           已存在的图数据库存储路径
	 * @param owlPath          要新增加的本体文件
	 * @param comefrom      如果要处理两个OWL的合并，comefrom表示来第几个本体,当前值为second
	 * @param isClear            是否清除已存在的同名图数据库
	 */
	public InsertTBoxToGraph(String dbPath, String owlPath){		
		//是否清除已经存在的数据库
		/*if(isClear){
			Tools4Graph.clearDb(dbPath);
		}*/
		
		//创建一个新的数据库或者打开一个已经存在的数据库，实例可以在多个线程中共享，但不能创建多个实例来指向同一个数据库
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
		Tools4Graph.registerShutdownHook(graphDb);
		try(Transaction tx = graphDb.beginTx()){
			nodeIndex = graphDb.index().forNodes(NODEINDEX);  //参数是索引的名字
			relationshipIndex = graphDb.index().forRelationships(RELATIONSHIPINDEX); //
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
	
	public void InsertOwlIntoGraph(){		
		System.out.println("preprocessing owlInfo for graph ... ...");
		PreOWL4Graph.handle(getOwlInfo());
		System.out.println("Insert TBox into Graph Database  ... ...");
		createNodesFromOwl(owlInfo);		
		createRelationshipsFromOwlInfo(owlInfo);	
		shutdown();
	}	
	
	/**
	 * 从本体信息(OWL Info)中创建节点，并记录节点的来源
	 * @param owl               已经解析的本体信息
	 * @param comefrom   本体来源
	 */	
	private void createNodesFromOwl(OWLInfo owl){
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
	 */
	private void createRelationshipsFromOwlInfo(OWLInfo owlInfo){
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
				createRelationship(subKey,supKey,comefrom,relType);				
			}
			tx.success();
		}
		finally{
			tx.close();
		}
	}	
	
	/**
	 * 创建一个图上的关系来表示包含公理，子类指向父类
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
		System.out.println("Insert TBox into graph database end.\n");
	}
}