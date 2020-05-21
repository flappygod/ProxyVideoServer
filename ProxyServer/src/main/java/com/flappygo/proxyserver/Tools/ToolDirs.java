package com.flappygo.proxyserver.Tools;

import android.os.Environment;
import android.os.StatFs;

import com.flappygo.proxyserver.Exception.LDirException;

import java.io.File;

/**********
 *
 * Package Name:com.flappygo.lipo.limagegetter.tools <br/>
 * ClassName: LDir <br/>
 * Function: 文件夹创建等 <br/>
 * date: 2016-3-9 上午10:14:47 <br/>
 *
 * @author lijunlin
 */
public class ToolDirs {

    /**************
     * 判断SD卡是否插入
     * @return
     */
    public static boolean isSDCardMounted() {
        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return true;
        } else {
            return false;
        }
    }

    //获取SD卡当前的剩余容量，以MB为单位
    public static long getSDAvailableSize() {
        if (isSDCardMounted()) {
            File file = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(file.getPath());
            //获得Sdcard上每个block的size
            long blockSize = statFs.getBlockSizeLong();
            //获取可供程序使用的Block数量
            long blockavailable = statFs.getAvailableBlocksLong();
            //计算标准大小使用：1024，当然使用1000也可以
            long blockavailableTotal = blockSize * blockavailable / 1024 / 1024;
            //当前总共的大小
            return blockavailableTotal;
        }
        return -1;
    }


    /************
     * 通过dirPath 创建文件夹
     *
     * @param dirPath
     *            文件夹目录
     * @param noMedia
     *            是否同时创建noMedia文件
     * @throws Exception
     *             异常
     */
    public static void createDir(String dirPath, boolean noMedia)
            throws Exception {
        // 如果没有文件夹插入进来
        if (!isSDCardMounted()) {
            // 抛出未能挂载的异常
            throw new LDirException(dirPath, "sdcard no mounted");
        }

        // 创建file
        File file = new File(dirPath);
        // 创建操作进行加锁
        synchronized (ToolDirs.class) {
            // 如果file不存在
            if (!file.exists()) {
                // 创建文件夹
                if (!file.mkdirs()) {
                    throw new LDirException(dirPath, "can't  create mkdirs");
                }
            }
        }

        // 如果设置了noMedia
        if (noMedia) {
            File nomidia = new File(dirPath + ".nomedia");
            if (!nomidia.exists()) {
                if (!nomidia.createNewFile()) {
                    throw new LDirException(dirPath,
                            "can't  create nomedia file");
                }
            }
        }
    }

    /************************
     * 删除文件夹下面的文件
     *
     * @param file
     *            文件夹路径
     * @param deleteDir
     *            是否同时删除文件夹
     * @return
     */
    public static void deleteDirFiles(File file, boolean deleteDir, boolean jumpCantDelete)
            throws Exception {

        // 如果是
        if (file.isDirectory()) {
            // 列出文件
            File[] childFiles = file.listFiles();
            // 没有文件直接返回
            if (childFiles == null || childFiles.length == 0) {
                return;
            }
            for (int i = 0; i < childFiles.length; i++) {
                deleteDirFiles(childFiles[i], deleteDir, jumpCantDelete);
            }
            // 如果确定连文件夹一起删除
            if (deleteDir) {
                // 无法删除文件夹
                if (!file.delete()) {
                    if (!jumpCantDelete) {
                        throw new LDirException(file.getAbsolutePath(),
                                "can't  delete dir");
                    }
                }
            }
        } else {
            // 如果删除失败
            if (!file.delete()) {
                if (!jumpCantDelete) {
                    throw new LDirException(file.getAbsolutePath(),
                            "can't  delete file");
                }
            }
            file = null;
        }
    }

    /*********
     * 删除文件夹下内容
     *
     * @param dirPath
     *            文件夹路径
     * @param deleteDir
     *            是否连带文件夹一起删除
     * @throws Exception
     *             异常
     */
    public static void deleteDirFiles(String dirPath, boolean deleteDir)
            throws Exception {
        // 如果没有文件夹插入进来
        if (!isSDCardMounted()) {
            // 抛出未能挂载的异常
            throw new LDirException(dirPath, "sdcard no mounted");
        }
        deleteDirFiles(new File(dirPath), deleteDir, false);
    }


    /**********************
     *
     * @param dirPath  地址
     * @throws Exception 错误
     */
    public static void deleteDirFiles(String dirPath) throws Exception {
        // 如果没有文件夹插入进来
        if (!isSDCardMounted()) {
            // 抛出未能挂载的异常
            throw new LDirException(dirPath, "sdcard no mounted");
        }
        deleteDirFiles(new File(dirPath), false, true);
    }
}
