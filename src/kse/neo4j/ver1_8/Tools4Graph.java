package kse.neo4j.ver1_8;

import java.io.File;
import java.io.IOException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.util.FileUtils;

/**
 * 图数据库操作中要用到的函数
 * @author Xuefeng Fu 2014-3-6
  */

public class Tools4Graph {
	
	/**
	 * 如果虚拟机没有正常退出，也会关闭图数据库
	 * @param graphDb  图数据库
	 */
	
	public static void registerShutdownHook(final GraphDatabaseService graphDb) {  		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
	
	/**
	 * 清空指定路径的图数据库数据
	 * @param dbPath     要清除的图数据库路径
	 */
	
	public static  void clearDb(String dbPath) {
		System.out.println("Clearing Graph Data on " + dbPath);
		try {
			FileUtils.deleteRecursively(new File(dbPath));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}	
	
	/**
	 * 图中关系的名称，便于索引
	 * @param sub   子类
	 * @param sup 	父类
	 * @return	sub_sup
	 */
	public static String getRelationshipName(String sub, String sup){
		return String.format("%s***%s", sub,sup);
	}
	
	/**
	 * 实例断言的命名，如A(x) return x***A
	 * @param individualSign 实例的名字
	 * @param sup 类别的名字
	 * @return
	 */
	public static String getIndividualRelName(String individualSign,String sup){
		return String.format("%s***%s", individualSign.replaceAll("http://", ""),sup); 
	}
}
