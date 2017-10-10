package kse.test;

import java.util.TreeSet;
/**
 * 测试TreeSet, TreeSet根据集合中类的compareTo函数按序插入元素
 * 2015-5-30
 * @author Xuefeng Fu
 *
 */
public class TreeSetTest {
	public static void main(String[] args){
		TreeSet<ForTest> ts = new TreeSet<>();
		ForTest f1 = new ForTest("flag1", 8, 7);
		ForTest f2 = new ForTest("flag2", 21, 5);
		ForTest f3 = new ForTest("flag3", 21, 7);
		ForTest f4 = new ForTest("flag4", 6, 2);
		ts.add(f1);
		ts.add(f3);
		ts.add(f4);
		ts.add(f2);
		ts.add(f4);
		for(ForTest ft : ts){
			System.out.println(ft);
		}
	}

}


