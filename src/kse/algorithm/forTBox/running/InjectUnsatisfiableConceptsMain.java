package kse.algorithm.forTBox.running;

import static kse.misc.Tools.getOWLsOfExperimentation;
import static kse.misc.Tools.getPrefixes;

import java.util.List;

import kse.algorithm.forTBox.preprocessing.InjectUnsatisfiableConcepts;
import kse.misc.Timekeeping;
/**
 * 测试，向一个存在不相交公理的本体中，注入不可满足概念
 * @author Xuefeng Fu
 *
 */
public class InjectUnsatisfiableConceptsMain {
	
	public static void main(String[] args){
		String owlFormatter = "owls/Original-2/%s.owl";
		List<String> owls = getOWLsOfExperimentation();
		List<String> prefixs = getPrefixes();
		int index = 10;  //处理的是Go本体
		int ucNumber = 103;                                                               //不可满足概念数
		InjectUnsatisfiableConcepts.MAXRECURSION = 10;     //父类递归的层数，越大生成的关系越复杂
		//boolean isClear = true;
		
		String owl = owls.get(index);
		String owlPath = String.format(owlFormatter, owl);
		String dbPath = String.format("neo4j-db-3/%s", owl);
		String prefix = prefixs.get(index);
		
		Timekeeping tk = Timekeeping.getTimekeeping();
		
		InjectUnsatisfiableConcepts app = new InjectUnsatisfiableConcepts(owlPath, dbPath, ucNumber,prefix);
		app.generalUc();
		tk.finish();
	}

}
