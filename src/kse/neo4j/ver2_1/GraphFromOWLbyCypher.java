package kse.neo4j.ver2_1;

import java.util.List;
import java.util.Set;

import kse.misc.Tools;
import kse.neo4j.ver1_8.PreOWL4Graph;
import kse.neo4j.ver1_8.Tools4Graph;
import kse.owl.AxiomMapping;
import kse.owl.OWLInfo;
import static kse.misc.GlobalParams.*;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;


/**
 * 使用Cypher2.1来维护图数据库，从owl文件中创建图数据库。<br>
 * 图数据库中节点label为：Concept, Role, Individual.<br>
 * 节点有属性：name, type:概念名或实例名或关系名 <br>
 * 图数据库中relationship为：MEMBEROF, INCLUDEDBY.  <br>
 * 关系relationship的属性 ComeFrom,RelNodeType(Role,Concept,Individual)
 * @author Xuefeng Fu
 */

public class GraphFromOWLbyCypher {	
	
	private GraphDatabaseService graphDB;	
	private String dbPath;
	private OWLInfo owlInfo;
	private String comefrom;                                               //该本体是第一个本体，还是第二个本体
	private StringBuilder query;
	private ExecutionEngine engine;                                   //Cypher引擎 
	private Index<Node> nodeIndex;                               //节点索引，方便查找节点	
	private Index<Relationship> relationshipIndex;        //关系索引，方便查找节点
	
	/**
	 * 构造函数
	 * @param dbPath           图数据库存储路径
	 * @param owlPath          要转换的本体文件
	 * @param comefrom      如果要处理两个OWL的合并，comefrom表示来第几个本体	 
	 */
	
	public GraphFromOWLbyCypher(String dbPath, String owlPath, String comefrom){		
		this.dbPath = dbPath;
		this.comefrom = comefrom;
		this.owlInfo = new OWLInfo(owlPath);	
		this.query = new StringBuilder();			
	}
	
	public GraphDatabaseService getGraphDB(){
		return this.graphDB;
	}
	
	public ExecutionEngine getEngine(){
		return this.engine;
	}
	public OWLInfo getOwlInfo() {
		return owlInfo;
	}

	/**
	 * 初始化图数据库
	 * @param isClear            是否清除原图数据库,如果是添加TBox，则不能清除原数据
	 */
	public void init(boolean isClear){
		System.out.println("Initializing graph database...");
		//是否清除已经存在的数据库
		if(isClear){
			Tools4Graph.clearDb(dbPath);
		}	
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase( dbPath );
		Tools4Graph.registerShutdownHook(graphDB);  //挂钩函数，程序结束自动调用来释放图数据库
		
		this. engine = new ExecutionEngine( graphDB );
		
		//为转换成图结构作进一步的预处理,如果是来自第二个数据库或者清除了数据库，则要重新预处理本体
		if(comefrom.equalsIgnoreCase(COMEFROMSECOND) || isClear){
			System.out.println("Preprocessing owl for building graph database ... ...");
			PreOWL4Graph.handle(owlInfo);			
		}
		createIndexOnName();	
		createIndexByCypher();
		System.out.println("Init end.");
	}
	
	/**
	 * 创建索引
	 */
	public void createIndexOnName(){
		System.out.println("Creating index for nodes...");
		try(Transaction tx = graphDB.beginTx()){			
			IndexManager indexM = graphDB.index();
			nodeIndex = indexM.forNodes(NODEINDEX);  //节点索引，参数是索引的名字			
			relationshipIndex = indexM.forRelationships(RELATIONSHIPINDEX); //关系索引				
			tx.success();
		}
	}
	
