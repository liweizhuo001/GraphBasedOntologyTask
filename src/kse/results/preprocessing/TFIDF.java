package kse.results.preprocessing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import kse.algorithm.auxiliaryClass.OAEIAlignmentOutput;
import kse.owl.OWLInfo;

public class TFIDF {
	static BufferedWriter bfw_Result= null;
	static double []thresholds={0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95,1.0};
	
	public static void main(String args[]) throws Exception
	{
		
		String []Ontology1={"cmt","Conference","confOf","edas","ekaw","iasted","sigkdd"};
		String []Ontology2={"cmt","Conference","confOf","edas","ekaw","iasted","sigkdd"};
		for (int x=0;x<Ontology1.length;x++)
		{
			String ontologyName1 = Ontology1[x];
			for(int y=x+1;y<Ontology2.length;y++)
			{			
				String ontologyName2 = Ontology2[y];
				/*String ontologyName1="Conference";
				String ontologyName2="iasted";*/

				String owlPath1="OAEIOntology/"+ontologyName1+".owl";
				String owlPath2="OAEIOntology/"+ontologyName2+".owl";
				
				
				OWLInfo owlInfo1= new OWLInfo(owlPath1);	
				OWLInfo owlInfo2= new OWLInfo(owlPath2);
				
				Set<String> concepts1 = owlInfo1.getConceptTokens();
				Set<String> objectproperties1 = owlInfo1.getObjPropertyTokens();	
				Set<String> dataproperties1 = owlInfo1.getDataPropertyTokens();
				Set<String> concepts2 = owlInfo2.getConceptTokens();
				Set<String> objectproperties2 = owlInfo2.getObjPropertyTokens();	
				Set<String> dataproperties2 = owlInfo2.getDataPropertyTokens();	
				
				ArrayList<String> conceptList1=new ArrayList<String>();
				ArrayList<String> conceptList2=new ArrayList<String>();
				for(String s:concepts1)
				{
					conceptList1.add(s);
				}
				for(String s:concepts2)
				{
					conceptList2.add(s);
				}
				
				ArrayList<String> editSimClass=tfidfSim(conceptList1,conceptList2);
				
				ArrayList<String> editSimObjectProperty=editSimilairty(objectproperties1,objectproperties2);
				ArrayList<String> editSimDataProperty=editSimilairty(dataproperties1,dataproperties2);
				
				
						
				//String resultPath2="Results/middle_Result.txt";				
				for(int i=0;i<thresholds.length;i++)
				{
					String resultPath="alignments/Results/"+ontologyName1+"-"+ontologyName2+"-tfidf_batch_"+thresholds[i];
					try
					{			
						bfw_Result=new BufferedWriter(new FileWriter(resultPath+".txt"));	
					}
					catch(IOException e)
					{
						e.printStackTrace();		
					}
					ArrayList<String> results=new ArrayList<String>();
					for(String s:editSimClass)
					{
						String parts[]=s.split(",");
						if(Double.parseDouble(parts[2])>=thresholds[i])
						{
							results.add(s);
							bfw_Result.append(s+"\n");
						}
					}
					for(String s:editSimObjectProperty)
					{
						String parts[]=s.split(",");
						if(Double.parseDouble(parts[2])>=thresholds[i])
						{
							results.add(s);
							bfw_Result.append(s+"\n");
						}
					}
					for(String s:editSimDataProperty)
					{
						String parts[]=s.split(",");
						if(Double.parseDouble(parts[2])>=thresholds[i])
						{
							results.add(s);
							bfw_Result.append(s+"\n");
						}
					}
					
					//OAEIAlignmentOutput out=new OAEIAlignmentOutput(resultPath,ontologyName1,ontologyName2);
					OAEIAlignmentOutput out=new OAEIAlignmentOutput(resultPath,owlInfo1.getURI(),owlInfo2.getURI());
					for(String s: results)
					{
						String parts[]=s.split(",");
						out.addMapping2Output(parts[0],parts[1],parts[2]);
					}
					out.saveOutputFile();
					bfw_Result.close();
				}
				
			}
			
		}
		System.out.println("The calculation of similarity based on TFIDF is finished!");
	}

	
	public static ArrayList<String> editSimilairty(Set<String> object1s, Set<String> object2s) 
	{
		ArrayList<String> editDistance=new ArrayList<String>();
		int number=0;
		for(String xx:object1s)
		{
			String str1=xx.toLowerCase().replace("_", "").replace("-", "").replace(" ", "").replace("'", " ").replace(":", " ").replace("(", " ").replace(")", " ").replace(",", " ").replace(", ", " ").replace(";", " ").replace("  ", " ").trim();
			for(String yy:object2s)
			{
				//System.out.println(str2);
				String str2=yy.toLowerCase().replace("_", "").replace("-", "").replace(" ", "").replace("'", " ").replace(":", " ").replace("(", " ").replace(")", " ").replace(",", " ").replace(", ", " ").replace(";", " ").replace("  ", " ").trim();
				int ld = similarityOfDistance(str1, str2);		
				float sim=1 - (float) ld / Math.max(str1.length(), str2.length());
				//editDistance.add(object1s.get(i)+","+object2s.get(j)+","+sim);
				if(sim>0)
					number++;
			/*	if(sim==1)
					System.out.println(object1s.get(i)+","+object2s.get(j));*/
				editDistance.add(xx+","+yy+","+sim);
			}
		}	
		System.out.println("编辑距离相似度不为0的个数为："+number);
		return editDistance;
	}
	
