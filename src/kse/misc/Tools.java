package kse.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;

import static kse.misc.GlobalParams.*;

/**
 * 一些全局要用工具
 * @author Xuefeng fu
 */

@SuppressWarnings("unused")
public class Tools {

	//整个试验的输出
	public static String result = "running/%s.txt";   

	/**
	 * 将某个字符串保持在result指定的文本文件中，内容可追加
	 * @param s
	 * @param fileName
	 * @param isAppend  是否追加，false为覆盖
	 */
	public static void saveToFile(String s, String fileName, boolean isAppend) {                
		//File file = new File(String.format(result,fileName));
		File file = new File(fileName);
		FileOutputStream fw = null;
		try {
			fw = new FileOutputStream(file, isAppend);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		OutputStreamWriter osw = new OutputStreamWriter(fw);
		BufferedWriter bw = new BufferedWriter(osw);
		try {
			bw.write(s);
			bw.newLine();
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//将公理集合保持在output指定的文本文件中,内容为覆盖
	public static void saveToFile(Set<OWLAxiom> axioms, String type, String outputFile) {
		File file = new File(outputFile);
		FileOutputStream fw = null;
		try {
			fw = new FileOutputStream(file, false); //后一个参数表示是否为追加，bool append
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		OutputStreamWriter osw = new OutputStreamWriter(fw);
		BufferedWriter bw = new BufferedWriter(osw);
		try {
			for(OWLAxiom axiom : axioms){
				String temp = axiom.toString();
				if(type!=null){
					if(type.equals("OWLClassAssertionAxiom")&&axiom instanceof OWLClassAssertionAxiom){				
						bw.write(temp);
						bw.newLine();				
					}
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	

	
	/**
	 * 取出一个函数属性的符号
	 * @param token
	 * @return funct_token
	 */
	public static String getFunctToken(String token){
		return String.format("funct_%s", token);
	}
	
	/**
	 * 取出一个实体否定的符号
	 * @param token
	 * @return negative_token
	 */
	public static String getNegativeToken(String token){
		return String.format("negative_%s", token);
	}
	
	/**
	 * 取出一个实体否定_逆的符号
	 * @param token
	 * @return negative_inverse_token
	 */
	public static String getNegativeInverseToken(String token){
		return String.format("negative_inverse_%s", token);
	}
	
	/**
	 * 取出一个实体否定_存在的符号
	 * @param token
	 * @return negative_existence_token
	 */
	public static String getNegativeExistenceToken(String token){
		return String.format("negative_existence_%s", token);
	}
	
	/**
	 * 取出一个实体否定_存在_逆的符号
	 * @param token
	 * @return negative_existence_inverse_token
	 */
	public static String getNegativeExistenceInverseToken(String token){
		return String.format("negative_existence_inverse_%s", token);
	}
	
	/**
	 * 去除实体否定的符号
	 * @param negative_token
	 * @return token
	 */
	public static String removeNegativeToken(String token){
		return token.replaceAll("negative_", "");
	}
	
	/**
	 * 取出一个属性结合存在量词的符号
	 * @param token
	 * @return existence_token
	 */
	public static String getExistenceToken(String token){
		return String.format("existence_%s", token);
	}
	
	/**
	 * 取出一个实体逆的符号
	 * @param token
	 * @return inverse_token
	 */
	public static String getInverseToken(String token){
		return String.format("inverse_%s", token);
	}
	/**
	 * 取出一个实体逆的符号
	 * @param token
	 * @return existence_inverse_token
	 */
	public static String getExistenceInverseToken(String token){
		return String.format("existence_inverse_%s", token);
	}
	
	/**
	 * 取出一个实体等价的符号
	 * @param token
	 * @return equivalent_token
	 */
	public static String getEquivalentToken(String token){
		return String.format("equivalent_%s", token);
	}	
	
	/**
	 * 取出原子角色的其他三种形式，inverse_*, existence_*, existence_inverse_*
	 * @return String[]
	 */
	public static String[] getRoleForms(String roleSign){
		String[] roleForms = new String[3];
		roleForms[0] = getInverseToken(roleSign);
		roleForms[1] = getExistenceToken(roleSign);
		roleForms[2] = getExistenceInverseToken(roleSign);		
		return roleForms;
	}
	
	/**
	 * 
	 * @param node
	 * @return roleName corresponding to the node
	 */
	public static String getRoleName(Node node){
		String roleName = null;
		String tmp = node.getProperty(NAMEPROPERTY).toString();
		if (node.getProperty(TYPEPROPERTY).toString().equals(ROLETYPE)) {
			if (tmp.startsWith("negative_inverse_")){
				roleName = tmp.replaceAll("negative_inverse_", "");
			}
			else if (tmp.startsWith("inverse_")) {
				roleName = tmp.substring(tmp.indexOf("_") + 1);
			}
			else if (tmp.startsWith("negative_")) {
				roleName = tmp.substring(tmp.indexOf("_") + 1);
			}
			roleName = tmp;
		}
		else if (tmp.startsWith("negative_existence_inverse_")) {
			roleName = tmp.replaceAll("negative_existence_inverse_", "");
		}
		else if (tmp.startsWith("negative_existence_")) {
			roleName = tmp.replaceAll("negative_existence_", "");
		}
		else if (tmp.startsWith("existence_")) {
			roleName = tmp.substring(tmp.indexOf("_") + 1);
		}		
		return roleName;
	}
	
	/**
	 * 清空StringBuilder
	 * @param query 要清空的查询
	 */
	public static void clear(StringBuilder query){
		query.delete(0, query.length());
	}

	public static List<String> getPrefixes(){
		List<String> prefixes = new ArrayList<>();
		prefixes.add("http://purl.obolibrary.org/obo/");                                      //aeo         
		//prefixes.add("http://purl.obolibrary.org/obo/");                                     //chebi
		prefixes.add("http://purl.obolibrary.org/obo/");                                       //cl.owl
		prefixes.add("http://www.loa-cnr.it/ontologies/DOLCE-Lite.owl#");    //Dolce-lite
		prefixes.add("http://reliant.teknowledge.com/DAML/SUMO.owl");    //economy-SDA已经是不协调本体，不做注入处理
		//prefixes.add("http://www.geneontology.org/formats/oboInOwl#");  //emap 
		prefixes.add("http://purl.obolibrary.org/obo/");                                     //fly_anatomy
		prefixes.add("http://purl.obolibrary.org/obo/");                                     //fma
		//prefixes.add("#");                                                                                            //full-galen     
		//prefixes.add("http://www.co-ode.org/ontologies/galen#");                     //galen
		prefixes.add("http://www.geneontology.org/owl/#");                              //go 
		prefixes.add("http://purl.obolibrary.org/obo/");                                       //plant
		prefixes.add("http://counterterror.mindswap.org/2005/terrorism.owl#");   //Terrosism 已经是不协调本体，不做注入处理
		/*13*/prefixes.add("http://reliant.teknowledge.com/DAML/Economy.owl#");                                                                                             //Transportation-SDA 已经是不协调本体，不做注入处理
		return prefixes;
	}

	public static  List<String> getOWLsOfExperimentation(){ 
		List<String> owls = new ArrayList<>();
		owls.add("aeo");
		owls.add("chemical");
		owls.add("chemical2");
		//owls.add("chebi");
		//owls.add("cl");
		owls.add("dolce");
		//owls.add("Economy-SDA");
		//owls.add("emap");
		owls.add("fly-anatomy");
		//owls.add("fma");
		//owls.add("full-galen");
		////owls.add("galen");
		owls.add("go");
		owls.add("ma");
		owls.add("not-galen");
		//owls.add("plant");
		// owls.add("Terrorism");
		// owls.add("Transportation-SDA");	
		//owls.add("km1500-4000");
		owls.add("proton_50_studis_T");
		return owls;
	}
	
	
	public static List<String> getTestOWLs(){
		List<String> owls = new ArrayList<>();
		/*1*/owls.add("AROMA-cmt-cocus");
		/*2*/owls.add("AROMA-ekaw-myreview");
		/*3*/owls.add("buggyPolicy");
		/*4*/owls.add("CHEM-A");
		/*5*/owls.add("DICE-A");
		/*6*/owls.add("Economy-SDA");
		/*7*/owls.add("GOMMA-cocus-paperdyne");
		/*8*/ owls.add("km1500-2000");
		/*9*/owls.add("km1500-3000");
		/*10*/owls.add("km1500-4000");
		/*11*/owls.add("km1500-5000");
		/*12*/owls.add("koala");
		/*13*/owls.add("LogMapLt-cocus-crs_dr");
		/*14*/owls.add("LogMapLt-cocus-ekaw");
		/*15*/owls.add("MaasMatch-cmt-sigkdd");
		/*16*/owls.add("miniTambis");
		/*17*/owls.add("tambis");
		/*18*/owls.add("Terrorism");
		/*19*/owls.add("Transportation-SDA2");
		/*20*/owls.add("University");		
		return owls;
	}
	
/*	public static String relToStr(Relationship arc){
		String rStr = arc.getStartNode().getProperty(NAMEPROPERTY).toString() +"-->" +
		                        arc.getEndNode().getProperty(NAMEPROPERTY).toString()+ "[" +
		                        arc.getProperty(COMEFROMPROPERTY).toString()+"]";
		return rStr;
	}*/
	
}
