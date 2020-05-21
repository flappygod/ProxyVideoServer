package com.flappygo.proxyserver.Config;

public class ServerConfig {

    //重试的时间，默认两分钟
    public static long NETWORK_RETRY_TIME = 2 * 60 * 1000;

    //设置分片暂时为两兆
    public static long FILE_SEGMENHT_SIZE = 1024 * 1024 * 2;

    //线程池的大小
    public static int THREAD_POOL_SIZE = 10;


}
