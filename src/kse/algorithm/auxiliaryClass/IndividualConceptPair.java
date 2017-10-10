package kse.algorithm.auxiliaryClass;


/**
 * 用于ABox修正，
 * 记录要修改的（删除或添加）实例和概念节点对
 * @author Xuefeng Fu
 *
 */
public class IndividualConceptPair {
	String individual;
	String concept;
	/**
	 * 构造函数
	 * @param i 实例
	 * @param c 概念
	 */
	public IndividualConceptPair(String i, String c){
		this.individual = i;
		this.concept = c;
	}
	
	public IndividualConceptPair(){
		this.individual= null;
		this.concept = null;
	}
	

	public String getIndividual() {
		return individual;
	}

	public String getConcept() {
		return concept;
	}

	@Override
	public String toString(){
		return String.format("IndividualConceptPair(%s,%s) ", individual, concept);
	}
	
	@Override
	public int hashCode(){
		return individual.length()*31 + concept.length()*3 + 1;
	}
	
	@Override
	public boolean equals(Object o){
		if(!(o instanceof IndividualConceptPair)){
			return false;
		}
		else{
			IndividualConceptPair mi = (IndividualConceptPair)o;
			if(mi.toString().equals(this.toString())){
				return true;
			}
			else{
				return false;
			}
		}
	}	
}
