package com.flappygo.proxyserver.ProxyServer.Serverm3u8;

import android.content.Context;

import com.flappygo.proxyserver.Download.Thread.ProxyThreadPoolExecutor;
import com.flappygo.proxyserver.Download.Thread.ProxyThreadPoolListener;
import com.flappygo.proxyserver.ServerID.ServerIDManager;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.flappygo.proxyserver.Download.Thread.ProxyDownloadThread;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import com.flappygo.proxyserver.ProxyServer.Models.DownloadDoneModel;
import com.flappygo.proxyserver.Download.Actor.DownLoadActor;
import com.flappygo.proxyserver.ServerPath.ServerPathManager;
import com.flappygo.proxyserver.Interface.ProxyCacheListener;
import com.koushikdutta.async.callback.CompletedCallback;
import com.flappygo.proxyserver.ProxyServer.ServerProxy;
import com.flappygo.proxyserver.Interface.ProxyServer;
import com.flappygo.proxyserver.Config.ServerConfig;
import com.flappygo.proxyserver.FlappyProxyServer;
import com.flappygo.proxyserver.Config.HttpConfig;
import com.flappygo.proxyserver.Tools.ToolIntenet;
import com.flappygo.proxyserver.Tools.ToolSDcard;
import com.flappygo.proxyserver.Tools.ToolString;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.http.Multimap;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//代理M3u8的地址
public class ProxyServerM3u8 implements ProxyServer {


    //上下文保存
    private Context context;

    //真实的actionID
    private String actionID;

    //文件分配的ID
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
    private List<ProxyServerM3u8Child> childServerList = new ArrayList<>();

    //当前正在执行的线程池
    private ProxyThreadPoolExecutor proxyThreadPoolExecutor;

    //缓存的监听
    private List<ProxyCacheListener> cacheListeners = new ArrayList<>();


