package kse.owl;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import kse.algorithm.forABox.preprocessing.NewInjectConflicts;
import kse.misc.Tools;
import uk.ac.manchester.cs.owl.owlapi.OWLAnonymousClassExpressionImpl;

import org.neo4j.cypher.internal.compiler.v1_9.commands.Has;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyAxiom;

import static kse.misc.GlobalParams.*;

/**
 * 取出owl文件中的信息，包括各类公理(axiom)，分TBox、ABox. 类（概念.concept.Class），对象属性（角色.role.object
 * property），数据属性（属性.attribute.data property） Get the information of owl file,
 * include axioms, classes, object properties , data properties.
 * [2014-5]
 * @author Xuefeng Fu
 */

public class OWLInfo {
	private String owlFileName;
	private OWLOntology ontology;
	private OWLOntologyManager manager ;
	private OWLDataFactory dataFactory;
	public String URI="";
	
	
	
	private Set<OWLAxiom> axiomsInTBox; // Set of TBox，本体中所有的TBox公理
	private Set<OWLAxiom> axiomsInABox; // Set of ABox，本体中所有的ABox公理

	// about ABox
	private Set<OWLObjectPropertyAssertionAxiom> axiomsOfObjProAssertion; // membership, R(a,b)
	private Set<OWLClassAssertionAxiom> axiomsOfClassAssertion; // menbership, A(a)
	private Set<OWLNegativeObjectPropertyAssertionAxiom> axiomsOfNegObjProAssertion; // membership, -R(a,b)，否定函数断言

	// about TBox
	private Set<OWLSubClassOfAxiom> axiomsOfSubClass; // Set of subclassof axiom
	@SuppressWarnings("rawtypes")
	private Set<OWLSubPropertyAxiom> axiomsOfSubProperty; // Set of subpropertyof axiom
	private Set<OWLFunctionalObjectPropertyAxiom> axiomsOfFunct;// Set of functional axiom
	private Set<OWLInverseObjectPropertiesAxiom> axiomsOfInverseProperty; // Set of inverseof axiom
	
	private Set<OWLDisjointClassesAxiom> axiomsOfDisjointClass;
	private Set<OWLDisjointObjectPropertiesAxiom> axiomsOfDisjointObjProperty;
	
	// 这个关系比较复杂，很多关系要从中分析，如存在,包括逆等价关系[逆概念关系，通过一个类等价于另一个类的补来定义(complementOf)]
	private Set<OWLEquivalentClassesAxiom> axiomsOfEquivalentClass; 
	
	// private Set<OWLEquivalentClassesAxiom> axiomsOfInverseClass;	
	// //互补概念关系，与一个类的补等价，通过一个类是另一个类的补来定义(complementOf),存在于等价关系中
	
	private Set<OWLEquivalentObjectPropertiesAxiom> axiomsOfEquivalentObjProperty; // 等价的对象属性
	
	private Set<OWLObjectPropertyDomainAxiom> axiomsOfObjectPropertyDomain;  //定义域集合
	private Set<OWLObjectPropertyRangeAxiom> axiomsOfObjectPropertyRanger;      //值域集合
	private Set<OWLDataPropertyDomainAxiom> axiomsOfDataPropertyDomain;      //定义域集合
	private Set<OWLDataPropertyRangeAxiom> axiomsOfDataPropertyRanger;      //值域集合

	private Set<String> conceptTokens;               // 去掉了前缀的符号，概念(原子概念)，在构图时会发生变化
	private Set<String> objPropertyTokens;       // 去掉了前缀的符号，属性(原子属性),对象属性(objectproperty)，构图中不会增减
	private Set<String> dataPropertyTokens;    // 数据属性符号
	private Set<String> individualTokens;          // 实例符号
	private HashMap<String, String> conceptLabels;          // 实例符号
	
	private List<AxiomMapping> tBoxAxiomMappings;  //TBox公理映射，对从本体到图做预处理
	private List<AxiomMapping> aBoxAxiomMappings;   //ABox映射

	public OWLOntology getOntology() {
		return this.ontology;
	}
	
	 public String getURI()
	 {
		 OWLOntologyID ontologyIRI = ontology.getOntologyID();
		 String OntoID = ontologyIRI.getOntologyIRI().toString();
		 return OntoID.replace("#", "");
	 }

	public Set<OWLAxiom> getAllAxioms() {
		return ontology.getAxioms();
	}

	public Set<OWLAxiom> getAxiomsInTBox() {
		return axiomsInTBox;
	}

	public Set<OWLAxiom> getAxiomsInABox() {
		return axiomsInABox;
	}

	public Set<OWLSubClassOfAxiom> getAxiomsOfSubClass() {
		return axiomsOfSubClass;
	}

	public Set<OWLFunctionalObjectPropertyAxiom> getAxiomsOfFunct() {
		return axiomsOfFunct;
	}

