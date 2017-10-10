package kse.neo4j.ver1_8;

import static kse.misc.GlobalParams.*;

import java.util.List;

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
 * 将ABox本体(membership assertion)添加到图数据库中.<br>
 * 前提:图数据库中已经有TBox结构，ABox中的概念和角色在图中都存在
 * 应用场景：TBox+ABox是一致的，当新增一个ABox导致不一致
 * 
 * @author Xuefeng fu
 *
 */

public class InsertABoxToGraph {
	private String comefrom;                                                //comefrom Abox本体来源
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
	public InsertABoxToGraph(String dbPath, String owlPath, String comefrom){		
		this.comefrom = comefrom;
		//创建一个新的数据库或者打开一个已经存在的数据库，实例可以在多个线程中共享，但不能创建多个实例来指向同一个数据库
		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );	
		Tools4Graph.registerShutdownHook(graphDb);
		try(Transaction tx = graphDb.beginTx()){
			nodeIndex = graphDb.index().forNodes(NODEINDEX);  //参数是节点索引的名字
			relationshipIndex = graphDb.index().forRelationships(RELATIONSHIPINDEX); //参数是关系索引的名字
		}
		owlInfo = new OWLInfo(owlPath);		
	}
	
	public OWLInfo getOwlInfo() {
		return owlInfo;
	}

	
	/**
	 * 从ABox映射中找出所有的Individual,并添加到图数据库中，其中实例与类建立memberof关系
	 */
	public void InsertABoxIntoGraph(){		
		//System.out.println("preprocessing owlInfo for graph ... ...");
		//PreOWL4Graph.handle(getOwlInfo());
		System.out.println("Insert ABox into Graph Database  ... ...");
		List<AxiomMapping> aBoxMapping = owlInfo.getABoxAxiomMapping();
		//开始节点添加的事务
		Transaction txForNode = graphDb.beginTx();   		
		try{
			System.out.println("	#Insert individual node into Graph...");
			for(AxiomMapping aMapping : aBoxMapping){
				String subKey = aMapping.getSubKey();                        //subkey为个体实例名				
				createNode(subKey, INDIVIDUALTYPE,comefrom); //创建实例节点				
			}
			txForNode.success(); //提交事务，节点添加完成
		}finally{
			txForNode.close();
		}
		//开始关系添加的事务	
		Transaction txForRelation = graphDb.beginTx();   
		try{
			System.out.println("	#Insert membership relation into Graph...");
			for(AxiomMapping aMapping : aBoxMapping){
				String subKey = aMapping.getSubKey();
				String supKey = aMapping.getSupKey();
				String relType = aMapping.getAxiomType();				
				createMembershipRelationship(subKey, supKey, comefrom, relType); //创建membership关系节点
			}
			txForRelation.success(); //提交事务，关系添加完成
		}finally{
			txForRelation.close();
		}
		
		shutdown();
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
	 * 创建一个图上的关系来表示MembershipAssertion，实例指向类, 图上的关系是memberof
	 * @param subKey           实例标识符 
	 * @param supKey           类表示符
	 * @param comefrom       关系来源本体
	 * @param relType           关系的类型，individual
	 */
	private void createMembershipRelationship(String subKey, String supKey, String comefrom, String relType){
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
			String relationshipName = Tools4Graph.getIndividualRelName(subKey,supKey);	//membership关系的名字就是实例的名字	
			IndexHits<Relationship> hits = relationshipIndex.query(NAMEPROPERTY,relationshipName); //查找关系是否存在
			//如果关系不存在，添加关系，并将新的关系添加到关系索引中
			if(hits.size()==0){
				relationship = subNode.createRelationshipTo(supNode,	GlobalAttribute.RelTypes.MEMBEROF);
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
		System.out.println("Insert ABox into graph database end.\n");
	}

}
