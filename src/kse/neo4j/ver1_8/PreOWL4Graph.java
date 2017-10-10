package kse.neo4j.ver1_8;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kse.misc.Tools;
import kse.owl.AxiomMapping;
import kse.owl.OWLInfo;

import static kse.misc.GlobalParams.*;
/**
 * 为从owl到图的转换做进一步的预处理
 * 在owlInfo的中在添加一些复杂概念和复杂Role
 * @author Xuefeng Fu
 *
 */
public class PreOWL4Graph {
	/** 
	 * @param owlInfo 完成了基础解析的本体信息
	 * @return 处理后的owlInfo
	 */
	public static OWLInfo handle(OWLInfo owlInfo){
		System.out.println("******  In mapping  ******");
		Set<String> concepts = owlInfo.getConceptTokens();
		Set<String> roles = owlInfo.getObjPropertyTokens();
		Set<String> attributes = owlInfo.getDataPropertyTokens();
		Set<String> roles4Graph = new HashSet<String>(); 
		Set<String> attributes4Graph = new HashSet<String>(); 
		
		//给角色增加其他三种形式 inverse_,existence_,existence_inverse_		
		for(String role : roles){
			roles4Graph.add(Tools.getInverseToken(role));
			concepts.add(Tools.getExistenceToken(role));
			concepts.add(Tools.getExistenceInverseToken(role));
		}
		roles.addAll(roles4Graph);		
		//给属性增加3种形式 inverse_,existence_,existence_inverse_	
		for(String attribute : attributes){
			//attributes4Graph.add(Tools.getInverseToken(attribute));
			concepts.add(Tools.getExistenceToken(attribute));
			//concepts.add(Tools.getExistenceInverseToken(attribute));
		}
		attributes.addAll(attributes4Graph);
		
		//根据映射在添加相应的新复杂概念和复杂公理
		List<AxiomMapping> aMappings = owlInfo.getTBoxAxiomMappings();
		for(AxiomMapping aMapping : aMappings){
			String subKey = aMapping.getSubKey();
			String supKey = aMapping.getSupKey();
			if(aMapping.getAxiomType().equals(CONCEPTLABEL)){
				if(!(concepts.contains(subKey))){
					concepts.add(subKey);
				}
				if(!(concepts.contains(supKey))){				
					concepts.add(supKey);
				}
			}
			else if(aMapping.getAxiomType().equals(ROLETYPE)){
				if(!roles.contains(subKey)){
					roles.add(subKey);
				}
				if(!roles.contains(supKey)){
					roles.add(supKey);			
				}
			}
		}
		return owlInfo;
	}
}
