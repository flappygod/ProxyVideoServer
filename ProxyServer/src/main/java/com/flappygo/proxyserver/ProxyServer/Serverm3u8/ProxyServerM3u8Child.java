package com.flappygo.proxyserver.ProxyServer.Serverm3u8;

import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.Download.Thread.ProxyDownloadThread;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.ServerProxy;
import com.flappygo.proxyserver.ProxyServer.Serverm3u8.Request.M3u8RequestCached;
import com.flappygo.proxyserver.ProxyServer.Serverm3u8.Request.M3u8RequestNetwork;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;


//子文件的M3u8请求
public class ProxyServerM3u8Child implements HttpServerRequestCallback {

    //代理服务
    private ProxyServer proxyServer;

    //真实
    private String trueAction;

    //下载器
    private DownLoadActor downLoadActor;

    //当前的m3u8是否是在线视频
    private boolean isAlive;


    //真实地址
    public ProxyServerM3u8Child(ProxyServer proxyServer,
                                String trueAction,
                                DownLoadActor actor,
                                boolean isAlive) {
        this.proxyServer = proxyServer;
        this.trueAction = trueAction;
        this.downLoadActor = actor;
        this.isAlive = isAlive;
        //添加action
        ServerProxy.getInstance().addVideoChildProxy(trueAction, this);
    }

    //设置actor
    public DownLoadActor getDownLoadActor() {
        return downLoadActor;
    }

    @Override
    public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
        //设置当前正在被请求的分段
        proxyServer.setPlayingSegmentPosition(downLoadActor.actorTag);

        //如果是直播的情况，我们只需要做个代理就好了，完全不需要做其他的处理
        if (isAlive) {
            new M3u8RequestNetwork(proxyServer, request, response, downLoadActor).doResponseNet();
            return;
        }

        //如果当前下载器已经下载完成了
        if (downLoadActor.isDownloaded()) {
            //发起一次请求
            new M3u8RequestCached(proxyServer, request, response, downLoadActor).doResponseCache();
        } else {
            //直接走网络代理
            new M3u8RequestNetwork(proxyServer, request, response, downLoadActor).doResponseNet();
        }

        //如果没有下载,但是之前有没有下载完成,那么就加入到线程池中，继续下载,除开直播
        if (!isAlive && !downLoadActor.isLoading()) {
            //创建下载线程
            ProxyDownloadThread thread = new ProxyDownloadThread(downLoadActor);
            //在线程池中执行
            if (proxyServer.getPoolExecutor() != null) {
                proxyServer.getPoolExecutor().execute(thread);
            }
        }
    }

    //移除
    public void stop() {
        ServerProxy.getInstance().removeVideoChildProxy(trueAction);
    }


}
