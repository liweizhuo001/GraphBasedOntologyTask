package kse.algorithm.forABox.running;

import kse.algorithm.forABox.preprocessing.ABoxFileHandle;
import kse.misc.Timekeeping;
/**
 * 主程序：合并或拆分ABox [2014-9]
 * @author Xuefeng
 */
public class ABoxFileHandleMain {	
	
	public static void withPercent(){
		int[] precent= new int[]{20};	//不相交类的百分比,实际在处理原始数据的时候是不需要考虑这个百分比的！！！
		int[] univNum = new int[]{2, 4, 6,8,10};
		for(int i=0;i<precent.length;i++){
			for(int j=0; j< univNum.length; j++){
				System.out.println(String.format("Precent:%s,  university:%s", precent[i], univNum[j]));
				ABoxFileHandle handle = new ABoxFileHandle(precent[i], univNum[j]);
				handle.go();
				handle = null;
			}
		}	
	}
	
	public static void withoutPercent(){
		int[] univNum = new int[]{2, 4, 6,8,10};		
		for(int i=0; i< univNum.length; i++){
			System.out.println(String.format("university:%s", univNum[i]));
			ABoxFileHandle handle = new ABoxFileHandle(univNum[i]);
			handle.go();
			handle = null;
		}
	}
	
	public static void main(String[] args){		
		Timekeeping.begin();
					
		withoutPercent();
		
		Timekeeping.end();
		Timekeeping.showInfo("Handling ABox ");
	}
}
