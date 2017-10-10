package kse.algorithm.forABox.preprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * 将一个owlABox文件分成两个，或者将多个合并成一个,没有涉及不相交概念和实例的生成.<br>
 * 处理原始生成的ABox，没有必要考虑不一致的百分比
 * From experiment of ISWC'13  [2013-5][2014-9修订]
 * @author Xuefeng Fu
 */

public class ABoxFileHandle {
	
	public String univOwlDir;	                     //本体文件夹
	public int percent;                                 //不相交概念的百分比
	public int univOwlNumber;                 //大学数目
	OWLOntologyManager manager;	
	
	private  List<File> sourceUnivOwls;    //（源）大学本体文件集合
	/**
	 * ABox本体文件处理
	 * @param percent TBox中不相交概念的占比
	 * @param univOwlNumber 大学的数量
	 */
	public ABoxFileHandle(int percent, int univOwlNumber){
		this.univOwlNumber = univOwlNumber;
		this.percent = percent;
		this.univOwlDir = getSourceDirByPercent(percent, univOwlNumber);		
		init();
	}
	
	public ABoxFileHandle(int univNum){
		this.univOwlNumber = univNum;
		this.univOwlDir = getSourceDirWithoutPercent(univNum);
		//System.out.println(univOwlDir);
		init();
	}
	
	public ABoxFileHandle(String dir){
		this.univOwlDir = dir;		
		this.percent = -1;
		init();
	}
	
	public void init(){
		sourceUnivOwls = new ArrayList<File>();		
		manager = OWLManager.createOWLOntologyManager();
	}
	
	public void go(){		
		File directory = new File(univOwlDir);		
		if(directory.isDirectory()){ //是否为目录，处理该目录下所有的相关的本体
			File[] files = directory.listFiles();			
			//如果只有一个文件，则分开
			if(files.length == 1){
				splitOne2Two(files[0]);
			}
			else{
				for(int i=0; i<files.length; i++){
					sourceUnivOwls.add(files[i]);
				}
				mergeOwl(sourceUnivOwls);
			}
		}
	}
	/**
	 * 将一个本体分割成两个
	 * @param ontologyFile 本体路径
	 */
	public void splitOne2Two(File ontologyFile){		
		//System.out.println(String.format("split owl %s", ontologyFile.getName()));
		//OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = null;
		try {
			ontology  = manager.loadOntologyFromOntologyDocument(ontologyFile);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			ontology = null;
		}		
		Set<OWLAxiom> willAddedin1 = new HashSet<OWLAxiom>();
		Set<OWLAxiom> willAddedin2 = new HashSet<OWLAxiom>();
		Random random = new Random();			
		for(OWLAxiom axiom: ontology.getAxioms()){			
			if(random.nextInt(10)>=5){  //基本按概率均匀分布
				willAddedin1.add(axiom);
			}
			else{
				willAddedin2.add(axiom);
			}		
		}			
		saveToOntology(willAddedin1, new File(getTargetFileName(percent,1,1)));
		saveToOntology(willAddedin2, new File(getTargetFileName(percent,1,2)));
	}
	
	/**将公理集保存到目标本体中
	 * @param willAddedin 公理集
	 * @param onto 目标本体文件
	 */
	public void saveToOntology(Set<OWLAxiom> willAddedin, File onto){
		OWLOntology ontology = null;
		IRI  iri = IRI.create(onto);
		//System.out.println("save "+iri.toString());
		try {			
			ontology = manager.createOntology(willAddedin);
			manager.saveOntology(ontology,iri);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}			
		catch (OWLOntologyStorageException e) {
			e.printStackTrace();
		}		
	}
	/**
	 * 将多个Abox合并成两个
	 * @param owlFiles 待合并的本体文件集合
	 * @param univNumber	大学的数量
	 */
	public void mergeOwl(List<File> owlFiles){
		Set<OWLAxiom> willAddedin1 = new HashSet<OWLAxiom>();
		Set<OWLAxiom> willAddedin2 = new HashSet<OWLAxiom>();
		for(File file : owlFiles){
			System.out.println(file.getName());
			OWLOntology ontology = null;
			try {
				ontology  = manager.loadOntologyFromOntologyDocument(file);
			} catch (OWLOntologyCreationException e) {
				e.printStackTrace();
				ontology = null;
			}			
			Random random = new Random();			
			for(OWLAxiom axiom: ontology.getAxioms()){			
				if(random.nextInt(10)>=5){  //按概率均匀分布
					willAddedin1.add(axiom);
				}
				else{
					willAddedin2.add(axiom);
				}		
			}
		}		
		//将合并后的本体保存到文件中
		saveToOntology(willAddedin1, new File(getTargetFileName(percent, univOwlNumber, 1)));
		saveToOntology(willAddedin2, new File(getTargetFileName(percent, univOwlNumber, 2)));
	}
	
	public static String getSourceDirByPercent(int percent, int numberOfUniv){
		return String.format("ForABoxRevi/P%d/Source_%d", percent, numberOfUniv);
		//return String.format("Owls/GraduallyExperiment/source%d", i);
	}
	
	public static String getSourceDirWithoutPercent(int numberOfUniv){
		return String.format("ForABoxRevi/Source_%d", numberOfUniv);
	}
	
	/**
	 * @param percent 不交类的百分比
	 * @param type 大学的数量
	 * @param no 排名
	 * @return
	 */
	public static String getTargetFileName(int percent, int type,int no){
		if(percent == -1){
			return getGraduallyFileName(type, no);
		}
		else{
			//return String.format("ForABoxRevi/P%d/univ_%d_%d.owl", percent,type,no);		
			return String.format("ForABoxRevi/univ_%d_%d.owl", type,no);	
		}
	}
	/**
	 * 渐进实验本体文件名规则,在新的实验中没有做渐进实验
	 * @param type
	 * @param no
	 * @return
	 */
	public static String getGraduallyFileName(int type,int no){
		return String.format("ForABoxRevi/GraduallyExperiment/source/univ_%d_%d.owl", type,no);		
	}
	
	public static String getBenchmarkName(int percent){
		return String.format("ForABoxRevi/P%s/univ-bench-lite.owl", percent);
	}	
}
