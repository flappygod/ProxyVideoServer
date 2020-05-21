package com.flappygo.proxyserver.ProxyServer.ServerHttp.Interface;

public interface ProxyServerHttpSegmentListener {

    //开始
    void  segmentProxyStart();

    //正常结束
    void  segmentProxyEnd();

    //非正常停止
    void  segmentProxyStoped();

}
