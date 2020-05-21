package com.flappygo.proxyserver.ServerPath;

import android.content.Context;

public class ServerPathManager {

    //当前的目录地址
    private String defaultDirpath;

    //上下文
    private Context context;

    //使用单例模式
    private static ServerPathManager instance;

    //获取单例
    public static ServerPathManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ServerPathManager.class) {
                if (instance == null) {
                    instance = new ServerPathManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    //构造器
    public ServerPathManager(Context context) {
        this.context = context;
    }


    //获取默认的保存地址
    public String getDefaultDirPath() {
        if (defaultDirpath == null) {
            synchronized (this) {
                try {
                    if (context.getExternalCacheDir() != null) {
                        defaultDirpath = context.getExternalCacheDir().getPath() + "/proxServerCache/";
                    } else if (context.getCacheDir() != null) {
                        defaultDirpath = context.getCacheDir().getPath() + "/proxServerCache/";
                    } else {
                        defaultDirpath = "/proxServerCache/";
                    }
                } catch (Exception e) {
                    defaultDirpath = "/proxServerCache/";
                }
                return defaultDirpath;
            }
        }
        return defaultDirpath;
    }


    //获取默认的UUID缓存地址
    public String getDefaultCachePath(String uuid) {
        return getDefaultDirPath() + uuid + "/";
    }


}
