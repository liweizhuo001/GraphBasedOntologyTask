package kse.algorithm.forMapping.revision;

import static kse.misc.GlobalParams.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.neo4j.cypher.internal.compiler.v1_9.executionplan.PartiallySolvedQuery;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.tooling.GlobalGraphOperations;
import org.parboiled.parserunners.ReportingParseRunner;

import kse.algorithm.auxiliaryClass.UnsatInformation;
import kse.misc.Tools;
import kse.neo4j.ver1_8.Tools4Graph;
import kse.neo4j.ver2_1.ExecCypher;
import scala.collection.parallel.ParIterableLike.Forall;


public class ScoringMappingRevision {
	String gPath; 
	GraphDatabaseService graphDB; 	
    //ArrayList<String> mappings; 
    List<MIMPP> mimpps;     //TBox中所有的MIMPPS(即最小冲突子集)
    Map<Relationship, List<MIMPP>> relMappingMIMPP; //每个匹配对对应的最小冲突子集
    HashMap<String, Double> mappings;  //每个匹配，对应一个相应的confidence
    //HashMap<String, Double> revisedMappings;
    Set<String> mappingNodes;  //把mappings中涉及到的Node存储下来(无法修改，因为当初你不知道它是概念还是属性)
    MappingDiagnosis diag ;
    //Index<Relationship> relationshipIndex;        //关系索引，方便查找节点
    List<Relationship> mappingRelationship;
    List<Relationship> removedRelationship;
    ArrayList<String> removedMappings;
    ArrayList<String> candidatMappings;
    
    HashMap<Relationship,HashMap<Node,Double>> candidateRelationships;
    ArrayList<String> addedRelationship;
    //ArrayList<String> candidatedMappings;
	
	public ScoringMappingRevision(String gDB, ArrayList<String> incoherenceMappings){
		this.gPath = gDB;
		this.graphDB =  new GraphDatabaseFactory().newEmbeddedDatabase(gDB);	
		this.diag = MappingDiagnosis.getDiagnosis(graphDB);	
		mappings=new HashMap<String, Double>();
		//revisedMappings=new HashMap<String, Double>();
		mappingNodes =new HashSet<>();
		mappingRelationship=new ArrayList<Relationship>();
		
		for (String map:incoherenceMappings)
		{
			String parts[]=map.split(",");

			if(parts[2].contains("|"))          //包含关系
			{
				if(parts[0].equals(parts[1]))
					mappings.put(parts[0]+"_1"+","+parts[1]+"_2", Double.parseDouble(parts[3]));
				else	
					mappings.put(parts[0]+","+parts[1], Double.parseDouble(parts[3]));
				//creatRelationshipForMappings(parts[0],parts[1]);
				
			}
			else   //等价关系
			{
				if(parts[0].equals(parts[1]))	
				{
					mappings.put(parts[0]+"_1"+","+parts[1]+"_2", Double.parseDouble(parts[3]));
					mappings.put(parts[1]+"_2"+","+parts[0]+"_1", Double.parseDouble(parts[3]));
				}
				else
				{
					mappings.put(parts[0]+","+parts[1], Double.parseDouble(parts[3]));
					mappings.put(parts[1]+","+parts[0], Double.parseDouble(parts[3]));
				}
				//creatRelationshipForMappings(parts[0],parts[1]);
				//creatRelationshipForMappings(parts[1],parts[0]);		
			}
			mappingNodes.add(parts[0]);
			mappingNodes.add(parts[1]);
		}	
		

		try(Transaction tx = graphDB.beginTx()){
			this.init();
			tx.success();
		}
	}
	
	private void init(){
		this.creatRelationshipForMappings();
		this.calRMappingM();
	}
	
	public void calRMappingM()
	{
		relMappingMIMPP = new HashMap<Relationship, List<MIMPP>>(); //关系到MIPP的映射
		//List<UnsatTriple> triples = diag.getUnsatTripleByRelationship(mappingNodes);	
		//mimpps = MappingDiagnosis.getDiagnosis(graphDB).compMIMPPs(triples,mappingRelationship);  //取出Graph中所有的MIMPP
		List<UnsatInformation> tetrads =diag.getUnsatInformationByRelationship(mappingNodes);
		mimpps = MappingDiagnosis.getDiagnosis(graphDB).compMIMPPsOfMappings(tetrads,mappingRelationship);  //取出Graph中所有的MIMPP
		//完成Relationship到MIPS的映射
		Index<Relationship> relIndex = graphDB.index().forRelationships(RELATIONSHIPINDEX);		
		relIndex = graphDB.index().forRelationships(RELATIONSHIPINDEX);
		System.out.println("Getting the relationship in MIPP...");
		for(MIMPP mimpp : mimpps)
		{			
			Set<Relationship> relOfDiag = mimpp.getincoherenceMappings(); //取出诊断
		/*	for(Relationship r: relOfDiag)
			{
				List<MIMPP> current = relMappingMIMPP.get(r);  
				if(current == null)
				{
					current = new ArrayList<MIMPP>();
					relMappingMIMPP.put(r,current);
				}
				if(!(current.contains(mimpp)))
				{
					current.add(mimpp);
				}				
			}*/
			//考虑角色的间接影响，但是方向还是有区别的
			
			for(Relationship r: relOfDiag)
			{
				if(r.getProperty(TYPEPROPERTY).equals("Role"))			//只单独对纯角色的等价进行了限制
				{
					Relationship stemRelationship=null;
					String comefrom=r.getStartNode().getProperty(COMEFROMPROPERTY).toString();
					String subKey=r.getStartNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
					String supKey=r.getEndNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
					if(subKey.equals(supKey)&&comefrom.equals(COMEFROMFIRST))
					{
						String relationshipName = Tools4Graph.getRelationshipName(subKey+"_1", supKey+"_2");		//***
						stemRelationship=relIndex.get(NAMEPROPERTY, relationshipName).getSingle();
					}
					else if((subKey.equals(supKey)&&comefrom.equals(COMEFROMSECOND)))
					{
						String relationshipName = Tools4Graph.getRelationshipName(subKey+"_2", supKey+"_1");		//***
						stemRelationship=relIndex.get(NAMEPROPERTY, relationshipName).getSingle();
					}
					else 
					{
						String relationshipName = Tools4Graph.getRelationshipName(subKey, supKey);		//***
						stemRelationship=relIndex.get(NAMEPROPERTY, relationshipName).getSingle();
					}
					List<MIMPP> current = relMappingMIMPP.get(stemRelationship);  
					if(current == null)
					{
						current = new ArrayList<MIMPP>();
						relMappingMIMPP.put(stemRelationship,current);
					}
					if(!(current.contains(mimpp)))
					{
						current.add(mimpp);
					}
					
				}
				else 
				{
					List<MIMPP> current = relMappingMIMPP.get(r);  
					if(current == null)
					{
						current = new ArrayList<MIMPP>();
						relMappingMIMPP.put(r,current);
					}
					if(!(current.contains(mimpp)))
					{
						current.add(mimpp);
					}		
				}
						
			}
		}
		//printRelMappingMIMPP();
		//printMIMPPs();
	}
	
