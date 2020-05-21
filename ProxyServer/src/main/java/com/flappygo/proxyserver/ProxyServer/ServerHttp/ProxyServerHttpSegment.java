package com.flappygo.proxyserver.ProxyServer.ServerHttp;

import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Interface.ProxyServerHttpSegmentListener;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Request.HttpRequestCached;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Request.HttpRequestNetwork;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import java.util.HashMap;


//子处理
public class ProxyServerHttpSegment {

    //代理服务
    private ProxyServer proxyServer;

    //下载器
    private DownLoadActor downLoadActor;

    //头部信息
    private HashMap<String, String> headers;

    //分段用于返回数据的response
    private AsyncHttpServerResponse response;

    //获取下载器
    public DownLoadActor getDownLoadActor() {
        return downLoadActor;
    }

    //当前的子类
    public ProxyServerHttpSegment(ProxyServer proxyServer,
                                  HashMap<String, String> headers,
                                  DownLoadActor downLoadActor,
                                  AsyncHttpServerResponse response) {
        this.proxyServer = proxyServer;
        this.headers = headers;
        this.downLoadActor = downLoadActor;
        this.response = response;
    }

    //代理
    public void proxy(final long startOffset,
                      final ProxyServerHttpSegmentListener listener) {

        //如果该分段缓存文件不是正在下载的状态，而且当前已经下载完毕了
        if (downLoadActor.isDownloaded()) {
            //取得offset
            long offset = startOffset - downLoadActor.getRangeStart();
            //那么这里就走缓存的流程
            new HttpRequestCached(proxyServer,
                    response,
                    listener,
                    downLoadActor.getFileAbsolutePath(),
                    offset).doResponseCache();
        } else {
            //长度
            long length = (downLoadActor.getRangeStart() + downLoadActor.getRangeLength() - startOffset);
            //那么这里就走网络的流程
            new HttpRequestNetwork(proxyServer,
                    headers,
                    response,
                    listener,
                    downLoadActor.getDownLoadUrl(),
                    startOffset,
                    length
            ).doResponseNet();
        }
    }

}
