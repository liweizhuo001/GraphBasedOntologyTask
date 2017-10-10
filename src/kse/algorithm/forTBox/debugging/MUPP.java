package kse.algorithm.forTBox.debugging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Relationship;

/**
 * 一个mipp也是一个mupp，故采用继承方式
 * mupp类, 从不可满足节点出发，有路径到一对不相交节点，有一个概念节点
 * @author Xuefeng Fu
 *
 */
public class MUPP extends MIPP {
	String unsatNode;                       //不可满足概念
	//List<Relationship> pathToP;   //到肯定点路径，在不同的MUPS中，这两条边都可能重复，但不能都重复, 加入的顺序就是路径方向
	//List<Relationship> pathToN;   //到否定点路径
	//Set<String> nodes;                   //mups中出现的节点      
	
	public  MUPP(){
		pathToP = new ArrayList<>();
		pathToN = new ArrayList<>();
		nodes = new HashSet<>();
	}
	
	public MUPP(String u, List<Relationship> pp, List<Relationship> np, Set<String> nodes){
		//super(pp,pn,nodes);
		this.unsatNode = u;
		this.pathToN = np;
		this.pathToP = pp;
		this.nodes = nodes;
	}	
	
	public String getUnsatNode() {
		return unsatNode;
	}
	public void setUnsatNode(String unsatNode){
		this.unsatNode = unsatNode;
	}

	@Override
	public String toString(){
		StringBuilder _toStr = new StringBuilder(String.format("[MUPS of %s]", unsatNode));
		for(String node:nodes){
			_toStr.append("#"+node);
		}
		return _toStr.toString();
	}
}
