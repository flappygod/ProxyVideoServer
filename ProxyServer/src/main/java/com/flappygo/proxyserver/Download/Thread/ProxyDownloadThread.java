package com.flappygo.proxyserver.Download.Thread;

import com.flappygo.proxyserver.Download.Actor.DownLoadActor;

public class ProxyDownloadThread extends Thread {

    //下载器
    private DownLoadActor actor;

    //设置
    public ProxyDownloadThread(DownLoadActor actor) {
        this.actor = actor;
    }

    //当前的下载器
    public DownLoadActor getActor() {
        return actor;
    }

    //取消下载
    public void cancel() {
        actor.cancle();
    }

    //执行
    public void run() {
        //进行下载
        if (actor != null) {
            actor.excuteSync();
        }
    }

}
