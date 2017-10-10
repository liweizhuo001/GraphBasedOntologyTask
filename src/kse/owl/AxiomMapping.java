package kse.owl;

/**
 * 本体中包含公理的映射，为转换成图做准备
 * 图的转换规则中对节点的处理，都可以在这里体现，包括对新节点的处理
 * 如：一个关系R1 includedby R2, 则exist_*, inverse_*, exist_inverse_*都要加入。
 * 否定包含则要做否定包含的处理
 * 
 * @author Xuefeng Fu
 */

public class AxiomMapping {
	private String subKey;	
	private String supKey;	
	//CONCEPTTYPE ROLETYPE INDIVIDUALTYPE
	private String axiomType;   //是关系包含，还是概念包含，还是membership断言(ABox)
	public AxiomMapping(String sub, String sup, String aType){
		this.subKey = sub;		
		this.supKey = sup;
		this.axiomType = aType;
	}
	public String getSubKey() {
		return subKey;
	}
	public String getSupKey() {
		return supKey;
	}
	public String getAxiomType() {
		return axiomType;
	}
	@Override
	public String toString(){
		return String.format("%s --> %s, type is %s",subKey,supKey,axiomType);
	}
}
