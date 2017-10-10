package kse.test.junit;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import kse.algorithm.auxiliaryClass.IndividualConceptPair;

import org.junit.Test;

public class IndividualConceptPairTest {

	@Test	
	public void testEquals() {
		String i1 = "abc";
		String c1 = "ddd";
		IndividualConceptPair ic1 = new IndividualConceptPair(i1,c1);
		IndividualConceptPair ic2 = new IndividualConceptPair(i1,c1);
		
		//boolean result = (ic1 == ic2);
		boolean result = ic1.equals(ic2);		
		//System.out.print(result);
		assertTrue(result);
	}
	@Test
	public void testSet(){
		Set<IndividualConceptPair> icps = new HashSet<IndividualConceptPair>();
		String i1 = "abc";
		String c1 = "ddd";
		IndividualConceptPair ic1 = new IndividualConceptPair(i1,c1);
		IndividualConceptPair ic2 = new IndividualConceptPair(i1,c1);
		IndividualConceptPair ic3 = new IndividualConceptPair(i1,c1);
		icps.add(ic1);
		icps.add(ic2);		
		//boolean result = (icps.size() == 2);
		//System.out.println(icps.size());
		//System.out.print(result);
		
		assertTrue(icps.contains(ic3));
	}

}
