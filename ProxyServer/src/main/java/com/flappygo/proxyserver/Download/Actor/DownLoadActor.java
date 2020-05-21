package com.flappygo.proxyserver.Download.Actor;

import android.os.Handler;
import android.os.Message;

import com.flappygo.proxyserver.Download.Cookie.CookieHolder;
import com.flappygo.proxyserver.Tools.ToolDirs;
import com.flappygo.proxyserver.Tools.ToolFileSize;
import com.flappygo.proxyserver.Tools.ToolLog;
import com.flappygo.proxyserver.Tools.ToolString;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


//下载工具
public class DownLoadActor {

    //下载器的TAG
    private String TAG = "DownLoadActor";

    //当前的状态
    private int downLoadState = -1;

    //下载失败
    public final static int ERROR = 3;

    //下载完成
    public final static int DONE = 2;

    //下载取消
    public final static int CANCEL = 1;

    //正在下载
    public final static int DOWNLOADING = 0;

    // 下载进度
    private int progress = 0;

    // 终止下载线程的标志
    private volatile boolean isStoped = false;

    // 当前actor是否正在下载中
    private volatile boolean isLoading = false;

    //需要下载的开始位置
    private volatile long rangeStart = 0;

    //需要下载的文件长度
    private volatile long rangeLength = 0;

    // 下载的地址
    private String fileUrlPath;

    // 下载保存的文件名称
    private String fileName;

    // 下载保存的路径
    private String fileDirPath;

    //当前的绝对地址
    private String fileAbsolutePath;

    // 下载的监听
    private DownLoadListener downLoadListener;

    // cookie
    private CookieHolder holder;

    //当前的tag
    public int actorTag = 0;

    //检查
    private boolean fileCheck = false;

    //头部需要放入的信息
    private HashMap<String, String> headerMap = new HashMap<>();


    /***********
     * 构造器
     * @param urlPath 需要下载的地址
     * @param dirpath 需要保存的地址
     */
    public DownLoadActor(String urlPath, String dirpath) {
        this.fileUrlPath = urlPath;
        this.fileDirPath = dirpath;
        initFileNameAndPath();
    }

    /***********
     * 构造器
     * @param urlPath 需要下载的地址
     * @param dirpath 需要保存的地址
     */
    public DownLoadActor(HashMap<String, String> headerMap, String urlPath, String dirpath) {
        this.headerMap = headerMap;
        this.fileUrlPath = urlPath;
        this.fileDirPath = dirpath;
        initFileNameAndPath();
    }

    /***********
     * 构造器
     * @param urlPath 需要下载的地址
     * @param dirpath 需要保存的地址
     * @param dirpath 需要保存的名称
     */
    public DownLoadActor(String urlPath, String dirpath, String fileName) {
        this.fileUrlPath = urlPath;
        this.fileDirPath = dirpath;
        this.fileName = fileName;
        initFileNameAndPath();
    }

    /***********
     * 构造器
     * @param urlPath  需要下载的地址
     * @param dirPath  需要保存的地址
     * @param fileName 需要保存的名称
     * @param start    下载开始的位置
     * @param length   需要下载的长度
     */
    public DownLoadActor(HashMap<String, String> headerMap,
                         String urlPath,
                         String dirPath,
                         String fileName,
                         long start,
                         long length) {
        this.headerMap = headerMap;
        this.fileUrlPath = urlPath;
        this.fileDirPath = dirPath;
        this.fileName = fileName;
        this.rangeStart = start;
        this.rangeLength = length;
        initFileNameAndPath();
    }

    public long getRangeStart() {
        return rangeStart;
    }

    public void setRangeStart(long rangeStart) {
        this.rangeStart = rangeStart;
    }

    public long getRangeLength() {
        return rangeLength;
    }

    public void setRangeLength(long rangeLength) {
        this.rangeLength = rangeLength;
    }

    public void setHeaderMap(HashMap<String, String> headerMap) {
        this.headerMap = headerMap;
    }

    //设置是否进行文件完整性检查
    public void setFileCheck(boolean fileCheck) {
        this.fileCheck = fileCheck;
    }

    //初始化设置文件的名称
    private void initFileNameAndPath() {
        //更新文件的真实名称
        if (fileName == null || fileName.equals("")) {
            fileName = ToolString.getNameString(fileUrlPath);
        }
        //文件存储在地址中的绝对路径
        fileAbsolutePath = fileDirPath + fileName;
    }

