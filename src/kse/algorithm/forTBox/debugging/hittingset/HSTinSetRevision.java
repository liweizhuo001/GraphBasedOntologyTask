package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kse.algorithm.Diagnosis;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import kse.algorithm.auxiliaryClass.UnsatTriple;
import kse.algorithm.forTBox.debugging.MIPP;
import kse.algorithm.forTBox.debugging.RefinementOperator;
import kse.algorithm.forTBox.debugging.RefinementSpace;
import kse.algorithm.forTBox.debugging.scoring.RevisionSpace;
import kse.misc.GlobalParams;
import kse.neo4j.ver2_1.ExecCypher;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * 基于图数据库的不协调TBox修正 先从图中计算MIPP，再从MIPP中提取diagnosis(MIPP中源自第一个本体的关系|公理) 通过Hitting
 * Set Tree(碰集树)计算最小碰集 [2015-5-31] 最小碰集就是unwanted集合，其中包含确定要删除的公理
 * 从unwanted集合中计算refinementSet, 其中包含待添加的公理(relationship)
 * 
 * @author Xuefeng Fu
 */
public class HSTinSetRevision {
	String gPath;
	GraphDatabaseService graphDB;
	// 关系到MIPS集合的映射，记录出现该关系(是诊断中的成员)的MIPS
	Map<Relationship, Set<MIPP>> relMappingMIPP;
	List<MIPP> mipps; // TBox中所有的MIPS
	Set<RevisionSpace> reviSpace;
	Diagnosis diag;
	List<Relationship> unWantedSet;
	List<RefinementSpace> refinementSet;
	List<Relationship> relInMIPPs; // 所有出现在MIPP中的Relationship

	public String getgPath() {
		return gPath;
	}

	public void setgPath(String gPath) {
		this.gPath = gPath;
	}

	public GraphDatabaseService getGraphDB() {
		return graphDB;
	}

	public void setGraphDB(GraphDatabaseService graphDB) {
		this.graphDB = graphDB;
	}

	public Map<Relationship, Set<MIPP>> getRelMappingMIPP() {
		return relMappingMIPP;
	}

	public void setRelMappingMIPP(Map<Relationship, Set<MIPP>> relMappingMIPP) {
		this.relMappingMIPP = relMappingMIPP;
	}

	public List<MIPP> getMipps() {
		return mipps;
	}

	public void setMipps(List<MIPP> mipps) {
		this.mipps = mipps;
	}

	public Set<RevisionSpace> getReviSpace() {
		return reviSpace;
	}

	public void setReviSpace(Set<RevisionSpace> reviSpace) {
		this.reviSpace = reviSpace;
	}

	public Diagnosis getDiag() {
		return diag;
	}

	public void setDiag(Diagnosis diag) {
		this.diag = diag;
	}

	public List<Relationship> getUnWantedSet() {
		return unWantedSet;
	}

	public void setUnWantedSet(List<Relationship> unWantedSet) {
		this.unWantedSet = unWantedSet;
	}

	public List<RefinementSpace> getRefinementSet() {
		return refinementSet;
	}

	public void setRefinementSet(List<RefinementSpace> refinementSet) {
		this.refinementSet = refinementSet;
	}

	public HSTinSetRevision(String gDB) {
		this.gPath = gDB;
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(gDB);
		this.diag = Diagnosis.getDiagnosis(graphDB);
		try (Transaction tx = graphDB.beginTx()) {
			this.init();
			tx.success();
		}
	}

	/**
	 * 初始化类中的变量
	 */
	public void init() {
		relMappingMIPP = new HashMap<>(); // 关系到MIPP的映射
		relInMIPPs = new ArrayList<>();
		List<UnsatTriple> triples = diag.getUnsatTripleByRelationship();
		mipps = Diagnosis.getDiagnosis(graphDB).compMIPPs(triples); // 取出Graph中所有的MIPP
		// 完成Relationship到MIPS的映射
		System.out.println("Getting the relationship in MIPP...");
		for (MIPP mipp : mipps) {
			List<Relationship> relOfDiag = mipp.getDiagnosis(); // 取出诊断
			for (Relationship r : relOfDiag) {
				// 将当前MIPP中来源本体1的公理加到所有公理集合中,去掉否定包含的关系
				if (r.getEndNode().getProperty(GlobalParams.NAMEPROPERTY)
						.toString().startsWith(GlobalParams.NEGATIVESIGN))
					continue;
				if (!(relInMIPPs.contains(r))) {
					relInMIPPs.add(r);
				}
			}
			for (Relationship r : relOfDiag) {
				Set<MIPP> current = relMappingMIPP.get(r);
				if (current == null) {
					current = new HashSet<>();
					relMappingMIPP.put(r, current);
				}
				if (!(current.contains(mipp))) {
					current.add(mipp);
				}
			}
		}
	}

	public void goRevising() {
		try (Transaction tx = graphDB.beginTx()) {
			unWantedSet = this.calculatingHSinSet(); // 在这里就是最小碰集
			this.calRevisionSpace(unWantedSet);
			RefinementOperator operator = new RefinementOperator();
			refinementSet = operator.calRefinementSet(unWantedSet, relMappingMIPP, graphDB);
			// operator.updateGraph(unWantedSet, refinementSet, graphDB);
			printInfoOfRevision();
		}
	}

	/**
	 * 输出修正信息
	 */
	public void printInfoOfRevision() {
		System.out.println("The size of unwanted:" + unWantedSet.size());
		for (Relationship r : unWantedSet) {
			System.out.println(GlobalFunct.relToStr(r) + " in MIPPs "
					+ relMappingMIPP.get(r).size());
		}
		int refinementSize = 0;
		for (RefinementSpace rs : refinementSet) {
			refinementSize += rs.getBeConnectedNodes().size();
		}
		System.out.println("The size of refinement " + refinementSize);
	}

