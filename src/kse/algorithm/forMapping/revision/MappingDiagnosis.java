package kse.algorithm.forMapping.revision;

import static kse.algorithm.auxiliaryClass.GlobalFunct.getDescendantNodesWithRecursionForMappings;
import static kse.algorithm.auxiliaryClass.GlobalFunct.getDisjointPairForMappings;
import static kse.algorithm.auxiliaryClass.GlobalFunct.getFatherNode;
import static kse.algorithm.auxiliaryClass.GlobalFunct.pathToList;
import static kse.algorithm.auxiliaryClass.GlobalFunct.getFatherNodeSignleSource;
import static kse.misc.GlobalParams.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.neo4j.cypher.internal.PathImpl;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import kse.algorithm.auxiliaryClass.DisjointPairForMappings;
import kse.algorithm.auxiliaryClass.UnsatInformation;
import kse.misc.Tools;

/**
 * 
 * Mapping诊断与修正类
 * 提供 MIPPs的计算函数
 * 最近修改时间：2017-10-30 ！important
 * @author Weizhuo Li
 **/

public class MappingDiagnosis {
	String owlPath;
	String gPath;
	GraphDatabaseService graphDB;
	ExecutionEngine engine;
	Set<Node> unsatNodes;
	Set<Node> refinedUnsatNodes;
	boolean hasDisjointLabel;
	
	public static MappingDiagnosis getDiagnosis(GraphDatabaseService graphDB){
		return new MappingDiagnosis(graphDB);
	}
	
	public MappingDiagnosis(GraphDatabaseService graphDB){
		this.graphDB = graphDB;
		engine = new ExecutionEngine(graphDB);
		unsatNodes = new HashSet<Node>();
		refinedUnsatNodes=new HashSet<Node>(); 
		hasDisjointLabel = false;
		//Index<Node> nodeIndex=new Index<Node>();
	}
	
	/**
	 * 计算图中不可满足节点，从不相交的节点队出发，分别计算他们所有的子节点
	 * 最后不相交节点的子节点集合的交就是不可满足的概念
	 * 为了节省后续MIPPs的计算时间，我们对这些结点进行了精练
	 */
	public  List<UnsatInformation> getUnsatInformationByRelationship(Set<String> mappingNodes)
	{
		System.out.println("Getting unsatisfactable nodes in TBox by relationship");
		List<UnsatInformation> unSatTriples = new ArrayList<>();				
		List<DisjointPairForMappings> disjointPairForMappings = getDisjointPairForMappings(graphDB);	//可能要加上一个归属	
		System.out.println("disjoint pairs:"+disjointPairForMappings.size());
		try(Transaction tx = graphDB.beginTx()){
			Index<Node> nodeIndex = graphDB.index().forNodes(NODEINDEX);		
			nodeIndex = graphDB.index().forNodes(NODEINDEX);	
			for(DisjointPairForMappings pair : disjointPairForMappings){
				String comefrom=pair.getSource();  //本体的来源
				Node first,second;
				if(comefrom.equals(COMEFROMFIRST))  //同名的两个概念可以用不同的本体来源来进行区分
				{
					first = nodeIndex.get(NAMEPROPERTY, pair.getFirst()+"_1").getSingle();
					second = nodeIndex.get(NAMEPROPERTY, pair.getSecond()+"_1").getSingle();
				}
				else
				{
					first = nodeIndex.get(NAMEPROPERTY, pair.getFirst()+"_2").getSingle();
					second = nodeIndex.get(NAMEPROPERTY, pair.getSecond()+"_2").getSingle();
				}
				
				/*System.out.println(first.getProperty(NAMEPROPERTY));
				System.out.println(second.getProperty(NAMEPROPERTY));*/
										
				Set<Node> firstDescendant = getDescendantNodesWithRecursionForMappings(first);
				Set<Node> secondDescendant =  getDescendantNodesWithRecursionForMappings(second);
						
				firstDescendant.retainAll(secondDescendant);//两集合的交的点
				unsatNodes.addAll(firstDescendant);
						
				Node refinedUnsatNode=null;
				Boolean flag=false; //存在不可满足的点
				
				for(Node unNode:firstDescendant)  //这只是一次遍历得到的点,一定是会经过mappingNode，但交集的区域未必是mappingsNode
				{					
					//Node node = nodeIndex.get(NAMEPROPERTY, unNode).getSingle();
					Node node=null;
					node = nodeIndex.get(NAMEPROPERTY, unNode.getProperty(NAMEPROPERTY).toString()+"_1").getSingle();
					if(node==null)
						node = nodeIndex.get(NAMEPROPERTY, unNode.getProperty(NAMEPROPERTY).toString()+"_2").getSingle();				
					//Set<String> ancestorNodes=getFatherNodeName(node);
					List<Node> ancestorNodes=getFatherNode(node);
					ancestorNodes.retainAll(firstDescendant);  //取两者的交
					if(ancestorNodes.isEmpty())  //如果是空集,间接证明node是第一个不相交的点,但有可能不唯一
					{
						refinedUnsatNode=unNode;
						refinedUnsatNodes.add(unNode);  //虽然精练的点相同，但是可能路径不同，因此仍需要保留整个信息
						flag=true;
						unSatTriples.add(new UnsatInformation(refinedUnsatNode,comefrom, pair.getFirst(),pair.getSecond(),comefrom));
					}			
				}
				//一条路径只需要考虑最初影响的那个点，后续的不一致性均可以解除		
				if(flag==true) //不需要进行多余的查找
					continue;
				Set<Node> candidateNodes=new HashSet<Node>();
				for(Node cNode:firstDescendant)
				{
					String nodename=cNode.getProperty(NAMEPROPERTY).toString();
					nodename=nodename.replace("existence_","").replace("inverse_", "");
					if(mappingNodes.contains(nodename)&&!candidateNodes.contains(cNode))
						candidateNodes.add(cNode);
				}					
				if(!candidateNodes.isEmpty())  //那么不一致的情况是由等价匹配中的点所引起
				{
					for(Node unNode:candidateNodes)  //这只是一次遍历得到的点,一定是会经过mappingNode，但交集的区域未必是mappingsNode
					{
						refinedUnsatNodes.add(unNode); //虽然精练的点相同，但是可能路径不同，因此仍需要保留整个信息
						String source=unNode.getProperty(COMEFROMPROPERTY).toString();						
						//if(unSatTriples.contains(o))
						unSatTriples.add(new UnsatInformation(unNode, comefrom,pair.getFirst(),pair.getSecond(),source));	
						/*UnsatInformation information=new UnsatInformation(unNode, comefrom,pair.getFirst(),pair.getSecond(),source);
						if(!unSatTriples.contains(information))
							unSatTriples.add(new UnsatInformation(unNode,comefrom, pair.getFirst(),pair.getSecond(),comefrom));			*/
					}
				}				
			}
			tx.success();
		}	
		/*for(String un:unsatNodes){
			System.out.println(un);
		}*/
		System.out.println("Number of UN  is "+unsatNodes.size() + " and the refinedNode is "+refinedUnsatNodes.size());		
		return unSatTriples;
	}
	
