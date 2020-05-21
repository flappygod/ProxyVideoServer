package com.flappygo.proxyserver.ProxyServer.Models;

import java.io.Serializable;

//下载完成的保存数据
public class DownloadDoneModel implements Serializable {

    //下载
    private static final long serialVersionUID = 8137497282037389685L;

    //当前的uuid
    private String uuid;
    //当前的url
    private String url;
    //当前的segment总数
    private long totalSegment;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getTotalSegment() {
        return totalSegment;
    }

    public void setTotalSegment(long totalSegment) {
        this.totalSegment = totalSegment;
    }
}
