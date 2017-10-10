package kse.algorithm.auxiliaryClass;

/**
 * 其实是一个三元组，一个不可满足概念和一对不相交概念
 * 存在该不可满足概念到不交概念的路径[2014-5]
 * @author Xuefeng Fu
 */

public class UnsatTriple extends DisjointPair {

	String unsat;
	public UnsatTriple(String unsat, String first, String second){
		super(first,second);
		this.unsat = unsat;
	}
	
	public String getUnsat(){
		return this.unsat;
	}
}
