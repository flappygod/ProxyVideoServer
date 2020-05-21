package com.flappygo.proxyserver.ServerID;

import android.content.Context;
import android.content.SharedPreferences;

import com.flappygo.proxyserver.Tools.ToolString;

import java.util.List;
import java.util.UUID;

public class ServerIDManager {


    // 首选项名称
    public final static String PREFERENCENAME = "com.flappygo.videoproxy";

    public final static String URLLIST = "com.flappygo.videoproxy.urllist";

    //单例模式
    private static ServerIDManager serverID;

    //上下文
    private Context context;

    //构造器，防止创建
    private ServerIDManager() {

    }

    //服务ID
    private ServerIDManager(Context context) {
        this.context = context;
    }

    //服务器的ID
    public static ServerIDManager getInstance(Context context) {
        if (serverID == null) {
            synchronized (ServerIDManager.class) {
                if (serverID == null) {
                    serverID = new ServerIDManager(context.getApplicationContext());
                }
            }
        }
        return serverID;
    }


    //获取URL的服务ID
    public String generateUrlID(String url) {
        //获取
        SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
        //地址
        String ret = mSharedPreferences.getString(url, null);
        //如果之前没有这个
        if (ret == null) {
            //生成一个UUID
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(url, uuid);
            editor.commit();


            return uuid;
        }
        return ret;
    }

    //获取列表
    public List<String> getUrls() {
        synchronized (this) {
            //获取
            SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
            String str = mSharedPreferences.getString(URLLIST, null);
            List<String> retList = ToolString.splitStrList(str, ",");
            return retList;
        }
    }

    //设置列表
    public void setUrls(List<String> arrayList) {
        synchronized (this) {
            //获取
            SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putString(URLLIST, ToolString.strListToStr(arrayList, ","));
            editor.commit();
        }
    }

    //新增url
    public List<String> addUrl(String url) {
        synchronized (this) {
            //获取
            SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
            String str = mSharedPreferences.getString(URLLIST, null);
            List<String> retList = ToolString.splitStrList(str, ",");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            if (!retList.contains(url)) {
                retList.add(url);
            }
            editor.putString(URLLIST, ToolString.strListToStr(retList, ","));
            editor.commit();
            return retList;
        }
    }

    //移除url
    public List<String> removeUrl(String url) {
        synchronized (this) {
            //获取
            SharedPreferences mSharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
            String str = mSharedPreferences.getString(URLLIST, null);
            List<String> retList = ToolString.splitStrList(str, ",");
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            if (retList.contains(url)) {
                retList.remove(url);
            }
            editor.putString(URLLIST, ToolString.strListToStr(retList, ","));
            editor.commit();
            return retList;
        }
    }

}
