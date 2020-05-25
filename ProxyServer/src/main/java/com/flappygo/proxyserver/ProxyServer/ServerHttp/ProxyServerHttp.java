package com.flappygo.proxyserver.ProxyServer.ServerHttp;

import android.content.Context;

import com.flappygo.proxyserver.Config.HttpConfig;
import com.flappygo.proxyserver.Config.ServerConfig;
import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.Download.Thread.ProxyDownloadThread;
import com.flappygo.proxyserver.Download.Thread.ProxyThreadPoolExecutor;
import com.flappygo.proxyserver.Download.Thread.ProxyThreadPoolListener;
import com.flappygo.proxyserver.FlappyProxyServer;
import com.flappygo.proxyserver.Interface.ProxyCacheListener;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.ProxyServer.Models.DownloadDoneModel;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Models.HttpSegmentModel;
import com.flappygo.proxyserver.ProxyServer.ServerProxy;
import com.flappygo.proxyserver.ServerID.ServerIDManager;
import com.flappygo.proxyserver.ServerPath.ServerPathManager;
import com.flappygo.proxyserver.Tools.ToolIntenet;
import com.flappygo.proxyserver.Tools.ToolSDcard;
import com.flappygo.proxyserver.Tools.ToolString;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.Multimap;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//请求用于HTTP等的请求
public class ProxyServerHttp implements ProxyServer {


    //上下文保存
    private Context context;

    //真实的actionID
    private String actionID;

    //视频的ID
    private String urlVideoID;

    //实际请求的地址
    private String urlPath;

    //用于处理的回调
    private HttpServerRequestCallback callback;

    //当前服务是否被停止
    private volatile boolean isStoped = false;

    //重试的时间
    private volatile long retryTime = 0;

    //子类
    private List<ProxyServerHttpSegment> childServerList = new ArrayList<>();

    //当前正在执行的线程池
    private ProxyThreadPoolExecutor proxyThreadPoolExecutor;

    //缓存的监听
    private List<ProxyCacheListener> cacheListeners = new ArrayList<>();


    //构造器
    public ProxyServerHttp(Context context, String actionID, String url) {
        super();
        this.context = context;
        this.actionID = actionID;
        this.urlPath = url;
        this.isStoped = false;
        this.urlVideoID = ServerIDManager.getInstance(context).getUrlVideoID(urlPath);
        this.addAction();
    }

