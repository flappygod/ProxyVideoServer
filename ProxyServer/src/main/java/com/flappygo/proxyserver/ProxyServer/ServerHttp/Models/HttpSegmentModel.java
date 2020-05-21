package com.flappygo.proxyserver.ProxyServer.ServerHttp.Models;

public class HttpSegmentModel {

    //真实的地址
    private String url;

    //开始的地址
    private long start;

    //停止的地址
    private long length;

    //当前的偏移量
    private long offset;

    //当前的分段序号
    private long segPostion;

    //当前的分段是否是最后一个分段
    private boolean isLast;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }


    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public long getSegPostion() {
        return segPostion;
    }

    public void setSegPostion(long segPostion) {
        this.segPostion = segPostion;
    }

    public String getSegmentName() {
        return "segmentData" + getSegPostion();
    }
}
