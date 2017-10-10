package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
/**
 * 使用集合来记录的碰集
 * @author Xuefeng Fu
 *
 */
public class HSTinSet {
	
	Set<Integer> mips;	
	Set<HSTinSet> children;
	int edgeValue;
	int state;
	HSTinSet parent;
	
	//root节点的edgeValue=-1 parent=null state=1; 有效的叶子状态为1
	public HSTinSet(){
		edgeValue=-1;
		parent = null;
		state = -1;
		children = new HashSet<HSTinSet>();
	}
	
	public Set<HSTinSet> getChildren(){
		return this.children;
	}
	
	public void setMIPP(Set<Integer> mipp){
		this.mips = mipp;
	}
	
	public Set<Integer> getMIPP() {
		return this.mips;
	}
	
	public int getState(){
		return this.state;
	}
	
	public int getEdgeValue(){
		return this.edgeValue;
	}
	
	public HSTinSet getParent(){
		return this.parent;
	}
	
	public void setValidState(){
		state = 1;
	}
	
	public void setInvalidState(){
		state = -1;
	}
	
	public boolean isValidState(){
		return state ==1;
	}
	
	//取出所有的叶子节点
	public List<HSTinSet> getLeafNode(){
		List<HSTinSet> leafNodes = new ArrayList<HSTinSet>();
		Set<HSTinSet> hsts = this.getChildren();
		while(hsts.size()>0){
			Set<HSTinSet> _hsts = new HashSet<HSTinSet>();			
			Iterator<HSTinSet> hstIterator = hsts.iterator();
			while(hstIterator.hasNext()){			
				HSTinSet hst = hstIterator.next();
				if(hst.getChildren().size() == 0 && hst.isValidState()){
					leafNodes.add(hst);
				}
				else{
					_hsts.addAll(hst.getChildren());
				}				
			}
			hsts = _hsts;
		}
		return leafNodes;
	}
	
	/**
	 * 从碰集树中提取碰集
	 */
	
	public List<List<Integer>> getHittingSets(){
		List<List<Integer>> hittingSets = new ArrayList<List<Integer>>();
		List<HSTinSet> leafNodes = getLeafNode();
		int index = 0;
		while(index < leafNodes.size()){
			List<Integer> hittingSet = new ArrayList<Integer>();
			HSTinSet node = leafNodes.get(index);
			while(node.parent != null){
				hittingSet.add(node.edgeValue);
				node = node.parent;
			}
			hittingSets.add(hittingSet);
			++index;
		}		
		return hittingSets;
	}
	
	public List<Integer> getMiniHittingSet(){
		List<Integer> miniHS = new ArrayList<>();
		List<List<Integer>> hittingSets = getHittingSets();
		int miniSize = 10000;
		for(List<Integer> hs: hittingSets){
			if(hs.size()<miniSize){
				miniSize = hs.size();
				miniHS = hs;
			}
		}
		return miniHS;
	}
	

	public void printHittingSet(){
		List<List<Integer>> hittingSets = getHittingSets();
		for(List<Integer> hittingSet : hittingSets){
			for(Integer i : hittingSet){
				System.out.print(i+"   ");
			}
			System.out.println();
		}
	}
	
	//取出当前节点到root节点的所有的边值集合
	public static Set<Integer> getAncestralEdges(HSTinSet node){
		Set<Integer> edges = new HashSet<Integer>();
		while(node !=null){
			edges.add(node.getEdgeValue());
			node = node.parent;
		}
		return edges;
	}

}