	public void printRelMappingMIMPP()
	{
		System.out.println("Print the minimal incoherent mappings:");
		for(Relationship r:relMappingMIMPP.keySet())
		{
			//System.out.println("The incoherent mapping is "+ r.getStartNode().getProperty(NAMEPROPERTY)+" "+ r.getEndNode().getProperty(NAMEPROPERTY));
			List<MIMPP> current =relMappingMIMPP.get(r);
			for(MIMPP a:current)
			{
				System.out.println("The unsatisfiable node is "+a.getNode());
				System.out.println("The minimal conflict set is ");
				a.printMIMPP();
			}
		}		
	}
	
	public void printMIMPPs()
	{
		System.out.println("Print the minimal incoherent mapping pairwise-pairs:");
		for(MIMPP c:mimpps)
		{
			//System.out.println("The incoherent mapping is "+ r.getStartNode().getProperty(NAMEPROPERTY)+" "+ r.getEndNode().getProperty(NAMEPROPERTY));
			String node =c.getNode();
			System.out.println("The unsatisfiable node is "+node +" and the source ontology is " +c.getSource());	
			System.out.println("The minimal conflict set is ");
			c.printMIMPP();
			/*for(Relationship r:c.getincoherenceMappings())
			{
				System.out.println(r.getStartNode().getProperty(NAMEPROPERTY)+"->" + r.getEndNode().getProperty(NAMEPROPERTY));
			}*/
		}		
	}
	
	
	public void creatRelationshipForMappings()
	{
			GlobalGraphOperations ggo= GlobalGraphOperations.at(graphDB);			
			int i=0;
			for(Relationship rel :  ggo.getAllRelationships())
			{	
				String mapping=rel.getStartNode().getProperty(NAMEPROPERTY)+","+rel.getEndNode().getProperty(NAMEPROPERTY);
				String _mapping1=rel.getStartNode().getProperty(NAMEPROPERTY)+"_1"+","+rel.getEndNode().getProperty(NAMEPROPERTY)+"_2";
				String _mapping2=rel.getStartNode().getProperty(NAMEPROPERTY)+"_2"+","+rel.getEndNode().getProperty(NAMEPROPERTY)+"_1";
				//System.out.println(mapping);
				if(mappings.keySet().contains(mapping)||mappings.keySet().contains(_mapping1)||mappings.keySet().contains(_mapping2))
				{
					mappingRelationship.add(rel);
					i++;
				}
				//角色的情况额外增加的判断(名字相等的时候)	
				else if(rel.getStartNode().getProperty(NAMEPROPERTY).toString().equals(rel.getEndNode().getProperty(NAMEPROPERTY).toString()))
				{
					//String a=mapping.replaceAll("existence_", "");	
					_mapping1=_mapping1.replaceAll("existence_", "").replaceAll("inverse_", "");	
					_mapping2=_mapping2.replaceAll("existence_", "");	

					if(mappings.keySet().contains(_mapping1)||mappings.keySet().contains(_mapping2))
					{
						mappingRelationship.add(rel);
						i++;
					}		
				}
				else //名字不相等的时候
				{
					mapping=mapping.replaceAll("existence_", "").replaceAll("inverse_", "");	
					/*_mapping1=mapping.replaceAll("inverse_", "");	
					_mapping2=mapping.replaceAll("existence_inverse_", "")*/;	
					if(mappings.keySet().contains(mapping))
					{
						mappingRelationship.add(rel);
						i++;
					}
				}
				//对于的角色需要考虑
			}
			System.out.println(String.format("Number of mappings' relationship is %d", i));
	}
	
