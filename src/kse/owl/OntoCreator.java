package kse.owl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;

/**
 * 使用OWLAPI创建新的本体文件 
 * [2014-3]
 * @author Xuefeng Fu 
 * 
 */
@SuppressWarnings("unused")
public class OntoCreator {
	public   String PREFIX ;
	private String ontoFile;                                           //新本体文件存储路径
	private OWLOntologyManager ontoManager;   //本体管理类
	private OWLOntology onto;                                  //本体类 
	private OWLDataFactory dataFactory ;             //本体公理工厂
	public OntoCreator(String ontoFile){
		this.ontoFile = ontoFile;		
		PREFIX = "http://www.seu.edu.cn/kse#";
		ontoManager = OWLManager.createOWLOntologyManager();
		dataFactory = ontoManager.getOWLDataFactory();
		try {
			onto = ontoManager.createOntology();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}		
	}
	
	public OntoCreator(String ontoFile, String prefix, OWLOntology onto, OWLOntologyManager manager){
		this.onto = onto;
		this.ontoFile = ontoFile;		
		this.PREFIX = prefix;
		this.ontoManager = manager;
		dataFactory = ontoManager.getOWLDataFactory();		
	}
	/**
	 * 将新建的本体保持到owl文件中
	 */
	public void saveOnto(){
		File ontologyFile = new File(ontoFile);
		if(ontologyFile.exists()){
			ontologyFile.delete();
		}
		try {
			OutputStream os = new FileOutputStream(ontologyFile, true);
			if(onto!=null && os!=null){
				ontoManager.saveOntology(onto, os);
			}
			else{
				System.out.println("Error, ontology or output stream is null");
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 插入属性包含公理
	 * @param subRole 子属性
	 * @param supRole 父属性
	 */
	public void insertSubsumptionOfRole(String subRole, String supRole ){
		OWLObjectProperty ocSubR = dataFactory.getOWLObjectProperty(IRI.create(PREFIX, subRole));
		OWLObjectProperty ocSupR = dataFactory.getOWLObjectProperty(IRI.create(PREFIX, supRole));		
		OWLSubObjectPropertyOfAxiom osopAxiom = dataFactory.getOWLSubObjectPropertyOfAxiom(ocSubR, ocSupR);
		System.out.println("Adding axiom ### " +osopAxiom);
		ontoManager.addAxiom(onto, osopAxiom);	
	}
	
	/**
	 * 插入类别包含公理
	 * @param subConcept 子概念
	 * @param supConcept 父概念
	 */
	public void insertSubsumptionOfConcept(String subConcept, String supConcept){		
		OWLClass ocSubC = dataFactory.getOWLClass(IRI.create(PREFIX, subConcept));
		OWLClass ocSupC = dataFactory.getOWLClass(IRI.create(PREFIX, supConcept));
		OWLSubClassOfAxiom oscAxiom = dataFactory.getOWLSubClassOfAxiom(ocSubC, ocSupC);
		System.out.println("Adding axiom ### " +oscAxiom);
		ontoManager.addAxiom(onto, oscAxiom);				
	}
	
	/**
	 * 插入函数约束的属性公理
	 * @param funcR 公理名
	 */
	public void insertFunctionalAxiom(String funcR){
		OWLObjectProperty ocSupR = dataFactory.getOWLObjectProperty(IRI.create(PREFIX, funcR));		
		OWLFunctionalObjectPropertyAxiom ofdpAxiom = dataFactory.getOWLFunctionalObjectPropertyAxiom(ocSupR);
		System.out.println("Adding axiom ### " +ofdpAxiom);
		ontoManager.addAxiom(onto, ofdpAxiom);		
	}
	
	/**
	 * 插入不相交的类
	 * @param c1 概念1
	 * @param c2 概念2
	 */
	public void insertDisjointClassesAxiom(String c1, String c2){
		OWLClass ocC1 = dataFactory.getOWLClass(IRI.create(PREFIX, c1));
		OWLClass ocC2 = dataFactory.getOWLClass(IRI.create(PREFIX, c2));				
		Set<OWLClass> classSet = new HashSet<OWLClass>();
		classSet.add(ocC1);
		classSet.add(ocC2);
		OWLDisjointClassesAxiom odcAxiom = dataFactory.getOWLDisjointClassesAxiom(classSet);
		System.out.println("Adding axiom ### " +odcAxiom);
		ontoManager.addAxiom(onto, odcAxiom);			
	}
	
	/**
	 * 插入类别成员断言
	 * @param concept  概念名，不包括前缀，前缀在类别实例化中已经定义
	 * @param individual 概念实例的名称
	 */
	public void insertMembershipAssertion(String concept, String individual){
		OWLClass owlC = dataFactory.getOWLClass(IRI.create(PREFIX, concept));
		OWLNamedIndividual owlI = dataFactory.getOWLNamedIndividual(IRI.create(PREFIX, individual));
	    OWLClassAssertionAxiom oaAxiom =	dataFactory.getOWLClassAssertionAxiom(owlC, owlI);
		System.out.println("Adding axiom ### " +oaAxiom);
		ontoManager.addAxiom(onto, oaAxiom);		
	}
	

	/**
	 * 插入属性实例断言
	 * @param r 属性名
	 * @param i1 实例1
	 * @param i2 实例2
	 */
	public void insertMembershipAssertion(String r, String i1, String i2){		
		OWLObjectProperty role = dataFactory.getOWLObjectProperty(IRI.create(PREFIX, r));				
		OWLNamedIndividual oni1 = dataFactory.getOWLNamedIndividual(IRI.create(PREFIX, i1));
		OWLNamedIndividual oni2 = dataFactory.getOWLNamedIndividual(IRI.create(PREFIX, i2));		
	    OWLObjectPropertyAssertionAxiom oopaAxiom =	dataFactory.getOWLObjectPropertyAssertionAxiom(role,oni1,oni2);
		System.out.println("Adding axiom ### " +oopaAxiom);
		ontoManager.addAxiom(onto, oopaAxiom);		
	}
	
	public void insertAxiom(OWLAxiom axiom){
		ontoManager.addAxiom(onto, axiom);
	}

}
