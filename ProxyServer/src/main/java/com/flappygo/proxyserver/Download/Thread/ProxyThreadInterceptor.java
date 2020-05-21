package com.flappygo.proxyserver.Download.Thread;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


/**********
 *
 * Package Name:com.flappygo.lipo.limagegetter.threadpool <br/>
 * ClassName: ThreadInterceptor <br/> 
 * Function: 对线程进行横切    <br/> 
 * date: 2016-3-9 上午11:55:18 <br/> 
 * 
 * @author lijunlin
 */
public class ProxyThreadInterceptor implements InvocationHandler {

	// 被代理的对象
	private Object target;

	// 线程死亡的监听
	private ThreadListener mThreadListener;

	// 线程死亡的监听
	public interface ThreadListener {
		
		/****
		 * 线程死亡
		 * @param thread
		 */
		void death(Thread thread);

		/**********
		 * 线程开始执行
		 * @param thread
		 */
		void began(Thread thread);
	}

	// 设置线程死亡的监听
	public void setThreadListener(ThreadListener listener) {
		this.mThreadListener = listener;
	}

	public void setTarget(Object target) {
		this.target = target;
	}

	/******
	 * 
	 * 线程执行完毕
	 * 
	 * @param method
	 *            方法
	 * @param args
	 *            参数
	 */
	private void afterRun(Method method, Object[] args) {
		if (method.getName().equals("run")) {
			if (mThreadListener != null) {
				mThreadListener.death((Thread) target);
			}
		}
	}

	/**********
	 * 线程开始执行
	 * 
	 * @param method
	 *            方法
	 * @param args
	 *            参数
	 */
	private void beforeRun(Method method, Object[] args) {
		if (method.getName().equals("run")) {
			if (mThreadListener != null) {
				mThreadListener.began((Thread) target);
			}
		}
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		beforeRun(method, args);
		Object ob = method.invoke(target, args);
		afterRun(method, args);
		return ob;
	}
}
