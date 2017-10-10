package kse.algorithm.forTBox.debugging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import org.neo4j.graphdb.Relationship;
import static kse.misc.GlobalParams.*;
/**
 * 本体对应的图上的MIPP类
 * @author Xuefeng Fu
 *
 */
public class MIPP {
	protected List<Relationship> pathToP;                         //肯定路径
	protected List<Relationship> pathToN;                        //否定路径,和肯定路径只有一个公共点，没有公共边
	protected Set<String> nodes;                                         //mips中出现的节点      
	protected List<Relationship> diagnosis;                       //一个mips中包含一个诊断，就是mipp路径中来自First本体的边
	 
	public  MIPP(){
		pathToP = new ArrayList<>();
		pathToN = new ArrayList<>();
		nodes = new HashSet<>();
		diagnosis = new ArrayList<>();  
	}
	
	public MIPP(List<Relationship> pp, List<Relationship> np, Set<String> nodes){
		this.pathToN = np;
		this.pathToP = pp;
		this.nodes = nodes;
		diagnosis = compDiagnosis();
	}
	
	public List<Relationship> getPathToP() {
		return pathToP;
	}

	public List<Relationship> getPathToN() {
		return pathToN;
	}

	public Set<String> getNodes() {
		return nodes;
	}
	
	public void setPathToP(List<Relationship> pathToP) {
		this.pathToP = pathToP;
	}

	public void setPathToN(List<Relationship> pathToN) {
		this.pathToN = pathToN;
	}

	public void setNodes(Set<String> nodes) {
		this.nodes = nodes;
	}

	/**
	 * 取出MIPS中来源First的关系(边)
	 */
	public List<Relationship> compDiagnosis(){
		List<Relationship> diags = new ArrayList<>();
		for(Relationship rel:pathToP){
			if(rel.getProperty(COMEFROMPROPERTY).toString().equals(COMEFROMFIRST)){ 
				diags.add(rel);
			}
		}
		for(Relationship rel:pathToN){
			if(rel.getProperty(COMEFROMPROPERTY).toString().equals(COMEFROMFIRST)){
				diags.add(rel);
			}
		}
		return diags;
	}
	
	public List<Relationship> getDiagnosis(){
		return this.diagnosis;
	}
	
	public void printMIPP(){
		for(Relationship arc : pathToP){
			System.out.println(GlobalFunct.relToStr(arc));
		}
		for(Relationship arc: pathToN){
			System.out.println(GlobalFunct.relToStr(arc));
		}		
	}
	
	@Override
	public String toString(){
		StringBuilder _toStr = new StringBuilder("[MIPS IN ONTOLOGY]");
		for(String node:nodes){
			_toStr.append("#"+node);
		}
		return _toStr.toString();
	}
	@Override
	public int hashCode(){
		return pathToP.size()*31 + pathToN.size()*3 + nodes.size();
	}
	
	//在集合中比较两个MIPP是否相同
	@Override
	public boolean equals(Object o){
		if(!(o instanceof MIPP)){
			return false;
		}
		else{
			MIPP mi = (MIPP)o;
			if(mi.toString().equals(this.toString())){
				return true;
			}
			else{
				return false;
			}
		}
	}	
}