	public void createIndexByCypher(){
		System.out.println("Creating Cypher index...");
		try(Transaction tx = graphDB.beginTx()){			
			
			//创建索引: Concept(Name) Individual(Name) Role(Name) 
			Tools.clear(query);			
			query.append(String.format("CREATE INDEX ON :%s(%s) ",CONCEPTLABEL,NAMEPROPERTY));
			engine.execute(query.toString());
			
			Tools.clear(query);
			query.append(String.format("CREATE INDEX ON :%s(%s) ",INDIVIDUALLABEL,NAMEPROPERTY));
			engine.execute(query.toString());
			
			Tools.clear(query);
			query.append(String.format("CREATE INDEX ON :%s(%s) ",ROLELABEL,NAMEPROPERTY));
			engine.execute(query.toString());
			
			//也可以使用语句
			//Schema schema = graphDb.schema();
		   // indexDefinition = schema.indexFor( DynamicLabel.label( "User" ) ).on( "username" ).create();
		   //	schema.awaitIndexOnline( indexDefinition, 10, TimeUnit.SECONDS )
			
			//创建Individual(Name)索引
			/*Tools.clear(query);
			query.append(String.format("CREATE INDEX ON :%s(%s) ",INDIVIDUALLABEL,NAMEPROPERTY));		
			engine.execute(query.toString());*/
			tx.success();
		}
	}
	
	/**
	 * 从一个本体中添加TBox到图数据库中
	 * @param isClear 是否清除原数据库
	 */
	public void addTBoxToGraphDB(boolean isClear){
		init(isClear);		
		System.out.println("Adding TBox to graph database.");
		createNodes();
		createRelationships();		
	}
	
	/**
	 * 从一个本体中的ABox添加到图数据库中
	 * @param isInit  是否要初始化，true则初始化
	 * @param isClear  是否要清除原有的数据， true则清除
	 */
	
	public void addABoxToGraphDB(boolean isInit, boolean isClear){
		//如果是没有初始化，则先初始化，这通常是针对添加ABox的情况
		if(isInit)
			init(isClear);
		
		System.out.println("Insert ABox into Graph Database  ... ...");
		List<AxiomMapping> aBoxMapping = owlInfo.getABoxAxiomMapping();
		System.out.println("#Insert individual node into Graph...");
		try(Transaction tx = graphDB.beginTx()){
			for(AxiomMapping aMapping : aBoxMapping){
				String subKey = aMapping.getSubKey();                        //subkey为个体实例名				
				createNode(subKey, INDIVIDUALLABEL); //创建实例节点				
			}
			tx.success();
			
		}	
		createRelationshipsOfABox();	
		
	}
	
	/**
	 * 从本体信息(OWL Info)中创建节点，并记录节点的来源,并加入索引
	 * @param owl               已经解析的本体信息
	 * @param comefrom   本体来源
	 */	
	private void createNodes(){
		System.out.println(String.format("#Creating class nodes from %s...", owlInfo.getOwlFileName()));
		//取出所有的谓词，主要是概念和对象属性，这时已经做了图转换前的预处理
		Set<String> concepts = owlInfo.getConceptTokens();
		Set<String> roles = owlInfo.getObjPropertyTokens();	
		System.out.println("The nodes size is " + (concepts.size() + roles.size()));
		//创建 Concept 节点
		try(Transaction tx = graphDB.beginTx()){
			for(String concept : concepts){				
				createNode(concept,CONCEPTLABEL);				
			}	
			//创建 role 节点
			for(String role : roles){				
			    createNode(role,ROLELABEL);
			}		
			tx.success();
		}
	}	
	
	/**
	 * 创建图数据库单个节点并添加到index中<br>
	 * 节点有一个标签(Concept, Role, Individual)，二个属性： 节点名 、节点来源
	 * @param nodeName  节点的名称
	 * @param nodeLabel   节点的标签
	 */
	
