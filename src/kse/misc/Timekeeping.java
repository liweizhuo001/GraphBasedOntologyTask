package kse.misc;

import java.util.Date;

public class Timekeeping {
	private static Date start;
	private static Date finished;
	private static String operate="%s have costed %f seconds.";
	public static Date getFinished() {
		return finished;
	}
	public static void setFinished(Date finished) {
		Timekeeping.finished = finished;
	}
	public static float getSeconds() {
		return seconds;
	}
	public static long getMillisecond() {
		return millisecond;
	}
	private static float seconds;
	private static long millisecond;
	public static void begin(){
		start = new Date();
	}
	public static void end(){
		finished = new Date();
		millisecond = finished.getTime() - start.getTime();
		seconds = millisecond/1000.0f;		
	}
	public static String infomation;
	public static void showInfo(String message){
		infomation = String.format(operate, message,seconds);
		System.out.println(infomation);
	}
	
	/**
	 * NO Static method
	 */
	private Date _start;
	private Date _end;
	private  float _seconds;
	
	private Timekeeping(){
		this._start = new Date();
	}	
	public static Timekeeping getTimekeeping(){
		return new Timekeeping();
	}	
	public void finish(){
		_end = new Date();
		float _millisecond = _end.getTime() - _start.getTime();
		_seconds = _millisecond/1000.0f;	
		System.out.println(String.format(operate, "Running",_seconds));
	}	
	
	public void finish(String msg){
		_end = new Date();
		float _millisecond = _end.getTime() - _start.getTime();
		_seconds = _millisecond/1000.0f;	
		System.out.println(String.format(operate, msg, _seconds));
	}	
	
	public float getRunningTime(){
		return _seconds; 
	}	
}




