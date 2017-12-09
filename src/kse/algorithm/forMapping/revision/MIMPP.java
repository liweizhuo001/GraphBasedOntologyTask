package kse.algorithm.forMapping.revision;

import static kse.misc.GlobalParams.COMEFROMFIRST;
import static kse.misc.GlobalParams.CONJUNCTION;
import static kse.misc.GlobalParams.COMEFROMPROPERTY;
import static kse.misc.GlobalParams.NAMEPROPERTY;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
/**
 * 本体对应的图上的MIMPP类
 * @author Weizhuo Li
 *
 */


public class MIMPP {
	protected List<Relationship> pathToP;                         //肯定路径(只包含mapping的部分)
	protected List<Relationship> pathToN;                        //否定路径,和肯定路径只有一个公共点，没有公共边(只包含mapping的部分)
	protected String node;                                        //MIMPP对应的不可满足的结点   
	protected String comefrom;									  //MIMPP对应的不可满足的结点的来源
	protected Set<Relationship> incoherenceMappings;                //产生不一致的mappings    
	protected Map<List<Relationship>, List<Node>> mappingClosure;       //CommonClosure	
	protected Map<List<Relationship>, Double> mappingClosureWeight;		//CommonClosure对应的权重
	
	
	public MIMPP(List<Relationship> pp, List<Relationship> np, String node,String comefrom, Map<List<Relationship>, List<Node>> mappingClosure,
			Map<List<Relationship>, Double> mappingClosureWeight){
		this.pathToN = np;  
		this.pathToP = pp;
		this.node = node;
		this.comefrom=comefrom;
		this.mappingClosure=mappingClosure;
		this.mappingClosureWeight=mappingClosureWeight;
		incoherenceMappings = compMinimalConflictSetForMappings();
	}
	
	public MIMPP(List<Relationship> pp, List<Relationship> np, String node,String comefrom, Map<List<Relationship>, List<Node>> mappingClosure,
			Map<List<Relationship>, Double> mappingClosureWeight,Set<Relationship> incoherenceMappings){
		this.pathToN = np;  //这里的正路径与负路径都一定包含在mappings中
		this.pathToP = pp;
		this.node = node;
		this.comefrom=comefrom;
		this.mappingClosure=mappingClosure;
		this.mappingClosureWeight=mappingClosureWeight;
		this.incoherenceMappings = incoherenceMappings;
	}
	
	public Set<Relationship> compMinimalConflictSet(){
		Set<Relationship> diags = new HashSet<Relationship>();
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
	
	public Set<Relationship> compMinimalConflictSetForMappings(){
		Set<Relationship> diags = new HashSet<Relationship>();
		for(Relationship rel:pathToP){
			if(rel.getProperty(COMEFROMPROPERTY).toString().equals(CONJUNCTION)){ 
				diags.add(rel);
			}
		}
		for(Relationship rel:pathToN){
			if(rel.getProperty(COMEFROMPROPERTY).toString().equals(CONJUNCTION)){
				diags.add(rel);
			}
		}
		return diags;
	}
	
	public Set<Relationship> getincoherenceMappings(){
		return this.incoherenceMappings;
	}
	
	public String getNode() {
		return node;
	}
	
	public String getSource() {
		return comefrom;
	}
	
	public void printMIMPP(){
		for(Relationship arc : pathToP){
			System.out.println(arc.getStartNode().getProperty(NAMEPROPERTY)+"->" + arc.getEndNode().getProperty(NAMEPROPERTY));
		}
		for(Relationship arc: pathToN){
			System.out.println(arc.getStartNode().getProperty(NAMEPROPERTY)+"->" + arc.getEndNode().getProperty(NAMEPROPERTY));
		}		
	}

}
