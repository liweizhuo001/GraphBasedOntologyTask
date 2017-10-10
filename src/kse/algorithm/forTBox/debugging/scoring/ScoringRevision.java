package kse.algorithm.forTBox.debugging.scoring;

import static kse.misc.GlobalParams.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import kse.algorithm.Diagnosis;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.algorithm.auxiliaryClass.UnsatTriple;
import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.RefinementOperator;
import kse.algorithm.forTBox.debugging.RefinementSpace;
import kse.neo4j.ver2_1.ExecCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;


/**
 * 基于图数据库的不协调TBox修正
 * 先从图中计算MIPP，再从MIPP中提取diagnosis(MIPP中源自第一个本体的关系|公理)
 * 完成关系到MIPP的映射,即一个关系可能出现在多个MIPP中，这样当修正选定了该关系时，也同时完成了这些MIPP的修正。
 * [2015-5-30]
 * 从MIPP中计算unwanted集合，其中包含确定要删除的公理
 * 从unwanted集合中计算refinementSet, 其中包含待添加的公理(relationship)
 * @author Xuefeng Fu
 */
public class ScoringRevision {
	String gPath; 
	GraphDatabaseService graphDB; 	
	//关系到MIPS集合的映射，记录出现该关系(是诊断中的成员)的MIPS
	Map<Relationship, Set<MIPP>> relMappingMIPP; 
	List<MIPP> mipps;     //TBox中所有的MIPS		
	Set<RevisionSpace> reviSpace;
	Diagnosis diag ;		
	List<Relationship> unWantedSet;
	List<RefinementSpace> refinementSet;	
	
	public List<Relationship> getUnWantedSet() {
		return unWantedSet;
	}

	public List<RefinementSpace> getRefinementSet() {
		return refinementSet;
	}
	
	public ScoringRevision(String gDB){
		this.gPath = gDB;
		this.graphDB =  new GraphDatabaseFactory().newEmbeddedDatabase(gDB);		
		this.diag = Diagnosis.getDiagnosis(graphDB);		
		try(Transaction tx = graphDB.beginTx()){
			this.init();
			tx.success();
		}
	}
	
