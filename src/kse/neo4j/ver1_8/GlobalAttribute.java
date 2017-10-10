package kse.neo4j.ver1_8;

import org.neo4j.graphdb.RelationshipType;

/**
 * 图数据库中需要用到的全局变量
 * @author Xuefeng Fu
 */

public class GlobalAttribute {
		/**
		 *  图数据库中，节点之间的关系，枚举类型.包含关系和成员关系 <br> 
		 *  The relationship between nodes of neo4j.		
		 */
		public static enum RelTypes implements RelationshipType {
			INCLUDEDBY, MEMBEROF
		} 
}
