package com.flappygo.proxyserver.Interface;

import java.util.concurrent.ScheduledThreadPoolExecutor;


//代理服务需要实现的相应方法，便于取得数据
public interface ProxyServer {

    //获取远程服务的地址
    String getUrl();

    //获取当前服务的UUID
    String getUrlUUID();

    //获取保存的目录
    String getUrlDicotry();

    //获取当前的线程池
    ScheduledThreadPoolExecutor getPoolExecutor();

    //设置当前正在执行的分段
    void setPlayingSegmentPosition(int postion);

    //停止服务
    void stopServer();

    //当前是否停止
    boolean isStoped();

    //开始缓存数据
    void startCache(ProxyCacheListener listener);
}