	public String getGPath() {
		return gPath;
	}

	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}

	public Map<Relationship, Set<MIPP>> getRelMappingMIPP() {
		return relMappingMIPP;
	}

	public List<MIPP> getMIPPs() {
		return mipps;
	}

	public Set<RevisionSpace> getReviSpace() {
		return reviSpace;
	}
	/**
	 * 调用函数来初始化类中的一些关键数据
	 */
	private void init(){
		this.calRMappingM();
	}
	
	public void goRevising(){
		try(Transaction tx = graphDB.beginTx()){
			
			unWantedSet = this.calUnwantedSet();	 //初始化一步
			this.calRevisionSpace(unWantedSet);
			
			RefinementOperator operator = new RefinementOperator();
			refinementSet = operator.calRefinementSet(unWantedSet, relMappingMIPP, graphDB);
			
			operator.updateGraph(unWantedSet, refinementSet, graphDB);  //修正方案
			printInfoOfRevision();
		}
	}
	/**
	 * 计算关系到MIPP的映射。
	 * 一个关系(公理)可能出现在多个MIPP中，该映射记录关系到包含它的MIPP集合的映射
	 */
	public void calRMappingM(){
		relMappingMIPP = new HashMap<>(); //关系到MIPP的映射
		List<UnsatTriple> triples = diag.getUnsatTripleByRelationship();		
		mipps = Diagnosis.getDiagnosis(graphDB).compMIPPs(triples);  //取出Graph中所有的MIPP
		//完成Relationship到MIPS的映射
		System.out.println("Getting the relationship in MIPP...");
		for(MIPP mipp : mipps){			
			List<Relationship> relOfDiag = mipp.getDiagnosis(); //取出诊断
			for(Relationship r: relOfDiag){
				Set<MIPP> current = relMappingMIPP.get(r);
				if(current == null){
					current = new HashSet<>();
					relMappingMIPP.put(r,current);
				}
				if(!(current.contains(mipp))){
					current.add(mipp);
				}				
			}
		}		
	}
	
	/**
	 * 计算待修正的关系,在MIPP以及关系与MIPP的映射的副本上操作，不影响源数据
	 * 新修改函数[15-5-30]
	 */
	
	public List<Relationship> calUnwantedSet(){
		List<Relationship> rels = new ArrayList<>();
		List<MIPP> _mipps = new ArrayList<>(); //MIPP集合的一个副本
		Map<Relationship, Set<MIPP>> _relMappingMIPP = new HashMap<>();	//每个关系包含对应的最小冲突子集	
		
		for(MIPP m : mipps){  //初始化MIPP集合副本
			_mipps.add(m);
		}
		for(Relationship r : relMappingMIPP.keySet()){ //初始化关系-MIPP集合映射副本
			Set<MIPP> _ms = new HashSet<>();
			for(MIPP m: relMappingMIPP.get(r)){
				_ms.add(m);				
			}	
			_relMappingMIPP.put(r, _ms);
		}	
		
		Relationship _rel = null;
		
		while(_mipps.size() !=0){
			_rel = getMaxRel(_relMappingMIPP);
			rels.add(_rel);
			updateState(_rel, _mipps, _relMappingMIPP);
		}		
		System.out.println("The size of set of relation for revising : "+ rels.size());
		return rels;
	}	
	//--End --
	
	/**
	 * 取出当前出现在MIPP集合中频率最高的Relationship
	 * @param _relMappingMIPP
	 * @return Relationship with max frequent
	 */
	public Relationship getMaxRel(Map<Relationship, Set<MIPP>> _relMappingMIPP){
		int max = 0;
		Relationship rel = null;
		for(Relationship r : _relMappingMIPP.keySet()){			
			if(r.getEndNode().getProperty(NAMEPROPERTY).toString().startsWith(NEGATIVESIGN))
				continue;
			if(_relMappingMIPP.get(r).size()>max){
				max = _relMappingMIPP.get(r).size();
				rel = r;
			}			
		}		
		System.out.println("Selected relation:"+GlobalFunct.relToStr(rel)+":"+max);
		return rel;
	}
	
	/**
	 * 更新副本数据的状态
	 * @param _rel 已选定的关系
	 * @param _mipps MIPP集合的副本
	 * @param _relMappingMIPP 关系-集合映射的副本
	 */
	public void updateState(Relationship _rel, List<MIPP> _mipps, Map<Relationship, Set<MIPP>> _relMappingMIPP){
		Set<MIPP> rels = _relMappingMIPP.get(_rel);
		_mipps.removeAll(rels); //更新MIPPS副本
		_relMappingMIPP.remove(_rel);
		for(Relationship r : _relMappingMIPP.keySet()){
			_relMappingMIPP.get(r).removeAll(rels);
		}
	}
	
	/**
	 * 计算unwanted集合要添加的关系集合
	 */
	
	public void calRevisionSpace(List<Relationship> rels){
		reviSpace = new HashSet<>();
		//int i=0;
		System.out.println("Calculating the revision space of relationship");
		//try(Transaction tx = graphDB.beginTx()){
			for(Relationship rel : rels){
				//System.out.println(++i+":"+GlobalFunct.relToStr(rel));
				Set<MIPP> mipps = relMappingMIPP.get(rel);     //待修正的关系对应的MIPS(有多个)
				Set<Node> nodes = compRevisionNodesOfRel(rel,mipps);
				reviSpace.add(new RevisionSpace(rel,mipps.size(),nodes));
			}
		//}		
	}
	
	/**
	 * 输出修正信息
	 */
	public void printInfoOfRevision(){
		System.out.println("The size of unwanted:" + unWantedSet.size());
		for(Relationship r : unWantedSet){
			System.out.println(GlobalFunct.relToStr(r)+" in MIPPs "+relMappingMIPP.get(r).size());
		}
		System.out.println("----------------------------------------------");
		int refinementSize=0;
		
		for(RefinementSpace rs : refinementSet)
		{	
			refinementSize += rs.getBeConnectedNodes().size();
			System.out.println(rs.getR().getStartNode().getProperty(NAMEPROPERTY));
			if(rs.getBeConnectedNodes().size()!=0)
			{
				System.out.println("Exist connected node!");
				for(Node n:rs.getBeConnectedNodes())
				{
					System.out.println(n.getProperty(NAMEPROPERTY));
				}
			}
			else
				System.out.println("Does not exist connected node!");
			
			
		}
		System.out.println("The size of refinement " + refinementSize);			
	}
	
	//************************************************************************************
	
	/**
	 * 计算TBox的修正算子，单个MIPP的修正方案
	 * 说明：以下函数很多以及过时，可能不再使用
	 * @return
	 */
	public void calRevisionSpace(){
		reviSpace = new TreeSet<>();
		//int i=0;
		System.out.println("Calculating the revision space of relationship");
		try(Transaction tx = graphDB.beginTx()){
			for(Relationship rel : relMappingMIPP.keySet()){
				//System.out.println(++i+":"+GlobalFunct.relToStr(rel));
				Set<MIPP> mipps = relMappingMIPP.get(rel);     //待修正的关系对应的MIPS(有多个)
				Set<Node> nodes = compRevisionNodesOfRel(rel,mipps);
				reviSpace.add(new RevisionSpace(rel,mipps.size(),nodes));
			}
		}		
	}
	
	/**
	 * 修正某个relationship需要添加的边的目标节点
	 * @param rel       要处理的关系
	 * @param mipps rel所在的mips
	 * @return
	 */
	
	public Set<Node> compRevisionNodesOfRel(Relationship rel, Set<MIPP> mipps){
		//Set<Node> beConnectedNodes = new HashSet<>();
		Set<Node> notConnectedNodes = new HashSet<>();
		Set<Node> mayConnectedNodes = new HashSet<>();
		for(MIPP mi : mipps){
			compRevisionNodesInMIPP(rel, mi, notConnectedNodes,mayConnectedNodes);	
		}
		mayConnectedNodes.removeAll(notConnectedNodes); //去除一定不能添加的节点
		return mayConnectedNodes;
	}
	
	/**
	 * 一个MIPS中和诊断中的relationship关联的节点
	 * @param rel 待处理的关系
	 * @param mipp 关系对应的MIPP
	 */
	
	public void compRevisionNodesInMIPP(Relationship rel , 
			                                                                    MIPP mipp, 
			                                                                    Set<Node> notConnectedNodes,
			                                                                    Set<Node> mayConnectedNodes){
		//修正路径上的节点，从待修正关系到路径的结尾的一段路径(暂称为修正路径)，这些节点一定会不能添边
		Set<Node> n_path = new HashSet<>();	 
		//Set<Node> mayBeAdded = new HashSet<>();  //可能添加的点
		List<Relationship> forRel = null;
		//找出诊断中的relationship在路径中出现的位置（由于是MIPS，故之会出现在一边）
		int index = -1; 
		index = GlobalFunct.indexOf(mipp.getPathToN(), rel);  
		if(index == -1){   //否定的路径中没有改结点
			index = GlobalFunct.indexOf(mipp.getPathToP(), rel);
			forRel = mipp.getPathToP().subList(index, mipp.getPathToP().size());
		}
		else{
			forRel = mipp.getPathToN().subList(index, mipp.getPathToN().size());
		}
		//System.out.println(relToStr(rel)+"#index:"+index);
		for(Relationship r : forRel){
			n_path.add(r.getStartNode());
			n_path.add(r.getEndNode());
		}		
		notConnectedNodes.addAll(n_path);
		
		n_path.remove(rel.getStartNode());
		mayConnectedNodes.addAll(ExecCypher.getParentNodes(n_path,graphDB));		
	}	
	/**
	 * 修正TBox,删除诊断中的relationship，添加必须的边
	 */
	public void reviseTBox(){
		System.out.println("Revising TBox...");
		//取出所有的MIPS
		Set<MIPP> mipps = new HashSet<>();
		for(Relationship r : relMappingMIPP.keySet()){
			mipps.addAll(relMappingMIPP.get(r));
		}
		//修正MIPS，修正完成就从集合中去除, revisionOperator 已经排序
		try(Transaction tx = graphDB.beginTx()){
			while(mipps.size()>0){			
				for(RevisionSpace ro : reviSpace ){
					Set<MIPP> targetMIPPs = reviseRelationship(ro,mipps);
					if(targetMIPPs.size()>0){
						mipps.removeAll(targetMIPPs); //修正一个Relationship后，就删除和当前已经修正的MIPS
					}
				}
			}	
			tx.success();
		}
	}
	//完成对修正算子的运算
	public Set<MIPP> reviseRelationship(RevisionSpace ro,Set<MIPP> mipps){
		//修正前，看与当前Relationship关联的mipp是否已经修正
		Set<MIPP> targetMIPPs = new HashSet<>();
		for(MIPP mi : relMappingMIPP.get(ro.getR())){
			if(mipps.contains(mi)){ //如果在mipps中说明还没有做修正
				targetMIPPs.add(mi);
			}
		}
		if(targetMIPPs.size()>0){ //说明与当前Relationship关联的MIPS并没有完全修正，执行修正
			updateGraph(ro);
		}
		return targetMIPPs;
	}
	
	/**
	 * 根据修正算子更新图数据库
	 * @param ro
	 */
	public void updateGraph(RevisionSpace ro){
		Relationship rel = ro.getR();
		String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
		String end = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
		StringBuilder query = new StringBuilder();

		String formatter = "WHERE n.Name='%s' and m.Name='%s' ";
		query.append("MATCH n-[r]->m ");
		query.append(String.format(formatter, start,end));
		query.append("DELETE r");
		
		System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
			
	    Set<Node> beConnected = ro.getBeConnectedNodes();
		ExecCypher.createRelationBetweenNodes(start, beConnected, graphDB);
	}	
	
	public void shutdown(){
		this.graphDB.shutdown();
	}
}