    //获取已经下载完成的文件的长度
    public long getDownloadedSize() {
        //长度
        File file = new File(fileAbsolutePath);
        //是否
        if (file.exists()) {
            try {
                return ToolFileSize.getFileSize(file);
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    //当前是否下载完成
    private volatile boolean downloaded;


    //都存在代表下载完成
    public boolean isDownloaded() {
        //已经下载完成
        if (downloaded == true) {
            return downloaded;
        } else {
            File trFile = new File(fileAbsolutePath);
            downloaded = trFile.exists();
            return downloaded;
        }
    }

    //当前是否正在下载
    public boolean isLoading() {
        return isLoading;
    }

    //获取当前的下载状态
    final public int getDownLoadState() {
        return downLoadState;
    }

    //获取保存在SD卡中的名称
    final public String getFileName() {
        return fileName;
    }

    //获取绝对地址
    final public String getFileAbsolutePath() {
        return fileAbsolutePath;
    }

    //获取下载的地址
    final public String getDownLoadUrl() {
        return fileUrlPath;
    }

    //cookie的holder
    public CookieHolder getHolder() {
        return holder;
    }

    //设置cookie的holder
    public void setHolder(CookieHolder holder) {
        this.holder = holder;
    }

    //设置下载的监听
    public void setDownLoadListener(DownLoadListener li) {
        downLoadListener = li;
    }

    //取消下载，是否取消成功需要在监听中获得回调
    public void cancle() {
        isStoped = true;
    }

    //获取progress
    public int getProgress() {
        return progress;
    }

    //同步执行
    public void excuteSync() {
        //判断当前是否正在loading
        if (isLoading) {
            return;
        } else {
            isLoading = true;
        }
        if (isStoped) {
            return;
        }
        excuteSync(downLoadListener);
    }

    //使用同步的方式进行调用
    private synchronized void excuteSync(DownLoadListener listener) {


        //设置起始位置的下载字节
        long offset = rangeStart;
        //文件的总大小
        long totalSize = 0;

        //断点续传的配置文件
        File fileConfig = null;
        //下载的数据文件
        File fileDatas = null;
        //真实下载的文件
        File fileActure = null;

        //文件
        RandomAccessFile randonFile = null;
        //数据流
        InputStream inputStream = null;
        try {
            //文件夹必须存在
            ToolDirs.createDir(fileDirPath, true);
            //配置文件
            fileConfig = new File(fileAbsolutePath + ".cfg");
            //数据临时文件
            fileDatas = new File(fileAbsolutePath + ".data");
            //真实下载的文件
            fileActure = new File(fileAbsolutePath);
            //如果已经存在了这个文件 校验文件大小，保证完整性
            if (fileActure.exists()) {

                //不检查文件完整性
                if (fileCheck == false) {
                    // 下载完成
                    downLoadState = DONE;
                    // 下载完成
                    if (listener != null) {
                        listener.downLoadSuccess(fileDirPath + fileName, fileName);
                    }
                    return;
                }

                //如果设置了下载长度
                if (rangeLength != 0) {
                    //如果我们需要的文件长度和已经下载的文件长度相等
                    if (rangeLength == ToolFileSize.getFileSize(fileActure)) {
                        //下载已经成功了
                        downLoadState = DONE;
                        //通知监听
                        if (listener != null) {
                            listener.downLoadSuccess(fileDirPath + fileName, fileName);
                        }
                        return;
                    }
                }
                //如果没有设置下载长度
                else {
                    //url开始连接
                    URL url = new URL(fileUrlPath);
                    //打开链接
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    //设置RequestProperty
                    conn.setRequestProperty("Accept-Encoding", "identity");
                    //设置 User-Agent
                    conn.setRequestProperty("User-Agent", "NetFox");
                    //设置cookie
                    setCookie(conn);
                    //初始化header
                    initHeader(conn);
                    //设置断点续传的开始位置
                    conn.setRequestProperty("Range", "bytes=" + rangeStart + "-");
                    //获取总大小
                    long fileSize = handleContentLength(conn, rangeStart);
                    //对比下载的文件的总大小是否相等
                    if (fileSize == ToolFileSize.getFileSize(fileActure)) {
                        // 下载完成
                        downLoadState = DONE;
                        // 下载完成
                        if (listener != null) {
                            //下载成功
                            listener.downLoadSuccess(fileDirPath + fileName, fileName);
                        }
                        //返回不再继续了
                        return;
                    }
                }
            }
            //如果文件不存在，但是两个配置文件存在
            else if (fileConfig.exists() && fileDatas.exists()) {
                //传入的数据
                DataInputStream confData = null;
                //log数据
                FileInputStream confin = null;
                try {
                    //获取到已经存储的长度数据
                    confin = new FileInputStream(fileConfig);
                    //获取到已经存储的长度数据
                    confData = new DataInputStream(confin);
                    // 读取到已经写入了多少
                    offset = confData.readLong();
                    //如果存储的offset,减去我们的开始不等于当前已经缓存的大小
                    if ((offset - rangeStart) != ToolFileSize.getFileSize(fileDatas)) {
                        //那么我们下载的offset必须从头开始
                        offset = rangeStart;
                    }
                } catch (Exception e) {
                    //进行初始化
                    offset = rangeStart;
                } finally {
                    //关闭流
                    if (confin != null) {
                        confin.close();
                    }
                    if (confData != null) {
                        confData.close();
                    }
                }
            }

            //如果还是起始位置，那么重新创建文件
            if (offset == rangeStart) {
                //创建
                fileConfig.createNewFile();
                //创建
                fileDatas.createNewFile();
            }

            //取得apk文件的写入
            randonFile = new RandomAccessFile(fileDatas, "rw");
            //定位到开始的地方
            randonFile.seek(offset - rangeStart);
            //url开始连接
            URL url = new URL(fileUrlPath);
            //打开链接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //设置RequestProperty
            conn.setRequestProperty("Accept-Encoding", "identity");
            //设置 User-Agent
            conn.setRequestProperty("User-Agent", "NetFox");
            //初始化cookie
            setCookie(conn);
            //初始化header
            initHeader(conn);
            //设置断点续传的开始位置
            conn.setRequestProperty("Range", "bytes=" + offset + "-");
            //如果是以我们设置的下载长度为准，那么就是我们的下载长度
            if (rangeLength != 0) {
                totalSize = rangeLength;
            }
            //如果不是以我们设置的下载长度为准，那么长度就是总长减去开始的一段
            else {
                //长度
                long contentLength = handleContentLength(conn, offset);

                //剩余长度加上已经下载的长度减去开始的长度，等于我们文件后面的长度
                totalSize = (contentLength - rangeStart);
            }

            //获取返回值
            int responseCode = conn.getResponseCode();

            //失败抛异常
            if (!(responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL)) {
                throw new Exception("Connection error:" + responseCode + " url:" + url);
            }
            //成功继续执行
            else {
                //保存cookie等数据
                generateSession(conn);
                //获取到input
                inputStream = conn.getInputStream();
                //缓存大小
                byte[] buffer = new byte[1024];
                //长度
                int readedLen = 0;
                //循环读取
                while ((readedLen = inputStream.read(buffer)) != -1) {
                    //线程被取消了，不再读了，如果说线程取消了，就不在写入了
                    if (isStoped) {
                        break;
                    }
                    //如果说没有长度限制，那么直接写就好了
                    if (rangeLength == 0) {
                        //写入数据
                        randonFile.write(buffer, 0, readedLen);
                        //新写入了len个字节
                        offset = offset + readedLen;

                    } else {
                        //如果有长度限制，但是还没有达到终点
                        if (offset + readedLen < rangeStart + rangeLength) {
                            //写入数据
                            randonFile.write(buffer, 0, readedLen);
                            //新写入了len个字节
                            offset = offset + readedLen;
                        }
                        //如果有长度限制，而且已经到达了终点
                        else {
                            //只写入到终点那么大
                            if ((rangeStart + rangeLength - offset) != 0) {
                                //防止为零的时候写入的数据有误
                                randonFile.write(buffer, 0, (int) (rangeStart + rangeLength - offset));
                                //已经达到终点
                                offset = rangeStart + rangeLength;
                            }
                            //我们认为已经达到终点了
                            readedLen = -1;
                            //然后break
                            break;
                        }
                    }
                    //下载中
                    downLoadState = DOWNLOADING;
                    //监听不为空
                    if (listener != null) {
                        listener.downLoading((int) ((offset - rangeStart) * 100 / totalSize));
                    }
                }
                //如果已经下载完毕了
                if (readedLen == -1) {

                    //重新进行命名
                    fileDatas.renameTo(fileActure);

                    // 下载完成
                    downLoadState = DONE;
                    // 下载完成
                    if (listener != null) {
                        listener.downLoadSuccess(fileDirPath + fileName, fileName);
                    }
                    return;
                }
                //如果还没有下载完就代表是取消的
                else {
                    // 下载取消
                    if (listener != null) {
                        listener.downloadCancled();
                    }
                    downLoadState = CANCEL;
                }
            }
        } catch (Exception e) {
            //错误
            downLoadState = ERROR;
            //发生错误
            if (listener != null) {
                listener.downloadError(e);
            }
            //删除
            if (fileActure != null) {
                fileActure.delete();
            }
        } finally {
            //保存当前的下载进度
            try {
                if (fileConfig != null) {
                    saveConfig(fileConfig, offset);
                }
            } catch (Exception ex) {
                ToolLog.e(TAG, ex.getMessage());
            }
            //关闭RandomAccessFile
            try {
                if (randonFile != null) {
                    randonFile.close();
                }
            } catch (Exception ex) {
                ToolLog.e(TAG, ex.getMessage());
            }
            //关闭inputStream
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception ex) {
                ToolLog.e(TAG, ex.getMessage());
            }
            isLoading = false;
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


    //初始化header
    private void initHeader(HttpURLConnection conn) {
        //头部
        if (headerMap != null) {
            //键值对
            Iterator iterator = headerMap.keySet().iterator();
            //遍历
            while (iterator.hasNext()) {
                //下一个
                String key = (String) iterator.next();
                //除开range,其他的都要
                if (key != null && !key.toLowerCase().trim().equals("range")) {
                    conn.setRequestProperty(key, headerMap.get(key));
                }
            }
        }
    }


    //使用异步的方式进行调用
    public void excute() {
        //判断是否忙碌
        if (isLoading) {
            return;
        } else {
            isLoading = true;
        }
        //处理
        final Handler proHanlder = new Handler() {
            public void handleMessage(Message msg) {
                //正在下载
                if (msg.what == DOWNLOADING && msg.arg1 != progress) {
                    progress = msg.arg1;
                    if (downLoadListener != null) {
                        downLoadListener.downLoading(progress);
                    }

                } else if (msg.what == CANCEL) {
                    // 下载取消
                    if (downLoadListener != null) {
                        downLoadListener.downloadCancled();
                    }
                } else if (msg.what == DONE) {
                    // 下载完成
                    if (downLoadListener != null) {
                        downLoadListener.downLoadSuccess(fileDirPath + fileName, fileName);
                    }
                } else if (msg.what == ERROR) {
                    // 下载失败
                    if (downLoadListener != null) {
                        downLoadListener.downloadError((Exception) msg.obj);
                    }
                }
            }
        };

        new Thread() {
            public void run() {

                //请求
                excuteSync(new DownLoadListener() {
                    @Override
                    public void downLoadSuccess(String path, String name) {
                        //下载完成了
                        Message m = new Message();
                        //成功
                        m.what = DONE;
                        //发送消息
                        proHanlder.sendMessage(m);
                    }

                    @Override
                    public void downLoading(int progress) {

                        //发送下载中的消息
                        Message m = new Message();
                        //下载中
                        m.what = DOWNLOADING;
                        //下载进度
                        m.arg1 = progress;
                        //移除之前的
                        proHanlder.removeMessages(DOWNLOADING);
                        //发送消息
                        proHanlder.sendMessage(m);
                    }

                    @Override
                    public void downloadError(Exception e) {

                        //下载出错咯
                        Message m = proHanlder.obtainMessage(ERROR, e);
                        //发送错误消息
                        proHanlder.sendMessage(m);
                    }

                    @Override
                    public void downloadCancled() {

                        Message m = new Message();
                        m.what = CANCEL;
                        proHanlder.sendMessage(m);
                    }
                });
            }
        }.start();
    }


    /************
     * 保存配置文件
     * @param configFile   文件
     * @param start        长度
     * @throws Exception   错误
     */
    private void saveConfig(File configFile, long start) throws Exception {
        //下载完成时候的进度配置保存
        {
            //配置数据
            FileOutputStream confout = null;
            //配置
            DataOutputStream confdata = null;
            try {
                //文件
                confout = new FileOutputStream(configFile);
                //数据
                confdata = new DataOutputStream(confout);
                //写入数据
                confdata.writeLong(start);
                //关闭
                confdata.close();
            } catch (Exception e) {
                //错误信息
                throw e;
            } finally {
                //关闭
                if (confdata != null) {
                    try {
                        confdata.close();
                    } catch (IOException ex) {
                        ToolLog.e(TAG, ex.getMessage());
                    }
                }
                //关闭
                if (confout != null) {
                    try {
                        confout.close();
                    } catch (IOException ex) {
                        ToolLog.e(TAG, ex.getMessage());
                    }
                }
            }
        }
    }


    /************
     * 设置已经被加入的cookie
     * @param conn
     */
    private void setCookie(HttpURLConnection conn) {
        if (holder != null && holder.getCookie() != null) {
            conn.addRequestProperty("Cookie", holder.getCookie());
        }
    }


    /************
     * 保存当前的cookie
     * @param conn 连接
     */
    private void generateSession(HttpURLConnection conn) {
        if (holder != null) {
            String sessionId = "";
            String cookieVal = "";
            String cookieKey = "";
            // 取cookie
            for (int i = 1; (cookieKey = conn.getHeaderFieldKey(i)) != null; i++) {
                if (cookieKey.equalsIgnoreCase("set-cookie")) {
                    cookieVal = conn.getHeaderField(i);
                    cookieVal = cookieVal.substring(0, cookieVal.indexOf(";"));
                    sessionId = sessionId + cookieVal;
                    holder.setCookie(sessionId);
                }
            }
        }
    }

}
