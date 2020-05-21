package com.flappygo.proxyserver;


import android.content.Context;
import android.os.Environment;

import com.flappygo.proxyserver.Interface.ProxyCacheListener;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.ProxyServerHttp;
import com.flappygo.proxyserver.ProxyServer.Serverm3u8.ProxyServerM3u8;
import com.flappygo.proxyserver.ServerID.ServerIDManager;
import com.flappygo.proxyserver.ServerPath.ServerPathManager;
import com.flappygo.proxyserver.Tools.ToolDirs;
import com.koushikdutta.async.AsyncServer;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

//服务
public class FlappyProxyServer {

    //端口号
    public static int PORT = 15688;

    //当前的context
    private Context context;

    //服务
    ConcurrentHashMap<String, ProxyServer> proxyServer = new ConcurrentHashMap<String, ProxyServer>();

    //单例模式
    private static FlappyProxyServer instance;


    //单例模式
    private FlappyProxyServer(Context context) {
        this.context = context;
    }

    //单例模式
    public static FlappyProxyServer getInstance(Context context) {
        if (instance == null) {
            synchronized (FlappyProxyServer.class) {
                if (instance == null) {
                    instance = new FlappyProxyServer(context);
                }
            }
        }
        return instance;
    }


    //获取本地服务器的地址
    public static String getLocalServerUrl() {
        return "http://127.0.0.1:" + FlappyProxyServer.PORT + "/";
    }


    //代理地址
    public String proxyStart(String url) {
        //判空处理
        if (url == null) {
            return "";
        }

        //不是http协议的不做处理
        if (!url.startsWith("http")) {
            return url;
        }

        //检查SD卡是否满了，如果满了就清理缓存
        checkSDCard();

        //获取这个URL所对应的UUID
        String uuid = ServerIDManager.getInstance(context).generateUrlID(url);

        //新增被代理的URL
        ServerIDManager.getInstance(context).addUrl(url);

        //如果当前已经存在服务对齐镜像
        if (getRunningServer(url) != null) {
            return getLocalServerUrl() + uuid;
        }

        //如果是M3u8进入M3U8的处理方式
        if (url.toLowerCase().endsWith("m3u8")) {
            //服务地址
            ProxyServerM3u8 server = new ProxyServerM3u8(context.getApplicationContext(), uuid, url);
            //监听
            server.listen(AsyncServer.getDefault(), PORT);
            //添加
            addServer(uuid, server);
            //返回的实际请求地址
            return getLocalServerUrl() + uuid;
        } else {
            //服务地址
            ProxyServerHttp server = new ProxyServerHttp(context.getApplicationContext(), uuid, url);
            //监听
            server.listen(AsyncServer.getDefault(), PORT);
            //添加
            addServer(uuid, server);
            //返回的实际请求地址
            return getLocalServerUrl() + uuid;
        }
    }

    //检查SD卡
    private void checkSDCard() {
        //大小
        long size = ToolDirs.getSDAvailableSize();

        //如果小于5个G
        if (size < 1024 * 5) {
            //清理缓存文件
            cleanAll();
        }
    }

    //获取缓存的路径
    public String getCacheDictionary() {
        return ServerPathManager.getInstance(context).getDefaultDirPath();
    }

    //停止代理
    public boolean proxyStop(String url) {
        //地址
        synchronized (proxyServer) {
            Iterator iterator = proxyServer.keySet().iterator();
            while (iterator.hasNext()) {
                //遍历
                String key = (String) iterator.next();
                //获取代理服务
                ProxyServer server = proxyServer.get(key);
                //如果是针对这个地址的代理服务
                if (server.getUrl().equals(url)) {
                    //停止
                    server.stopServer();
                    //移除
                    proxyServer.remove(key);
                    //返回成功
                    return true;
                }
            }
        }
        return false;
    }


    //缓存
    public String proxyCache(String url,
                             ProxyCacheListener listener) {
        //判空处理
        if (url == null) {
            return "";
        }

        //不是http协议的不做处理
        if (!url.startsWith("http")) {
            return url;
        }

        checkSDCard();

        //获取这个URL所对应的UUID
        String uuid = ServerIDManager.getInstance(context).generateUrlID(url);

        //新增被代理的URL
        ServerIDManager.getInstance(context).addUrl(url);

        //获取当前正在运行的服务
        ProxyServer runningServer = getRunningServer(url);
        //如果当前已经存在服务对齐镜像
        if (runningServer != null) {
            //开始缓存
            runningServer.startCache(listener);
        }

        //如果是M3u8进入M3U8的处理方式
        if (url.toLowerCase().endsWith("m3u8")) {
            //服务地址
            ProxyServerM3u8 server = new ProxyServerM3u8(context.getApplicationContext(), uuid, url);
            //监听
            server.listen(AsyncServer.getDefault(), PORT);
            //添加
            addServer(uuid, server);
            //开始缓存
            server.startCache(listener);
            //返回的实际请求地址
            return getLocalServerUrl() + uuid;
        } else {
            //服务地址
            ProxyServerHttp server = new ProxyServerHttp(context.getApplicationContext(), uuid, url);
            //监听
            server.listen(AsyncServer.getDefault(), PORT);
            //添加
            addServer(uuid, server);
            //开始缓存
            server.startCache(listener);
            //返回的实际请求地址
            return getLocalServerUrl() + uuid;
        }
    }

    //清理当前的Proxy
    public boolean cleanProxy(String url) {
        //判空处理
        if (url == null) {
            return false;
        }

        //不是http协议的不做处理
        if (!url.startsWith("http")) {
            return true;
        }

        //获取这个URL所对应的UUID
        String uuid = ServerIDManager.getInstance(context).generateUrlID(url);

        //获取UUID的缓存地址
        String path = ServerPathManager.getInstance(context).getDefaultCachePath(uuid);

        //移除
        ServerIDManager.getInstance(context).removeUrl(url);

        //返回
        try {
            ToolDirs.deleteDirFiles(path);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    //清理所有的
    public boolean cleanAll() {
        List<String> path = ServerIDManager.getInstance(context).getUrls();
        for (int s = 0; s < path.size(); s++) {
            boolean flag = cleanProxy(path.get(s));
            if (flag == false) {
                return false;
            }
        }
        return true;
    }

    //获取被代理过的url
    public List<String> getProxyUrls() {
        List<String> path = ServerIDManager.getInstance(context).getUrls();
        return path;
    }


    //获取当前正在runnin的服务
    private ProxyServer getRunningServer(String url) {
        synchronized (proxyServer) {
            Iterator iterator = proxyServer.keySet().iterator();
            //遍历
            while (iterator.hasNext()) {
                //创建
                String key = (String) iterator.next();
                //获取服务
                ProxyServer server = proxyServer.get(key);
                //如果已经存在
                if (server.getUrl().equals(url)) {
                    return server;
                }
            }
            return null;
        }
    }

    //添加server
    private void addServer(String key, ProxyServer server) {
        synchronized (proxyServer) {
            proxyServer.put(key, server);
        }
    }

    //获取server
    private void getServer(String key) {
        synchronized (proxyServer) {
            proxyServer.get(key);
        }
    }

    //移除
    private void removeServer(String key) {
        synchronized (proxyServer) {
            proxyServer.remove(key);
        }
    }


}
