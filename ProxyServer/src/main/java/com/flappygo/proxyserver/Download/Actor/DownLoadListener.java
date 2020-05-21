package com.flappygo.proxyserver.Download.Actor;


/***************
 * 下载的监听
 */
public interface DownLoadListener {
	
	/*******
	 * 下载成功  
	 * @param path  下载成功保存到的路径
	 */
	void downLoadSuccess(String path, String name);
	
	/********
	 * 下载中
	 * @param progress 下载的进度
	 */
	void downLoading(int progress);
	
	
	/**************
	 * 下载失败
	 * @param e 下载失败的原因
	 */
	void downloadError(Exception e);
	
	/*************
	 * 下载被取消
	 */
	void downloadCancled();

}
