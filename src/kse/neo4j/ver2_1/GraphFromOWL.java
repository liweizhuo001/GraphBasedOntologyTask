package kse.neo4j.ver2_1;

import static kse.misc.GlobalParams.*;

import java.util.List;
import java.util.Set;

import kse.misc.Tools;
import kse.neo4j.ver1_8.GlobalAttribute;
import kse.neo4j.ver1_8.PreOWL4Graph;
import kse.neo4j.ver1_8.Tools4Graph;
import kse.owl.AxiomMapping;
import kse.owl.OWLInfo;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
//import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.schema.IndexDefinition;
import org.neo4j.graphdb.schema.Schema;

/**
 * 使用函数方法来构建图数据库，建立了节点索引和关系索引
 * [2015-5]
 * @author Xuefeng Fu
 */
@SuppressWarnings("unused")
public class GraphFromOWL {
	private String comefrom;                                               //该本体是第一个本体，还是第二个本体
	private GraphDatabaseService graphDB;	
	private String dbPath;
	private OWLInfo owlInfo;
	private Index<Node> nodeIndex;                               //节点索引，方便查找节点	
	private Index<Relationship> relationshipIndex;        //关系索引，方便查找节点
	private Label roleLabel;
	private Label conceptLabel;
	private Label individualLabel;
	
