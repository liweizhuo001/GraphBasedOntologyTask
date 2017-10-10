package kse.misc.running;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.semanticweb.owlapi.model.IRI;


/**
 * 一些java函数的测试
 * @author Xuefeng Fu
 *
 */
public class JavaUsingDemo {
	/**
	 * 测试集合中是否会有相同的元素，结果是不会添加相同的元素
	 */
	@SuppressWarnings("unchecked")
	public static void setTest(){
		Set<String> forTest = new HashSet<String>();
		forTest.add("Str1");
		forTest.add("Str2");
		forTest.add("Str3");
		forTest.add("Str9");
		//forTest.add("Str1");  //集合不会添加重复项
		
		/*for(String s : forTest){
			System.out.println(s);
		}*/
		Set<String> forTest2 = new HashSet<String>();
		forTest2.add("Str8");
		forTest2.add("Str1");
		forTest2.add("Str3");
		forTest2.add("Str9");
		forTest2.add("Str91");

		
		/*forTest2.addAll(forTest);		
		System.out.println(forTest2);
		
		forTest.retainAll(forTest2);			
		System.out.println(forTest);	*/
		
		/*Collection<String> union = CollectionUtils.union(forTest, forTest2);
		for(String s : union)
			System.out.print(s+"  ");*/
		
		Collection<String> interset = CollectionUtils.intersection(forTest, forTest2);
		for(String s : interset)
			System.out.print(s+"  ");
		
	}
	
	public static void apacheCollectionTest(){
		
	}
	
	public static void test3(){
		Set<Integer> forTest = new HashSet<Integer>();
		forTest.add(new Integer(1));
		forTest.add(new Integer(2));
		if(forTest.contains(new Integer(2)))
			System.out.println("YES");
	}
	
	public static void randomTest(){
		Random random = new Random();
		int seed = 100;
		for(int i=0;i<10;i++){
			System.out.print(random.nextInt(seed)+"#");
		}
	}
	
	public static void StringTest(){
		String t = "http://www.Department0.University0.edu/FullProfessor4/Publication16";
		String rt = t.replaceAll("http://", "");
		System.out.println(rt);
	}
	
	public static void IRITest(){
		IRI iri;
		iri = IRI.create("http://purl.obolibrary.org/obo/FBbt_00003883");
		System.out.println(iri.getFragment());
		System.out.println(iri.getScheme());
		System.out.println(iri.getStart());
		System.out.println(iri.toURI().getFragment());
	}
	public static void main(String[] args){
		//StringTest();
		//IRITest();
		//setTest();
		 System.out.println(System.getProperty("java.library.path"));
	}
	
	
}
