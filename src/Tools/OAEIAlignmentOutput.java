/*******************************************************************************
 * Copyright 2012 by the Department of Computer Science (University of Oxford)
 * 
 *    This file is part of LogMap.
 * 
 *    LogMap is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMap is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMap.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package Tools;


import java.io.File;
import java.io.FileWriter;
import java.net.URL;


public class OAEIAlignmentOutput {
	
	File alignmentFile;
	FileWriter fw;
	String URI1="";
	String URI2="";
	/*ArrayList<String> classAlignments=new ArrayList<String>();
	ArrayList<String> propertyAlignments=new ArrayList<String>();
	ArrayList<String> instanceAlignments=new ArrayList<String>();*/
	
	
	/**
	 * Same format than OAEIRDFAlignmentFormat, but with different ouput.
	 * SEALS requires the creation of a temporal file and returning its URL
	 * @param name
	 * @throws Exception 
	 */
	public OAEIAlignmentOutput(String pathname,String o1, String o2) throws Exception
	{	
		setOutput(pathname);	
		printHeader(o1, o2);	
	}
	
	
	protected void setOutput(String pathname) throws Exception {
		alignmentFile = File.createTempFile(pathname, ".rdf");
		//alignmentFile=new File(pathname+".rdf");
		fw = new FileWriter(alignmentFile);
	}

	
	private void printHeader(String oiri1, String oiri2) throws Exception{
		fw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
		
		fw.write("<rdf:RDF xmlns=\"http://knowledgeweb.semanticweb.org/heterogeneity/alignment\"\n"); 
		fw.write("\txmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"); 
		fw.write("\txmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\">\n");
		
		fw.write("\n");
		
		fw.write("\t<Alignment>\n");
		fw.write("\t<xml>yes</xml>\n");
		fw.write("\t<level>0</level>\n");
		fw.write("\t<type>??</type>\n");

		fw.write("\t<onto1>" + oiri1 +"</onto1>\n");
		fw.write("\t<onto2>" + oiri2 +"</onto2>\n");
		fw.write("\t<uri1>" + oiri1 +"</uri1>\n");
		fw.write("\t<uri2>" + oiri2 +"</uri2>\n");
		URI1=oiri1;
		URI2=oiri2;		
	}
	
	
	
	
	private void printTail() throws Exception{
		
		fw.write("\t</Alignment>\n");
		fw.write("</rdf:RDF>\n");
	
	}
	
/*	public void addClassMapping2Output(String iri_str1, String iri_str2, int dir_mapping, double conf) throws Exception {
		addMapping2Output(iri_str1, iri_str2, dir_mapping, conf);
	}
	
	public void addDataPropMapping2Output(String iri_str1, String iri_str2, int dir_mapping, double conf) throws Exception {
		addMapping2Output(iri_str1, iri_str2, dir_mapping, conf);
	}
	
	public void addObjPropMapping2Output(String iri_str1, String iri_str2, int dir_mapping, double conf) throws Exception{
		addMapping2Output(iri_str1, iri_str2, dir_mapping, conf);
	}
	
	public void addInstanceMapping2Output(String iri_str1, String iri_str2, double conf)  throws Exception {
		addMapping2Output(iri_str1, iri_str2, LogMap_Lite.EQ, conf);
	}*/
	
	
	
	public void addMapping2Output(String iri_str1, String iri_str2,String in_str3) throws Exception
	{
		
		fw.write("\t<map>\n");
		fw.write("\t\t<Cell>\n");
		
		/*fw.write("\t\t\t<entity1 rdf:resource=\"" + iri_str1 +"\"/>\n");
		  fw.write("\t\t\t<entity2 rdf:resource=\"" + iri_str2 +"\"/>\n");	*/
		fw.write("\t\t\t<entity1 rdf:resource=\""+URI1 +"#"+iri_str1+"\""+"/>\n");
		fw.write("\t\t\t<entity2 rdf:resource=\""+URI2 +"#"+iri_str2+"\""+"/>\n");
			
		//fw.write("\t\t<measure rdf:datatype=\"xsd:float\">" + getRoundConfidence(conf) + "</measure>\n");
		fw.write("\t\t\t<measure rdf:datatype=\"xsd:float\">" + Double.parseDouble(in_str3) + "</measure>\n");
		
		fw.write("\t\t\t<relation>=</relation>\n");
			
		fw.write("\t\t</Cell>\n");
		fw.write("\t</map>\n");
	}

	
	public void saveOutputFile() throws Exception{		
		printTail();
		fw.flush();
		fw.close();
		
	}
	
	public URL returnAlignmentFile() throws Exception{
		return alignmentFile.toURI().toURL();
	}
}
