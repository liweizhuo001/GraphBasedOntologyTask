package kse.algorithm.auxiliaryClass;
/**
 * 辅助类，记录不相交的节点对以及相应的出处[2017-10]
 * @author Weizhuo Li 
 */
public class DisjointPairForMappings {
	
	String first;
	String second;
	String comefrom;  //不交配对的出处
	public DisjointPairForMappings(String f, String s, String comefrom){
		this.first = f;
		this.second = s;
		this.comefrom=comefrom;
	}
	
	public DisjointPairForMappings(){
		this.first= null;
		this.second = null;
		this.comefrom=null;
	}
	
	public String getFirst() {
		return first;
	}
	public String getSecond() {
		return second;
	}
	public String getSource() {
		return comefrom;
	}
	@Override
	public String toString(){
		return String.format("DisjointPair(%s,%s) ", first, second);
	}
}
