package kse.owl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import com.ibm.icu.math.BigDecimal;

/**
 * 取出owl文件中的信息，包括各类公理(axiom)，分TBox、ABox. 类（概念.concept.Class），对象属性（角色.role.object
 * property），数据属性（属性.attribute.data property） Get the information of owl file,
 * include axioms, classes, object properties , data properties.
 * [2014-5]
 * @author Xuefeng Fu
 */

public class MappingInfo {
	private ArrayList<String> mappings; 
	private ArrayList<ArrayList<String>> MinimcalConflictSet; 
	OntModel ontology = ModelFactory.createOntologyModel();
	
	public MappingInfo(String MappingPath) throws IOException 
	{
		if(MappingPath.contains(".rdf"))
		{
			mappings=getReference(MappingPath);
		}
		else
		{
			BufferedReader Alignment = new BufferedReader(new FileReader(new File(MappingPath)));
			mappings = new ArrayList<String>();
			String lineTxt = null;
			while ((lineTxt = Alignment.readLine()) != null) {
				String line = lineTxt.trim(); // 去掉字符串首位的空格，避免其空格造成的错误
				// line=line.toLowerCase();//全部变成小写
				mappings.add(line);
			}
		}
	}
	
	public ArrayList<String> getMappings()
	{
		return mappings;
	}
	
	public ArrayList<String> getReference(String alignmentFile)
	{
		  ArrayList<String> references=new ArrayList<String>();  
		    Model model = ModelFactory.createDefaultModel();
			InputStream in = FileManager.get().open( alignmentFile );
	        if (in == null) {
	        	System.out.println("File: " + alignmentFile + " not found!");
	            throw new IllegalArgumentException( "File: " + alignmentFile + " not found");
	        }
	        model.read(in,"");
			//model.read(in,null);
	        //解析方式1(针对YAM++)
			/*Property alignmententity1, alignmententity2;
			alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity1");
			alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity2");
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#measure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignment#relation");*/
			//OntClass temp;
	        //解释方式2(针对AML++,reference alignments)
		    Property alignmententity1 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity1");
			Property alignmententity2 = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmententity2");//alignment
			Property value = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentmeasure");
			Property relation = model.getProperty("http://knowledgeweb.semanticweb.org/heterogeneity/alignmentrelation");

	        ResIterator resstmt = model.listSubjectsWithProperty(alignmententity1);	//两者方法是一样的 
		    Resource temp;
			while(resstmt.hasNext()){
				temp = resstmt.next();//temp是三元组
				String entity1 = temp.getPropertyResourceValue(alignmententity1).getLocalName();//获取本体1的本体
				String entity2 = temp.getPropertyResourceValue(alignmententity2).getLocalName();//获取本体2的本体
				String Confidence=temp.getProperty(value).getObject().asLiteral().getString();//获取信念值
				String Relation=temp.getProperty(relation).getObject().toString();//获取匹配的关系
				
				//比较笨的方法
				/*String Confidence=temp.getProperty(value).getLiteral().toString();
				Confidence=Confidence.replace("^^xsd:float", "").replace("^^http://www.w3.org/2001/XMLSchema#float", "");
				System.out.println(Confidence);*/
				
				
			//	System.out.println(Relation);
			//	System.out.println(entity1+" "+entity2);
			//	System.out.println(entity1+" "+entity2+" "+Relation+" "+Confidence);
				
				//输出所有的三元组
		/*		StmtIterator stmt = model.listStatements();
				while(stmt.hasNext()){
					System.out.println(stmt.next());
				}*/
				//entity1=entity1.replace("-", "_");//为了画图方便，将'-'替换成'_'		
				//entity2=entity2.replace("-", "_");//为了画图方便，将'-'替换成'_'	
				BigDecimal   b   =   new   BigDecimal(Double.parseDouble(Confidence));  //四舍五入的方式
				Double confidence =   b.setScale(2,  BigDecimal.ROUND_HALF_UP).doubleValue();  
				
				StringBuilder map=new StringBuilder();
				map.append(entity1);
				map.append(",");
				map.append(entity2);
				map.append(",");
				map.append(Relation);
				map.append(",");
				map.append(confidence);
				references.add(map.toString());				
				//references.add(entity1+","+entity2+","+Relation+","+confidence);//统一转化成小写
				
				//references.add(entity1+" = "+entity2);//统一转化成小写
			}
			return references;
	}
}
