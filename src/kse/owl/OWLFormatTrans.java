package kse.owl;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectProperty;
//import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
//import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;

import kse.misc.Timekeeping;
import kse.misc.Tools;

/**
 * 转换owl的格式,将本体转换成owl的格式，并且只保留DL-Lite的公理
 * 处理概念包含，角色包含
 * [2014-5]
 * @author Xuefeng fu 
 */
public class OWLFormatTrans {
	
	OWLInfo owl;
	OntoCreator creator;
	String source;
	String target;
	
	public OWLFormatTrans(String s, String t){
		this.source = s;
		this.target = t;
		owl = new OWLInfo(s);
		creator = new OntoCreator(t);		
	}
	
	public void simpleTrans(){
		for( OWLAxiom axiom : owl.getAllAxioms()){
			creator.insertAxiom(axiom);			
		}
		creator.saveOnto();
	}
	
	@SuppressWarnings("rawtypes")
	public void go(){
	    Set<OWLSubClassOfAxiom>	axiomsOfSubClass = owl.getAxiomsOfSubClass();
	    Set<OWLSubPropertyAxiom> axiomsOfSubProperty = owl.getAxiomsOfSubProperty();
	    Set<OWLDisjointClassesAxiom> axiomsOfDisjointClass = owl.getAxiomsOfDisjointClass();
	    Set<OWLDisjointObjectPropertiesAxiom> axiomsOfDisjointObjProperty = owl.getAxiomsOfDisjointObjProperty();
	   // Set<OWLObjectPropertyRangeAxiom> axiomsOfObjectPropertyRanger = owl.getAxiomsOfObjectPropertyRanger();
	    //Set<OWLObjectPropertyDomainAxiom> axiomsOfObjectPropertyDomain = owl.getAxiomsOfObjectPropertyDomain();
	     
		//处理owlInfo中的等价类
	    
		//*****处理TBox******		
		//axiomsOfSubClass 类别包含公理,特别注意要处理，A includedby exist_R.B的形式
		//有其他的形式，但当前只从等价类中添加这种形式
		for(OWLSubClassOfAxiom oscoAxiom: axiomsOfSubClass){			
			OWLClassExpression superClass = oscoAxiom.getSuperClass();
			OWLClassExpression subClass = oscoAxiom.getSubClass();			
			//处理A  includedby exist_R.B 的形式
			if(superClass instanceof OWLObjectSomeValuesFrom && subClass instanceof OWLClass ){
				creator.insertAxiom(oscoAxiom);
			}			
			else if(superClass instanceof OWLClass){				
				if(subClass instanceof OWLObjectSomeValuesFrom){   //处理 existenc_A includeby B 的形式
					creator.insertAxiom(oscoAxiom);
				}				
				else if(subClass instanceof OWLClass){      //处理 A includeby B 的形式		
					creator.insertAxiom(oscoAxiom);				
				}		
			}			
		}				
		//axiomsOfSubProperty 属性包含公理,
		for(OWLSubPropertyAxiom ospaAxiom : axiomsOfSubProperty){		
			if((ospaAxiom instanceof OWLObjectProperty)){
				creator.insertAxiom(ospaAxiom);				
			}
		}
			
		//axiomsOfDisjointClass 不相交类的处理 A和B不交 添加 A includedby negative_B
		for(OWLDisjointClassesAxiom odcaAxiom : axiomsOfDisjointClass){	
			if(odcaAxiom.getClassExpressionsAsList().size() == 2){ //只处理两个类不交的情况
				creator.insertAxiom(odcaAxiom);
			}
		}
		
		//axiomsOfDisjointObjProperty 不相交属性(对象公理)的处理
		for(OWLDisjointObjectPropertiesAxiom odopa : axiomsOfDisjointObjProperty){
			if(odopa.getObjectPropertiesInSignature().size()==2){
				creator.insertAxiom(odopa);
			}
		}		
		
		//定义域
		/*for(OWLObjectPropertyDomainAxiom domainAxiom : axiomsOfObjectPropertyDomain){
			creator.insertAxiom(domainAxiom);
		}
		
		//值域
		for(OWLObjectPropertyRangeAxiom rangeAxiom : axiomsOfObjectPropertyRanger){
			creator.insertAxiom(rangeAxiom);
		}*/
		
		creator.saveOnto();
	}
	
	public static void main(String[] args){
		Timekeeping.begin();
		List<String> owls = Tools.getOWLsOfExperimentation();
		for(String owl : owls){
			System.out.println("Transform owl: "+owl);
			String s = String.format("ForTBoxRevi/original/%s.owl",owl);
			String t = String.format("ForTBoxRevi/transform/%s.owl",owl);			
			OWLFormatTrans trans = new OWLFormatTrans(s, t);
			//trans.go();
			trans.simpleTrans(); //简单的将本体原样从一种格式转到另一种格式
		}
		Timekeeping.end();
		Timekeeping.showInfo("Trans ontology ");
	}
}
