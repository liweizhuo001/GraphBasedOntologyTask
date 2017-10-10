package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 构建碰集树
 * @author Xuefeng Fu
 *
 */
public class BuildingHSTinSet {
	 HSTinSet root;
	 List<Set<Integer>> mipses ;
	 //List<List<Integer>> mipsesInTree ; //已经添加到树中的mips
	 public void setMipses(List<Set<Integer>> mipses) {
		this.mipses = mipses;
	}
	public List<Set<Integer>> getMipses() {
		return mipses;
	}
	public HSTinSet getRoot(){
		return this.root;
	}
	
	public BuildingHSTinSet(){
		 root = new HSTinSet();
		 mipses = new ArrayList<Set<Integer>>();
	 }
	
	public BuildingHSTinSet(List<Set<Integer>> mipses){
		root = new HSTinSet();
		this.mipses = mipses;
	}
	
	public void run(){
		Random random = new Random();
		int rootIndex = random.nextInt(mipses.size());	
		rootIndex = 0;
		root.mips = mipses.get(rootIndex);	
		buildingTree(root);
	}
	
	//构建碰集树是一个递归的过程
	public void buildingTree(HSTinSet node){
		//System.out.println(node.edgeValue+"  ");
			
		for(Integer iEdgeValue : node.getMIPP()){
			boolean isEnd = true;
			Set<Integer> _hittingSet = HSTinSet.getAncestralEdges(node);	
			int mIndex = 0;
			for(mIndex=0; mIndex<mipses.size(); mIndex++){
				//待选定的MIPS中不能有_hittingSet中的节点和i,
				//不用判断欲添加的mips是否在碰集树中，因为判断条件能够保证
				Set<Integer> mips = mipses.get(mIndex);						
				if(mips.contains(iEdgeValue) || Misc.hasIntersction( _hittingSet,mips)){
					continue;
				}					
				else{
					HSTinSet newNode = new HSTinSet();
					newNode.setMIPP(mips);
					newNode.parent = node;
					newNode.edgeValue = iEdgeValue;
					newNode.setValidState();
					node.getChildren().add(newNode);
					isEnd = false;
					buildingTree(newNode);
					break;  //!important
				}
			}
			if(mIndex == mipses.size()){//表明正常结束
				HSTinSet newNode = new HSTinSet();				
				newNode.parent = node;
				newNode.edgeValue = iEdgeValue;
				newNode.setValidState();  //正常结束的叶子节点
				node.getChildren().add(newNode);
			}
			if(isEnd){
				HSTinSet newNode = new HSTinSet();				
				newNode.parent = node;
				newNode.edgeValue = iEdgeValue;
				newNode.setInvalidState();
				node.getChildren().add(newNode);				
			}
		}
	}	
	/**
	 * 判断是否与添加子节点的条件冲突, 不能修改mips
	 * 二个条件
	 * 1. 新的mips中不包含任一从当前节点到root节点的边值
	 * 2. 边值集合中和任一MIPS的并不能大于1!!!这个条件不满足
	*/
	public boolean isConflict(HSTinSet node, Set<Integer> mips, int iEdgeValue){		
		Set<Integer> _hittingSet = HSTinSet.getAncestralEdges(node);	
		_hittingSet.add(iEdgeValue);
		_hittingSet.retainAll(mips);
		if(_hittingSet.size()>0){
			return true;
		}		
		
		//保证在边值集合中，不会出现某个mips中的两个或两个一上的元素
		/*while(node.parent!=null){
			node = node.parent;
			if(node.getMIPP().contains(iEdgeValue)){
				return true;
			}
		}*/			
		return false;
	}
	
}