	@SuppressWarnings("rawtypes")
	public Set<OWLSubPropertyAxiom> getAxiomsOfSubProperty() {
		return axiomsOfSubProperty;
	}

	public Set<String> getConceptTokens() {
		return this.conceptTokens;
	}

	public Set<String> getObjPropertyTokens() {
		return this.objPropertyTokens;
	}

	public Set<String> getDataPropertyTokens() {
		return dataPropertyTokens;
	}
	
	public Set<String> getIndividualTokens() {
		return individualTokens;
	}
	
	public HashMap<String, String> getConceptLabels() {
		return conceptLabels;
	}

	public Set<OWLObjectPropertyDomainAxiom> getAxiomsOfObjectPropertyDomain() {
		return axiomsOfObjectPropertyDomain;
	}

	public Set<OWLObjectPropertyRangeAxiom> getAxiomsOfObjectPropertyRanger() {
		return axiomsOfObjectPropertyRanger;
	}

	public Set<OWLInverseObjectPropertiesAxiom> getAxiomsOfInverseProperty() {
		return axiomsOfInverseProperty;
	}

	public Set<OWLDisjointObjectPropertiesAxiom> getAxiomsOfDisjointObjProperty() {
		return axiomsOfDisjointObjProperty;
	}

	public Set<OWLEquivalentClassesAxiom> getAxiomsOfEquivalentClass() {
		return axiomsOfEquivalentClass;
	}

	public Set<OWLObjectPropertyAssertionAxiom> getAxiomsOfObjProAssertion() {
		return axiomsOfObjProAssertion;
	}

	public Set<OWLClassAssertionAxiom> getAxiomsOfClassAssertion() {
		return axiomsOfClassAssertion;
	}

	public Set<OWLDisjointClassesAxiom> getAxiomsOfDisjointClass() {
		return axiomsOfDisjointClass;
	}

	public Set<OWLEquivalentObjectPropertiesAxiom> getAxiomsOfEquivalentObjProperty() {
		return axiomsOfEquivalentObjProperty;
	}

	public Set<OWLNegativeObjectPropertyAssertionAxiom> getAxiomsOfNegObjProAssertion() {
		return axiomsOfNegObjProAssertion;
	}
	
	public List<AxiomMapping> getTBoxAxiomMappings() {
		return tBoxAxiomMappings;
	}
	
	public List<AxiomMapping> getABoxAxiomMapping() {
		return aBoxAxiomMappings;
	}

	public String getOwlFileName() {
		return owlFileName;
	}

