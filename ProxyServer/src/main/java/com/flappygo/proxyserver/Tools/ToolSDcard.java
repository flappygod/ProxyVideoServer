/*
 * Copyright 2013 The JA-SIG Collaborative. All rights reserved.
 * distributed with this file and available online at
 * http://www.etong.com/
 */
package com.flappygo.proxyserver.Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


/**
 * <p>Title: WriteSDcard</p>
 * <p>Package: com.etong.mall.utils</p>
 * <p>Description: TODO(这里用一句话描述这个类的作用)</p>
 *
 * @author:邵天元
 * @version:2015-5-19下午2:03:32
 * @since 1.0
 */
public class ToolSDcard {


    /***************
     * 将String写入到文件中
     *
     * @param dirPath  文件的路径 以/结尾
     * @param fileName 文件名称
     * @param message  写入文件的内容
     */
    public static void writeStringSdcard(String dirPath, String fileName, String message) {

        try {
            ToolDirs.createDir(dirPath, false);
            File file = new File(dirPath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fout = new FileOutputStream(file);
            byte[] bytes = message.getBytes();
            fout.write(bytes);
            fout.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /***************
     * 读取文件路径的文件字符串
     *
     * @param dirPath  文件的路径 以/结尾
     * @param fileName 文件名称
     */
    public static String readStringSdcard(String dirPath, String fileName) {
        try {
            File file = new File(dirPath + fileName);
            if (!file.exists()) {
                return null;
            }
            FileInputStream fin = new FileInputStream(file);
            //新建一个字节数组
            byte[] b = new byte[fin.available()];
            //将文件中的内容读取到字节数组中
            fin.read(b);
            fin.close();
            //再将字节数组中的内容转化成字符串形式输出
            String ret = new String(b);
            return ret;
        } catch (Exception e) {
            return null;
        }
    }


    /***************
     * 将对象写入到文件中
     *
     * @param dirPath  文件的路径 以/结尾
     * @param fileName 文件名称
     * @param object  写入文件的内容
     */
    public static boolean writeObjectSdcard(String dirPath, String fileName, Object object) {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dirPath + fileName));
            oos.writeObject(object);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /***************
     * 读取文件路径的对象
     *
     * @param dirPath  文件的路径 以/结尾
     * @param fileName 文件名称
     */
    public static Object getObjectSdcard(String dirPath, String fileName) {
        try {
            ObjectInputStream oos = new ObjectInputStream(new FileInputStream(dirPath + fileName));
            Object object = oos.readObject();
            return object;
        } catch (Exception e) {
            return null;
        }
    }

}
