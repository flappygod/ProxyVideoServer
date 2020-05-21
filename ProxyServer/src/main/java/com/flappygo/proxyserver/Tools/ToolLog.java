package com.flappygo.proxyserver.Tools;

import android.util.Log;

/**********
 * 手动横切了Log
 * ClassName: LLongUtil <br/> 
 * Function: TODO 功能说明 <br/> 
 * date: 2016-3-9 上午10:13:07 <br/> 
 * 
 * @author lijunlin
 */
public class ToolLog {
	
	//设置是否显示log
	public static boolean  showErrorLog=false;
	
	
	/*************
	 * 显示log
	 * @param tag  debugTag
	 * @param msg  显示msg
	 */
	public static void d(String tag, String msg){
		if(!showErrorLog){
			return ;
		}
		Log.d(tag, msg);
	}
	
	public static void d(String tag, String msg, Throwable tr){
		if(!showErrorLog){
			return ;
		}
		Log.d(tag, msg, tr);
	}
	
	public static void e(String tag, String msg){
		if(!showErrorLog){
			return ;
		}
		Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Throwable tr){
		if(!showErrorLog){
			return ;
		}
		Log.e(tag, msg, tr);
	}
	
	public static void i(String tag, String msg){
		if(!showErrorLog){
			return ;
		}
		Log.i(tag, msg);
	}
	
	public static void i(String tag, String msg, Throwable tr){
		if(!showErrorLog){
			return ;
		}
		Log.i(tag, msg, tr);
	}
	
	public static void w(String tag, String msg){
		if(!showErrorLog){
			return ;
		}
		Log.w(tag, msg);
	}
	
	public static void w(String tag, String msg, Throwable tr){
		if(!showErrorLog){
			return ;
		}
		Log.w(tag, msg, tr);
	}
	
	public static void w(String tag, Throwable tr){
		if(!showErrorLog){
			return ;
		}
		Log.w(tag, tr);
	}


}
