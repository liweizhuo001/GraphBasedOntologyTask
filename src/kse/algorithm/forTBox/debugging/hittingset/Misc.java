package kse.algorithm.forTBox.debugging.hittingset;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Misc {
	//判断两个list内容相同
	public static boolean isEqualList(List<?> list1, List<?> list2) {
		if (list1.size() == list2.size()) {
			for (int i = 0; i < list1.size(); i++) {
				if (list1.get(i) != list2.get(i)){
					return false;
				}
			}
			return true;
		}		 
		else {
			return false;
		}	
	}
	/**
	 * 判断两个列表是否有交集
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static boolean hasIntersction(List<?> list1, List<?> list2){
		for(int i=0;i<list1.size();i++){
			if(list2.contains(list1.get(i))){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断两个集合是否有交集
	 * @param set1
	 * @param set2
	 * @return
	 */
	public static boolean hasIntersction(Set<Integer> set1, Set<Integer> set2){
		for(Integer iValue : set1){
			if(set2.contains(iValue)){
				return true;
			}
		}
		return false;
	}
	
	public static void main(String[] args){
		List<String> l1 = new ArrayList<String>();
		l1.add("1");
		List<String> l2 = new ArrayList<String>();
		l2.add("1");
		System.out.println(isEqualList(l1,l2));
	}
}
