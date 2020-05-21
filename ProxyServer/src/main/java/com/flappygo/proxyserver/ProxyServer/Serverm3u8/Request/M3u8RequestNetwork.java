package com.flappygo.proxyserver.ProxyServer.Serverm3u8.Request;

import com.flappygo.proxyserver.Config.ServerConfig;
import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//网络请求开始
public class M3u8RequestNetwork {

    //父类
    private ProxyServer parent;
    //请求
    private AsyncHttpServerRequest request;
    //返回消息
    private AsyncHttpServerResponse response;
    //下载器
    private DownLoadActor downLoadActor;


    //重试的时间
    private long retryTime = 0;
    //请求开始的offset
    private long rangeStart = 0;
    //请求结束的offset
    private long rangeStopd = 0;

    //当前是否暂停
    private boolean awaitFlag = false;

    //头部信息
    private HashMap<String, String> headerKeyValues;

    //请求
    public M3u8RequestNetwork(ProxyServer parent,
                              AsyncHttpServerRequest request,
                              AsyncHttpServerResponse response,
                              DownLoadActor downLoadActor) {
        //父类赋值
        this.parent = parent;
        //请求
        this.request = request;
        //回复
        this.response = response;
        //下载器
        this.downLoadActor = downLoadActor;
        //经过初始化的头部信息
        this.headerKeyValues = initHeaders(request);
        //监听
        this.response.setWriteableCallback(new WritableCallback() {
            @Override
            public void onWriteable() {
                //如果正在等待
                if (awaitFlag == true) {
                    //停止等待
                    awaitFlag = false;
                    //继续写入数据
                    doResponseNet();
                }
            }
        });
    }

    //进入网络请求环节
    public void doResponseNet() {
        //重新设置range
        resetRange(headerKeyValues);
        //实际请求网络
        doResponseNet(response, headerKeyValues);
    }

    //取出头部信息
    private HashMap<String, String> initHeaders(AsyncHttpServerRequest request) {
        //获取headers
        Multimap headers = request.getHeaders().getMultiMap();
        //获取key
        Iterator headersIterator = headers.keySet().iterator();
        //默认从零开始
        rangeStart = 0;
        //头部的header
        HashMap<String, String> headerKeyValues = new HashMap<>();
        //取出请求的参数
        while (headersIterator.hasNext()) {
            //获取请求中的key
            String key = (String) headersIterator.next();
            //获取请求中的value
            String value = headers.getString(key);
            //添加
            if (key != null &&
                    !key.toLowerCase().equals("host") &&
                    !key.toLowerCase().equals("range")) {
                headerKeyValues.put(key, value);
            }
            //vlc过来的奇葩请求Range
            if (key.toLowerCase().equals("range")) {

                //获取到的Range
                try {
                    //开始的位置
                    String start = value.toLowerCase().replace("bytes=", "");
                    //开始的位置
                    start = start.trim().split("-")[0];
                    //跳过多少
                    rangeStart = Long.parseLong(start);
                } catch (Exception ex) {
                    //从零开始
                    rangeStart = 0;
                }

                //获取到end
                try {
                    //开始的位置
                    String start = value.toLowerCase().replace("bytes=", "");
                    //结束的位置
                    start = start.trim().split("-")[1];
                    //跳过多少
                    rangeStopd = Long.parseLong(start);
                } catch (Exception ex) {
                    //从零开始
                    rangeStopd = 0;
                }
            }
        }
        return headerKeyValues;
    }

    //设置当前请求的Range
    private void resetRange(HashMap<String, String> headerKeyValues) {
        //实际的Range
        if (rangeStopd != 0) {
            headerKeyValues.put("Range", "bytes=" + rangeStart + "-" + rangeStopd);
        } else {
            headerKeyValues.put("Range", "bytes=" + rangeStart + "-");
        }
    }

    //处理net
    private void doResponseNet(AsyncHttpServerResponse response,
                               HashMap<String, String> headerKeyValues) {

        //此处对请求进行相应的代理处理
        try {
            //开始连接
            URL url = new URL(downLoadActor.getDownLoadUrl());
            //打开链接
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //迭代器
            Iterator reqIterator = headerKeyValues.keySet().iterator();
            //遍历
            while (reqIterator.hasNext()) {
                //获取请求中的key
                String key = (String) reqIterator.next();
                //获取请求中的value
                String value = headerKeyValues.get(key);
                //添加
                conn.setRequestProperty(key, value);
            }
            //返回connection的Code
            response.code(conn.getResponseCode());
            //移除
            Map<String, List<String>> maps = conn.getHeaderFields();
            //map
            HashMap<String, List<String>> hashMap = new HashMap<>();
            //遍历
            Iterator resIterator = maps.keySet().iterator();
            //next
            while (resIterator.hasNext()) {
                String key = (String) resIterator.next();
                if (key != null &&
                        !key.toLowerCase().equals("expires")) {
                    hashMap.put(key, maps.get(key));
                }
            }
            response.getHeaders().addAll(hashMap);
            //设置
            InputStream inputStream = conn.getInputStream();
            //重试
            resetRetryTime();
            //缓存大小
            byte[] buffer = new byte[1024];
            //长度
            int len = 0;
            //循环读取
            while ((len = inputStream.read(buffer)) != -1) {
                //如果已经停止了，就跳出循环，停止处理
                if (parent.isStoped()) {
                    //停止写入了，停止了
                    response.end();
                    //退出了
                    break;
                }
                //不是等待状态
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
                        //减去没有写入的
                        rangeStart -= bufferList.remaining();
                        //进入等待模式
                        awaitFlag = true;
                        //停止
                        break;
                    }
                }
            }
            //关闭连接
            inputStream.close();
            //停止
            conn.disconnect();
            //直到我们针对文件读取完毕才罢休
            if (len == -1) {
                response.end();
            }
        } catch (IOException e) {
            //在重试时间之内
            if (isNeedRetry()) {
                //等待300毫秒
                waitMilliseconds(250);
                //重新请求
                if (!parent.isStoped()) {
                    doResponseNet();
                }
            } else {
                //结束
                response.end();
            }
        }
    }

    //等待多少毫秒
    private void waitMilliseconds(int milli) {
        try {
            Thread.sleep(milli);
        } catch (Exception exception) {
            //等待失败
        }
    }

    //重置时间
    private void resetRetryTime() {
        retryTime = 0;
    }


    //是否需要重新
    private boolean isNeedRetry() {
        //已经停止了的就不需要
        if (parent.isStoped()) {
            return false;
        }
        //重试时间初始化
        if (retryTime == 0) {
            retryTime = System.currentTimeMillis();
        }
        //检查是否在重试时间之内
        if (System.currentTimeMillis() - retryTime < ServerConfig.NETWORK_RETRY_TIME) {
            return true;
        } else {
            return false;
        }
    }
}
