package kse.algorithm.forABox.running;

import java.util.List;
import java.util.Set;

import kse.algorithm.auxiliaryClass.DisjointPair;
import kse.algorithm.forABox.preprocessing.NewInjectConflicts;

public class NewInjectConflictsMain {
	
	public static void disjointTest(NewInjectConflicts app){
		List<Set<String>> blockSet = app.blockingConcept();		
		List<DisjointPair> disjointPairs = app.getDisjointConceptsFromBlock(blockSet);	
		List<DisjointPair> descenantPair = app.getDescenantClass(disjointPairs);
		for(DisjointPair pair : descenantPair){
			System.out.println(pair.getFirst()+"***"+pair.getSecond());
		}
		System.out.println(disjointPairs.size()+"#"+descenantPair.size());
	}

	public static void main(String[] args) {
		
		int percent = 40;                //百分比
		//int[] univNum = new int[]{2, 4, 6, 8, 10};               //大学的数量
		int inconIndiNum = 300; //要添加的导致不一致的实例断言数
		
		NewInjectConflicts app = new NewInjectConflicts(percent, inconIndiNum);
		disjointTest(app);
		//app.go(univNum);		
		app.shutdown();
	}
}
