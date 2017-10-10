package kse.results.preprocessing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import kse.algorithm.auxiliaryClass.OAEIAlignmentOutput;
import kse.owl.OWLInfo;

public class editDistanceForAnatomy {
	static BufferedWriter bfw_Result= null;
	static double []thresholds={0.6,0.65,0.7,0.75,0.8,0.85,0.9,0.95,1.0};
	//static double []thresholds={0.8,1.0};
	
	public static void main(String args[]) throws Exception
	{			
		String ontologyName1 = "mouse";
		String ontologyName2 = "human";
		String owlPath1 = "OAEIOntology/" + ontologyName1 + ".owl";
		String owlPath2 = "OAEIOntology/" + ontologyName2 + ".owl";

		OWLInfo owlInfo1 = new OWLInfo(owlPath1);
		OWLInfo owlInfo2 = new OWLInfo(owlPath2);

		Set<String> concepts1 = owlInfo1.getConceptTokens();
		Set<String> objectproperties1 = owlInfo1.getObjPropertyTokens();
		Set<String> dataproperties1 = owlInfo1.getDataPropertyTokens();
		HashMap<String, String> conceptLabel1 = owlInfo1.getConceptLabels();
		Set<String> concepts2 = owlInfo2.getConceptTokens();
		Set<String> objectproperties2 = owlInfo2.getObjPropertyTokens();
		Set<String> dataproperties2 = owlInfo2.getDataPropertyTokens();
		HashMap<String, String> conceptLabel2 = owlInfo2.getConceptLabels();
		
		HashMap<String, String> labelToConcept1 = new HashMap<String, String>();
		HashMap<String, String> labelToConcept2 = new HashMap<String, String>();
		
		for(String concept:conceptLabel1.keySet())
		{
			String label=conceptLabel1.get(concept);
			labelToConcept1.put(label.toLowerCase(), concept);
		}
		
		for(String concept:conceptLabel2.keySet())
		{
			String label=conceptLabel2.get(concept);
			labelToConcept2.put(label.toLowerCase(), concept);
		}

		ArrayList<String> editSimClass = editSimilairtyLabel(concepts1, conceptLabel1, concepts2, conceptLabel2);
		ArrayList<String> editSimObjectProperty = editSimilairty(objectproperties1, objectproperties2);
		ArrayList<String> editSimDataProperty = editSimilairty(dataproperties1, dataproperties2);

		// String resultPath2="Results/middle_Result.txt";
		for (int i = 0; i < thresholds.length; i++) {
			String resultPath = "alignments/Results/" + ontologyName1 + "-" + ontologyName2 + "-edit_batch_"
					+ thresholds[i];
			try {
				bfw_Result = new BufferedWriter(new FileWriter(resultPath + ".txt"));
			} catch (IOException e) {
				e.printStackTrace();
			}
			ArrayList<String> results = new ArrayList<String>();
			for (String s : editSimClass) {
				String parts[] = s.toLowerCase().split(",");
				if (Double.parseDouble(parts[2]) >= thresholds[i]) 
				{
					s=labelToConcept1.get(parts[0])+","+labelToConcept2.get(parts[1])+","+parts[2];
					results.add(s);
					bfw_Result.append(s + "\n");
				}
			}
			for (String s : editSimObjectProperty) {
				String parts[] = s.split(",");
				if (Double.parseDouble(parts[2]) >= thresholds[i]) {					
					results.add(s);
					bfw_Result.append(s + "\n");
				}
			}
			for (String s : editSimDataProperty) {
				String parts[] = s.split(",");
				if (Double.parseDouble(parts[2]) >= thresholds[i]) {
					results.add(s);
					bfw_Result.append(s + "\n");
				}
			}

			//OAEIAlignmentOutput out = new OAEIAlignmentOutput(resultPath, ontologyName1, ontologyName2);
			OAEIAlignmentOutput out=new OAEIAlignmentOutput(resultPath,owlInfo1.getURI(),owlInfo2.getURI());
			for (String s : results) {
				String parts[] = s.split(",");
				out.addMapping2Output(parts[0], parts[1], parts[2]);
			}
			out.saveOutputFile();
			bfw_Result.close();		
		}
		System.out.println("The calculation of similarity based on edit distance is finished!");
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
	
	public static ArrayList<String> editSimilairtyLabel(Set<String> object1s,HashMap<String, String> conceptLabel1, Set<String> object2s,HashMap<String, String> conceptLabel2) 
	{
		ArrayList<String> editDistance=new ArrayList<String>();
		int number=0;
		for(String xx:object1s)
		{
			xx=conceptLabel1.get(xx);
			String str1=xx.toLowerCase().replace("_", "").replace("-", "").replace(" ", "").replace("'", " ").replace(":", " ").replace("(", " ").replace(")", " ").replace(",", " ").replace(", ", " ").replace(";", " ").replace("  ", " ").trim();
			for(String yy:object2s)
			{
				yy=conceptLabel2.get(yy);
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
}