    //构造器
    public ProxyServerM3u8(Context context, String actionID, String url) {
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

                //主要的地址
                String mainPath = getUrlDicotry() + getM3u8FileName();

                //header返回的
                String headPath = getUrlDicotry() + getM3u8HeadName();

                //判断文件
                File file = new File(mainPath);

                //heade
                File fileHead = new File(headPath);

                //获取headers
                Multimap headers = request.getHeaders().getMultiMap();
                //获取key
                Iterator headersIterator = headers.keySet().iterator();
                //请求的header
                HashMap<String, String> requestMaps = new HashMap<>();
                //取出请求的参数
                while (headersIterator.hasNext()) {
                    //获取请求中的key
                    String key = (String) headersIterator.next();
                    //获取请求中的value
                    String value = headers.getString(key);
                    //M3u8主文件我们就不进行断点续传处理了，没见过哪个播放器这么搞的,这里移除host和range
                    if (key != null &&
                            !key.toLowerCase().equals("host") &&
                            !key.toLowerCase().equals("range")) {
                        requestMaps.put(key, value);
                    }
                }

                //设置关闭的监听
                response.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        response.end();
                    }
                });

                //如果存在
                if (file.exists() && fileHead.exists()) {
                    doForCache(requestMaps, response);
                }
                //如果不存在已经保存的m3u8文件
                else {
                    doForNet(requestMaps, response);
                }
            }
        };
        ServerProxy.getInstance().addVideoProxy(actionID, callback);
    }

    //通过缓存进行处理
    private void doForCache(HashMap requestMaps,
                            final AsyncHttpServerResponse response) {
        try {
            //返回对应的header文件
            HashMap responesMaps = (HashMap) ToolSDcard.getObjectSdcard(getUrlDicotry(), getM3u8HeadName());

            //返回字符串
            String responseStr = ToolSDcard.readStringSdcard(getUrlDicotry(), getM3u8FileName());

            //请求成功
            response.code(HttpConfig.NET_SUCCESS_PART);

            //所有的paths进入代理流程
            responseStr = initPaths(requestMaps, responseStr);

            //获取bytes
            byte[] bytes = responseStr.getBytes();

            //返回当前的Range
            List<String> range = new ArrayList<>();
            range.add("bytes " + 0 + "-" + (bytes.length - 1) + "/" + bytes.length);
            responesMaps.put("Content-Range", range);

            //返回当前的大小
            List<String> datas = new ArrayList<>();
            datas.add(Long.toString(bytes.length));
            responesMaps.put("Content-Length", datas);

            //返回真实数据
            response.getHeaders().addAll(responesMaps);

            //写入数据
            ByteBufferList bufferList = new ByteBufferList(bytes);
            //写入进去
            response.write(bufferList);
            //结束
            response.end();
        } catch (Exception ex) {
            //结束
            response.end();
        }
    }

    //请求
    private void doForNet(HashMap<String, String> requestMaps,
                          final AsyncHttpServerResponse response) {

        try {

            //url开始连接
            URL url = new URL(urlPath);
            //打开链接
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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
            //移除
            Map<String, List<String>> maps = conn.getHeaderFields();
            //map
            HashMap<String, List<String>> responesMaps = new HashMap<>();
            //迭代器
            Iterator resIterator = maps.keySet().iterator();
            //遍历添加responseHeader
            while (resIterator.hasNext()) {
                //遍历
                String key = (String) resIterator.next();
                //当前的Range
                if (key != null &&
                        !key.toLowerCase().equals("expires") &&
                        !key.toLowerCase().equals("content-length") &&
                        !key.toLowerCase().equals("content-range")) {
                    responesMaps.put(key, maps.get(key));
                }
            }

            //设置
            InputStream inputStream = conn.getInputStream();
            //重置时间
            resetRetryTime();
            //将数据转换为String
            String responseStr = ToolString.convertStreamToStr(inputStream, "utf-8");


            //不是直播的情况才缓存，是直播的话，这个文件会一直更新，我们不能缓存
            if (!ToolString.isLive(responseStr)) {
                //保存
                ToolSDcard.writeStringSdcard(getUrlDicotry(), getM3u8FileName(), responseStr);
                //写入header
                ToolSDcard.writeObjectSdcard(getUrlDicotry(), getM3u8HeadName(), responesMaps);
            }

            //所有的paths进入代理流程
            responseStr = initPaths(requestMaps, responseStr);

            //获取bytes
            byte[] bytes = responseStr.getBytes();

            //返回当前的Range
            List<String> range = new ArrayList<>();
            range.add("bytes " + 0 + "-" + (bytes.length - 1) + "/" + bytes.length);
            responesMaps.put("Content-Range", range);

            //返回当前的大小
            List<String> datas = new ArrayList<>();
            datas.add(Long.toString(bytes.length));
            responesMaps.put("Content-Length", datas);

            //返回真实数据
            response.getHeaders().addAll(responesMaps);

            //写入数据
            ByteBufferList bufferList = new ByteBufferList(responseStr.getBytes());
            //写入进去
            response.write(bufferList);
            //完成
            response.end();
            //写完了
            inputStream.close();
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
                    doForNet(requestMaps, response);
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

    //头部信息
    private String getM3u8HeadName() {
        return urlVideoID + "head.data";
    }

    //获取主要文件的名称
    private String getM3u8FileName() {
        return urlVideoID + ".data";
    }


    //初始化所有paths
    private String initPaths(HashMap requestMaps, String responseStr) {

        //当前threadPool
        ProxyThreadPoolExecutor threadPool = new ProxyThreadPoolExecutor(ServerConfig.THREAD_POOL_SIZE);

        //我们需要将m3u8中的地址替换为我们自己的地址
        String retStr = responseStr;

        //获取所有的播放列表
        List<String> paths = ToolString.getM3u8UrlList(responseStr);

        //列表
        List<ProxyServerM3u8Child> childList = new ArrayList<>();

        //当前是否是直播
        boolean isAlive = ToolString.isLive(responseStr);

        //判断
        for (int s = 0; s < paths.size(); s++) {

            //如果不是http开头的
            if (!paths.get(s).startsWith("http")) {

                //真实的子文件下载地址
                String trueUrlPath = ToolString.generateChildPath(urlPath, paths.get(s));
                //我们用于创建的Action地址
                String trueAction = ToolString.generateActionPath(paths.get(s), actionID);
                //真实的保存文件的地址
                String trueDictionary = getUrlDicotry();
                //替换掉实际地址
                retStr = retStr.replace(paths.get(s), trueAction);
                //创建下载器
                DownLoadActor actor = new DownLoadActor(requestMaps, trueUrlPath, trueDictionary);
                //设置下载器的tag
                actor.actorTag = s;
                //开启线程池
                ProxyDownloadThread thread = new ProxyDownloadThread(actor);
                //加入到线程池中执行
                threadPool.execute(thread);
                //创建
                ProxyServerM3u8Child requestCallback = new ProxyServerM3u8Child(this, trueAction, actor, isAlive);
                //添加子请求
                childList.add(requestCallback);

            }
            //如果是http开头的
            else {

                //获取实际的域名信息
                String trueUrlPath = paths.get(s);
                //地址
                String trueAction = ToolString.generateActionPath(paths.get(s), actionID);
                //真实的保存文件的地址
                String trueDictionary = getUrlDicotry();
                //替换掉实际地址
                retStr = retStr.replace(paths.get(s), trueAction);
                //创建下载器
                DownLoadActor actor = new DownLoadActor(requestMaps, trueUrlPath, trueDictionary);
                //设置下载器的tag,方便我们确认下载排序的序号
                actor.actorTag = s;
                //开启线程池
                ProxyDownloadThread thread = new ProxyDownloadThread(actor);
                //加入到线程池中执行
                threadPool.execute(thread);
                //创建
                ProxyServerM3u8Child requestCallback = new ProxyServerM3u8Child(this, trueAction, actor, isAlive);
                //添加子请求
                childList.add(requestCallback);

            }
        }

        //设置线程池的监听
        if (!isAlive) {
            threadPool.setProxyThreadPoolListener(new ProxyThreadPoolListener() {
                @Override
                public void threadDone(int remain, String threadID) {
                    List<ProxyServerM3u8Child> segments = childServerList;
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
                    List<ProxyServerM3u8Child> segments = childServerList;
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
                        //缓存完成
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
        }

        cancelAllDownloading();

        synchronized (this) {
            //赋值
            childServerList = childList;
            //赋值
            proxyThreadPoolExecutor = threadPool;
        }

        //返回字符串
        return retStr;

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

    //停止所有的服务
    private void cancelAllChild() {
        synchronized (this) {
            for (int s = 0; s < childServerList.size(); s++) {
                childServerList.get(s).stop();
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

    //获取
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
            //停止所有的子类代理
            cancelAllChild();
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

        //设置当前的监听
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
                //返回
                return;
            }
            //停止当前的线程池
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