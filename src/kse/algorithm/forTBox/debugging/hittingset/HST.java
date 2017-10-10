package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.List;

public class HST {
	List<Integer> axiomList;
	
	List<HST> children;
	int edgeValue;
	int state;
	HST parent;
	
	//root节点的edgeValue=-1 parent=null state=1;
	public HST(){
		edgeValue=-1;
		parent = null;
		state = -1;
		children = new ArrayList<HST>();
	}
	
	public List<HST> getChildren(){
		return this.children;
	}
	
	public void setAxiomList(List<Integer> axioms){
		this.axiomList = axioms;
	}
	
	public List<Integer> getAxiomList() {
		return axiomList;
	}
	
	public int getState(){
		return this.state;
	}
	
	public int getEdgeValue(){
		return this.edgeValue;
	}
	
	public HST getParent(){
		return this.parent;
	}
	
	public void setValidState(){
		state = 1;
	}
	
	public void setInvalidState(){
		state = -1;
	}
	
	//取出所有的叶子节点
	public List<HST> getLeafNode(){
		List<HST> leafNodes = new ArrayList<HST>();
		List<HST> hsts = this.getChildren();
		while(hsts.size()>0){
			List<HST> _hsts = new ArrayList<HST>();
			int index = 0;
			while(index < hsts.size()){
				HST hst = hsts.get(index);
				if(hst.getChildren().size() == 0){
					leafNodes.add(hst);
				}
				else{
					_hsts.addAll(hst.getChildren());
				}
				++index;
			}
			hsts = _hsts;
		}
		return leafNodes;
	}
	
	public List<List<Integer>> getHittingSets(){
		List<List<Integer>> hittingSets = new ArrayList<List<Integer>>();
		List<HST> leafNodes = getLeafNode();
		int index = 0;
		while(index < leafNodes.size()){
			List<Integer> hittingSet = new ArrayList<Integer>();
			HST node = leafNodes.get(index);
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
	public static List<Integer> getAncestralEdges(HST node){
		List<Integer> edges = new ArrayList<Integer>();
		while(node !=null){
			edges.add(node.getEdgeValue());
			node = node.parent;
		}
		return edges;
	}

}
