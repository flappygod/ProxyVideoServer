package com.flappygo.proxyserver.Download.Thread;

public interface ProxyThreadPoolListener {

    //线程执行完毕就调用
    void threadDone(int remain, String threadID);

    //没有更多的线程在执行的时候调用
    void threadNomore();
}
