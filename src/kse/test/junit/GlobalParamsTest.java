package kse.test.junit;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GlobalParamsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGetOwlPath() {
		String owlFile = "owls/univ-bench.owl";
		String owlPath = "univ-bench.owl";
		String predictReturn = "owls/" + owlPath ;
		boolean result = owlFile.equals(predictReturn);
		//System.out.println(owlPath);		
		assertTrue(result);
	}

}
