package kse.algorithm.auxiliaryClass;

import static kse.misc.GlobalParams.NAMEPROPERTY;

import org.neo4j.graphdb.Node;

/**
 * 存在该不可满足概念到不交概念对的路径[2017-10]
 * @author WeiZhuo Li
 */

public class UnsatInformation extends DisjointPairForMappings {

	Node node;
	String unsat;
	String pairsource;
	
	
	public UnsatInformation(Node node, String pairsource,String first, String second,String comefrom){
		super(first,second,comefrom);
		this.node=node;
		this.pairsource=pairsource;
		this.unsat=node.getProperty(NAMEPROPERTY).toString();
	}
	
	public String getUnsat(){
		return this.unsat;
	}
	
	public Node getNode(){
		return this.node;
	}
	
	public String getPairSoucre()
	{
		return this.pairsource;
	}
}