	public void createNode(String nodeName, String nodeLabel){
		
		/*if(!isNodeExistence(query,_node)){ //节点是否存在
			Tools.clear(query);
			//参数分别是: 节点label，节点名， 节点来源
			String createNodeFormatter = "CREATE(_node:%s{Name:'%s', ComeFrom:'%s'}) RETURN _node";	
			query.append(String.format(createNodeFormatter, nodeLabel, nodeName, this.comefrom));
			ExecCypher.simpleCypher(query,graphDB);
		}	*/	
		//参数分别是:节点label, 节点名, 用来判断某个节点是否已经存在图中
		
		//查询图中是否存在指定的概念节点,会自动调用索引	
		
		//if(!(isNodeExistence(nodeLabel,nodeName))){ //节点是否存在
		//也可以使用约束的方法  CREATE CONSTRAINT ON (node:Concept) ASSERT node.Name IS UNIQUE
		if( !isNodeExistenceByIndex(nodeName)){
			Tools.clear(query);
			String createNodeFormatter = "CREATE  (_node:%s{Name:'%s', ComeFrom:'%s'}) RETURN _node";	
			query.append(String.format(createNodeFormatter, nodeLabel, nodeName, this.comefrom));
			//System.out.println(query.toString());
			ExecutionResult result = engine.execute(query.toString());
			//Add node to index
			if(result.columnAs("_node").hasNext()){
				Node _n = (Node) result.columnAs("_node").next();
				nodeIndex.add(_n, NAMEPROPERTY, nodeName);
			}	
		}			
	}	
	
