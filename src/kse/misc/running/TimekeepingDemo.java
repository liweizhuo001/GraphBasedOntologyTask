package kse.misc.running;

import kse.misc.*;
/**
 * Demo for class Timekeeping
 * @author Xuefeng fu 2014-2
 *
 */
public class TimekeepingDemo {
	public static void main(String[] args){
		Timekeeping.begin();
		
		int temp;
		for(int i=0;i<100;i++)
			for(int j=0;j<100;j++)
				for(int k=0;k<10;k++){
					temp = i+j+k;
					System.out.println(temp);
				}
		
		Timekeeping.end();
		Timekeeping.showInfo("Test keeping");
	}
}