    //添加action
    private void addAction() {
        callback = new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {

                //主要文件是否存在
                String mainPath = getUrlDicotry() + getHttpFileName();
                String headPath = getUrlDicotry() + getHttpHeadName();

                //如果文件存在
                File file = new File(mainPath);
                File fileTwo = new File(headPath);

                //设置关闭的监听
                response.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        response.end();
                    }
                });

                //如果存在
                if (file.exists() && fileTwo.exists()) {
                    doForCache(request, response);
                }
                //如果不存在已经保存的m3u8文件
                else {
                    doForNet(request, response);
                }
            }
        };
        ServerProxy.getInstance().addVideoProxy(actionID, callback);
    }

    //通过缓存进行处理
    private void doForCache(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
        try {

            //获取headers
            Multimap headers = request.getHeaders().getMultiMap();

            //获取key
            Iterator headersIterator = headers.keySet().iterator();

            //请求开始的offset
            long rangeStart = 0;

            //请求结束的offset
            long rangeStopd = 0;

            //请求的header
            HashMap<String, String> requestMaps = new HashMap<>();

            //当前请求是否包含range
            boolean containRange = false;

            //取出请求的参数
            while (headersIterator.hasNext()) {

                //获取请求中的key
                String key = (String) headersIterator.next();

                //获取请求中的value
                String value = headers.getString(key);

                //M3u8主文件我们就不进行断点续传处理了，没见过哪个播放器这么搞的
                if (key != null &&
                        !key.toLowerCase().equals("host") &&
                        !key.toLowerCase().equals("range")) {
                    requestMaps.put(key, value);
                }

                //vlc过来的奇葩请求Range
                if (key.toLowerCase().equals("range")) {
                    containRange = true;
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

            //返回字符串
            String contentLength = ToolSDcard.readStringSdcard(getUrlDicotry(), getHttpFileName());
            Map responseMap = (Map) ToolSDcard.getObjectSdcard(getUrlDicotry(), getHttpHeadName());

            //如果总大小不为零，而且小于start,直接返回错误
            if (Long.parseLong(contentLength) != 0 &&
                    Long.parseLong(contentLength) <= rangeStart) {
                response.end();
                return;
            }

            //请求成功，我们模范真实服务器的返回方式，如果有range传入我们返回的是206
            if (containRange) {
                response.code(HttpConfig.NET_SUCCESS_PART);
            } else {
                response.code(HttpConfig.NET_SUCCESS);
            }

            //返回返回的参数
            response.getHeaders().addAll(responseMap);

            //结束
            handleResponseRange(rangeStart, Long.parseLong(contentLength), response);

            //所有的paths进入代理流程
            initPaths(requestMaps,
                    Long.parseLong(contentLength),
                    rangeStart,
                    rangeStopd,
                    response);

        } catch (Exception ex) {
            //结束
            response.end();
        }
    }

    //请求
    private void doForNet(final AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {

        try {
            //获取headers
            Multimap headers = request.getHeaders().getMultiMap();

            //获取key
            Iterator headersIterator = headers.keySet().iterator();

            //头部的header
            HashMap<String, String> requestMaps = new HashMap<>();

            //请求开始的offset
            long rangeStart = 0;

            //请求结束的offset
            long rangeStopd = 0;

            //取出请求的参数
            while (headersIterator.hasNext()) {

                //获取请求中的key
                String key = (String) headersIterator.next();

                //获取请求中的value
                String value = headers.getString(key);

                //M3u8主文件我们就不进行断点续传处理了，没见过哪个播放器这么搞的
                if (key != null &&
                        !key.toLowerCase().equals("host") &&
                        !key.toLowerCase().equals("range")) {
                    requestMaps.put(key, value);
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
                        String stop = value.toLowerCase().replace("bytes=", "");
                        //结束的位置
                        stop = stop.trim().split("-")[1];
                        //跳过多少
                        rangeStopd = Long.parseLong(stop);
                    } catch (Exception ex) {
                        //从零开始
                        rangeStopd = 0;
                    }
                }
            }

            //实际的Range
            if (rangeStopd != 0) {
                requestMaps.put("Range", "bytes=" + rangeStart + "-" + rangeStopd);
            } else {
                requestMaps.put("Range", "bytes=" + rangeStart + "-");
            }


            //url开始连接
            URL url = new URL(urlPath);
            //打开链接
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //重置时间
            resetRetryTime();
            //迭代器
            Iterator reqIterator = requestMaps.keySet().iterator();
            //遍历
            while (reqIterator.hasNext()) {
                //获取请求中的key
                String key = (String) reqIterator.next();
                //获取请求中的value
                String value = requestMaps.get(key);
                //添加
                conn.setRequestProperty(key, value);
            }


            //返回connection的Code
            response.code(conn.getResponseCode());

            //取得长度
            long contentLength = handleContentLength(conn, rangeStart);

            //返回数据
            handleResponseHeaders(conn, response);

            //处理response
            handleResponseRange(rangeStart, contentLength, response);

            //所有的paths进入代理流程
            initPaths(requestMaps, contentLength, rangeStart, rangeStopd, response);

            //停止
            conn.disconnect();
        }
        //如果是其他的错误
        catch (Exception ex) {
            //在重试时间之内
            if (isNeedRetry()) {
                //等待300毫秒
                waitMilliseconds(250);
                //如果没有停止就重试
                if (!isStoped) {
                    doForNet(request, response);
                }
            } else {
                //结束
                response.end();
            }
        }
    }

    //处理contentRange
    private long handleContentLength(HttpURLConnection connection, long rangeStart) {
        //获取长度
        long contentLength = 0;
        //获取header数据
        Map<String, List<String>> conMaps = connection.getHeaderFields();
        //迭代器
        Iterator iterator = conMaps.keySet().iterator();
        //首先取得content-range中的大小
        while (iterator.hasNext()) {
            //遍历
            String key = (String) iterator.next();
            //找到content-length
            if (key != null &&
                    key.toLowerCase().equals("content-range")) {
                //找到collection
                Collection collection = conMaps.get(key);
                //迭代器
                Iterator iteratorOne = collection.iterator();
                //有下一个
                while (iteratorOne.hasNext()) {
                    //紫都城
                    String str = (String) iteratorOne.next();
                    if (str != null && str.contains("/")) {
                        String len[] = str.split("/");
                        if (len.length > 1) {
                            //长度
                            String lenMem = len[len.length - 1];
                            //长度
                            contentLength = Long.parseLong(lenMem);
                            //存在就返回
                            return contentLength;
                        }
                    }
                }
            }
        }
        //遍历
        while (iterator.hasNext()) {
            //遍历
            String key = (String) iterator.next();
            //找到content-length
            if (key != null &&
                    key.toLowerCase().equals("content-length")) {
                //找到collection
                Collection collection = conMaps.get(key);
                //迭代器
                Iterator iteratorOne = collection.iterator();
                //有下一个
                while (iteratorOne.hasNext()) {
                    //长度
                    String str = (String) iteratorOne.next();
                    //长度
                    contentLength = Long.parseLong(str);
                    //长度
                    return contentLength + rangeStart;
                }
                break;
            }
        }
        //如果没有拿到
        contentLength = connection.getContentLength() + rangeStart;
        //返回
        return contentLength;
    }

    //处理请求的头部
    private void handleResponseHeaders(HttpURLConnection connection,
                                       AsyncHttpServerResponse response) {

        //获取当前的头部文件
        Map<String, List<String>> conMaps = connection.getHeaderFields();
        Map<String, List<String>> retMaps = new HashMap<>();
        Iterator iterator = conMaps.keySet().iterator();

        //遍历
        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            if (key != null &&
                    //去掉range
                    !key.toLowerCase().equals("content-range") &&
                    //去掉长度
                    !key.toLowerCase().equals("content-length") &&
                    //去掉超时
                    !key.toLowerCase().equals("expires")) {
                retMaps.put(key, conMaps.get(key));
            }
        }
        //返回
        response.getHeaders().addAll(retMaps);

        //写入retmaps
        ToolSDcard.writeObjectSdcard(getUrlDicotry(), getHttpHeadName(), retMaps);
    }


    //处理请求的头部range
    private void handleResponseRange(long rangeStart,
                                     long contentLength,
                                     AsyncHttpServerResponse response) {

        //去掉返回数据中的超时字段等多余字段
        HashMap filterMap = new HashMap();

        //返回当前的Range
        List<String> bytes = new ArrayList<>();
        bytes.add("bytes " + rangeStart + "-" + (contentLength - 1) + "/" + contentLength);
        filterMap.put("Content-Range", bytes);

        //返回当前的大小
        List<String> datas = new ArrayList<>();
        datas.add(Long.toString(contentLength - rangeStart));
        filterMap.put("Content-Length", datas);

        //清空
        response.getHeaders().addAll(filterMap);

        //保存当前的长度
        ToolSDcard.writeStringSdcard(getUrlDicotry(), getHttpFileName(), Long.toString(contentLength));

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
        //已经停止了就不需要重试了
        if (isStoped) {
            return false;
        }
        //重试时间初始化
        if (retryTime == 0) {
            retryTime = System.currentTimeMillis();
        }
        //判断时间
        if (System.currentTimeMillis() - retryTime < ServerConfig.NETWORK_RETRY_TIME) {
            return true;
        } else {
            return false;
        }
    }


    //处理
    private void initPaths(HashMap<String, String> headers,
                           long contentLength,
                           long start,
                           long end,
                           AsyncHttpServerResponse response) {

        //当前threadPool
        ProxyThreadPoolExecutor threadPool = new ProxyThreadPoolExecutor(ServerConfig.THREAD_POOL_SIZE);

        //获取所有的播放列表
        List<HttpSegmentModel> paths = ToolString.getHttpSegments(urlPath, contentLength, start, end);

        //列表
        final List<ProxyServerHttpSegment> childList = new ArrayList<>();

        //判断
        for (int s = 0; s < paths.size(); s++) {
            //实际下载文件的地址
            String trueUrlPath = paths.get(s).getUrl();
            //真实的保存文件的地址
            String trueDictionary = getUrlDicotry();
            //创建下载器
            DownLoadActor actor = new DownLoadActor(headers,
                    trueUrlPath,
                    trueDictionary,
                    paths.get(s).getSegmentName(),
                    paths.get(s).getStart(),
                    paths.get(s).getLength());
            //设置下载器的tag,方便我们确认下载排序的序号
            actor.actorTag = s;
            //开启线程池
            ProxyDownloadThread thread = new ProxyDownloadThread(actor);
            //加入到线程池中执行
            threadPool.execute(thread);
            //创建
            ProxyServerHttpSegment requestCallback = new ProxyServerHttpSegment(this, headers, actor, response);
            //添加子请求
            childList.add(requestCallback);
        }
        //调用代理
        ProxyServerHttpSegProxer procter = new ProxyServerHttpSegProxer(this, childList, response, start);

        //进行代理
        procter.proxy();

        //设置线程池的监听
        threadPool.setProxyThreadPoolListener(new ProxyThreadPoolListener() {
            @Override
            public void threadDone(int remain, String threadID) {
                List<ProxyServerHttpSegment> segments = childServerList;
                //已经下载的数量
                int downloaded = 0;
                //当前是否下载
                for (int s = 0; s < segments.size(); s++) {
                    if (segments.get(s).getDownLoadActor().isDownloaded()) {
                        downloaded++;
                    }
                }
                //监听
                synchronized (cacheListeners) {
                    int progress = (int) (downloaded * 1.0 / segments.size() * 100);
                    //监听
                    for (int s = 0; s < cacheListeners.size(); s++) {
                        cacheListeners.get(s).cachedProgress(progress);
                    }
                }
            }

            @Override
            public void threadNomore() {
                List<ProxyServerHttpSegment> segments = childServerList;
                //已经下载的数量
                int downloaded = 0;
                //当前是否下载
                for (int s = 0; s < segments.size(); s++) {
                    if (segments.get(s).getDownLoadActor().isDownloaded()) {
                        downloaded++;
                    }
                }
                //下载
                if (downloaded == segments.size()) {
                    //写入完成的数据
                    DownloadDoneModel downloadDoneModel = new DownloadDoneModel();
                    //设置url地址
                    downloadDoneModel.setUrl(urlPath);
                    //设置urlVideoID
                    downloadDoneModel.setVideoID(urlVideoID);
                    //缓存完成
                    downloadDoneModel.setTotalSegment(segments.size());
                    //完成
                    ToolSDcard.writeObjectSdcard(getUrlDicotry(), urlVideoID + "done.data", downloadDoneModel);
                    //监听
                    synchronized (cacheListeners) {
                        for (int s = 0; s < cacheListeners.size(); s++) {
                            cacheListeners.get(s).cachedSuccess();
                        }
                        cacheListeners.clear();
                    }
                }
                //修改
                else {
                    //还没有下载完成,而且有网络的情况下
                    if (ToolIntenet.isNetworkAvailable(context)) {
                        //遍历
                        for (int s = 0; s < segments.size(); s++) {
                            //没有下载完成的继续下载
                            if (!segments.get(s).getDownLoadActor().isDownloaded()) {
                                //开启线程池
                                ProxyDownloadThread thread = new ProxyDownloadThread(segments.get(s).getDownLoadActor());
                                //加入到线程池中执行
                                proxyThreadPoolExecutor.execute(thread);
                            }
                        }
                    } else {
                        cancelAllListener();
                    }

                }
            }
        });

        //关闭当前的线程池
        cancelAllDownloading();

        //同步
        synchronized (this) {

            //赋值
            childServerList = childList;

            //赋值
            proxyThreadPoolExecutor = threadPool;
        }
    }


    //停止所有的下载线程
    private void cancelAllDownloading() {
        synchronized (this) {
            if (proxyThreadPoolExecutor != null) {
                //停止线程池
                proxyThreadPoolExecutor.shutdownNow();
            }
        }
    }


    //取消所有的监听
    private void cancelAllListener() {
        //缓存已经停止
        synchronized (cacheListeners) {
            for (int s = 0; s < cacheListeners.size(); s++) {
                cacheListeners.get(s).cachedStoped();
            }
            cacheListeners.clear();
        }
    }

    //获取真实的URL
    @Override
    public String getUrl() {
        return urlPath;
    }

    //获取分配的actionID
    @Override
    public String getActionID() {
        return actionID;
    }

    //获取实际的保存地址
    @Override
    public String getUrlDicotry() {
        return ServerPathManager.getInstance(context).getDefaultCachePath(urlVideoID);
    }

    //获取主要文件的名称
    private String getHttpFileName() {
        return urlVideoID + ".data";
    }

    //获取主要文件的头部响应名称
    private String getHttpHeadName() {
        return urlVideoID + "head.data";
    }

    @Override
    public ProxyThreadPoolExecutor getPoolExecutor() {
        synchronized (this) {
            return proxyThreadPoolExecutor;
        }
    }

    //停止服务器
    @Override
    public void stop() {
        //如果还没有停止
        if (isStoped == false) {
            //停止
            isStoped = true;
            //移除
            ServerProxy.getInstance().removeVideoProxy(actionID);
            //取消所有的下载线程
            cancelAllDownloading();
            //取消所有的监听
            cancelAllListener();
        }
    }

    @Override
    public boolean isStoped() {
        return isStoped;
    }

    @Override
    public void startCache(ProxyCacheListener listener) {

        //完成下载的文件
        DownloadDoneModel model = (DownloadDoneModel) ToolSDcard.getObjectSdcard(getUrlDicotry(), urlVideoID + "done.data");

        //如果已经存在了
        if (model != null) {
            //已经缓存完成了
            listener.cachedSuccess();
            //返回
            return;
        }

        //添加监听
        synchronized (cacheListeners) {
            cacheListeners.add(listener);
        }

        //如果当前的所有的线程为零
        if (proxyThreadPoolExecutor == null ||
                proxyThreadPoolExecutor.getAllThread().size() == 0) {
            openThreadToStartCache();
        }
    }

    @Override
    public void stopCache() {
        //取消下载线程
        cancelAllDownloading();
        //取消所有监听
        cancelAllListener();
    }


    //开启线程以便于缓存当前文件
    private void openThreadToStartCache() {
        //开启线程，优先执行
        new Thread() {
            public void run() {
                try {
                    //拼接网址
                    String urlPath = FlappyProxyServer.getLocalServerUrl() + actionID;
                    //打开网址
                    URL url = new URL(urlPath);
                    //打开
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    //获取长度
                    conn.getContentLength();
                    //停止
                    conn.disconnect();
                } catch (Exception ex) {

                }
            }
        }.start();
    }

    //当前正在执行的分段
    private volatile int segmentPostion = 0;

    @Override
    public void setPlayingSegmentPosition(int postion) {
        //锁住
        synchronized (this) {
            //如果不是播放下一个片段
            if (Math.abs(postion - segmentPostion) <= 1) {
                //设置当前的播放片段
                segmentPostion = postion;
                return;
            }
            cancelAllDownloading();
            //设置当前的segmentPostion
            segmentPostion = postion;
            //重新创建线程池
            ProxyThreadPoolExecutor memPool = new ProxyThreadPoolExecutor(ServerConfig.THREAD_POOL_SIZE);
            //重新开始线程池
            for (int s = 0; s < childServerList.size(); s++) {
                if (s >= segmentPostion - 1) {
                    ProxyDownloadThread thread = new ProxyDownloadThread(childServerList.get(s).getDownLoadActor());
                    memPool.execute(thread);
                }
            }
            proxyThreadPoolExecutor = memPool;
        }
    }


}