	/**
	 * 构建节点之间的关系
	 */
	private void createRelationships(){		
		List<AxiomMapping> aMappings = owlInfo.getTBoxAxiomMappings();		
		System.out.println("#Create relationship on graph of ontology "+owlInfo.getOwlFileName()+",Mapping size is "+aMappings.size());
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每2000个提交一次
		while(iCount<aMappings.size()){
			try(Transaction tx = graphDB.beginTx()){
				iEnd = iCount + ONCE_REL_NUMBER;
				if(iEnd > aMappings.size()){
					iEnd = aMappings.size();
				}
				while(iCount < iEnd){
					AxiomMapping aMapping = aMappings.get(iCount);
					//处理概念包含
					String subKey = aMapping.getSubKey();
					String supKey = aMapping.getSupKey();
					String relNodeType = aMapping.getAxiomType();
					createRelationship(subKey,supKey,relNodeType, INCLUDEDREL);	
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
	 * 一个关系有三个属性：关系来源(first|second)，关系的名称(sub***sup)，关系的类型(INCLUDEDBY|MEMBEROF),使用index来查询
	 * @param subKey               子类标识符 
	 * @param supKey               父类表示符
	 * @param relNodeType      关系的节点类型，是role的关系还是concept的关系
	 * @param relType               关系的类型，是INCLUDEDBY的关系还是MEMBEROF的关系
	 */
	public void createRelationship(String subKey, String supKey, String relNodeType, String relType){	
		if(!(subKey.equalsIgnoreCase(supKey))){
			String matchFormatter = "MATCH (sub:%s{Name:'%s'}),(sup:%s{Name:'%s'}) " ;			
			String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";			
			Tools.clear(query);		
			
			query.append(String.format(matchFormatter,relNodeType, subKey, relNodeType, supKey));
			query.append(String.format(createFormatter, relType, relNodeType, comefrom));
			//System.out.println(query.toString());
			engine.execute(query.toString());
		}
	}
	
	public void createRelationshipsOfABox(){
		System.out.println("#Insert membership relation into Graph...");
		List<AxiomMapping> aBoxMapping = owlInfo.getABoxAxiomMapping();
		/*for(AxiomMapping aMapping : aBoxMapping){
			String subKey = aMapping.getSubKey();
			String supKey = aMapping.getSupKey();
			String relNodeType = aMapping.getAxiomType();				
			createRelationshipOfABox(subKey, supKey,  relNodeType, MEMBEROFREL); //创建membership关系节点
		}
		*/
		//System.out.println("#Create relationship on graph of ontology "+owlInfo.getOwlFileName()+",Mapping size is "+aMappings.size());
		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每2000个提交一次
		while(iCount<aBoxMapping.size()){
			try(Transaction tx = graphDB.beginTx()){
				iEnd = iCount + ONCE_REL_NUMBER;
				if(iEnd > aBoxMapping.size()){
					iEnd = aBoxMapping.size();
				}
				while(iCount < iEnd){
					AxiomMapping aMapping = aBoxMapping.get(iCount);
					//处理概念包含
					String subKey = aMapping.getSubKey();
					String supKey = aMapping.getSupKey();
					String relNodeType = aMapping.getAxiomType();
					createRelationshipOfABox(subKey,supKey,relNodeType, MEMBEROFREL);	
					++iCount;					
				}	
				tx.success();
				System.out.println(iCount);
				if(iCount==aBoxMapping.size()-1)  //可能会出错
					break;
			}
		}
	}
	
	/**
	 * 创建一个图上的关系来表示包含公理，子类指向父类<br>
	 * 一个关系有三个属性：关系来源(first|second)，关系的名称(sub***sup)，关系的类型(INCLUDEDBY|MEMBEROF),使用index来查询
	 * @param subKey               子类标识符 
	 * @param supKey               父类表示符
	 * @param relNodeType      关系的节点类型，是role的关系还是concept的关系
	 * @param relType               关系的类型，是INCLUDEDBY的关系还是MEMBEROF的关系
	 */
	public void createRelationshipOfABox(String subKey, String supKey, String relNodeType, String relType){	
		if(!(subKey.equalsIgnoreCase(supKey))){
			String matchFormatter = "MATCH (sub:%s{Name:'%s'}),(sup:%s{Name:'%s'}) " ;			
			String createFormatter = "CREATE UNIQUE (sub)-[:%s{RelNodeType:'%s',ComeFrom:'%s' }]->(sup) ";			
			Tools.clear(query);		
			
			query.append(String.format(matchFormatter,relNodeType, subKey, CONCEPTLABEL, supKey));
			query.append(String.format(createFormatter, relType, relNodeType, comefrom));
			//System.out.println(query.toString());
			engine.execute(query.toString());
		}
	}

	public boolean isNodeExistenceByIndex(String nodeName){
		Node node = nodeIndex.get(NAMEPROPERTY, nodeName).getSingle();
		if(node != null){
			return true;
		}
		else{			
			return false;
		}
	}
	
	/**
	 * 判断某个关系是否存在，如果不存在返回false
	 * @param relName
	 * @return
	 */
	public boolean isRelExistenceByIndex(String relName){		
		Relationship rel = relationshipIndex.get(NAMEPROPERTY, relName).getSingle();
		if(rel != null) {
			return true;
		}
		else{			
			return false;
		}
	}
	/**
	 * 图数据库中是否存在某个节点
	 * @param nodeName 节点名
	 * @param labelName  标签名(Concept, Role, Individual)
	 * @return 某节点是否存在
	 */
	public  boolean isNodeExistence(String nodeName, String labelName){
		String nodeQueryFormatter = "MATCH(_node:%s {Name: '%s'})  RETURN _node; "; 	
		String _query = String.format(nodeQueryFormatter, labelName, nodeName);
		System.out.println("Cypher:"+_query);
		boolean isExistence = false;
		ExecutionResult result =  engine.execute(_query);
		if(result.columnAs("_node").hasNext()){
			System.out.println("The node has exist in the graph database. ");
			isExistence = true;
		}								
		return isExistence;
	}
	
	//String createNodeFormatter = "CREATE  (_node:%s{Name:'%s', ComeFrom:'%s'}) RETURN _node";	
	//query.append(String.format(createNodeFormatter, nodeLabel, nodeName, this.comefrom));
	
	/**
	 * 输出当前图中所有的节点
	 */
	public void listNodes(){
		String _query = "MATCH (n) RETURN (n)";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			System.out.println(result.dumpToString());
		}	
	}
	
	public void listNodes(String label){
		String _query = "MATCH (n:%s) RETURN (n)";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(String.format(_query, label));
			tx.success();
			System.out.println(result.dumpToString());
		}	
	}
	/**
	 * 输出所有的关系
	 */
	public void listRelationships(){
		String _query = "MATCH (a-[r]->b) RETURN r";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			System.out.println(result.dumpToString());
		}
	}
	
	/**
	 * 输出所有的直接路径
	 */
	public void listPaths(){
		String _query = "MATCH p=(a-[*1]->b) RETURN p ";
		try(Transaction tx = graphDB.beginTx()){		
			ExecutionResult result =  engine.execute(_query);
			tx.success();
			System.out.println(result.dumpToString());
		}		
	}
	
	public void shutDown(){
		this.graphDB.shutdown();
	}
}