	/**
	 * 通过碰集树计算碰集
	 * 
	 * @return
	 */
	public List<Relationship> calculatingHSinSet() {
		System.out.println("In calculating hitting set...");
		List<Relationship> miniHS = new ArrayList<>();
		List<Set<Integer>> mipps = transMIPPsToInt();
		// 输出mips
		for (Set<Integer> mipp : mipps) {
			for (Integer rel : mipp) {
				System.out.print(rel + "  ");
			}
			System.out.println();
		}
		BuildingHSTinSet building = new BuildingHSTinSet(mipps);
		building.run();
		HSTinSet root = building.getRoot();
		System.out.println("Calculating mini hitting set...");
		List<Integer> miniHSinInt = root.getMiniHittingSet(); // 取出碰集树，以整数的形式存储
		miniHS = transIntToRel(miniHSinInt); // 将碰集转换成关系的列表
		System.out.println("End of calculating hitting set");
		return miniHS;
	}

	/**
	 * 计算unwanted集合要添加的关系集合
	 */

	public void calRevisionSpace(List<Relationship> rels) {
		reviSpace = new HashSet<>();
		// int i=0;
		System.out.println("Calculating the revision space of relationship");
		for (Relationship rel : rels) {
			// System.out.println(++i+":"+GlobalFunct.relToStr(rel));
			Set<MIPP> mipps = relMappingMIPP.get(rel); // 待修正的关系对应的MIPS(有多个)
			Set<Node> nodes = compRevisionNodesOfRel(rel, mipps);
			reviSpace.add(new RevisionSpace(rel, mipps.size(), nodes));
		}
	}

	/**
	 * 修正某个relationship需要添加的边的目标节点
	 * 
	 * @param rel
	 *            要处理的关系
	 * @param mipps
	 *            rel所在的mips
	 * @return
	 */

	public Set<Node> compRevisionNodesOfRel(Relationship rel, Set<MIPP> mipps) {
		// Set<Node> beConnectedNodes = new HashSet<>();
		Set<Node> notConnectedNodes = new HashSet<>();
		Set<Node> mayConnectedNodes = new HashSet<>();
		for (MIPP mi : mipps) {
			compRevisionNodesInMIPP(rel, mi, notConnectedNodes,
					mayConnectedNodes);
		}
		mayConnectedNodes.removeAll(notConnectedNodes); // 去除一定不能添加的节点
		return mayConnectedNodes;
	}

	/**
	 * 一个MIPS中和诊断中的relationship关联的节点
	 * 
	 * @param rel
	 *            待处理的关系
	 * @param mipp
	 *            关系对应的MIPP
	 */

	public void compRevisionNodesInMIPP(Relationship rel, MIPP mipp,
			Set<Node> notConnectedNodes, Set<Node> mayConnectedNodes) {
		// 修正路径上的节点，从待修正关系到路径的结尾的一段路径(暂称为修正路径)，这些节点一定会不能添边
		Set<Node> n_path = new HashSet<>();
		// Set<Node> mayBeAdded = new HashSet<>(); //可能添加的点
		List<Relationship> forRel = null;
		// 找出诊断中的relationship在路径中出现的位置（由于是MIPS，故之会出现在一边）
		int index = -1;
		index = GlobalFunct.indexOf(mipp.getPathToN(), rel);
		if (index == -1) {
			index = GlobalFunct.indexOf(mipp.getPathToP(), rel);
			forRel = mipp.getPathToP().subList(index, mipp.getPathToP().size());
		} else {
			forRel = mipp.getPathToN().subList(index, mipp.getPathToN().size());
		}
		// System.out.println(relToStr(rel)+"#index:"+index);
		for (Relationship r : forRel) {
			n_path.add(r.getStartNode());
			n_path.add(r.getEndNode());
		}
		notConnectedNodes.addAll(n_path);

		n_path.remove(rel.getStartNode());
		mayConnectedNodes.addAll(ExecCypher.getParentNodes(n_path, graphDB));
	}

	/**
	 * 修正TBox,删除诊断中的relationship，添加必须的边
	 */

	/**
	 * 将MIPPS中存储的形式转换成整数的形式，其实就是将MIPP中出现的Relationship存在一个数组中，
	 * 用数组中的下标表示Relationship
	 * 
	 * @return 整数形式的MIPP集合
	 */
	public List<Set<Integer>> transMIPPsToInt() {
		List<Set<Integer>> mippsInInt = new ArrayList<Set<Integer>>();
		for (MIPP mipp : mipps) {
			List<Relationship> relOfDiag = mipp.getDiagnosis(); // 取出诊断
			Set<Integer> mippInInt = new HashSet<>();
			for (Relationship r : relOfDiag) {
				if (relInMIPPs.contains(r)) {
					mippInInt.add(relInMIPPs.indexOf(r));
				}
			}
			mippsInInt.add(mippInInt);
		}
		return mippsInInt;
	}

	/**
	 * 将整数型的Hitting set转换成关系型
	 * 
	 * @param hsInInt
	 *            使用整数表达的碰集
	 * @return 使用关系表达的碰集
	 */
	public List<Relationship> transIntToRel(List<Integer> hsInInt) {
		List<Relationship> hsInRel = new ArrayList<Relationship>();
		for (int i : hsInInt) {
			hsInRel.add(relInMIPPs.get(i));
		}
		return hsInRel;
	}

	public void shutdown() {
		this.graphDB.shutdown();
	}

}
