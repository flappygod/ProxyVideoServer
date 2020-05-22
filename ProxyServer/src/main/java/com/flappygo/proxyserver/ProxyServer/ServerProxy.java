package com.flappygo.proxyserver.ProxyServer;

import com.flappygo.proxyserver.Config.ServerConfig;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class ServerProxy extends AsyncHttpServer {

    //单例
    private static ServerProxy instance;

    //单例模式
    private ServerProxy() {
        super();
    }

    //单例模式
    public static ServerProxy getInstance() {
        if (instance == null) {
            synchronized (ServerProxy.class) {
                if (instance == null) {
                    instance = new ServerProxy();
                    //监听
                    instance.listen(AsyncServer.getDefault(), ServerConfig.PORT);
                }
            }
        }
        return instance;
    }


    //添加子类
    public void addVideoChildProxy(String trueAction, HttpServerRequestCallback serverRequestCallback) {
        instance.get(trueAction, serverRequestCallback);
    }


    //添加
    public void addVideoProxy(String uuid, HttpServerRequestCallback serverRequestCallback) {
        instance.get("/" + uuid, serverRequestCallback);
    }

    //移除
    public void removeVideoProxy(String uuid) {
        instance.removeAction(AsyncHttpGet.METHOD, "/" + uuid);
    }

}