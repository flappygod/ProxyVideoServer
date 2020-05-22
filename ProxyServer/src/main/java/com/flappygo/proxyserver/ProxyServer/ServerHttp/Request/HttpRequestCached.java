package com.flappygo.proxyserver.ProxyServer.ServerHttp.Request;

import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Interface.ProxyServerHttpSegmentListener;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import java.io.FileInputStream;

//缓存的单个请求处理
public class HttpRequestCached {

    //父类
    private ProxyServer proxyServer;

    //返回消息
    private AsyncHttpServerResponse response;

    //下载完成后的地址
    private String path;

    //开始播放的位置
    private long startOffset;

    //当前是否暂停
    private boolean awaitFlag = false;

    //监听
    private ProxyServerHttpSegmentListener listener;

    //缓存
    public HttpRequestCached(ProxyServer parent,
                             AsyncHttpServerResponse res,
                             ProxyServerHttpSegmentListener listener,
                             String path,
                             long startOffset) {
        this.startOffset = startOffset;
        this.response = res;
        this.listener = listener;
        this.proxyServer = parent;
        this.path = path;
        this.response.setWriteableCallback(new WritableCallback() {
            @Override
            public void onWriteable() {
                //如果正在等待
                if (awaitFlag == true) {
                    //停止等待
                    awaitFlag = false;
                    //继续写入数据
                    doResponseCache();
                }
            }
        });
    }

    //进入本地缓存环节
    public void doResponseCache() {
        //返回给本地
        try {
            //创建
            FileInputStream inputStream = new FileInputStream(path);
            //跳过这么多
            inputStream.skip(startOffset);
            //缓存大小
            byte[] buffer = new byte[1024];
            //长度
            int len = 0;
            //循环读取
            while ((len = inputStream.read(buffer)) != -1) {

                //整个服务已经关闭了，停止
                if (proxyServer.isStoped()) {
                    //结束了
                    if (listener != null) {
                        listener.segmentProxyStoped();
                    }
                    break;
                }

                //等待flag
                if (awaitFlag == false) {
                    //创建
                    byte[] proxByte = new byte[len];
                    //内存拷贝
                    System.arraycopy(buffer, 0, proxByte, 0, len);
                    //写入数据
                    ByteBufferList bufferList = new ByteBufferList(proxByte);
                    //写入进去
                    response.write(bufferList);
                    //写入了多少
                    startOffset += len;
                    //等待
                    if (bufferList.remaining() > 0) {
                        //offset
                        startOffset -= bufferList.remaining();
                        //没有写进去，然后继续
                        awaitFlag = true;
                        //跳出
                        break;
                    }
                }
            }
            //关闭连接
            inputStream.close();
            //如果是正常结束
            if (len == -1) {
                //成功结束
                if (listener != null) {
                    listener.segmentProxyEnd();
                }
            }
        } catch (Exception ex) {
            //意外结束
            if (listener != null) {
                listener.segmentProxyStoped();
            }
        }
    }


}
