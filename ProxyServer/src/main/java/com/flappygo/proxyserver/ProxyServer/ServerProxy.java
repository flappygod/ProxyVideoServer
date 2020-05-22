package com.flappygo.proxyserver.ProxyServer;

import com.flappygo.proxyserver.Config.ServerConfig;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.AsyncHttpGet;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.util.ArrayList;
import java.util.List;

public class ServerProxy extends AsyncHttpServer {

    //移除
    private List<String> addedStrs = new ArrayList<>();

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
        synchronized (addedStrs) {
            if (!addedStrs.contains(trueAction)) {
                addedStrs.add(trueAction);
                instance.addAction(AsyncHttpGet.METHOD, trueAction, serverRequestCallback);
            }
        }
    }

    //移除子类
    public void removeVideoChildProxy(String trueAction) {
        synchronized (addedStrs) {
            if (addedStrs.contains(trueAction)) {
                addedStrs.remove(trueAction);
                instance.removeAction(AsyncHttpGet.METHOD, trueAction);
            }
        }
    }

    //添加
    public void addVideoProxy(String actionID, HttpServerRequestCallback serverRequestCallback) {
        synchronized (addedStrs) {
            if (!addedStrs.contains(actionID)) {
                addedStrs.add(actionID);
                instance.addAction(AsyncHttpGet.METHOD, "/" + actionID, serverRequestCallback);
            }
        }
    }

    //移除
    public void removeVideoProxy(String actionID) {
        synchronized (addedStrs) {
            if (addedStrs.contains(actionID)) {
                addedStrs.remove(actionID);
                instance.removeAction(AsyncHttpGet.METHOD, "/" + actionID);
            }
        }
    }

}
