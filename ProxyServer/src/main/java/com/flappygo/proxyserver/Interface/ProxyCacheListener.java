package com.flappygo.proxyserver.Interface;

public interface ProxyCacheListener {

    //缓存进度
    void cachedProgress(int progress);

    //缓存全部完成
    void cachedSuccess();

    //监听
    void cachedStoped();

}
