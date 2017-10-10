package kse.owl.running;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

public class OwlAPIDemo {
	public  String PREFIX ;
	private String ontoFile;                                           //新本体文件存储路径
	private OWLOntologyManager ontoManager;   //本体管理类
	private OWLOntology ontology;                                  //本体类 
	private OWLDataFactory dataFactory ;             //本体公理工厂
	
	public OwlAPIDemo(String file){
		this.PREFIX = "#";
		ontoManager =  OWLManager.createOWLOntologyManager();
		dataFactory = OWLManager.getOWLDataFactory();
		this.ontoFile = file; 
		try {
			ontology = ontoManager.loadOntologyFromOntologyDocument(new File(ontoFile));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
	
	public void insertDisjointClassesAxiom(String c1, String c2){
		OWLClass ocC1 = dataFactory.getOWLClass(IRI.create(PREFIX, c1));
		OWLClass ocC2 = dataFactory.getOWLClass(IRI.create(PREFIX, c2));				
		Set<OWLClass> classSet = new HashSet<OWLClass>();
		classSet.add(ocC1);
		classSet.add(ocC2);
		OWLDisjointClassesAxiom odcAxiom = dataFactory.getOWLDisjointClassesAxiom(classSet);
		System.out.println("Adding axiom ### " +odcAxiom);
		ontoManager.addAxiom(ontology, odcAxiom);			
	}
	
	public void saveAs(String newOntoFile){
		File newOwl = new File(newOntoFile);
		OutputStream os;
		try {
			os = new FileOutputStream(newOwl, true);
			ontoManager.saveOntology(ontology, os);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}	
	}
	
	public static void main(String[] args){
		String owl = "owls/DIY/diyOnto.owl";
		String newOwl = "owls/DIY/diyOnto_new.owl";
		OwlAPIDemo app = new OwlAPIDemo(owl);
		app.insertDisjointClassesAxiom("C1", "C2");
		app.saveAs(newOwl);
	}
}