	public static int similarityOfDistance(String str1, String str2) {
		int n = str1.length();
		int m = str2.length();
		char ch1, ch2; // str1
		int temp;
		if (n == 0) {
			return m; // 判断其中字符串1为空的情况
		}
		if (m == 0) // 判断其中字符串2为空的情况
		{
			return n;
		}
		// 定义一个(n+1)*(m+1)的矩阵来计算编辑距离
		int[][] d = new int[n + 1][m + 1];
		// 初始化矩阵
		for (int i = 0; i <= n; i++) {
			d[i][0] = i;
		}
		for (int j = 0; j <= m; j++) {
			d[0][j] = j;
		}
		// 按照计算字符串们的算法依次填充矩阵中的值
		for (int i = 1; i < n + 1; i++) {
			ch1 = str1.charAt(i - 1); // 提取字符串的1第i个字母,因为i初始值为1，效果是一样的
			for (int j = 1; j <= m; j++) {
				ch2 = str2.charAt(j - 1); // 提取字符串的1第i个字母,因为i初始值为1，效果是一样的
				if (ch1 == ch2) {
					temp = 0;
				} else {
					temp = 1;
				}
				d[i][j] = min(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1]
						+ temp);
			}
		}
		return d[n][m];
	}

	private static int min(int one, int two, int three) // 计算三者之间的最小值
	{
		int min = one;
		if (two < min) {
			min = two;
		}
		if (three < min) {
			min = three;
		}
		return min;
	}
	
	 public static ArrayList<String> tfidfSim(ArrayList<String> Classes1, ArrayList<String> Classes2)
	  {
		  ArrayList<String> tfidfSimiliary=new ArrayList<String>();
		  ArrayList<String> Tokens1=new ArrayList<String>();
			//将本体1中的概念基于'_'进行切割。
			for(int i=0;i<Classes1.size();i++)
			{
				String concept1=Classes1.get(i);
				String []concept_token=tokeningWord(concept1).split(" ");
				//String []concept_token=concept1.split("_");
				for(int k=0;k<concept_token.length;k++)
				{
					Tokens1.add(concept_token[k]);
				}
			}
			
			ArrayList<String> Concept_tokens1=new ArrayList<String>();
			ArrayList<String> TF_IDF1=new ArrayList<String>();
			for(int i=0;i<Classes1.size();i++)
			{
				String concept1=Classes1.get(i);
				String []concept_token=tokeningWord(concept1).split(" ");
				//String []concept_token=concept1.split("_");
				//int tf[]=new int[concept_token.length];////tf其实没必要，如果将一个概念当做一个网页的话。
				double concept_token_idf[]=new double[concept_token.length];
				for(int j=0;j<concept_token.length;j++)
				{						
					for(int k=0;k<Tokens1.size();k++)
					{
						if(Tokens1.get(k).equals(concept_token[j]))//IDF 中的Dw加1
						{
							concept_token_idf[j]++;   //就一个概念统计词频
						}
					}			
				}		
				//计算概念中每个tokens的权重
				double concept_token_weight[]=new double[concept_token.length];
				double Max_weight=0;
				for(int L=0;L<concept_token.length;L++)
				{		
					concept_token_weight[L]=Math.log(Classes1.size()/concept_token_idf[L]);
					if(Max_weight<concept_token_weight[L])
					{
						Max_weight=concept_token_weight[L];
					}
				}
				
				double normalized_concept_token_weight[]=new double[concept_token.length];
				
				//进行存储
				String cpt_tokens="";
				String cpt_weight="";
				for(int L=0;L<concept_token.length;L++)
				{		
					normalized_concept_token_weight[L]=concept_token_weight[L]/Max_weight;
					if(cpt_tokens.equals(""))
						cpt_tokens=concept_token[L];
					else
					{
						cpt_tokens=cpt_tokens+","+concept_token[L];
					}
					if(cpt_weight.equals(""))
						cpt_weight=cpt_weight+normalized_concept_token_weight[L];
					else
					{
						cpt_weight=cpt_weight+","+normalized_concept_token_weight[L];
					}		
				}
				
			//	System.out.print(cpt_tokens+" "+cpt_weight+"\n");
				//System.out.println(cpt_weight);
				Concept_tokens1.add(cpt_tokens);
				TF_IDF1.add(cpt_weight);		
			}
			
			System.out.println("Classes1 has been tokens");
			ArrayList<String> Tokens2 = new ArrayList<String>();
			// 将本体1中的概念基于'_'进行切割。
			for (int i = 0; i < Classes2.size(); i++) {
				String concept2 = Classes2.get(i);
				String []concept_token2=tokeningWord(concept2).split(" ");
				//String[] concept_token2 = concept2.split("_");
				for (int k = 0; k < concept_token2.length; k++) {
					Tokens2.add(concept_token2[k]);
				}
			}
			ArrayList<String> Concept_tokens2 = new ArrayList<String>();
			ArrayList<String> TF_IDF2 = new ArrayList<String>();

			for (int i = 0; i < Classes2.size(); i++) {
				String concept2 = Classes2.get(i);
				String []concept_token2=tokeningWord(concept2).split(" ");
				//String[] concept_token2 = concept2.split("_");
				// int[concept_token.length];////tf其实没必要，如果将一个概念当做一个网页的话。
				double concept_token_idf2[] = new double[concept_token2.length];
				for (int j = 0; j < concept_token2.length; j++) {
					for (int k = 0; k < Tokens2.size(); k++) {
						if (Tokens2.get(k).equals(concept_token2[j]))// IDF 中的Dw加1
						{
							concept_token_idf2[j]++;
						}
					}
				}
				// 计算概念中每个tokens的权重
				double concept_token_weight2[] = new double[concept_token2.length];
				double Max_weight2 = 0;
				for (int L = 0; L < concept_token2.length; L++) {
					concept_token_weight2[L] = Math.log(Classes2.size()/ concept_token_idf2[L]);
					if (Max_weight2 < concept_token_weight2[L]) {
						Max_weight2 = concept_token_weight2[L];
					}
				}

				double normalized_concept_token_weight2[] = new double[concept_token2.length];
				// 进行存储
				String cpt_tokens2 = "";
				String cpt_weight2 = "";
				for (int L = 0; L < concept_token2.length; L++) {
					normalized_concept_token_weight2[L] = concept_token_weight2[L]/ Max_weight2;
					if (cpt_tokens2.equals(""))
						cpt_tokens2 = concept_token2[L];
					else 
					{
						cpt_tokens2 = cpt_tokens2 + "," + concept_token2[L];
					}
					if (cpt_weight2.equals(""))
						cpt_weight2 = cpt_weight2+ normalized_concept_token_weight2[L];
					else 
					{
						cpt_weight2 = cpt_weight2 + ","+ normalized_concept_token_weight2[L];
					}

				}
				//	System.out.print(cpt_tokens2 + " " + cpt_weight2 + "\n");
				// System.out.println(cpt_weight);
				Concept_tokens2.add(cpt_tokens2);
				TF_IDF2.add(cpt_weight2);
			}
			System.out.println("Classes2 has been tokens");
			
			String concept1="";
			String concept2="";
			double TF_IDF_similarity=0;
			int sum=0;
			for(int i=0;i<Classes1.size();i++)
			{
				concept1=Classes1.get(i);
				for(int j=0;j<Classes2.size();j++)
				{
					concept2=Classes2.get(j);
					//因为每个概念与相应的其分解的tokens的标号是一致的
					TF_IDF_similarity=TFIDF_similarity(concept1,concept2,Concept_tokens1.get(i),TF_IDF1.get(i),Concept_tokens2.get(j),TF_IDF2.get(j));
					//bfw_TDIDF_Similarities.append(Classes1.get(i)+","+Classes2.get(j)+","+TF_IDF_similarity+"\n");
					if(TF_IDF_similarity>0)
						sum++;
					tfidfSimiliary.add(concept1+","+concept2+","+TF_IDF_similarity);		
				}
			}
			System.out.println("TFIDF不为0的个数为:"+sum);
		  return tfidfSimiliary;
	  }
	 
	 public static double TFIDF_similarity(String concept1,String concept2,String tokens1,String TF_IDF1,String tokens2,String TF_IDF2)
	  {
		  String[] concept_token1 = tokens1.split(",");
		  String[] concept_token2 = tokens2.split(",");
		  String[] tfidf1 = TF_IDF1.split(",");
		  String[] tfidf2 = TF_IDF2.split(",");
		  boolean[] flag1=new boolean[concept_token1.length];
		  boolean[] flag2=new boolean[concept_token2.length];

		  String commen_token="";
		  for(int i=0;i<concept_token1.length;i++)
		  {
			  String token1=concept_token1[i];
			  for(int j=0;j<concept_token2.length;j++)
			  {
				  String token2=concept_token2[j];
				  //如果子串匹配则，对应的标签又false变为true
				  if(token1.equals(token2))
				  {
					  flag1[i]=true;
					  flag2[j]=true;
					  commen_token=commen_token+token1+"  ";					
				  }
			  }
		  }

		  double sum1=0;
		  double common1=0;		
		  for(int i=0;i<tfidf1.length;i++)
		  {
			  if(flag1[i]==true)
			  {
				  common1=common1+Double.parseDouble(tfidf1[i]);
			  }
			  sum1=sum1+Double.parseDouble(tfidf1[i]);
		  }

		  double sum2=0;
		  double common2=0;		
		  for(int j=0;j<tfidf2.length;j++)
		  {
			  if(flag2[j]==true)
			  {
				  common2=common2+Double.parseDouble(tfidf2[j]);
			  }
			  sum2=sum2+Double.parseDouble(tfidf2[j]);
		  }

		  double similarity=(common1+common2)/(sum1+sum2);
		 /* if(similarity>0)
		  {
			  System.out.println(concept1+"与"+concept2+"共同的子串为"+commen_token);
			  System.out.println(concept1+"与"+concept2+"的相似度为"+similarity);
		  }	*/	
		  return similarity;
	  }	
	 
	  public static String tokeningWord(String str)
	  {
	   		String s1=str;
	   		//s1="Registration_SIGMOD_Member";
	   		String ss = "";
	   		for(int i=0;i<s1.length()-1;i++){
	   			char aa=s1.charAt(i+1);
	   			char a=s1.charAt(i);
	   			if(Character.isUpperCase(a) && i==0)//如果首字母是大写则直接添加
	   			{
	   				ss=ss+String.valueOf(a);
	   			}
	   			else if(Character.isUpperCase(a) &&Character.isLowerCase(aa)&& i!=0)//如果非字母是大写则需要插入分隔符
	   			{
	   				ss=ss+" "+String.valueOf(a);
	   			}	
	   			else if(a=='-'&&aa=='-')//当出现字符"-","_" 而且后面aa是大写，则不做操作
	   			{
	   				//continue;
	   				ss=ss+" ";//等于间接将'_','-'进行了替换
	   			}
	   			else if((a=='-'||a=='_')||a=='.'||a==',')//当出现字符"-","_" 而且后面aa是大写，则不做操作
	   			{
	   				//continue;
	   				ss=ss+" ";//等于间接将'_','-'进行了替换
	   			}
	   			else if(Character.isUpperCase(a)&&Character.isUpperCase(aa))
	   			{
	   				ss=ss+String.valueOf(a);
	   			}	
	   			else if(Character.isLowerCase(a)&&Character.isUpperCase(aa))//前面小写后面接大写
	   			{
	   				ss=ss+String.valueOf(a)+" ";
	   			}	
	   			else  //其实情况正常添加
	   			{             
	   				ss=ss+String.valueOf(a);
	   			}
	   		}
	   		ss=ss+s1.charAt(s1.length()-1);
	   		ss=ss.replace("  ", " ").trim();
	   		return ss.toLowerCase().replaceAll("_|-","");		
	   }
}