	public void goRevising()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				if(maximumImfactorRelationship.size()==1)  //集合是唯一的
					_rel=maximumImfactorRelationship.get(0);
				else  
				{
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					List<Relationship> minmumClosuredRelationship=getMinClosure(maximumImfactorRelationship);
					if(minmumClosuredRelationship.size()==1)   //集合是唯一的
						_rel=minmumClosuredRelationship.get(0);
					else
					{
						List<Relationship> minmumWeightedRelationship=getMinWeight(minmumClosuredRelationship);
						//if(minmumWeightedRelationship.size()==1)				
						_rel=minmumWeightedRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingP1P3P2()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				if(maximumImfactorRelationship.size()==1)  //集合是唯一的
					_rel=maximumImfactorRelationship.get(0);
				else  
				{
					List<Relationship> minmumWeightedRelationship=getMinWeight(maximumImfactorRelationship);
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					if(minmumWeightedRelationship.size()==1)   //集合是唯一的
						_rel=minmumWeightedRelationship.get(0);
					else
					{
						List<Relationship> minmumClosuredRelationship=getMinClosure(minmumWeightedRelationship);
						//if(minmumWeightedRelationship.size()==1)				
						_rel=minmumClosuredRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingP2P1P3()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();			
				List<Relationship> minmumClosuredRelationship=getMinClosure(weightedRelationship);
				if(minmumClosuredRelationship.size()==1)  //集合是唯一的
					_rel=minmumClosuredRelationship.get(0);
				else  
				{
					List<Relationship> maximumImfactorRelationship=getMaxRel(minmumClosuredRelationship,relMappingMIMPP);
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					if(maximumImfactorRelationship.size()==1)   //集合是唯一的
						_rel=maximumImfactorRelationship.get(0);
					else
					{
						List<Relationship> minmumWeightedRelationship=getMinWeight(maximumImfactorRelationship);
						//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
						_rel=minmumWeightedRelationship.get(0);
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingP2P3P1()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();			
				List<Relationship> minmumClosuredRelationship=getMinClosure(weightedRelationship);
				if(minmumClosuredRelationship.size()==1)  //集合是唯一的
					_rel=minmumClosuredRelationship.get(0);
				else  
				{
					List<Relationship> minmumWeightedRelationship=getMinWeight(minmumClosuredRelationship);
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings

					if(minmumWeightedRelationship.size()==1)   //集合是唯一的
						_rel=minmumWeightedRelationship.get(0);
					else
					{
						List<Relationship> maximumImfactorRelationship=getMaxRel(minmumWeightedRelationship,relMappingMIMPP);		
						_rel=maximumImfactorRelationship.get(0);
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingP3P1P2()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();			
				List<Relationship> minmumWeightedRelationship=getMinWeight(weightedRelationship);
				if(minmumWeightedRelationship.size()==1)  //集合是唯一的
					_rel=minmumWeightedRelationship.get(0);
				else  
				{
					List<Relationship> maximumImfactorRelationship=getMaxRel(minmumWeightedRelationship,relMappingMIMPP);		
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings

					if(maximumImfactorRelationship.size()==1)   //集合是唯一的
						_rel=maximumImfactorRelationship.get(0);
					else
					{
						List<Relationship> minmumClosuredRelationship=getMinClosure(maximumImfactorRelationship);
						_rel=minmumClosuredRelationship.get(0);
						
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingP3P2P1()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();			
				List<Relationship> minmumWeightedRelationship=getMinWeight(weightedRelationship);
				if(minmumWeightedRelationship.size()==1)  //集合是唯一的
					_rel=minmumWeightedRelationship.get(0);
				else  
				{
					List<Relationship> minmumClosuredRelationship=getMinClosure(minmumWeightedRelationship);			
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					if(minmumClosuredRelationship.size()==1)   //集合是唯一的
						_rel=minmumClosuredRelationship.get(0);
					else
					{
						List<Relationship> maximumImfactorRelationship=getMaxRel(minmumClosuredRelationship,relMappingMIMPP);	
						_rel=maximumImfactorRelationship.get(0);
						
					}
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingBagging()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();			
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				List<Relationship> minmumWeightedRelationship=getMinWeight(weightedRelationship);
				List<Relationship> minmumClosuredRelationship=getMinClosure(weightedRelationship);
				
				List<Relationship> possibleRemovedRelationship=statistic(maximumImfactorRelationship,minmumWeightedRelationship,minmumClosuredRelationship);
				
				if(possibleRemovedRelationship.size()==1)  //集合是唯一的
					_rel=possibleRemovedRelationship.get(0);
				else  
				{
					_rel=possibleRemovedRelationship.get(0);
					/*Random ran1 = new Random(possibleRemovedRelationship.size());
					int index=ran1.nextInt(possibleRemovedRelationship.size());
					_rel=possibleRemovedRelationship.get(index);*/
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}

	public void goRevisingbyImpactor()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				_rel=maximumImfactorRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。			
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingbyImpactorPlusClosure()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				if(maximumImfactorRelationship.size()==1)  //集合是唯一的
					_rel=maximumImfactorRelationship.get(0);
				else  
				{
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					List<Relationship> minmumClosuredRelationship=getMinClosure(maximumImfactorRelationship);
					_rel=minmumClosuredRelationship.get(0);
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingbyClosure()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
					Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();
					
					List<Relationship> minmumClosuredRelationship=getMinClosure(weightedRelationship);
					_rel=minmumClosuredRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。	
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingbyWeight()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{				
				Set<Relationship>  weightedRelationship=relMappingMIMPP.keySet();					
				List<Relationship> minmumWeightedRelationship=getMinWeight(weightedRelationship);	
				_rel=minmumWeightedRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	
	
	public void goRevisingbyWeightPlusImpactor()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
				List<Relationship> maximumImfactorRelationship=getMaxRel(relMappingMIMPP);
				if(maximumImfactorRelationship.size()==1)  //集合是唯一的
					_rel=maximumImfactorRelationship.get(0);
				else
				{
						List<Relationship> minmumWeightedRelationship=getMinWeight(maximumImfactorRelationship);
						//if(minmumWeightedRelationship.size()==1)				
						_rel=minmumWeightedRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
				}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	public void goRevisingbyWeightPlusClosure()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			removedRelationship=new ArrayList<Relationship>();
			candidateRelationships= new HashMap<Relationship,HashMap<Node,Double>>();
			while(!mimpps.isEmpty())		
			{	
				//HashMap<Relationship,ArrayList<String>> candidateRelationships= new HashMap<Relationship,ArrayList<String>>();
					Set<Relationship> weightedRelationship=relMappingMIMPP.keySet();
					List<Relationship> minmumClosuredRelationship=getMinClosure(weightedRelationship);
					if(minmumClosuredRelationship.size()==1)   //集合是唯一的
						_rel=minmumClosuredRelationship.get(0);
					else
					{
						List<Relationship> minmumWeightedRelationship=getMinWeight(minmumClosuredRelationship);
						//if(minmumWeightedRelationship.size()==1)				
						_rel=minmumWeightedRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
					}
				HashMap<Node,Double> candidated_r= new HashMap<Node,Double>();
				candidated_r=findcandidateRelationships(_rel);
				//updateState(_rel, candidated_r,mimpps, relMappingMIMPP); 
				//更新操作
				List<MIMPP> removedSet=relMappingMIMPP.get(_rel);			
				mimpps.removeAll(removedSet);  //同方向的最小冲突子集都会被移除(已经包含的角色的可能)			
				relMappingMIMPP.remove(_rel);  //基于角色同化的可能也间接的包含在里面了		
				java.util.Iterator<Entry<Relationship, List<MIMPP>>> iter= relMappingMIMPP.entrySet().iterator();
				while(iter.hasNext())
				{
					Relationship r=iter.next().getKey();					
					for(int i=0;i<relMappingMIMPP.get(r).size();i++)
					{
						MIMPP mimpp=relMappingMIMPP.get(r).get(i);
						if(mimpp.incoherenceMappings.contains(_rel))
						{
							relMappingMIMPP.get(r).remove(mimpp);
							i--;
						}
					}
					if(relMappingMIMPP.get(r).isEmpty())
						iter.remove();
				}		
				removedRelationship.add(_rel); //其实在角色的更新时就不需要再做一次关于角色的过滤操作了
				if(!candidated_r.isEmpty())
				candidateRelationships.put(_rel, candidated_r);					
				//updateGraphDynamic(_rel,candidateRelationships, graphDB);
			}	
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入
			updateGraph(removedRelationship,candidateRelationships, graphDB);
			printInfoOfRevision();		
		}
	}
	
	
	
	
/*	public void goRevising()
	{		
		try(Transaction tx = graphDB.beginTx())
		{
			Relationship _rel = null;
			Map<Relationship, Set<MIMPP>> cloned_relMappingMIMPP = relMappingMIMPP;	//每个关系包含对应的最小冲突子集	
			ArrayList<Relationship> candidateRelationships= new ArrayList<Relationship>();
			cloned_relMappingMIMPP=cloneForMappingRelation();
			List<MIMPP> _mimpps = new ArrayList<>(); //MIPP集合的一个副本
			for(MIMPP m : mimpps){  //初始化MIPP集合副本
				_mimpps.add(m);
			}
			while(!_mimpps.isEmpty())		
			{					
				List<Relationship> maximumImfactorRelationship=getMaxRel(cloned_relMappingMIMPP);
				if(maximumImfactorRelationship.size()==1)  //集合是唯一的
					_rel=maximumImfactorRelationship.get(0);
				else  
				{
					//通过单个的relationship定位到各自的冲突子集即MIMPP，然后来计算相应闭包的个数，考虑移除闭包个数较少的mappings
					List<Relationship> minmumClosuredRelationship=getMinClosure(maximumImfactorRelationship);
					if(minmumClosuredRelationship.size()==1)   //集合是唯一的
						_rel=minmumClosuredRelationship.get(0);
					else
					{
						List<Relationship> minmumWeightedRelationship=getMinWeight(minmumClosuredRelationship);
						_rel=minmumWeightedRelationship.get(0);  //这里就考虑第一个，当然也可以选取人工选取的方式。				
					}
				}
				HashMap<Relationship,ArrayList<String>> candidated_r= new HashMap<Relationship,ArrayList<String>>();
				candidated_r=findcandidateRelationships(_rel);
				updateState(_rel, candidated_r,_mimpps, cloned_relMappingMIMPP); 
			}
			//更新操作,加candidated_r加入到原始的mapping中，如果有矛盾则不加入	
		}
	}*/
	
	public Map<Relationship, Set<MIMPP>> cloneForMappingRelation()
	{
		Map<Relationship, Set<MIMPP>> cloned_relMappingMIMPP = new HashMap<>();	//每个关系包含对应的最小冲突子集
		List<MIMPP> cloned_mimpps = new ArrayList<>(); //MIPP集合的一个副本
		for(MIMPP m : mimpps){  //初始化MIPP集合副本
			cloned_mimpps.add(m);
		}
		for(Relationship r : relMappingMIMPP.keySet()){ //初始化关系-MIPP集合映射副本
			Set<MIMPP> _ms = new HashSet<>();
			for(MIMPP m: relMappingMIMPP.get(r)){
				_ms.add(m);				
			}	
			cloned_relMappingMIMPP.put(r, _ms);
		}
		return cloned_relMappingMIMPP;
	}
	
	public List<Relationship> getMaxRel(Map<Relationship, List<MIMPP>> _relMappingMIPP) //计算每个mapping关联的MIMPP
	{
		int max = 0;
		//Relationship rel = null;
		List<Relationship> maximumRelationship= new ArrayList<Relationship>();	
		//HashMap<K, V>
		//第一个循序找到关联最大的值是多少
		for(Relationship r : _relMappingMIPP.keySet())
		{				
			if(_relMappingMIPP.get(r).size()>max)
			{
				max = _relMappingMIPP.get(r).size();
				//rel = r;
			}			
		}
		//第二轮循环才能找到对应的Relationship的集合
		for(Relationship r : _relMappingMIPP.keySet())
		{				
			if(_relMappingMIPP.get(r).size()==max)
			{
				maximumRelationship.add(r);
			}			
		}
		
		//System.out.println("Selected relation:"+GlobalFunct.relToStr(rel)+":"+max);
		return maximumRelationship;
	}
	
	public List<Relationship> getMaxRel(List<Relationship> relationships,Map<Relationship, List<MIMPP>> _relMappingMIPP) //计算每个mapping关联的MIMPP
	{
		int max = 0;
		List<Relationship> maximumRelationship= new ArrayList<Relationship>();	
		//HashMap<K, V>
		//第一个循序找到关联最大的值是多少
		for(Relationship r : relationships)
		{				
			if(_relMappingMIPP.get(r).size()>max)
			{
				max = _relMappingMIPP.get(r).size();
				//rel = r;
			}			
		}
		//第二轮循环才能找到对应的Relationship的集合
		for(Relationship r : _relMappingMIPP.keySet())
		{				
			if(_relMappingMIPP.get(r).size()==max)
			{
				maximumRelationship.add(r);
			}			
		}
		
		//System.out.println("Selected relation:"+GlobalFunct.relToStr(rel)+":"+max);
		return maximumRelationship;
	}
	
	public List<Relationship> getMinClosure(List<Relationship> maximumImfactorRelationship) //计算每个mapping关联的MIMPP
	{
		int min = 100;
		//Relationship rel = null;
		List<Relationship> minimumRelationship= new ArrayList<Relationship>();	
		HashMap<Integer,List<Relationship>> map=new HashMap<Integer,List<Relationship>>();	
		//第一个循序找到关联最大的值是多少
		for(Relationship r : maximumImfactorRelationship)
		{	
			int num=0;
			List<Relationship> _rRelationship= new ArrayList<>();
			List<MIMPP> sets=relMappingMIMPP.get(r);
			for(MIMPP m:sets)
			{
				for(List<Relationship> mappingPairs:m.mappingClosure.keySet())  //间接说明这些mapping对是存在闭包的
				{
					if(mappingPairs.contains(r))
					{
						num=num+1;
						//num=num+m.mappingClosure.get(mappingPairs).size();
					}
				}
		
				//m.mappingClosure.get(m.getincoherenceMappings());  //即每个冲突子集闭包的个数
			}
			if(min>num)
				min=num;
			if(map.get(num)==null)  //闭包个数为num不在存在mapping中，重新建立
			{
				_rRelationship.add(r);
				map.put(num, _rRelationship);
			}
			else  //闭包个数为num不在存在mapping中
			{
				map.get(num).add(r);
				
			}
		}
		minimumRelationship=map.get(min);
		return minimumRelationship;	
	}
	
	public List<Relationship> getMinWeight(List<Relationship> minmumClosuredRelationship) //计算每个mapping关联的MIMPP
	{
		double minWeight = 1;
		//Relationship rel = null;
		List<Relationship> minimumRelationship= new ArrayList<>();	
		HashMap<Double,List<Relationship>> map=new HashMap<Double,List<Relationship>>();	
		//第一个循序找到关联最大的值是多少
		for(Relationship r : minmumClosuredRelationship)
		{	
			String mapping=r.getStartNode().getProperty(NAMEPROPERTY)+","+r.getEndNode().getProperty(NAMEPROPERTY);
			double weight=0.0;
			//名字相等，把属性附带的情况给排除掉
			if(r.getStartNode().getProperty(NAMEPROPERTY).equals(r.getEndNode().getProperty(NAMEPROPERTY)))
			{
				//在这里只要判断一个方向即可，因为mappings还没移除
				String _mapping=r.getStartNode().getProperty(NAMEPROPERTY)+"_1"+","+r.getEndNode().getProperty(NAMEPROPERTY)+"_2";
				_mapping=_mapping.replaceAll("existence_", "").replaceAll("inverse_", "");		
				weight=mappings.get(_mapping);
			}
			//名字不相等，把属性附带的情况给排除掉
			else 
			{
				mapping=mapping.replaceAll("existence_", "").replaceAll("inverse_", "");
				weight=mappings.get(mapping);
			}
				
			//double weight=mappings.get(mapping);
			List<Relationship> _rRelationship= new ArrayList<>();
			
			if(minWeight>weight)
				minWeight=weight;
			if(map.get(weight)==null)  //选取权重最小的值进行移除
			{
				_rRelationship.add(r);
				map.put(weight, _rRelationship);
			}
			else  //闭包个数为num不在存在mapping中
			{
				map.get(weight).add(r);			
			}
		}
		minimumRelationship=map.get(minWeight);
		return minimumRelationship;	
	}
	
	public List<Relationship> getMinClosure(Set<Relationship> weightedRelationship) //计算每个mapping关联的MIMPP
	{
		int min = 100;
		//Relationship rel = null;
		List<Relationship> minimumRelationship= new ArrayList<Relationship>();	
		HashMap<Integer,List<Relationship>> map=new HashMap<Integer,List<Relationship>>();	
		//第一个循序找到关联最大的值是多少
		for(Relationship r : weightedRelationship)
		{	
			int num=0;
			List<Relationship> _rRelationship= new ArrayList<>();
			List<MIMPP> sets=relMappingMIMPP.get(r);
			for(MIMPP m:sets)
			{
				for(List<Relationship> mappingPairs:m.mappingClosure.keySet())  //间接说明这些mapping对是存在闭包的
				{
					if(mappingPairs.contains(r))
					{
						num=num+1;
						//num=num+m.mappingClosure.get(mappingPairs).size();
					}
				}
		
				//m.mappingClosure.get(m.getincoherenceMappings());  //即每个冲突子集闭包的个数
			}
			if(min>num)
				min=num;
			if(map.get(num)==null)  //闭包个数为num不在存在mapping中，重新建立
			{
				_rRelationship.add(r);
				map.put(num, _rRelationship);
			}
			else  //闭包个数为num不在存在mapping中
			{
				map.get(num).add(r);
				
			}
		}
		minimumRelationship=map.get(min);
		return minimumRelationship;	
	}
	
	public List<Relationship> statistic(List<Relationship> impact,List<Relationship> closure, List<Relationship> weight) //计算每个mapping关联的MIMPP
	{
		int max=0;
		List<Relationship> possibleRemovedRelationship= new ArrayList<Relationship>();	
		HashMap<Relationship,Integer> map=new HashMap<Relationship,Integer>();	
		//第一个循序找到关联最大的值是多少
		for(Relationship r : impact)
		{	
			if(map.keySet().contains(r))
			{
				int num=map.get(r);
				map.put(r, num+1);
			}	
			else
			{
				map.put(r, 1);
			}
			if(max<map.get(r))
				max=map.get(r);
		}
		for(Relationship r : closure)
		{	
			if(map.keySet().contains(r))
			{
				int num=map.get(r);
				map.put(r, num+1);
			}	
			else
			{
				map.put(r, 1);
			}
			if(max<map.get(r))
				max=map.get(r);
		}
		for(Relationship r : weight)
		{	
			if(map.keySet().contains(r))
			{
				int num=map.get(r);
				map.put(r, num+1);
			}	
			else
			{
				map.put(r, 1);
			}
			if(max<map.get(r))
				max=map.get(r);
		}
		for(Relationship r:map.keySet())
		{
			if(map.get(r)==max)
				possibleRemovedRelationship.add(r);
		}
		return possibleRemovedRelationship;	
	}
	
	public List<Relationship> getMinWeight(Set<Relationship> weightedRelationship) //计算每个mapping关联的MIMPP
	{
		double minWeight = 1;
		//Relationship rel = null;
		List<Relationship> minimumRelationship= new ArrayList<>();	
		HashMap<Double,List<Relationship>> map=new HashMap<Double,List<Relationship>>();	
		//第一个循序找到关联最大的值是多少
		for(Relationship r : weightedRelationship)
		{	
			String mapping=r.getStartNode().getProperty(NAMEPROPERTY)+","+r.getEndNode().getProperty(NAMEPROPERTY);
			double weight=0.0;
			//名字相等，把属性附带的情况给排除掉
			if(r.getStartNode().getProperty(NAMEPROPERTY).equals(r.getEndNode().getProperty(NAMEPROPERTY)))
			{
				//在这里只要判断一个方向即可，因为mappings还没移除
				String _mapping=r.getStartNode().getProperty(NAMEPROPERTY)+"_1"+","+r.getEndNode().getProperty(NAMEPROPERTY)+"_2";
				_mapping=_mapping.replaceAll("existence_", "").replaceAll("inverse_", "");		
				weight=mappings.get(_mapping);
			}
			//名字不相等，把属性附带的情况给排除掉
			else 
			{
				mapping=mapping.replaceAll("existence_", "").replaceAll("inverse_", "");
				weight=mappings.get(mapping);
			}
				
			//double weight=mappings.get(mapping);
			List<Relationship> _rRelationship= new ArrayList<>();
			
			if(minWeight>weight)
				minWeight=weight;
			if(map.get(weight)==null)  //选取权重最小的值进行移除
			{
				_rRelationship.add(r);
				map.put(weight, _rRelationship);
			}
			else  //闭包个数为num不在存在mapping中
			{
				map.get(weight).add(r);			
			}
		}
		minimumRelationship=map.get(minWeight);
		return minimumRelationship;	
	}
	
	public HashMap<Node,Double> findcandidateRelationships(Relationship r) //找到对应移除mapping的闭包
	{
		//HashMap<Relationship,ArrayList<String>> candidateRelationship=new HashMap<Relationship,ArrayList<String>>();
		HashMap<Node,Double> candidateNodes=new HashMap<Node,Double>();
		List<MIMPP> sets=relMappingMIMPP.get(r);
		for(MIMPP m:sets)
		{
			for(List<Relationship> mappingPairs:m.mappingClosure.keySet())  //间接说明这些mapping对是存在闭包的
			{
				if(mappingPairs.contains(r))  //如果将角色归一化，那很多延生出来的闭包也就消失了(事实上也不需要太多)
				{	
					List<Node> candidate=m.mappingClosure.get(mappingPairs);
					Double weight=m.mappingClosureWeight.get(mappingPairs);
					for(Node a:candidate)
					{
						candidateNodes.put(a, weight);
					}
				}
			}
		}
		//创建关系
		//Node node=r.getStartNode();
		//candidateRelationship.put(r, candidateNodes);	
		return candidateNodes;	
	}
	
	public void updateGraph(List<Relationship> removedRelationship,HashMap<Relationship,HashMap<Node,Double>> candidateRelationships,GraphDatabaseService graphDB) 
	{
		candidatMappings=new ArrayList<String>();
		removedMappings=new ArrayList<String>();
		// 删除移除的边
		for(Relationship rel:removedRelationship)
		{
			String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
			String end = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
			String source = rel.getStartNode().getProperty(COMEFROMPROPERTY).toString();
			String target = rel.getEndNode().getProperty(COMEFROMPROPERTY).toString();
			if(rel.getProperty(TYPEPROPERTY).toString().equals("Role"))
			{
				
				StringBuilder query = new StringBuilder();
				String formatter = "WHERE n.Name='%s' and m.Name='%s' and n.ComeFrom='%s' and m.ComeFrom='%s'";
				query.append("MATCH n-[r]->m ");
				query.append(String.format(formatter, start, end, source, target));
				query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
				
				Tools.clear(query);
				query.append("MATCH n-[r]->m ");
				query.append(String.format(formatter, "existence_"+start, "existence_"+end, source, target));
				query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
				
				Tools.clear(query);
				query.append("MATCH n-[r]->m ");
				query.append(String.format(formatter, "inverse_"+start, "inverse_"+end, source, target));
				query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
				
				Tools.clear(query);
				query.append("MATCH n-[r]->m ");
				query.append(String.format(formatter, "existence_inverse_"+start, "existence_inverse_"+end, source, target));
				query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());			
			}		
			else
			{
				StringBuilder query = new StringBuilder();
				String formatter = "WHERE n.Name='%s' and m.Name='%s' and n.ComeFrom='%s' and m.ComeFrom='%s'";
				query.append("MATCH n-[r]->m ");
				query.append(String.format(formatter, start, end, source, target));
				query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
			}
			
			//考虑可能是属性的情况
			start=start.replace("existence_", "").replace("inverse_", "");
			end=end.replace("existence_", "").replace("inverse_", "");
			
			double weight=0;
			
			//removedMappings.add(start.replace("_1", "").replace("_2", "")+","+end.replace("_1", "").replace("_2", "")+","+weight+" "+source+"->"+target);
			if(start.equals(end)) //名字相等
			{
				String comefrom=rel.getStartNode().getProperty(COMEFROMPROPERTY).toString();								
				if(comefrom.equals(COMEFROMFIRST))
				{
					weight=mappings.get(start+"_1"+","+end+"_2");
					mappings.remove(start+"_1"+","+end+"_2");  //没效果
				}
				else
				{
					weight=mappings.get(start+"_2"+","+end+"_1");
					mappings.remove(start+"_2"+","+end+"_1");  //没效果
				}
			}				
			else
			{		
				//String comefrom=rel.getStartNode().getProperty(COMEFROMPROPERTY).toString();
				if(mappings.get(start+","+end)==null)
					continue;
				weight=mappings.get(start+","+end);
				mappings.remove(start+","+end);  //没效果
			}
			removedMappings.add(start.replace("_1", "").replace("_2", "")+","+end.replace("_1", "").replace("_2", "")+","+weight+" "+source+"->"+target);
			
		}
		// 加入refinement中的集合
		addedRelationship=new ArrayList<String>();
		for (Relationship rel : candidateRelationships.keySet())
		{
			/*if(removedRelationship.contains(rel))  //移除的边不应再被添加。
				continue;*/
			HashMap<Node,Double> beConnected = candidateRelationships.get(rel);
			for(Node n:beConnected.keySet())
			{
				//String type=rel.getProperty(TYPEPROPERTY).toString(); //角色作为候选集添加的情况很低
				boolean flag=true;			
				//重名的时候会产生问题
				//ExecCypher.createRelationBetweenNodesNameWithWeight(rel.getStartNode().getProperty(NAMEPROPERTY).toString(), n,beConnected.get(n), graphDB);
				ExecCypher.createRelationBetweenNodesWithWeight(rel.getStartNode(),n,beConnected.get(n), graphDB);
				
				//打印输出
				/*GlobalGraphOperations ggo= GlobalGraphOperations.at(graphDB);
				int i=0;
				for(Relationship relationship :  ggo.getAllRelationships()){
					++i;
					System.out.println(relationship);
					System.out.println(relationship.getStartNode().getProperty(NAMEPROPERTY)+" "+relationship.getEndNode().getProperty(NAMEPROPERTY));
					System.out.println(relationship.getProperty(TYPEPROPERTY).toString());
					System.out.println(relationship.getType());
					if(relationship.hasProperty(WEIGHTEDPROPERTY))  //判断是否有这个属性
					{
						System.out.println(relationship.getProperty(WEIGHTEDPROPERTY).toString());
					}
				}
				System.out.println(String.format("Number of relationship is %d", i));*/
				
				//如果存在最小冲突子集,则删除该边
				MappingDiagnosis diag1 = MappingDiagnosis.getDiagnosis(graphDB);
				//diag1.getUnsatTripleByRelationship(mappingNodes);
				diag1.getUnsatInformationByRelationship(mappingNodes);
				if (diag1.unsatNodes.size()!=0) 
				{
					String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
					//String end = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
					String end = n.getProperty(NAMEPROPERTY).toString();
					String source = rel.getStartNode().getProperty(COMEFROMPROPERTY).toString();
					//String target = rel.getEndNode().getProperty(COMEFROMPROPERTY).toString();
					String target = n.getProperty(COMEFROMPROPERTY).toString();
					
					StringBuilder query = new StringBuilder();
					String formatter = "WHERE n.Name='%s' and m.Name='%s' and n.ComeFrom='%s' and m.ComeFrom='%s'";
					query.append("MATCH n-[r]->m ");
					query.append(String.format(formatter, start, end, source, target));
					query.append("DELETE r");
					System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
					
					flag=false;
				}			
				if (flag==true) 
				{
					String mappingPairs=rel.getStartNode().getProperty(NAMEPROPERTY).toString()+","+n.getProperty(NAMEPROPERTY).toString();
					Double weight=beConnected.get(n);	
					//String direction=rel.getStartNode().getProperty(COMEFROMPROPERTY).toString()+"->"+rel.getEndNode().getProperty(COMEFROMPROPERTY).toString();
					String direction=rel.getStartNode().getProperty(COMEFROMPROPERTY).toString()+"->"+n.getProperty(COMEFROMPROPERTY).toString();
					
					addedRelationship.add(rel.getStartNode().getProperty(NAMEPROPERTY).toString()+"->"+n.getProperty(NAMEPROPERTY).toString()
							+" "+weight+" Direction:"+direction);
					candidatMappings.add(rel.getStartNode().getProperty(NAMEPROPERTY).toString().replace("_1", "").replace("_2", "")
							+","+n.getProperty(NAMEPROPERTY).toString().replace("_1", "").replace("_2", "")+","+weight);
					mappings.put(mappingPairs, weight);
				}
			}
		}
		//this.graphDB=graphDB;
	}
	
/*	public void updateGraphDynamic(Relationship rel,HashMap<Relationship,List<Node>> candidateRelationships,GraphDatabaseService graphDB) 
	{
		// 删除移除的边
		String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
		String end = rel.getEndNode().getProperty(NAMEPROPERTY).toString();
		StringBuilder query = new StringBuilder();
		String formatter = "WHERE n.Name='%s' and m.Name='%s' ";
		query.append("MATCH n-[r]->m ");
		query.append(String.format(formatter, start, end));
		query.append("DELETE r");
		System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
		// 加入refinement中的集合
	
		List<Node> beConnected = candidateRelationships.get(rel);
		for(Node n:beConnected)
		{
			//ExecCypher.createRelationBetweenNodes(rel.getStartNode().getProperty(NAMEPROPERTY).toString(), beConnected, graphDB);
			ExecCypher.createRelationBetweenNodes(rel.getStartNode().getProperty(NAMEPROPERTY).toString(), n, graphDB);
			//如果引入了新的冲突子集(这个时间开销肯定要大一些)
			List<UnsatTriple> _triples = diag.getUnsatTripleByRelationship();	
			List<MIMPP> _mimpps = MappingDiagnosis.getDiagnosis(graphDB).compMIMPPs(_triples,mappingRelationship);  //取出Graph中所有的MIMPP
			//如果引入了新的冲突子集
			if (_mimpps.size()>mimpps.size()) 
			{
				//String start = rel.getStartNode().getProperty(NAMEPROPERTY).toString();
				end = n.getProperty(NAMEPROPERTY).toString();
				StringBuilder new_query = new StringBuilder();
				formatter = "WHERE n.Name='%s' and m.Name='%s' ";
				new_query.append("MATCH n-[r]->m ");
				new_query.append(String.format(formatter, start, end));
				new_query.append("DELETE r");
				System.out.println(ExecCypher.simpleCypher(query, graphDB).dumpToString());
			}
		}
		
		ExecCypher.createRelationBetweenNodes(rel.getStartNode().getProperty(NAMEPROPERTY).toString(), beConnected, graphDB);
		for (Node node : candidateRelationships.get(rel))
		{
			Set<Node> beConnected = rs.getBeConnectedNodes();
			ExecCypher.createRelationBetweenNodes(rs.getR().getProperty(NAMEPROPERTY).toString(), beConnected, graphDB);
		}
	}*/
	
	public void printInfoOfRevision()
	{
		System.out.println("--------------------------------------------------------");
		System.out.println("移除的mappings个数为："+removedRelationship.size()+" 具体如下：");
		
		
		//candidatedMappings=new ArrayList<String>();
		for(Relationship a:removedRelationship)
		{
			//System.out.println(a.getProperty(WEIGHTEDPROPERTY).toString());
			//对属性的情况进行一下预处理
			String start=a.getStartNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
			String end=a.getEndNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
			String direction=a.getStartNode().getProperty(COMEFROMPROPERTY).toString()+"->"+a.getEndNode().getProperty(COMEFROMPROPERTY).toString();
			System.out.println(start+"->"+end+" Direction:"+direction);
			
		}
		System.out.println("--------------------------------------------------------");
		System.out.println("待添加的mappings具体如下：");
		int number=0;
		for(Relationship rel :candidateRelationships.keySet())
		{
			for(Node n:candidateRelationships.get(rel).keySet())
			{
				String direction=rel.getStartNode().getProperty(COMEFROMPROPERTY).toString()+"->"+n.getProperty(COMEFROMPROPERTY).toString();
				System.out.println(rel.getStartNode().getProperty(NAMEPROPERTY).toString()+"->"+n.getProperty(NAMEPROPERTY).toString()+" Direction:"+direction);			
				number++;
			}
		}
		System.out.println("待添加的mappings的个数为："+number);
		System.out.println("--------------------------------------------------------");
		System.out.println("已添加的mappings为："+addedRelationship.size()+" 具体如下：");
		for(String a:addedRelationship)
		{
			System.out.println(a);
		}	
		System.out.println("修复后的mappings为：");
		System.out.println("--------------------------------------------------------");
		ArrayList<String> revisionMapping=getMappings();
		for(String a:revisionMapping)
		{
			System.out.println(a);
		}
		/*for(String a:mappings.keySet())
		{
			System.out.println(a+" "+mappings.get(a));
		}*/
	}
	
	public ArrayList<String> getMappings()
	{
		ArrayList<String> revisedMappings=new ArrayList<String>();
		ArrayList<String> subMappings=new ArrayList<String>();
		try(Transaction tx = graphDB.beginTx())
		{
			for(Relationship rel:mappingRelationship)
			{
				String start=rel.getStartNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
				String end=rel.getEndNode().getProperty(NAMEPROPERTY).toString().replace("existence_", "").replace("inverse_", "");
				int number=0;
				double weight=0;
				//String direction="";
				if(start.equals(end))
				{
					if(mappings.keySet().contains(start+"_1"+","+end+"_2"))
					{
						weight=mappings.get(start+"_1"+","+end+"_2");
						number++;
					}
					if(mappings.keySet().contains(start+"_2"+","+end+"_1"))
					{
						number++;
						weight=mappings.get(start+"_2"+","+end+"_1");
					}
				}
				else
				{
					if(mappings.keySet().contains(start+","+end))
					{
						number++;
						weight=mappings.get(start+","+end);
					}
					if(mappings.keySet().contains(end+","+start))
					{
						number++;
						weight=mappings.get(end+","+start);
					}
				}
				if(number==2)
				{
					StringBuilder str1=new StringBuilder ();
					str1.append(start);  //"http://"
					str1.append(",");
					str1.append(end);
					str1.append(",=,");
					str1.append(weight);
					StringBuilder str2=new StringBuilder ();
					str2.append(end);
					str2.append(",");
					str2.append(start);
					str2.append(",=,");
					str2.append(weight);
					if(!revisedMappings.contains(str1.toString())&&!revisedMappings.contains(str2.toString()))
						revisedMappings.add(str1.toString());
					/*if(!revisedMappings.contains(start+","+end+","+"=,"+weight)&&!revisedMappings.contains(end+","+start+","+"=,"+weight))
						revisedMappings.add(start+","+end+","+"=,"+weight);*/
				}
				else if(number==1)
				{
					if(!subMappings.contains(start+","+end))
						subMappings.add(start+","+end);
					/*if(!revisedMappings.contains(start+","+end+","+"sub,"+weight))
						revisedMappings.add(start+","+end+","+"sub,"+weight);*/
				}
			}
			for(String map:subMappings)
			{
				String parts[]=map.split(",");
				if(mappings.keySet().contains(parts[0]+","+parts[1]))
					revisedMappings.add(parts[0]+","+parts[1]+","+"sub,"+mappings.get(parts[0]+","+parts[1]));
				else if(mappings.keySet().contains(parts[0]+"_1"+","+parts[1]+"_2"))
					revisedMappings.add(parts[0]+","+parts[1]+","+"sub,"+mappings.get(parts[0]+"_1"+","+parts[1]+"_2"));
				else if(mappings.keySet().contains(parts[0]+"_2"+","+parts[1]+"_1")) 
					revisedMappings.add(parts[1]+","+parts[0]+","+"sub,"+mappings.get(parts[0]+"_2"+","+parts[1]+"_1"));
			}
			tx.success();
		}
		
		 return revisedMappings;
	}
	
	public ArrayList<String> getRemoveMappings()
	{
		return removedMappings;
	}
	
	public ArrayList<String> getCandidateMappings()
	{
		return candidatMappings;
	}
	
	public List<Relationship> getMappingRelationship()
	{
		return mappingRelationship;
	}
	
	
	public void shutdown(){
		this.graphDB.shutdown();
	}

}
