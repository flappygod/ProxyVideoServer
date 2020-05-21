package com.flappygo.proxyserver.ProxyServer.Serverm3u8.Request;

import com.flappygo.proxyserver.Config.HttpConfig;
import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

//M3u8缓存的单个请求处理
public class M3u8RequestCached {

    //父类
    private ProxyServer parent;

    //请求
    private AsyncHttpServerRequest request;

    //返回消息
    private AsyncHttpServerResponse response;

    //下载器
    private DownLoadActor downLoadActor;


    //请求开始的offset
    private long rangeStart = 0;

    //请求结束的offset
    private long rangeStopd = 0;

    //当前是否暂停
    private boolean awaitFlag = false;

    //M3u8缓存的单个请求处理
    public M3u8RequestCached(ProxyServer parent,
                             AsyncHttpServerRequest request,
                             AsyncHttpServerResponse response,
                             DownLoadActor actor) {
        this.parent = parent;
        this.request = request;
        this.response = response;
        this.downLoadActor = actor;
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

        //初始化头部信息
        boolean containRange = initHeaders();

        //请求成功
        if (containRange) {
            response.code(HttpConfig.NET_SUCCESS_PART);
        } else {
            response.code(HttpConfig.NET_SUCCESS);
        }

        //获取文件的长度
        long contentLength = downLoadActor.getDownloadedSize();

        //响应文件
        HashMap retMap = new HashMap();

        //返回当前的Range
        List<String> bytes = new ArrayList<>();
        bytes.add("bytes " + rangeStart + "-" + (contentLength - 1) + "/" + contentLength);
        retMap.put("Content-Range", bytes);

        //返回当前的大小
        List<String> datas = new ArrayList<>();
        datas.add(Long.toString(contentLength - rangeStart));
        retMap.put("Content-Length", datas);

        //设置
        response.getHeaders().addAll(retMap);
    }

    //初始化头部信息，主要是拿出开始和结束
    private boolean initHeaders() {
        boolean containRnage = false;
        //获取headers
        Multimap headers = request.getHeaders().getMultiMap();
        //获取key
        Iterator headersIterator = headers.keySet().iterator();
        //默认从零开始
        rangeStart = 0;
        //取出请求的参数
        while (headersIterator.hasNext()) {
            //获取请求中的key
            String key = (String) headersIterator.next();
            //获取请求中的value
            String value = headers.getString(key);
            //vlc过来的奇葩请求Range
            if (key.toLowerCase().equals("range")) {
                //包含range
                containRnage = true;
                //获取到的Range
                try {
                    //开始的位置
                    String start = value.replace("bytes=", "").split("-")[0];
                    //跳过多少
                    rangeStart = Long.parseLong(start);
                } catch (Exception ex) {
                    //从零开始
                    rangeStart = 0;
                }
                //获取到end
                try {
                    //开始的位置
                    String start = value.replace("bytes=", "").split("-")[1];
                    //跳过多少
                    rangeStopd = Long.parseLong(start);
                } catch (Exception ex) {
                    //从零开始
                    rangeStopd = 0;
                }
            }
        }
        return containRnage;
    }

    //进入本地缓存环节
    public void doResponseCache() {
        //返回给本地
        try {
            //创建
            FileInputStream inputStream = new FileInputStream(downLoadActor.getFileAbsolutePath());
            //文件
            if (rangeStart != 0) {
                //跳过这么多
                inputStream.skip(rangeStart);
            }
            //缓存大小
            byte[] buffer = new byte[1024];
            //长度
            int len = 0;
            //循环读取
            while ((len = inputStream.read(buffer)) != -1) {
                //如果已经停止了，就跳出循环，停止处理
                if (parent.isStoped()) {
                    //结束了
                    response.end();
                    break;
                }
                //等待
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
                    rangeStart += len;
                    //等待
                    if (bufferList.remaining() > 0) {
                        //offset
                        rangeStart -= bufferList.remaining();
                        //进入等待模式
                        awaitFlag = true;
                        //跳出
                        break;
                    }
                }
            }
            //关闭连接
            inputStream.close();
            //如果读取完毕了，那么就结束了
            if (len == -1) {
                response.end();
            }
        } catch (Exception ex) {
            response.end();
        }
    }


}
