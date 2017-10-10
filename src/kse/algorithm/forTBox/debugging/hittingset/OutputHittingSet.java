package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class OutputHittingSet {
	
	public OutputHittingSet(){	}
	public static List<Set<Integer>> mipses = new ArrayList<Set<Integer>>();            //mips集合
	//public static List<Integer> leafNode = new ArrayList<Integer>();                                //叶子节点
	//public static List<List<Integer>> rightHitTree = new ArrayList<List<Integer>>();  //碰集树集合
	
	@SuppressWarnings("serial")
	public static void main(String[] args){		
		/*List<Integer> mips1 = new ArrayList<Integer>(){{add(1);add(2);}};
		List<Integer> mips2 = new ArrayList<Integer>(){{add(3);add(4);add(5);}};
		List<Integer> mips3 = new ArrayList<Integer>(){{add(4);add(7);}};*/
		
		Set<Integer> mips1 = new HashSet<Integer>(){{add(2);add(3);add(4);}};
		Set<Integer> mips2 = new HashSet<Integer>(){{add(4);add(7);}};
		Set<Integer> mips3 = new HashSet<Integer>(){{add(3);add(5);add(6);}};
		Set<Integer> mips4 = new HashSet<Integer>(){{add(1);add(2);add(3);}};
		Set<Integer> mips5 = new HashSet<Integer>(){{add(1);add(5);}};
		Set<Integer> mips6 = new HashSet<Integer>(){{add(2);add(7);}};
		
		mipses.add(mips1);
		mipses.add(mips2);
		mipses.add(mips3);
		mipses.add(mips4);
		mipses.add(mips5);
		mipses.add(mips6);
		
		/*printList(mips1);
		printList(mips2);
		printList(mips3);*/
		
		BuildingHSTinSet building = new BuildingHSTinSet(mipses);	
		building.run();
		HSTinSet root = building.getRoot();
		root.printHittingSet();	
		
	}	

	public static void printList(List<Integer> mips){
		for(Integer i : mips){
			System.out.print(i+"    ");
		}
		System.out.println();
	}
}
