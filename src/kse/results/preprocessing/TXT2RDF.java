package kse.results.preprocessing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import kse.algorithm.auxiliaryClass.OAEIAlignmentOutput;

public class TXT2RDF {
	static BufferedWriter bfw_Result= null;
	public static void main(String[] args) throws Exception
	{
		String mappingsPath = "alignments/crs-cmt_test1.txt";
		String ontologyName1="crs";
		String ontologyName2="cmt";
		ArrayList<String> mappings = new ArrayList<String>();

		BufferedReader Alignment = new BufferedReader(new FileReader(new File(mappingsPath)));
		mappings = new ArrayList<String>();
		String lineTxt = null;
		while ((lineTxt = Alignment.readLine()) != null) 
		{
			String line = lineTxt.trim(); // 去掉字符串首位的空格，避免其空格造成的错误
			// line=line.toLowerCase();//全部变成小写
			mappings.add(line);
		}
		
		try
		{			
			bfw_Result=new BufferedWriter(new FileWriter(mappingsPath));	
		}
		catch(IOException e)
		{
			e.printStackTrace();		
		}
		mappingsPath=mappingsPath.replace(".txt", "");
		OAEIAlignmentOutput out=new OAEIAlignmentOutput(mappingsPath,ontologyName1,ontologyName2);
		for(String s: mappings)
		{
			String parts[]=s.split(",");
			out.addMapping2Output(parts[0],parts[1],parts[3]);
		}
		out.saveOutputFile();
		bfw_Result.close();	
		System.out.println("Transformation is finished!");
	}
	

}
