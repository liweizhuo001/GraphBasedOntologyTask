package kse.algorithm.auxiliaryClass;
/**
 * 辅助类，记录不相交的节点对[2014-5]
 * @author Xuefeng 
 */
public class DisjointPair {
	
	String first;
	String second;
	public DisjointPair(String f, String s){
		this.first = f;
		this.second = s;
	}
	
	public DisjointPair(){
		this.first= null;
		this.second = null;
	}
	
	public String getFirst() {
		return first;
	}
	public String getSecond() {
		return second;
	}
	@Override
	public String toString(){
		return String.format("DisjointPair(%s,%s) ", first, second);
	}
}
