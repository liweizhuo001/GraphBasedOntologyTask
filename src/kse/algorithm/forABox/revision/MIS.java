package kse.algorithm.forABox.revision;

import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.COMEFROMPROPERTY;
import static kse.misc.GlobalParams.COMEFROMNEW;
import static kse.misc.GlobalParams.MEMBEROFREL;
import static kse.misc.GlobalParams.NAMEPROPERTY;

import java.util.HashSet;
import java.util.Set;

import kse.algorithm.auxiliaryClass.IndividualConceptPair;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.cypher.internal.PathImpl;

/**
 * Minimal Inconsistent Subset
 * 这里只记录要包含要删除的边的路径
 * [2014-8]
 * @author Xuefeng
 */
public class MIS {
	//protected List<Relationship> pathContainFirstMemberof;      //包含源自First实例断言的路径
	protected PathImpl pathContainFirstIndividual;
	protected Node individualNode;
	protected String individualNodeName;                                        // 实例节点
	
	protected Node endNode;
	protected String endNodeName;                                                              //路径中的顶节点
	
	protected Set<String> midNodeNames;                                                  //路径中所有的节点	  
	protected Set<Node> midNodes;
	protected IndividualConceptPair beDeletedPair;
	 
	public PathImpl getPathContainFirstIndividual() {
		return pathContainFirstIndividual;
	}

	public Node getIndividualNode() {
		return individualNode;
	}

	public Node getEndNode() {
		return endNode;
	}

	public Set<Node> getMidNodes() {
		return midNodes;
	}

	public String getIndividualNodeName() {
		return individualNodeName;
	}

	public String getEndNodeName() {
		return endNodeName;
	}

	public Set<String> getMidNodeNames() {
		return midNodeNames;
	}

	public IndividualConceptPair getBeDeletedPair() {
		return beDeletedPair;
	}

	public MIS(){}
	
	/**
	 * @param path 路径
	 */
	public MIS(PathImpl path){
		this.pathContainFirstIndividual = path;		
		this.endNode= path.endNode();
		this.endNodeName = endNode.getProperty(NAMEPROPERTY).toString();
		this.midNodeNames = new HashSet<String>();
		this.midNodes = new HashSet<Node>();
		init();	
	}

	/**
	 * 取出MIS路径中的相关信息
	 */
	
	protected void init(){
		for(Relationship rel : pathContainFirstIndividual.relationships()){
			String comefrom = rel.getProperty(COMEFROMPROPERTY).toString();
			String relType = rel.getType().name();  //取出关系的类型名			
			if((comefrom.equalsIgnoreCase(COMEFROMFIRST) ||comefrom.equalsIgnoreCase(COMEFROMNEW))
					&& relType.equalsIgnoreCase(MEMBEROFREL)){
				individualNode = rel.getStartNode();
				individualNodeName = individualNode.getProperty(NAMEPROPERTY).toString();		
				
				String delEndNodeName = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
				beDeletedPair = new IndividualConceptPair(individualNodeName, delEndNodeName);
				//System.out.println(individualNode.getProperty(NAMEPROPERTY).toString()+"###"+relType+"###"+comefrom);
				//System.out.println(endNode.getProperty(NAMEPROPERTY).toString());
				break;
			}
			
			//去掉头尾两node			
			for(Node node : pathContainFirstIndividual.nodes()){
				String nodeName = node.getProperty(NAMEPROPERTY).toString();
				if(nodeName.equalsIgnoreCase(individualNodeName) || nodeName.equalsIgnoreCase(endNodeName) ){
					continue;
				}
				else{
					midNodeNames.add(nodeName);
					midNodes.add(node);
				}
			}
		}	
	}	

	@Override
	public String toString(){
		StringBuilder _toStr = new StringBuilder();
		_toStr.append(this.individualNodeName);
		for(String node:midNodeNames){
			_toStr.append("#"+node);
		}
		_toStr.append("#"+this.endNodeName);
		return _toStr.toString();
	}
	
	@Override
	public int hashCode(){
		return (individualNodeName.length())*31 + midNodeNames.size()*3 + 1;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof MIS)){
			return false;
		}
		else{
			MIS mi = (MIS)o;
			if(mi.toString().equals(this.toString())){
				return true;
			}
			else{
				return false;
			}
		}
	}	
}
