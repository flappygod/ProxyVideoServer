# ProxyVideoServer
ProxyServer for video

很多视频播放器是不带缓存功能的，这里使用代理服务器的方式解决该问题，在本地搭建服务器连接真实资源并缓存，并将缓存后的数据返回给播放器。

使用方式:


String trueUrl = FlappyProxyServer.getInstance(getApplicationContext()).proxyStart(url);


传入地址为真实视频地址，返回url为经过代理的地址，目前支持m3u8及mp4等文件的播放，其他文件暂时没有测试。
