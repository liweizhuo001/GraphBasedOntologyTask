package kse.algorithm.forTBox.debugging;

import static kse.misc.GlobalParams.NAMEPROPERTY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.neo4j.ver2_1.ExecCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * 获取unWantedSet(待删除的关系/公理后的操作)
 * 2015-5-31
 * @author Xuefeng Fu
 */

public class RefinementOperator {
	public RefinementOperator(){}
	public  List<RefinementSpace> calRefinementSet(List<Relationship> unwants, 
																							  Map<Relationship, Set<MIPP>> relMappingMIPP, 
			                                                                                  GraphDatabaseService graphDB){
		List<RefinementSpace> spaces = new ArrayList<>();
		for(Relationship r:unwants){
			Set<MIPP> mipps = relMappingMIPP.get(r);
			Set<Node> nodesForConnecting = compRevisionNodesOfRel(r,mipps,graphDB );
			spaces.add(new RefinementSpace(r, nodesForConnecting));
		}
		
		return spaces;		
	}
	
	/**
	 * 修正某个relationship需要添加的边的目标节点
	 * @param rel       要处理的关系
	 * @param mipps rel所在的mips
	 * @return
	 */
	
	public Set<Node> compRevisionNodesOfRel(Relationship rel, Set<MIPP> mipps, GraphDatabaseService graphDB){
		//Set<Node> beConnectedNodes = new HashSet<>();
		Set<Node> notConnectedNodes = new HashSet<>();
		Set<Node> mayConnectedNodes = new HashSet<>();
		for(MIPP mi : mipps){
			compRevisionNodesInMIPP(rel, mi, notConnectedNodes,mayConnectedNodes,graphDB);	
		}
		mayConnectedNodes.removeAll(notConnectedNodes); //去除一定不能添加的节点
		return mayConnectedNodes;
	}
	
	/**
	 * 一个MIPS中和诊断中的relationship关联的节点
	 * @param rel 待处理的关系
	 * @param mipp 关系对应的MIPP
	 */
	
	public void compRevisionNodesInMIPP(Relationship rel , MIPP mipp, Set<Node> notConnectedNodes,
		Set<Node> mayConnectedNodes,GraphDatabaseService graphDB)
	{
		//修正路径上的节点，从待修正关系到路径的结尾的一段路径(暂称为修正路径)，这些节点一定会不能添边
		Set<Node> n_path = new HashSet<>();	 
		//Set<Node> mayBeAdded = new HashSet<>();  //可能添加的点
		List<Relationship> forRel = null;
		//找出诊断中的relationship在路径中出现的位置（由于是MIPP，故之会出现在一边）
		int index = -1; 
		index = GlobalFunct.indexOf(mipp.getPathToN(), rel);
		if(index == -1){  //不在否定的路径中出现
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
	 * 根据修正后的集合更新图数据库
	 * 删除unwanted, 添加refinement
	 * @param ro
	 */
	public void updateGraph(List<Relationship> rels, 
			                                         List<RefinementSpace> refinementSet,
			                                         GraphDatabaseService graphDB){
		//删除unwant集合中的关系
		for(Relationship rel : rels){
			String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
			String end = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
			StringBuilder query = new StringBuilder();	
			String formatter = "WHERE n.Name='%s' and m.Name='%s' ";
			query.append("MATCH n-[r]->m ");
			query.append(String.format(formatter, start,end));
			query.append("DELETE r");			
			System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
		}
		//加入refinement中的集合
		for(RefinementSpace rs : refinementSet){	
		    Set<Node> beConnected = rs.getBeConnectedNodes();
			//ExecCypher.createRelationBetweenNodes(rs.getR().getProperty(NAMEPROPERTY).toString(), beConnected, graphDB);
		    ExecCypher.createRelationBetweenNodes(rs.getR().getStartNode().getProperty(NAMEPROPERTY).toString(), beConnected, graphDB);
		}
	}	
	
	
}
