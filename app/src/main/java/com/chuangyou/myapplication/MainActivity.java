package com.chuangyou.myapplication;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


import com.flappygo.proxyserver.FlappyProxyServer;
import com.flappygo.proxyserver.Interface.ProxyCacheListener;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;


import java.util.ArrayList;

public class MainActivity extends Activity implements SurfaceHolder.Callback {

    //数据
    private MediaPlayer mediaPlayer;

    //请求MP4
    //private String url = "http://vfx.mtime.cn/Video/2019/03/19/mp4/190319212559089721.mp4";

    //private String url = "http://hk1.nspace.online/movie/fiction/na/alita.mp4";

    //private String url = "http://hk1.nspace.online/movie/fiction/na/alita.mp4";

    //private String url = "http://data.vod.itc.cn/?new=/91/42/pZqBu6MSS6qxJ83Lg979vf.mp4&vid=103937904&plat=17&mkey=TFnKEKpZxMTGPzyTf0wiSldd87RL7Z57&ch=null&user=api&uid=1608272337357415&SOHUSVP=VDu23U0Yy1SYOspSg9G7kMO1uHxStSw_3rI-9Y_I7uc&pt=1&prod=56&pg=1&eye=0&cv=1.0.0&qd=68000&src=11050001&ca=4&cateCode=300&_c=1&appid=tv";

    //private String url = "https://media.w3.org/2010/05/sintel/trailer.mp4";

    //private String url = "http://cctvalih5ca.v.myalicdn.com/live/cctv1_2/index.m3u8";

    //private String url = "https://cn7.qxreader.com/hls/20200122/59353b55ae2b1f8aba0f26b5b7c7977f/1579693880/index.m3u8";

    private String url = "https://baidu.com-l-baidu.com/20190813/14599_f58526fd/1000k/hls/index.m3u8";

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);


        //设置main
        setContentView(R.layout.activity_main);
        //设置video
        SurfaceView surfaceView = findViewById(R.id.video);
        //获取holder
        SurfaceHolder holder = surfaceView.getHolder();
        //设置
        holder.setKeepScreenOn(true);
        //添加回调
        holder.addCallback(this);

//        FlappyProxyServer.getInstance(getApplicationContext()).proxyCacheStart(url, new ProxyCacheListener() {
//            @Override
//            public void cachedProgress(int progress) {
//
//                System.out.println("PROGRESS::" + progress);
//            }
//
//            @Override
//            public void cachedSuccess() {
//
//                System.out.println("SUCCESSS");
//            }
//
//            @Override
//            public void cachedStoped() {
//
//                System.out.println("STOPED");
//            }
//        });


//        Handler handler = new Handler() {
//            public void handleMessage(Message message) {
//                FlappyProxyServer.getInstance(getApplicationContext()).proxyStop(url);
//            }
//        };
//        handler.sendEmptyMessageDelayed(1, 20000);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        reelaseMedia();

        ArrayList<String> options = new ArrayList<>();
        //设置音频输出模块
        options.add("--aout=opensles");
        //启用音频时间拉伸(默认启用)
        options.add("--audio-time-stretch");
        //文件缓存的大小
        options.add("--file-caching=2000");
        //减少花屏现象
        options.add("--network-caching=2000");
        //减少花屏现象
        options.add("--sout-mux-caching=2000");
        //修改解码器
        options.add("--codec=mediacodec,iomx,all");
        //RTSP帧缓冲大小，默认大小为100000
        options.add("--rtsp-frame-buffer-size=10000");
        //RTSP采用TCP传输方式
        options.add("--rtsp-tcp");
        //不重复播放
        options.add("--http-reconnect");
        //verbosity
        options.add("-vvv");
        //设置
        LibVLC libVLC = new LibVLC(getBaseContext(), options);
        //对这个地址进行代理
        String trueUrl = FlappyProxyServer.getInstance(getApplicationContext()).proxyStart(url,null);
        //创建media
        final Media media = new Media(libVLC, Uri.parse(trueUrl));
        //创建player
        mediaPlayer = new MediaPlayer(media);
        //设置suface
        mediaPlayer.getVLCVout().setVideoSurface(holder.getSurface(), holder);
        //添加
        mediaPlayer.getVLCVout().attachViews();
        //设置大小
        mediaPlayer.getVLCVout().setWindowSize(holder.getSurfaceFrame().width(), holder.getSurfaceFrame().height());
        //设置midia
        mediaPlayer.setMedia(media);
        //设置
        mediaPlayer.play();

        //跳转
        Handler handler = new Handler() {
            public void handleMessage(Message message) {
                //mediaPlayer.setPosition(0.8f);
            }
        };
        handler.sendEmptyMessageDelayed(1, 20000);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        reelaseMedia();
    }

    //释放
    private void reelaseMedia() {
        //播放器
        if (mediaPlayer != null) {
            //暂停
            mediaPlayer.pause();
            //停止
            mediaPlayer.stop();
            //释放
            if (mediaPlayer.getMedia() != null && mediaPlayer.getMedia() instanceof Media) {
                ((Media) mediaPlayer.getMedia()).releaseForce();
            }
            //释放
            mediaPlayer.releaseForce();
            //取消
            FlappyProxyServer.getInstance(getApplicationContext()).proxyStop(url,null);
            //停止
            mediaPlayer = null;
        }
    }


    public void onDestroy() {
        reelaseMedia();
        super.onDestroy();
    }
}