	/**
	 * 利用unSatTriples的信息来进行MIPPs的求解
	 * 这里采用的cypher查询语言，但因为存在cycle的原因，并没有单一的数据结构效率高
	 */
	public List<MIMPP> compMIMPPsOfMappings(List<UnsatInformation> tetrads,List<Relationship> mappingRelationship){
		List<MIMPP> mimpps = new ArrayList<>();			
		StringBuilder query = new StringBuilder();

		int iCount = 0;
		int iEnd = 0;
		//为了避免Transaction过大，每MAXMUPS个提交一次		
		while(iCount < tetrads.size()){
			try(Transaction tx = graphDB.beginTx()){
				//System.out.print(iCount+"   ");
				iEnd = iCount + MAXMUPS;
				if(iEnd >= tetrads.size()){
					iEnd = tetrads.size();					
				}
				System.out.print(iEnd+" "); //输出最后一个三元组的序号
				while(iCount < iEnd){	
					//System.out.println("----------------------------------------"+iCount);
					//*******使用参数，手册上认为参数的模式会提供查询的效率
					UnsatInformation tetrad = tetrads.get(iCount);					
					Map<String, Object> params = new HashMap<String, Object>();
					String pos = tetrad.getFirst();
					String neg = tetrad.getSecond();
					String unsatNode = tetrad.getUnsat();
					String comefrom = tetrad.getSource();  //不可满足点的来源
					String pairSource=tetrad.getPairSoucre();	//Pair的来源
							
					params.put("pos", pos);
					params.put("neg", neg);	
					params.put("uc", unsatNode);
					params.put("comefrom", comefrom);
					params.put("pairSource", pairSource);	
		
					if(pos.equals(unsatNode))  //考虑pos=unsatNode时，查询语句无法执行的情况
					{						
						Tools.clear(query);					
						query.append("MATCH np=(uc-[:INCLUDEDBY*]->_n) ");
						query.append("WHERE _n.Name={neg} and uc.Name={uc} "); 
						query.append(" and all(x in nodes(np) WHERE 1=size(filter(y in nodes(np) where x=y))) ");
						query.append(" and uc.ComeFrom={comefrom} and _n.ComeFrom={pairSource} ");
						query.append("RETURN np");						
						ExecutionResult result = engine.execute(query.toString(),params);					
						//考虑pos=unsatNode的情况
						ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
						while(resultInIterator.hasNext())
						{
							Map<String,Object> unsatMap = resultInIterator.next();					
							PathImpl nPath = (PathImpl)unsatMap.get("np");
						
							List<Relationship> pp = new ArrayList<Relationship>();
							List<Relationship> pn = pathToList(nPath);	 //修改3	
							pn.retainAll(mappingRelationship);	
							//判断pn，pp经过的点是否是唯一的。
							/*Boolean cycleflag=false;
							cycleflag=IsExistCycle(pn);
							if(cycleflag)  //若果存在环的话，那么一点不是最小冲突子集，肯定存在冗余
								continue;*/
							Set<Relationship> incoherences = new HashSet<Relationship>();
							//考虑可能出现Relationship相等的情况
							for(Relationship rel:pp)
							{
								if(!incoherences.contains(rel))
									incoherences.add(rel);
							}
							for(Relationship rel:pn)
							{
								if(!incoherences.contains(rel))
									incoherences.add(rel);
							}
							boolean repetition=false;
							for(MIMPP mappings:mimpps)
							{
								if(mappings.getincoherenceMappings().equals(incoherences))
								{
									repetition=true;
									break;
								}					
							}
							if(repetition==true)
								continue;	
							
							//闭包的信息需要存储，方便进行评估
							Map<List<Relationship>, List<Node>> mappingClosure=new HashMap<List<Relationship>, List<Node>>(); //每个匹配对对应的最小冲突子集
							Map<List<Relationship>, Double> mappingClosureWeight=new HashMap<List<Relationship>, Double>(); //每个匹配对对应的最小冲突子集										
    						//其中pp,pn可能有一个为空
							MIMPP mimpp = new MIMPP(pp, pn, unsatNode, comefrom, mappingClosure, mappingClosureWeight,
									incoherences);
							if (!mimpps.contains(mimpp))
							{
								mimpps.add(mimpp);
							}

						}							
						//++iCount;		
					}	
					
					else
					{
						Tools.clear(query);
						/*query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n), ");
						query.append("np=(uc-[r2:INCLUDEDBY*]->_n) ");
						query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
						query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "); // r1,r2是关系的集合，查询要求r1和r2没有公共的关系
						query.append(" and uc.ComeFrom={comefrom} and n.ComeFrom={pairSource} and _n.ComeFrom={pairSource} ");
						query.append("RETURN pp,np");*/
						
						query.append("MATCH pp=(uc-[r1:INCLUDEDBY*]->n), ");
						query.append("np=(uc-[r2:INCLUDEDBY*]->_n) ");
						query.append("WHERE all(r in r1 WHERE all(g in r2 WHERE not(g=r)))  ");
						query.append(" and all(x in nodes(pp) WHERE 1=size(filter(y in nodes(pp) where x=y))) ");
						query.append(" and all(x in nodes(np) WHERE 1=size(filter(y in nodes(np) where x=y))) ");
						query.append(" and n.Name={pos} and _n.Name={neg} and uc.Name={uc} "); // r1,r2是关系的集合，查询要求r1和r2没有公共的关系
						query.append(" and uc.ComeFrom={comefrom} and n.ComeFrom={pairSource} ");  //测试一下，减少一点的约束条件会不会提到速度
						query.append("RETURN pp,np");
						
						//long tic=System.currentTimeMillis();
						ExecutionResult result = engine.execute(query.toString(), params);
						
						String string=result.toString();
						if(string.equals("empty iterator"))
						{
							iCount++;
							continue;							
						}

						// 考虑pos=unsatNode的情况,注意消除冗余的边
						ResourceIterator<Map<String, Object>> resultInIterator = result.iterator();
						
						while (resultInIterator.hasNext()) {
							Map<String, Object> unsatMap = resultInIterator.next();
							//long toc=System.currentTimeMillis();
							//System.out.println("cyhpe语句消耗的时间为："+(toc-tic)+"ms");
							PathImpl pPath = (PathImpl) unsatMap.get("pp");
							PathImpl nPath = (PathImpl) unsatMap.get("np");
							//toc=System.currentTimeMillis();
							//System.out.println("查询路径消耗的时间为："+(toc-tic)+"ms");
							// refinedUnsatNodes
							List<Relationship> pp = pathToList(pPath); // 修改2
							List<Relationship> pn = pathToList(nPath); // 修改3

							/*Boolean cycleflag1 =false,cycleflag2=false;
							cycleflag1 = IsExistCycle(pp);
							cycleflag2 = IsExistCycle(pn);
							if (cycleflag1 || cycleflag2) // 若果存在环的话，那么一点不是最小冲突子集，肯定存在冗余
								continue;*/
							pp.retainAll(mappingRelationship);
							pn.retainAll(mappingRelationship);

							// 先看是否最小冲突子集是否重复
							Set<Relationship> incoherences = new HashSet<Relationship>();
							for (Relationship rel : pp) 
							{
								if(!incoherences.contains(rel))
									incoherences.add(rel);
							}
							for (Relationship rel : pn) 
							{
								if(!incoherences.contains(rel))
									incoherences.add(rel);
							}
							boolean repetition = false;
							for (MIMPP mappings : mimpps) 
							{
								if (mappings.getincoherenceMappings().equals(incoherences)) 
								{
									repetition = true;
									break;
								}
							}
							if (repetition == true)
								continue;
							// 闭包的信息需要存储，方便进行评估
							Map<List<Relationship>, List<Node>> mappingClosure = new HashMap<List<Relationship>, List<Node>>(); // 每个匹配对对应的最小冲突子集
							Map<List<Relationship>, Double> mappingClosureWeight = new HashMap<List<Relationship>, Double>(); // 每个匹配对对应的最小冲突子集
							double weight1 = 1.0;
							for (Relationship map1 : pp) {
								Node node1 = map1.getEndNode();
								weight1 = weight1 * Double.parseDouble(map1.getProperty(WEIGHTEDPROPERTY).toString());
								List<Node> father1 = getFatherNodeSignleSource(node1);
								if (father1.isEmpty())
									continue;
								double weight2 = 1.0;
								for (Relationship map2 : pn) 
								{
									Node node2 = map2.getEndNode();
									weight2 = weight2
											* Double.parseDouble(map2.getProperty(WEIGHTEDPROPERTY).toString());
									List<Node> father2 = getFatherNodeSignleSource(node2);
									if (father2.isEmpty())
										continue;
									father2.retainAll(father1); // 想到两者的交
									// 移除不可满足的点
									List<Node> closureNode = new ArrayList<Node>();
									for (Node a : father2) {
										//System.out.println(a.getProperty(NAMEPROPERTY).toString());
										//System.out.println(a.getProperty(COMEFROMPROPERTY));
										if (a.getProperty(NAMEPROPERTY).toString().startsWith(NEGATIVESIGN)) // 不考虑negative开头的词
											continue;
										if (a.getProperty(NAMEPROPERTY).toString().equals(unsatNode)) // 名字不能相同
											continue;
										/*if (a.getProperty(NAMEPROPERTY).toString().equals(unsatNode)&&a.getProperty(COMEFROMPROPERTY).toString().equals(comefrom)) // 名字不能相同
											continue;*/
										
										/*if (a.getProperty(NAMEPROPERTY).toString().contains("existence_")) // 名字不能相同
											continue;
										if (a.getProperty(NAMEPROPERTY).toString().contains("inverse_")) // 名字不能相同
											continue;
										if (a.getProperty(NAMEPROPERTY).toString().contains("existence_inverse_")) // 名字不能相同
											continue;*/
										closureNode.add(a);
										// closureNode.add(a);
									}
									if (!closureNode.isEmpty()) {

										List<Relationship> mappingsClosure = new ArrayList<Relationship>();
										mappingsClosure.add(map1);
										mappingsClosure.add(map2);
										mappingClosure.put(mappingsClosure, closureNode);
										double inferencedWeight = 1 - (1 - weight1) * (1 - weight2);
										mappingClosureWeight.put(mappingsClosure, inferencedWeight);
									}
								}
							}
							// 其中pp,pn可能有一个为空
						MIMPP mimpp = new MIMPP(pp, pn, unsatNode, comefrom, mappingClosure, mappingClosureWeight,incoherences);
						mimpps.add(mimpp);												
					}			
				  }
				  iCount++;					
				}	
				tx.success();
				//System.out.println(iCount);	
				if(iCount >= tetrads.size()-1)  //可能会出错
					break;							
			}
		}	
		System.out.println("Number of MIMPP is "+mimpps.size());
		return mimpps;		
	}
	
	/**
	 * 用程序来判断是否存在环（cypher语句直接可以实现）
	 * */
	public boolean IsExistCycle(List<Relationship> relationships)  //注意comefrom
	{
		Set<Node> nodes=new HashSet<Node>();
		for(Relationship r:relationships)
		{
			if(nodes.contains(r.getEndNode()))
			//if(!nodes.add(r.getEndNode()))	
			{
				return true;
			}
			else 
			{
				nodes.add(r.getStartNode());
				nodes.add(r.getEndNode());
			}
		}
		return false;
	}
	

}
