package kse.algorithm.forTBox.debugging.scoring;
import org.neo4j.graphdb.Relationship;
/**
 * 为了便于排序，记录关系(对应本体中的公理)在MIPP中出现频率
 * 2015-5-30 !obsolete
 * @author Xuefeng Fu
 *
 */
public class RelFreqInMIPPs implements Comparable<RelFreqInMIPPs>{
	Relationship rel;
	int frequency;
	
	public Relationship getRel() {
		return rel;
	}

	public void setRel(Relationship rel) {
		this.rel = rel;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public RelFreqInMIPPs(Relationship r, int f){
		this.rel = r;
		this.frequency = f;
	}
	 
	@Override
	public int compareTo(RelFreqInMIPPs obj) { //降序
		return  obj.getFrequency() -frequency ;
	}
	
	@Override
	public String toString(){
		return rel+":" +frequency;
	}
}
