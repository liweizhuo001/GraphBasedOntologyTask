package kse.algorithm.forTBox.debugging.scoring;

import java.util.Set;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * 修正空间
 * 记录一个Relationship出现在MIPS中的频率
 * 如果选择该Relationship做修正，要添的目标节点
 * @author Xuefeng Fu
 */
public class RevisionSpace implements Comparable<RevisionSpace>{
	Relationship r;  //待修正的关系，其实就是删除该关系
	int times;
	Set<Node> beConnectedNodes; //如修正则需要添的目的节点集合，从r的startnode建立到这些节点的关系
	public RevisionSpace(Relationship r,int times, Set<Node> nodes){
		this.r = r;
		this.times = times;
		this.beConnectedNodes = nodes;
	}
	public Relationship getR() {
		return r;
	}
	public int getTimes() {
		return times;
	}
	public Set<Node> getBeConnectedNodes() {
		return beConnectedNodes;
	}
	@Override
	public boolean equals(Object o){
		if(!(o instanceof RevisionSpace)){
			return false;
		}
		else{
			RevisionSpace ro = (RevisionSpace)o;
			if(!(GlobalFunct.isRelEquals(r, ro.getR()))){
				return false;
			}
			else if(times!=ro.getTimes()){
				return false;
			}
			else if(beConnectedNodes.size()!=ro.getBeConnectedNodes().size()){
					return false;
			}
			else{
				return true;
			}
		}
	}
	@Override
	public int compareTo(RevisionSpace ro) { //先按出现的频率排序，之后按添边的数目排序(默认排序为升序)
		int v=0;
		if(times!=ro.getTimes()){  //times越大，返回越小, 根据times降序
			v = ro.getTimes() - times;
		}
		else{
			v =beConnectedNodes.size() -ro.getBeConnectedNodes().size() ; //升序
		}
		return v;
	}	
	@Override
	public String toString(){
		return r + ":" + times + ":" + beConnectedNodes.size();		
	}
}