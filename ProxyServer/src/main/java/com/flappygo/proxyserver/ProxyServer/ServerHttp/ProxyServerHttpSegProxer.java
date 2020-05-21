package com.flappygo.proxyserver.ProxyServer.ServerHttp;

import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Interface.ProxyServerHttpSegmentListener;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import java.util.List;

public class ProxyServerHttpSegProxer {

    //服务
    private ProxyServer proxyServer;

    //列表
    private List<ProxyServerHttpSegment> segmentList;

    //开始的位置
    private volatile long startOffset;

    //开始的position
    private volatile int startPosition;

    //返回参数
    private AsyncHttpServerResponse response;

    //是否停止
    private boolean isStoped = false;

    //监听
    private ProxyServerHttpSegmentListener listener;

    //构造器
    public ProxyServerHttpSegProxer(ProxyServer proxyServer,
                                    List<ProxyServerHttpSegment> segments,
                                    final AsyncHttpServerResponse response,
                                    long start) {
        this.proxyServer = proxyServer;
        this.segmentList = segments;
        this.response = response;
        this.startOffset = start;
        this.listener = new ProxyServerHttpSegmentListener() {
            @Override
            public void segmentProxyStart() {

            }

            @Override
            public void segmentProxyEnd() {
                if (startPosition + 1 < segmentList.size()) {
                    //当前的position
                    startPosition = startPosition + 1;
                    //当前的position开始
                    startOffset = segmentList.get(startPosition).getDownLoadActor().getRangeStart();
                    //下一步
                    gotoNextSegment();
                } else {
                    stopProxy();
                }
            }

            @Override
            public void segmentProxyStoped() {
                stopProxy();
            }
        };
    }

    //开始执行
    public void proxy() {
        //遍历
        for (int s = 0; s < segmentList.size(); s++) {
            //获取分段服务代理
            ProxyServerHttpSegment segment = segmentList.get(s);
            //定位在哪个位置
            if (startOffset >= segment.getDownLoadActor().getRangeStart() &&
                    startOffset < segment.getDownLoadActor().getRangeStart() +
                            segment.getDownLoadActor().getRangeLength()) {
                //进入代理流程
                startPosition = s;
                //前往相应的片段才处理
                gotoNextSegment();
                //跳出循环
                return;
            }
        }
        //停止
        response.end();
    }


    //前往下一个片段
    private void gotoNextSegment() {

        new Thread() {
            public void run() {

                if (isStoped == false) {

                    //通知正在播放哪个片段
                    proxyServer.setPlayingSegmentPosition(startPosition);

                    //获取片段
                    ProxyServerHttpSegment segment = segmentList.get(startPosition);

                    //开始执行这个片段的操作
                    segment.proxy(startOffset, listener);
                }

            }
        }.start();

    }

    //停止
    public void stopProxy() {
        //结束标志
        isStoped = true;
        //结束响应
        response.end();
    }

}
