package kse.algorithm.forTBox.debugging;

import java.util.Set;
import kse.algorithm.auxiliaryClass.GlobalFunct;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

/**
 * 关系的优化空间，待删除的关系以及与之对应的要添加的关系集合
 * 2015-3-31
 * @author Xuefeng Fu
 */

public class RefinementSpace {
	Relationship r; // 待修正的关系，其实就是删除该关系

	Set<Node> beConnectedNodes; // 如修正则需要添的目的节点集合，从r的startnode建立到这些节点的关系

	public RefinementSpace(Relationship r, Set<Node> nodes) {
		this.r = r;
		this.beConnectedNodes = nodes;
	}

	public Relationship getR() {
		return r;
	}

	public Set<Node> getBeConnectedNodes() {
		return beConnectedNodes;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof RefinementSpace)) {
			return false;
		} else {
			RefinementSpace ro = (RefinementSpace) o;
			if (!(GlobalFunct.isRelEquals(r, ro.getR()))) {
				return false;
			} else if (beConnectedNodes.size() != ro.getBeConnectedNodes()
					.size()) {
				return false;
			} else {
				return true;
			}
		}
	}
}
