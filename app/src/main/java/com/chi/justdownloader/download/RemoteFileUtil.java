package com.chi.justdownloader.download;

import android.util.Log;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created on 2018/4/29 16:29.
 *
 * @author askilledhand
 */

public class RemoteFileUtil {

    private static final String TAG = "RemoteFileUtil";

    /**
     * 获取服务端的文件名
     *
     * @param fileUrl 下载链接
     * @return 根据链接生成的文件名
     */
    public static String getRemoteFileName(String fileUrl) {
        int index = fileUrl.lastIndexOf("/");
        //找到最后一个?的位置，进行字符串分割
        int endIndex = fileUrl.indexOf("?");
        if (endIndex >= 0) {
            return fileUrl.substring(index, endIndex);
        } else {
            return fileUrl.substring(index, fileUrl.length());
        }
    }


    /**
     * 获取服务器的文件长度以及是否支持断点续传
     *
     * @param fileUrl 下载链接
     * @return RemoteFile
     */
    public static RemoteFile getRemoteFileLength(String fileUrl) {
        RemoteFile remoteFile = new RemoteFile(false, 0);
        HttpURLConnection connection;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10 * 1000);
            connection.setRequestProperty("Accept-Ranges", "bytes");
            connection.setRequestProperty("Connection", "Keep-Alive");
            //加上这个头部，则可以防止出现getContentLength()为-1的问题
            connection.setRequestProperty("Accept-Encoding", "identity");
            //必须加这个头部，否则无法返回正常支持断点续传的响应码206
            connection.setRequestProperty("Range", "bytes=0-");
            connection.connect();
            Log.d(TAG, "fileUrl: " + fileUrl);
            Log.d(TAG, "getContentLength: " + connection.getContentLength());
            Log.d(TAG, "getResponseCode: " + connection.getResponseCode());
            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 206) {
                remoteFile.setLength(connection.getContentLength());
                //设置该文件是否支持断点续传
                remoteFile.setSupportRange(connection.getResponseCode() == 206);
                return remoteFile;
            } else {
                return remoteFile;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return remoteFile;
        }
    }

}
