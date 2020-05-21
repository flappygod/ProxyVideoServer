package com.flappygo.proxyserver.Exception;


/**************
 *
 * Package Name:com.flappygo.lipo.limagegetter.exception <br/>
 * ClassName: LDirException <br/> 
 * Function: 文件夹操作报错 <br/> 
 * date: 2016-3-9 上午10:27:50 <br/> 
 * 
 * @author lijunlin
 */
public class LDirException extends Exception {

	
	private static final long serialVersionUID = 1L;
	
	//报错的地址
	private String dirPath;
	
	
	/*******
	 * 
	 * @param dirPath 文件夹地址
	 */
    public LDirException(String dirPath) {
    	this.dirPath=dirPath;
    }

  
    public LDirException(String dirPath, String detailMessage) {
        super(detailMessage);
    	this.dirPath=dirPath;
    }

    
    public LDirException(String dirPath, String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    	this.dirPath=dirPath;
    }

    
    public LDirException(String dirPath, Throwable throwable) {
        super(throwable);
    	this.dirPath=dirPath;
    }


    /******
     * 获取报错的地址
     * @return
     */
	public String getDirPath() {
		return dirPath;
	}


	/******
	 * 设置dirPath
	 * @param dirPath
	 */
	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	} 
	
    
   

}
