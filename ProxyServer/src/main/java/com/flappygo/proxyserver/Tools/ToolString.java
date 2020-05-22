package com.flappygo.proxyserver.Tools;

import com.flappygo.proxyserver.Config.ServerConfig;
import com.flappygo.proxyserver.ProxyServer.ServerHttp.Models.HttpSegmentModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ToolString {

    /**********************
     * 读取流数据
     *
     * @param is 输入流
     * @return 转换后的字符串
     * @throws IOException 异常
     */
    public static String convertStreamToStr(InputStream is, String charset) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
        StringBuilder sb = new StringBuilder();
        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (Exception e) {
            throw e;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                throw e;
            }
        }
        String data = sb.toString();
        return data;
    }

    //将http请求的文件进行分段,我们这里不需要stop，因为我们只需要所有的都下载完得了
    public static List<HttpSegmentModel> getHttpSegments(String url,
                                                         long contentLength,
                                                         long start,
                                                         long stop) {

        //设置
        List<HttpSegmentModel> retlist = new ArrayList<>();

        //取得总共我们需要多少个分段
        long count = contentLength / ServerConfig.FILE_SEGMENHT_SIZE;
        long remain = contentLength % ServerConfig.FILE_SEGMENHT_SIZE;
        if (remain > 0) {
            count++;
        }
        //然后
        for (int s = 0; s < count; s++) {
            //创建分段信息
            HttpSegmentModel segmentModel = new HttpSegmentModel();
            //设置下载的url
            segmentModel.setUrl(url);
            //设置分段的序号
            segmentModel.setSegPostion(s);
            //开始的位置
            segmentModel.setStart(s * ServerConfig.FILE_SEGMENHT_SIZE);

            //如果已经超出限制
            if ((s + 1) * ServerConfig.FILE_SEGMENHT_SIZE > contentLength) {
                //长度
                segmentModel.setLength(remain);
            } else {
                //设置长度
                segmentModel.setLength(ServerConfig.FILE_SEGMENHT_SIZE);
            }

            //是不是最后一个
            if (s == count - 1) {
                segmentModel.setLast(true);
            } else {
                segmentModel.setLast(false);
            }

            retlist.add(segmentModel);
        }
        return retlist;
    }

    //获取M3u8的所有分段
    public static List<String> getM3u8UrlList(String str) {
        //创建arrayList
        List<String> retArray = new ArrayList<>();
        //切片
        String splitStrs[] = str.replace("#EXT-X-ENDLIST", "").split("#EXTINF:");
        //遍历
        for (int s = 1; s < splitStrs.length; s++) {
            String m3u8Path = null;
            String urlSplit[] = splitStrs[s].split(",");
            if (urlSplit.length > 1) {
                m3u8Path = urlSplit[1];
            } else if (urlSplit.length > 0) {
                m3u8Path = urlSplit[0];
            }
            //解析到了地址
            if (m3u8Path != null) {
                retArray.add(m3u8Path.trim());
            }
        }
        return retArray;
    }

    //是否是在线直播类型
    public static boolean isLive(String str) {
        if (str.contains("#EXT-X-ENDLIST")) {
            return false;
        } else {
            return true;
        }
    }

    //获取地址
    public static String generateChildPath(String mainPath, String relativePath) {
        //字符串
        if (mainPath == null || mainPath == "") {
            return "";
        }
        try {
            //真实的路径
            String truePath = mainPath.substring(0, mainPath.lastIndexOf("/"));
            //相对路径
            String nextPath = relativePath;
            //地址拼接
            if (nextPath.startsWith("/")) {
                nextPath = relativePath.substring(1, nextPath.length());
            }
            //多少个反斜杠
            int count = nextPath.split("/").length - 1;
            //取得地址
            for (int s = 0; s < count; s++) {
                truePath = truePath.substring(0, truePath.lastIndexOf("/"));
            }
            //返回
            return truePath + "/" + nextPath;

        } catch (Exception ex) {
            return "";
        }
    }

    //构建
    public static String generateActionPath(String relativePath, String actionID) {
        if (relativePath.startsWith("/")) {
            return "/" + actionID + relativePath;
        } else {
            return "/" + actionID + "/" + relativePath;
        }
    }

    //构建
    public static String generateActionPathHttp(String httpPath, String actionID) {
        String retPath = httpPath;
        if (retPath.startsWith("http")) {
            retPath = retPath.replace(getHttpDomainPath(retPath), "");
        }
        return "/" + actionID + "/" + retPath;
    }


    //获取文件的名称
    public static String getNameString(String str) {
        //字符串
        if (str == null || str == "") {
            return "";
        }
        try {
            String name = str.substring(str.lastIndexOf("/") + 1, str.length());
            return name;
        } catch (Exception ex) {
            return "";
        }
    }

    //地址
    public static String getHttpDomainPath(String path) {

        try {
            String head = path.substring(0, path.indexOf("//") + 2);
            String tail = path.replace(head, "");
            String ret = head + "/" + tail.substring(tail.indexOf("/"), tail.length());
            return ret;
        } catch (Exception ex) {
            return "";
        }

    }

    public static List<String> splitStrList(String str, String split) {
        List<String> rets = new ArrayList<>();
        if (str != null && split != null) {
            String[] strs = str.split(split);
            for (int s = 0; s < strs.length; s++) {
                rets.add(strs[s]);
            }
        }
        return rets;
    }

    public static String strListToStr(List<String> strs, String split) {
        if (strs != null && split != null) {
            String retStr = "";
            for (int s = 0; s < strs.size(); s++) {
                if (s == strs.size() - 1) {
                    retStr = retStr + strs.get(s);
                } else {
                    retStr = retStr + strs.get(s) + split;
                }
            }
            return retStr;
        }
        return null;
    }

}