	/**
	 * 构造函数
	 * @param dbPath           图数据库存储路径
	 * @param owlPath                  要转换的本体文件
	 * @param comefrom      如果要处理两个OWL的合并，comefrom表示来第几个本体
	 */
	public GraphFromOWL(String dbPath, String owlPath, String comefrom){		
		this.dbPath = dbPath;	
		this.comefrom = comefrom;			
		owlInfo = new OWLInfo(owlPath);		
		conceptLabel = DynamicLabel.label(CONCEPTLABEL);
		roleLabel = DynamicLabel.label(ROLELABEL);
		individualLabel = DynamicLabel.label(INDIVIDUALLABEL);
	} 
	
	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}

	private void init(boolean isClear){
		//是否清除已经存在的数据库
		if(isClear){
			Tools4Graph.clearDb(dbPath);
		}
		//创建一个新的数据库或者打开一个已经存在的数据库，实例可以在多个线程中共享，但不能创建多个实例来指向同一个数据库
		graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩，程序结束自动调用
		//创建索引
		try(Transaction tx = graphDB.beginTx()){
			
			System.out.println("Creating index for nodes and relationships on name");
			IndexManager indexM = graphDB.index();
			nodeIndex = indexM.forNodes(NODEINDEX);  //节点索引，参数是索引的名字			
			relationshipIndex = indexM.forRelationships(RELATIONSHIPINDEX); //关系索引
			
			createIndexOnNameByCypher();  //创建 Concept(Name) 索引
		}
	}
	
	public void addTBoxToGraphDB(boolean isClear){		
		init(isClear);
		System.out.println("Preprocessing owlInfo for graph ... ...");
		PreOWL4Graph.handle(owlInfo);
		System.out.println("Create graph database from owlInfo ... ...");
		createNodes();		
		createRelationshipsFromTBox();	
	}	
	
	/**
	 * 从一个本体中添加ABox到图数据库中
	 */
	
	public void addABoxToGraphDB(){
		System.out.println("Insert ABox into Graph Database  ... ...");
		List<AxiomMapping> aBoxMapping = owlInfo.getABoxAxiomMapping();
		System.out.println("#Insert individual node into Graph...");
		try(Transaction tx = graphDB.beginTx()){
			for(AxiomMapping aMapping : aBoxMapping){
				String subKey = aMapping.getSubKey();                        //subkey为个体实例名				
				createNode(subKey, individualLabel); //创建实例节点				
			}
			tx.success();
		}
		try(Transaction tx = graphDB.beginTx()){
			System.out.println("#Insert membership relation into Graph...");
			for(AxiomMapping aMapping : aBoxMapping){
				String subKey = aMapping.getSubKey();
				String supKey = aMapping.getSupKey();
				String relNodeType = aMapping.getAxiomType();				
				createRelationship(subKey, supKey,  relNodeType, GlobalAttribute.RelTypes.MEMBEROF); //创建membership关系节点
			}
			tx.success();
		}
	}
	
	/**
	 * 从本体信息(OWL Info)中创建节点，并记录节点的来源
	 */	
	private void createNodes(){
		System.out.println(String.format("#Creating class nodes from %s...", owlInfo.getOwlFileName()));
		//取出所有的谓词，主要是概念和对象属性，这时已经做了图转换前的预处理
		Set<String> concepts = owlInfo.getConceptTokens();
		Set<String> roles = owlInfo.getObjPropertyTokens();		
		System.out.println("The size of nodes is " + (concepts.size()+roles.size()));
		
		try(Transaction tx = graphDB.beginTx()){  //开始事务		
			//创建概念节点
			for(String concept : concepts){				
				createNode(concept,conceptLabel);				
			}	
			//创建 属性/角色 节点
			for(String role : roles){				
			    createNode(role,roleLabel);
			}
			
			tx.success(); //提交事务，节点添加完成
		}
	}
	
	/**
	 * 创建图数据库单个节点并添加到index中,节点有一个label和两个属性(Name,ComeFrom)
	 * @param name            节点的名称
	 * @param type              节点的label
	 * @param comefrom    节点来源本体(first or second)
	 * @return 已创建或者存在的节点
	 */
	private Node createNode(String name,  Label label){		
		//查询图中是否存在指定的概念节点，query的参数(节点属性名，节点属性值),索引的时候是对某个节点的某个属性来索引
		Node node = nodeIndex.query(NAMEPROPERTY, name).getSingle();
		//创建一个节点，有三个属性(节点名，节点类型，节点来源)
		if(node == null){			
			node = graphDB.createNode(label);
			node.setProperty(NAMEPROPERTY, name);
			node.setProperty(COMEFROMPROPERTY, comefrom);		
			//node.addLabel(label);
			nodeIndex.add(node, NAMEPROPERTY,name);
			//System.out.println(nodeIndex.query(NAMEPROPERTY, name).size());
		}				
		return node;
	}	
	
	/**
	 * 从本体中构建关系
	 */
	private void createRelationshipsFromTBox(){
		System.out.println("#Create relationship on graph of ontology "+owlInfo.getOwlFileName());
		//Set<OWLAxiom> Axioms = owlInfo.getAxiomsInTBox();
		List<AxiomMapping> aMappings = owlInfo.getTBoxAxiomMappings();
		System.out.println("Mapping size is "+ aMappings.size());
		/*Transaction tx = graphDB.beginTx(); 
		try{
			for(AxiomMapping aMapping : aMappings){
				//处理概念包含
				String subKey = aMapping.getSubKey();
				String supKey = aMapping.getSupKey();
				String relNodeType = aMapping.getAxiomType();
				createRelationship(subKey,supKey,relNodeType,GlobalAttribute.RelTypes.INCLUDEDBY);				
			}
			tx.success();
		}
		finally{
			tx.close();
		}*/
		
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每2000个提交一次
		while(iCount<aMappings.size()){
			try(Transaction tx = graphDB.beginTx()){
				iEnd = iCount+2000;
				if(iEnd >= aMappings.size())
					iEnd = aMappings.size();
				while(iCount < iEnd){
					AxiomMapping aMapping = aMappings.get(iCount);
					//处理概念包含
					String subKey = aMapping.getSubKey();
					String supKey = aMapping.getSupKey();
					String relNodeType = aMapping.getAxiomType();
					createRelationship(subKey,supKey,relNodeType,GlobalAttribute.RelTypes.INCLUDEDBY);			
					++iCount;
				}	
				tx.success();
				System.out.println(iCount);
				if(iCount==aMappings.size()-1)  //可能会出错
					break;
			}
		}
		
	}
	
	/**
	 * 创建一个图上的关系来表示包含公理，子类指向父类<br>
	 * 一个关系有三个属性：关系来源(first|second)，关系的名称(sub***sup)，关系的类型(INCLUDEDBY|MEMBEROF)
	 * @param subKey           子类标识符 
	 * @param supKey           父类表示符
	 * @param relNodeType           关系的类型，是role的关系还是concept的关系
	 */
	private void createRelationship(String subKey, String supKey, String relNodeType, GlobalAttribute.RelTypes relType ){
		Relationship relationship = null;				
		Node subNode=null, supNode=null;		
		//找出节点来，图中是先把概念节点和role节点添加进去了
		try{
			 subNode = nodeIndex.query(NAMEPROPERTY, subKey).getSingle();		
			 supNode = nodeIndex.query(NAMEPROPERTY, supKey).getSingle();		
		}catch(Exception e){
			System.out.println("Exception: "+subKey + " * " + supKey);
		}			
		//若节点不存在，则创建节点
		if(subNode == null){
			Label label = DynamicLabel.label(relNodeType);
			createNode(subKey,label);
		}
		if(supNode == null){
			Label label = DynamicLabel.label(relNodeType);
			createNode(supKey,label);
		}
		
		String relationshipName = Tools4Graph.getRelationshipName(subKey, supKey);			
		//System.out.println("Creating relationship "+ relationshipName);
		//IndexHits<Relationship> hits = relationshipIndex.query(NAMEPROPERTY,relationshipName); //查找关系是否存在
		//如果关系不存在，添加关系，并将新的关系添加到关系索引中
		//if(hits.size()==0){
			relationship = subNode.createRelationshipTo(supNode,relType);
			relationship.setProperty(COMEFROMPROPERTY, comefrom);
			relationship.setProperty(NAMEPROPERTY, relationshipName);
			relationship.setProperty(TYPEPROPERTY, relNodeType);
			relationshipIndex.add(relationship, NAMEPROPERTY, relationshipName);
		//}
	}
	
	//建立基于Cypher的索引，在执行Cypher时提高效率，这类索引只能用于带标签的节点
	public void createIndexOnNameByCypher(){
		System.out.println("Creating index on name property of node with label");
		/*StringBuilder query = new StringBuilder();
		ExecutionEngine engine = new ExecutionEngine(graphDB);
			
		Tools.clear(query);			
		query.append(String.format("CREATE INDEX ON :%s(%s) ",CONCEPTLABEL,NAMEPROPERTY));
		engine.execute(query.toString());*/
			
		/*Tools.clear(query);
		query.append(String.format("CREATE INDEX ON :%s(%s) ",INDIVIDUALLABEL,NAMEPROPERTY));		
		engine.execute(query.toString());*/	
		
		try ( Transaction tx = graphDB.beginTx() )
		{
		    Schema schema = graphDB.schema();
		    schema.indexFor( conceptLabel ).on(NAMEPROPERTY ).create();  //创建索引,返回IndexDefinition indexDefinition;
		    schema.indexFor( roleLabel ).on(NAMEPROPERTY ).create();
		    tx.success();
		}
			
	}	
	public void shutdown(){
		this.graphDB.shutdown();
	}
}


