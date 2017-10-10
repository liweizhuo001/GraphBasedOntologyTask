package kse.misc;

import java.util.HashSet;
import java.util.Set;

/**
 * 记录实验中的全局参数，使用静态参数
 * @author Xuefeng Fu
 */

public class GlobalParams {	
	
	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	public static final Set<String> EXCLUDEDSIGNATURES = new HashSet() {
		{
			add("owl:Thing");
			add("owl:Nothing");
		}
	};
	
	//图操作相关
	public static final String NAMEPROPERTY = "Name";                    //图中节点属性，一个节点有 name=Xxx
	public static final String TYPEPROPERTY = "Type"  ;                     //图中节点属性，表明节点的类别，是概念还是角色或实例
	public static final String WEIGHTEDPROPERTY = "Weight"  ;                     //图中节点属性，表明节点的类别，是概念还是角色或实例
	
	public static final String CONCEPTTYPE  = "Concept"  ;                 //概念类别的取值
	public static final String ROLETYPE  = "Role"  ;                                //角色类别的取值
	public static final String INDIVIDUALTYPE = "Individual";           //实例类型
	
	//图中节点的Label	
	public static final String CONCEPTLABEL = "Concept"  ;                 //概念
	public static final String ROLELABEL  = "Role"  ;                                //角色
	public static final String INDIVIDUALLABEL = "Individual";           //实例
	
	public static final String MEMBEROFCONCEPE  = "MemberOfConcept"  ;        //类的成员，图数据库中relationship的属性
	public static final String MEMBEROFROLE  = "MemberOfRole"  ;                      //角色的成员，图数据库中relationship的属性	
	
	public static final String COMEFROMPROPERTY = "ComeFrom";             //关系来源
	public static final String COMEFROMFIRST = "First";                                 //来自第一个本体
	public static final String COMEFROMSECOND = "Second";                         //来自第二个本体
	public static final String CONJUNCTION = "Conj";                                        //公共	
	public static final String COMEFROMNEW = "NewAdd";                              //新添加的
	
	public static final String RELATIONSHIPINDEX = "RelIndex";                //关系索引名
	public static final String NODEINDEX = "NodeIndex";                                //节点索引名		
	
	//图数据库中 Relationship的类型
	public static final String INCLUDEDREL = "INCLUDEDBY";
	public static final String MEMBEROFREL = "MEMBEROF";
	
	//本体到图的转换相关全局属性
	public static final String NEGATIVESIGN = "negative_";          //否定关系符号
	public static final String EXISTENCESIGN = "existence_";          //否定关系符号
	public static final String INVERSESIGN = "inverse_";          //否定关系符号		


	public static final String Funct_Property = "Funct_Property";	
	public static final String PropertyFunctFormat = "Property:%s";
	public static final String ClassIncludedFormat = "Class:%s includedby %s";
	public static final String PropertyIncludedFormat = "Property:%s includedby %s";
	public static final String ClassEquivalentFormat = "Class:%s equivalent %s";
	public static final String PropertyEquivalentFormat = "Property:%s equivalent %s";
	
	public static final int ONCE_REL_NUMBER = 2000;  //用于图数据库时，一次事务提交的添加关系数目
	public static final int MAXMUPS = 500;
	public static final int MAXMIPS = 200;
	
	//public static String owlFormatter = "owls/%s";

	

}




