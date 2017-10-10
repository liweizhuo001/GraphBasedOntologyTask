package kse.algorithm.forTBox.debugging.hittingset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BuildingHST {
	 HST root;
	 List<List<Integer>> mipses ;
	 //List<List<Integer>> mipsesInTree ; //已经添加到树中的mips
	 public void setMipses(List<List<Integer>> mipses) {
		this.mipses = mipses;
	}
	public List<List<Integer>> getMipses() {
		return mipses;
	}
	public HST getRoot(){
		return this.root;
	}
	
	public BuildingHST(){
		 root = new HST();
		 mipses = new ArrayList<List<Integer>>();
		 //mipsesInTree = new ArrayList<List<Integer>>();
	 }
	
	public BuildingHST(List<List<Integer>> mipses){
		root = new HST();
		//mipsesInTree = new ArrayList<List<Integer>>();
		this.mipses = mipses;
	}
	
	public void run(){
		Random random = new Random();
		int rootIndex = random.nextInt(mipses.size());				
		root.axiomList = mipses.get(rootIndex);
		//mipsesInTree.add(mipses.get(rootIndex));
		buildingTree(root);
	}
	
	//构建碰集树是一个递归的过程
	public void buildingTree(HST node){
		//System.out.println(node.edgeValue+"  ");
		List<Integer> _hittingSet = HST.getAncestralEdges(node);		
		for(Integer i : node.getAxiomList()){
			boolean isEnd = true;
			for(int j=0; j<mipses.size(); j++){
				//待选定的MIPS中不能有_hittingSet中的节点和i,
				//不用判断欲添加的mips是否在碰集树中，因为判断条件能够保证
				List<Integer> mips = mipses.get(j);
				if(mips.contains(i) || Misc.hasIntersction( _hittingSet,mips)){
					continue;
				}
				else{
					HST newNode = new HST();
					newNode.setAxiomList(mips);
					newNode.parent = node;
					newNode.edgeValue = i;
					newNode.setValidState();
					node.getChildren().add(newNode);
					isEnd = false;
					buildingTree(newNode);
					break;  //!important
				}
			}
			if(isEnd){
				HST newNode = new HST();				
				newNode.parent = node;
				newNode.edgeValue = i;
				newNode.setInvalidState();
				node.getChildren().add(newNode);				
			}
		}
	}				 
}