	/**
	 * OWLInfo构造函数.
	 * 
	 * @param owlFile owl file name with full path
	 */
	public OWLInfo(String owlFile) {
		
		manager = OWLManager.createOWLOntologyManager();
		dataFactory = manager.getOWLDataFactory();
		
		if(owlFile.contains("/")){
			owlFileName = owlFile.substring(owlFile.lastIndexOf("/") + 1);
		}
		else if(owlFile.contains("\\")){
			owlFileName = owlFile.substring(owlFile.lastIndexOf("\\") + 1);
		}
		//owlFileName = owlFileName.substring(0, owlFileName.lastIndexOf("."));
		System.out.println(String.format("Ontology %s Initializing...",owlFileName));
		File ontologyFile = new File(owlFile);
		
		try {
			ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology = null;
		}
		//初始化，解析，映射
		if (ontology != null) {	
			URI=ontology.getOntologyID().getOntologyIRI().toString();
			initAxiomSets();
			parseOwl();
			try{
				MappingAxioms();  //调用映射后  概念集合会变化(属性集合不变)
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	// 初始化各个公理集合
	@SuppressWarnings("rawtypes")
	private void initAxiomSets() {
		// System.out.println(ontology.getAxiomCount()); // the count of axiom
		axiomsInABox = ontology.getABoxAxioms(false); // get ABox axiom. 有问题，会漏掉
		axiomsInTBox = ontology.getTBoxAxioms(true); // get TBox axioms. 有问题，会漏掉一些公理

		// TBox集合初始化
		axiomsOfSubClass = new HashSet<OWLSubClassOfAxiom>();
		axiomsOfFunct = new HashSet<OWLFunctionalObjectPropertyAxiom>();
		axiomsOfSubProperty = new HashSet<OWLSubPropertyAxiom>();
		axiomsOfInverseProperty = new HashSet<OWLInverseObjectPropertiesAxiom>();
		axiomsOfDisjointClass = new HashSet<OWLDisjointClassesAxiom>();
		axiomsOfDisjointObjProperty = new HashSet<OWLDisjointObjectPropertiesAxiom>();
		axiomsOfEquivalentClass = new HashSet<OWLEquivalentClassesAxiom>();
		axiomsOfEquivalentObjProperty = new HashSet<OWLEquivalentObjectPropertiesAxiom>();
		axiomsOfObjectPropertyDomain = new HashSet<OWLObjectPropertyDomainAxiom>();
		axiomsOfObjectPropertyRanger = new HashSet<OWLObjectPropertyRangeAxiom>();
		axiomsOfDataPropertyDomain= new HashSet<OWLDataPropertyDomainAxiom>();      //定义域集合
		axiomsOfDataPropertyRanger= new HashSet<OWLDataPropertyRangeAxiom>();      //定义域集合

		// ABox集合初始化
		axiomsOfNegObjProAssertion = new HashSet<OWLNegativeObjectPropertyAssertionAxiom>();
		axiomsOfObjProAssertion = new HashSet<OWLObjectPropertyAssertionAxiom>();
		axiomsOfClassAssertion = new HashSet<OWLClassAssertionAxiom>();
		
		//公理映射初始化
		tBoxAxiomMappings = new ArrayList<AxiomMapping>();
		aBoxAxiomMappings = new ArrayList<AxiomMapping>();
	}

	/**
	 * 取出owl文件中的相关信息，公理集合(TBox公理集， ABox公理集)，概念集合，属性集合
	 */
	@SuppressWarnings("rawtypes")
	private void parseOwl() {		
		// 取出本体中的所有类和对象属性--概念和角色
		conceptTokens = new HashSet<String>();
		objPropertyTokens = new HashSet<String>();
		dataPropertyTokens = new HashSet<String>();
		individualTokens = new HashSet<String>();
		conceptLabels =new HashMap<String, String> ();
		// 简化符号
		for (OWLClass signature : ontology.getClassesInSignature()) {             //概念
			String token = signature.getIRI().getFragment();
			if(token.equals("Nothing")||token.equals("Thing"))//常规本体
				continue;
        	else if(token.equals("DbXref")||token.equals("Subset")||token.equals("Synonym")
        			||token.equals("ObsoleteClass")||token.equals("SynonymType")||token.equals("Definition"))
				continue;
			if (token != null && !EXCLUDEDSIGNATURES.contains(token)) 
			{
				conceptTokens.add(token);
				String label=null,comment=null;
	        	for(OWLAnnotation anno : signature.getAnnotations(ontology, dataFactory.getRDFSLabel()))
	        	{        		
	        		if (anno.getValue() instanceof OWLLiteral)
	        		{
	        			OWLLiteral val=(OWLLiteral)anno.getValue();  			
	        			label=val.getLiteral();

	        		}
	        	}
	        	/*for(OWLAnnotation comm : signature.getAnnotations(ontology, dataFactory.getRDFSComment()))
	        	{
	        		if (comm.getValue() instanceof OWLLiteral)
	        		{
	        			OWLLiteral val=(OWLLiteral)comm.getValue();
	        			comment=val.getLiteral();
	        		}
	        	} */   
	        	//System.out.println(token+"  "+label);
	        	if(label!=null&&!label.equals(token))//存在自己等于自己的label
	        		conceptLabels.put(token, label);	
				/*else if(label!=null&&comment!=null&&!comment.equals(""))  
				{
					conceptLabels.add(token+"--"+comment.trim());	
				}*/
				else 
				{
					conceptLabels.put(token, token);
				}
	        }
			
		}
		for (OWLObjectProperty signature : ontology.getObjectPropertiesInSignature()) {   //对象属性
			String token = signature.getIRI().getFragment(); // get the fragment of iri after  #
			if(token.equals("topObjectProperty")||token.equals("bottomObjectProperty"))//常规本体
				continue;
			//医学本体
			else if(token.equals("ObsoleteProperty")||token.equals("UNDEFINED"))
				continue;
			if (token != null) {
				objPropertyTokens.add(token);
			}
		}
		for(OWLDataProperty signature : ontology.getDataPropertiesInSignature()){           //数据属性
			String token = signature.getIRI().getFragment();
			if(token.equals("topDataProperty")||token.equals("bottomDataProperty"))
				continue;
			if(token!=null){
				dataPropertyTokens.add(token);
			}					
		}		
		for(OWLNamedIndividual oni : ontology.getIndividualsInSignature()){                      //命名个体
			String token = oni.getIRI().getFragment();
			individualTokens.add(token);
		}
		
		for(OWLNamedIndividual oni : ontology.getIndividualsInSignature()){                      //命名个体
			String token = oni.getIRI().getFragment();
			individualTokens.add(token);
		}
		
		
		for(OWLAxiom axiom : ontology.getAxioms()){
			
			// 处理TBox,八类，概念包含、函数属性、属性包含、逆属性、不交概念、不交属性、等价类、等价属性
			
			if (axiom instanceof OWLSubClassOfAxiom) { // subclassof
				
				OWLSubClassOfAxiom oscAxiom = (OWLSubClassOfAxiom)axiom;
				OWLClassExpression superClass = oscAxiom.getSuperClass();
				OWLClassExpression subClass = oscAxiom.getSubClass();
				//处理复杂的包含，将他们简化，一个类被多个类的交包含
				if( (subClass instanceof OWLClass) && (superClass instanceof OWLObjectIntersectionOf) ){
					OWLObjectIntersectionOf ooiAxiom = (OWLObjectIntersectionOf)superClass;
					//将概念的交 分拆成独立的概念
					Set<OWLClassExpression> oioClasses =  ooiAxiom.asConjunctSet();//取出合取集合，当目标是对象的交时可用
					for(OWLClassExpression oioClass : oioClasses){					
						OWLSubClassOfAxiom newSCAxiom = dataFactory.getOWLSubClassOfAxiom(subClass, oioClass);//添加新的包含公理					
						axiomsOfSubClass.add(newSCAxiom);					
					}			
				}
				//如果是多个类的并包含于某个类
				else if ( (subClass instanceof OWLObjectUnionOf) && (superClass instanceof OWLClass) ){
					OWLObjectUnionOf oouAxiom = (OWLObjectUnionOf)subClass;
					Set<OWLClassExpression> oouClasses = oouAxiom.asDisjunctSet();//取出析取集合，当目标是对象的并时可用
					for(OWLClassExpression oouClass : oouClasses){					
						OWLSubClassOfAxiom newSCAxiom = dataFactory.getOWLSubClassOfAxiom(oouClass,superClass);//添加新的包含公理						
						axiomsOfSubClass.add(newSCAxiom);					
					}	
				}
				else if  ( (subClass instanceof OWLClass) && (superClass instanceof OWLClass) ){
					axiomsOfSubClass.add(oscAxiom);	
				}
				
			} else if (axiom instanceof OWLFunctionalObjectPropertyAxiom) { // functional
				axiomsOfFunct.add((OWLFunctionalObjectPropertyAxiom) axiom);
			} else if (axiom instanceof OWLSubPropertyAxiom) { // subpropertyof
				axiomsOfSubProperty.add((OWLSubPropertyAxiom) axiom);
			} else if (axiom instanceof OWLInverseObjectPropertiesAxiom) { // inverseof
				axiomsOfInverseProperty	.add((OWLInverseObjectPropertiesAxiom) axiom);
			} else if (axiom instanceof OWLDisjointClassesAxiom) { // disjoint classes
				axiomsOfDisjointClass.add((OWLDisjointClassesAxiom) axiom);
			} else if (axiom instanceof OWLDisjointObjectPropertiesAxiom) { // disjoint object property
				axiomsOfDisjointObjProperty.add((OWLDisjointObjectPropertiesAxiom) axiom);
			} else if (axiom instanceof OWLEquivalentClassesAxiom) { // 取出等价类公理
				axiomsOfEquivalentClass.add((OWLEquivalentClassesAxiom) axiom);
			} else if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) { // 等价属性
				axiomsOfEquivalentObjProperty.add((OWLEquivalentObjectPropertiesAxiom) axiom);
			}else if(axiom instanceof OWLObjectPropertyDomainAxiom){           //objectproperty中domain的处理
				axiomsOfObjectPropertyDomain.add( (OWLObjectPropertyDomainAxiom)axiom);
			}else if(axiom instanceof OWLObjectPropertyRangeAxiom){              //objectproperty中range的处理
				axiomsOfObjectPropertyRanger.add( (OWLObjectPropertyRangeAxiom)axiom);
			}	
			else if(axiom instanceof OWLDataPropertyDomainAxiom){              //dataproperty中domain的处理
				axiomsOfDataPropertyDomain.add( (OWLDataPropertyDomainAxiom)axiom);
			}
			else if(axiom instanceof OWLDataPropertyRangeAxiom){              //dataproperty中range的处理
				axiomsOfDataPropertyRanger.add( (OWLDataPropertyRangeAxiom)axiom);
			}
			
				
			//处理ABox，三类，概念断言、属性断言、逆属性断言
			
			if (axiom instanceof OWLClassAssertionAxiom) {
				axiomsOfClassAssertion.add((OWLClassAssertionAxiom) axiom); //概念断言
			} else if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
				axiomsOfObjProAssertion.add((OWLObjectPropertyAssertionAxiom) axiom); //属性断言
			} else if (axiom instanceof OWLNegativeObjectPropertyAssertionAxiom) {
				axiomsOfNegObjProAssertion.add((OWLNegativeObjectPropertyAssertionAxiom) axiom); //逆属性断言
			}			
		}

		// 进一步处理等价公理
		parseEquivalentAxiom(axiomsOfEquivalentClass);

		
		//说明：没有使用API提供的getABoxAxioms、getTBoxAxioms，测试中发现会漏掉好多的公理
		//ontology.getDataPropertiesInSignature()   通过这个函数可以获取数据数据(attribute)的数目			
		
	}

	/**
	 * 根据数据的特点来处理 由于等价公理比较复杂，这里当前只处理等价类，它包含交，并(我们不考虑并)，补，存在等，需求	 
	 * @param eqAxiom  : 等价结构公理
	 */	
	public void parseEquivalentAxiom(Set<OWLEquivalentClassesAxiom> eqAxioms) {		
		for (OWLEquivalentClassesAxiom eqAxiom : eqAxioms) {			
			List<OWLClassExpression> eqClasses = eqAxiom	.getClassExpressionsAsList();
			OWLClassExpression leftPart = eqClasses.get(0);
			OWLClassExpression rightPart = eqClasses.get(1);
			/*if (secondConcept instanceof OWLObjectComplementOf) {  //互补
				// axiomsOfInverseClass.add(eqAxiom);
			} */
			//ObjectIntersectionOf() 两个概念交
			//形式：EquivalentClasses(<Dean> ObjectIntersectionOf(ObjectSomeValuesFrom(<headOf> <College>)) )
			//这是A==两个概念的交，也有与多个概念的交等价
			if( (leftPart instanceof OWLClass) && (rightPart instanceof OWLObjectIntersectionOf) ){
				OWLObjectIntersectionOf ooiAxiom = (OWLObjectIntersectionOf)rightPart;
				//将概念的交 分拆成一个个的概念
				Set<OWLClassExpression> oioClasses =  ooiAxiom.asConjunctSet();//取出合取集合，当目标是对象的交时可用
				for(OWLClassExpression oioClass : oioClasses){					
					OWLSubClassOfAxiom newSCAxiom = dataFactory.getOWLSubClassOfAxiom(leftPart, oioClass);//添加新的包含公理					
					axiomsOfSubClass.add(newSCAxiom);					
				}			
			}
			//如果是和多个并等价
			else if((leftPart instanceof OWLClass) && (rightPart instanceof OWLObjectUnionOf) ){
				OWLObjectUnionOf oouAxiom = (OWLObjectUnionOf)rightPart;
				Set<OWLClassExpression> oouClasses = oouAxiom.asDisjunctSet();//取出析取集合，当目标是对象的并时可用
				for(OWLClassExpression oouClass : oouClasses){					
					OWLSubClassOfAxiom newSCAxiom = dataFactory.getOWLSubClassOfAxiom(oouClass,leftPart);//添加新的包含公理						
					axiomsOfSubClass.add(newSCAxiom);					
				}	
			}
		}
	}
	
	/**
	 * 依次处理各类公理集合，将所有的公理转化成包含的关系，便于图的构建,图转换预处理.<br>
	 * TBox都转化成类别包含，Abox转化成实例和类别的memberof关系.<br>
	 * 非常重要的一个函数，图的转换全部基于这个映射
	 */
	
	@SuppressWarnings("rawtypes")
	public void MappingAxioms(){	
		
		String subKey = null;
		String supKey = null;
		AxiomMapping aMapping = null;
		
		//*****处理TBox******		
		//axiomsOfSubClass 类别包含公理,特别注意要处理，A includedby exist_R.B的形式
		//有其他的形式，但当前只从等价类中添加这种形式
		for(OWLSubClassOfAxiom oscoAxiom: axiomsOfSubClass){			
			OWLClassExpression superClass = oscoAxiom.getSuperClass();
			OWLClassExpression subClass = oscoAxiom.getSubClass();			
			//处理A  includedby exist_R.B 的形式
			if(superClass instanceof OWLObjectSomeValuesFrom){
				OWLObjectSomeValuesFrom oosvfAxiom = (OWLObjectSomeValuesFrom)superClass;
				String existRole = oosvfAxiom.getProperty().asOWLObjectProperty().getIRI().getFragment();
				supKey = Tools.getExistenceToken(existRole);  //将数据转换成概念  existence_*					
				
				//出现了新的概念名，加入到概念集合中,最后在图的构造前统一来处理
				//处理 exist_R.A  includedby exist_R.B 的形式，无须处理，这会变成exist_R.  includedby exist_R				
				
				 if(subClass instanceof OWLClass){
					OWLClass owlClsOfSub = (OWLClass)subClass;
					subKey = owlClsOfSub.getIRI().getFragment();
					aMapping= new AxiomMapping(subKey,supKey,CONCEPTTYPE);			
				}
			}
			
			else if(superClass instanceof OWLClass){
				supKey = superClass.asOWLClass().getIRI().getFragment();
				
				if(subClass instanceof OWLObjectSomeValuesFrom){   //处理 existenc_A includeby B 的形式
					OWLObjectSomeValuesFrom oosvfAxiomOfSub = (OWLObjectSomeValuesFrom)subClass;
					String existRoleOfSub = oosvfAxiomOfSub.getProperty().asOWLObjectProperty().getIRI().getFragment();
					subKey = Tools.getExistenceToken(existRoleOfSub);  //将数据转换成概念  existence_*
					aMapping= new AxiomMapping(subKey,supKey,CONCEPTTYPE);	
				}				
				else if(subClass instanceof OWLClass){      //处理 A includeby B 的形式		
					subKey = subClass.asOWLClass().getIRI().getFragment();
					aMapping= new AxiomMapping(subKey,supKey,CONCEPTTYPE);					
				}		
			}
			if(aMapping!=null){
				tBoxAxiomMappings.add(aMapping);			
			}
		}		
		//axiomsOfSubProperty 属性包含公理,
		for(OWLSubPropertyAxiom ospaAxiom : axiomsOfSubProperty){		//貌似没有将值给加进去			
			OWLPropertyExpression subProperty = ospaAxiom.getSubProperty();	
			OWLPropertyExpression superProperty = ospaAxiom.getSuperProperty();
			//if( ospaAxiom instanceof OWLObjectProperty){
				/*subKey = ((OWLObjectProperty)ospaAxiom.getSubProperty()).getIRI().getFragment();
				supKey = ((OWLObjectProperty)ospaAxiom.getSuperProperty()).getIRI().getFragment();*/
				
				subKey = ((OWLObjectProperty)subProperty).getIRI().getFragment();
				supKey =((OWLObjectProperty)superProperty).getIRI().getFragment();
				
				aMapping= new AxiomMapping(subKey,supKey,ROLETYPE);			
				tBoxAxiomMappings.add(aMapping);	
				//再加入其他三种形式，存在existence_，逆inverse_，存在逆existence_inverse
				aMapping= new AxiomMapping(Tools.getInverseToken(subKey),Tools.getInverseToken(supKey),ROLETYPE);			
				tBoxAxiomMappings.add(aMapping);				
				aMapping= new AxiomMapping(Tools.getExistenceToken(subKey),Tools.getExistenceToken(supKey),CONCEPTTYPE);			
				tBoxAxiomMappings.add(aMapping);				
				aMapping= new AxiomMapping(Tools.getExistenceInverseToken(subKey),Tools.getExistenceInverseToken(supKey),CONCEPTTYPE);			
				tBoxAxiomMappings.add(aMapping);	
			//}
		}
		
		/*  axiomsOfInverseProperty 跳过*/		
		//axiomsOfDisjointClass 不相交类的处理 A和B不交 添加 A includedby negative_B
		for(OWLDisjointClassesAxiom odcaAxiom : axiomsOfDisjointClass){			
			List<OWLClassExpression> classes = odcaAxiom.getClassExpressionsAsList();			
			subKey = ((OWLClass)classes.get(0)).getIRI().getFragment();
			supKey = ((OWLClass)classes.get(1)).getIRI().getFragment();
			aMapping= new AxiomMapping(subKey,Tools.getNegativeToken(supKey),CONCEPTTYPE);	
			tBoxAxiomMappings.add(aMapping);		
			/*aMapping= new AxiomMapping(supKey,Tools.getNegativeToken(subKey),CONCEPTTYPE);	
			tBoxAxiomMappings.add(aMapping);	*/
			//System.out.println(aMapping);
		}
		
		//axiomsOfDisjointObjProperty 不相交属性(对象公理)的处理
		for(OWLDisjointObjectPropertiesAxiom odopa : axiomsOfDisjointObjProperty){
			Set<OWLObjectProperty>oops = odopa.getObjectPropertiesInSignature();
			OWLObjectProperty[] oopsInArray = (OWLObjectProperty[]) oops.toArray();
			subKey = oopsInArray[0].getIRI().getFragment();
			supKey = oopsInArray[1].getIRI().getFragment();			
			
			aMapping= new AxiomMapping(subKey,Tools.getNegativeToken(supKey),ROLETYPE);	
			tBoxAxiomMappings.add(aMapping);			
			//添加其他三种形式
			aMapping= new AxiomMapping(Tools.getInverseToken(subKey),Tools.getNegativeInverseToken(supKey),ROLETYPE);	
			tBoxAxiomMappings.add(aMapping);		
			aMapping= new AxiomMapping(Tools.getExistenceToken(subKey),Tools.getNegativeExistenceToken(supKey),CONCEPTTYPE);	
			tBoxAxiomMappings.add(aMapping);		
			aMapping= new AxiomMapping(Tools.getExistenceInverseToken(subKey),Tools.getNegativeExistenceInverseToken(supKey),CONCEPTTYPE);	
			tBoxAxiomMappings.add(aMapping);		
		}	
		
		
		for(OWLObjectPropertyDomainAxiom domainAxiom : axiomsOfObjectPropertyDomain){
			OWLObjectPropertyExpression oop = domainAxiom.getProperty();  //对象属性
	        OWLClassExpression oc = domainAxiom.getDomain();                         //对象属性的值域
	        subKey = oop.getNamedProperty().getIRI().getFragment();
	        if(!(oc instanceof OWLAnonymousClassExpressionImpl))
	        {
	        	supKey = oc.asOWLClass().getIRI().getFragment();
	        	aMapping = new AxiomMapping(Tools.getExistenceToken(subKey),supKey, CONCEPTTYPE);
		        tBoxAxiomMappings.add(aMapping);
	        }
			else if (oc.asDisjunctSet() != null) //析取的形式
			{
				 for(OWLClassExpression a:oc.asDisjunctSet())
				 {
					 if(a==null)
						 continue;
					 supKey=a.asOWLClass().getIRI().getFragment();
					 aMapping = new AxiomMapping(supKey,Tools.getExistenceToken(subKey), CONCEPTTYPE);
				     tBoxAxiomMappings.add(aMapping);
				 }
			}
			else if (oc.asConjunctSet() != null) //合取的形式
			{
				 for(OWLClassExpression a:oc.asConjunctSet())
				 {
					 if(a==null)
						 continue;
					 supKey=a.asOWLClass().getIRI().getFragment();
					 aMapping = new AxiomMapping(Tools.getExistenceToken(subKey),supKey, CONCEPTTYPE);
				     tBoxAxiomMappings.add(aMapping);
				 }
			}	        
	        else{
	        	continue;
	        }
	        /*aMapping = new AxiomMapping(Tools.getExistenceToken(subKey),supKey, CONCEPTTYPE);
	        tBoxAxiomMappings.add(aMapping);*/
		}
		
		for(OWLObjectPropertyRangeAxiom rangeAxiom : axiomsOfObjectPropertyRanger){
			OWLObjectPropertyExpression oop = rangeAxiom.getProperty();
			OWLClassExpression oc = rangeAxiom.getRange();   
			subKey = oop.getNamedProperty().getIRI().getFragment();
			if (!(oc instanceof OWLAnonymousClassExpressionImpl)) 
			{				
				supKey = oc.asOWLClass().getIRI().getFragment();
				aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey), supKey, CONCEPTTYPE);
				tBoxAxiomMappings.add(aMapping);
			} 
			else if (oc.asDisjunctSet() != null) // 析取的形式
			{
				for (OWLClassExpression a : oc.asDisjunctSet()) 
				{
					if (a == null)
						continue;
					supKey = a.asOWLClass().getIRI().getFragment();
					//aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey), supKey, CONCEPTTYPE);
					aMapping = new AxiomMapping(supKey, Tools.getExistenceInverseToken(subKey), CONCEPTTYPE);
					tBoxAxiomMappings.add(aMapping);
				}
			} 
			else if (oc.asConjunctSet() != null) // 合取的形式
			{
				for (OWLClassExpression a : oc.asConjunctSet()) {
					if (a == null)
						continue;
					supKey = a.asOWLClass().getIRI().getFragment();
					aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey), supKey, CONCEPTTYPE);
					tBoxAxiomMappings.add(aMapping);
				}
			} 
			 else{
				 continue;
			 }
		     /*aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey),supKey, CONCEPTTYPE);
		     tBoxAxiomMappings.add(aMapping);*/
		}
		
		for(OWLDataPropertyDomainAxiom domainAxiom : axiomsOfDataPropertyDomain){
			OWLDataProperty oop = (OWLDataProperty) domainAxiom.getProperty();
			OWLClassExpression oc = domainAxiom.getDomain();   
			subKey = oop.getIRI().getFragment();
			if (!(oc instanceof OWLAnonymousClassExpressionImpl)) 
			{			
				supKey = oc.asOWLClass().getIRI().getFragment();
				aMapping = new AxiomMapping(Tools.getExistenceToken(subKey), supKey, CONCEPTTYPE);
				tBoxAxiomMappings.add(aMapping);
			} 
			else if (oc.asDisjunctSet() != null) // 析取的形式
			{
				for (OWLClassExpression a : oc.asDisjunctSet()) {
					if (a == null)
						continue;
					supKey = a.asOWLClass().getIRI().getFragment();
					aMapping = new AxiomMapping(supKey,Tools.getExistenceToken(subKey) , CONCEPTTYPE);
					tBoxAxiomMappings.add(aMapping);
				}
			} 
			else if (oc.asConjunctSet() != null) // 合取的形式
			{
				for (OWLClassExpression a : oc.asConjunctSet()) {
					if (a == null)
						continue;
					supKey = a.asOWLClass().getIRI().getFragment();
					aMapping = new AxiomMapping(Tools.getExistenceToken(subKey), supKey, CONCEPTTYPE);
					tBoxAxiomMappings.add(aMapping);
				}
			}
			 else{
				 continue;
			 }
		     /*aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey),supKey, CONCEPTTYPE);
		     tBoxAxiomMappings.add(aMapping);*/
		}
		
		/*//通常DataProperty的Data类型太多不太适合作为图中的结点来进行处理，且预先要加入Data类型的等价关系，很大程度上影响遍历的速度
		for(OWLDataPropertyRangeAxiom domainAxiom : axiomsOfDataPropertyRanger){
			OWLDataProperty oop = (OWLDataProperty) domainAxiom.getProperty();
			OWLDataRange oc = domainAxiom.getRange();     
			 if(!(oc.isDatatype()))
			 {
				 subKey = oop.getIRI().getFragment();
				 supKey=oc.asOWLDatatype().getIRI().getFragment();
			 }
			 else{
				 continue;
			 }
		     aMapping = new AxiomMapping(Tools.getExistenceInverseToken(subKey),supKey, CONCEPTTYPE);
		     tBoxAxiomMappings.add(aMapping);
		}*/
		
		
		
		/*private Set<OWLDataPropertyDomainAxiom> axiomsOfDataPropertyDomain;      //定义域集合
		private Set<OWLDataPropertyRangeAxiom> axiomsOfDataPropertyRanger;      //值域集合
*/		
		//*****处理ABox******		
		
		for(OWLClassAssertionAxiom ocaAxiom : axiomsOfClassAssertion){ // membership, A(a)
			
			if(!(ocaAxiom.getClassExpression().isAnonymous())){
				supKey = ocaAxiom.getClassExpression().asOWLClass().getIRI().getFragment();  
			}
			
			if(ocaAxiom.getIndividual() instanceof OWLNamedIndividual){
				
				//subKey =((OWLNamedIndividual)(ocaAxiom.getIndividual())).getIRI().getFragment().toString();
				subKey =((OWLNamedIndividual)(ocaAxiom.getIndividual())).getIRI().toString(); //个体 Individual 不应该去除前缀
				//去掉http://	//
				subKey = subKey.replace("http://", "");
			}
			else{
				continue;		
			}
			aMapping = new AxiomMapping(subKey, supKey, INDIVIDUALTYPE);
			aBoxAxiomMappings.add(aMapping);			
		}
		
		for(OWLObjectPropertyAssertionAxiom oopaAxiom : axiomsOfObjProAssertion ){ // membership, R(a,b)
			//System.out.println(oopaAxiom.getSubject()+"###"+oopaAxiom.getObject());
			supKey = oopaAxiom.getProperty().asOWLObjectProperty().getIRI().getFragment();  //Object property
			String existenceSupKey = Tools.getExistenceToken(supKey);
			String existenceInverseSupkey = Tools.getExistenceInverseToken(supKey);
			
			/*String subject = oopaAxiom.getSubject().asOWLNamedIndividual().getIRI().getFragment().toString();
			String object = oopaAxiom.getObject().asOWLNamedIndividual().getIRI().getFragment().toString();*/	
			
			String subject = oopaAxiom.getSubject().asOWLNamedIndividual().getIRI().toString();
			String object = oopaAxiom.getObject().asOWLNamedIndividual().getIRI().toString();
			
			aMapping = new AxiomMapping(subject, existenceSupKey, MEMBEROFCONCEPE);
			aBoxAxiomMappings.add(aMapping);
			
			aMapping = new AxiomMapping(object, existenceInverseSupkey, MEMBEROFCONCEPE);
			aBoxAxiomMappings.add(aMapping);					
		}
	}

	/**
	 * 取出在构造图中使用的公理的数量
	 */
	
	public int getAvailableAxiomsSize() {
		int size = 0;
		size += this.axiomsOfDisjointClass.size();
		size += this.axiomsOfDisjointObjProperty.size();
		size += this.axiomsOfFunct.size();
		size += this.axiomsOfInverseProperty.size();
		size += this.axiomsOfSubClass.size();
		size += this.axiomsOfSubProperty.size();
		size += this.axiomsOfEquivalentObjProperty.size();
		size += this.axiomsOfEquivalentClass.size();
		return size;
	}
	/**
	 * 全局函数，获取不相交公理
	 * @param owlPath owl路径
	 * @return 不相交公理集合
	 */
	public static Set<OWLDisjointClassesAxiom> getDisjointness(String owlPath){
		Set<OWLDisjointClassesAxiom> disjointAxioms = new HashSet<>();
		try {
			File ontologyFile = new File(owlPath);
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
			for(OWLAxiom axiom : ontology.getAxioms()){
				if(axiom instanceof OWLDisjointClassesAxiom){
					disjointAxioms.add((OWLDisjointClassesAxiom)axiom);
				}
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();			
		}			
		return disjointAxioms;
	}
}
