package kse.test;

public class ForTest  implements Comparable<ForTest> {
		int first;
		int second;
		String flag;
		public int getFirst() {
			return first;
		}
		public int getSecond() {
			return second;
		}
		public ForTest(String flag, int f, int s){
			this.flag = flag;
			this.first = f;
			this.second = s;
		}
		
		@Override
		public int compareTo(ForTest obj) { 
			int v=0;
			if(first !=obj.getFirst()){  
				v = obj.getFirst() - first; //降序
			}
			else{
				v =second - obj.getSecond() ; //升序
			}
			return v;
		}	
		@Override
		public String toString(){
			return flag + ":" + first + "#" + second;		
		}
}
