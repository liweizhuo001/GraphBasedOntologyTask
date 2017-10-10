package kse.test;

import java.util.HashSet;
import java.util.Set;



public class Test {

	public static void testSet(){
		Set<Integer> ints_1 = new HashSet<>();
		Set<Integer> ints_2 = new HashSet<>();
		for(int i=1; i<=5; i++){
			ints_1.add(i);
		}
		for(int i=6; i<=8; i++){
			ints_2.add(i);
		}
		
		ints_1.retainAll(ints_2);
		System.out.println(ints_1.size()+":");
		for(Integer i : ints_1){
			System.out.print(i+"  ");
		}
		System.out.println();
		System.out.println("******");
		for(Integer i : ints_2){
			System.out.println(i);
		}
		
	}
	public static void main(String[] args) {		
		testSet();
	}

}
