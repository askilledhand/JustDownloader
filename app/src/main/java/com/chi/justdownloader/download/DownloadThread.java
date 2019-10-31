package com.chi.justdownloader.download;

import android.content.Context;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import com.chi.justdownloader.db.DatabaseHelper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public class DownloadThread extends Thread {

    private static final String TAG = "DownloadThread";
    private Context context;
    private DatabaseHelper mDatabaseHelper;
    private static final int MALLOC_REVEIVE_BUFFER = 16 * 1024;
    private static final int MALLOC_COPY_BUFFER = 128 * 1024;
    private static final String DEFAULT_FILE_PATH = Environment.getExternalStorageDirectory() +
            File.separator + "Download";
    //下载的URL
    private String url;
    //存放的目录
    private String filePath = DEFAULT_FILE_PATH;
    //保存的名字
    private String fileName;
    //线程ID
    private int threadId;
    //文件的起始位置，用于分片
    private int startIndex;
    //文件的终止位置
    private int endIndex;
    //文件开始下载的位置
    private int receive;
    //文件长度
    private int fileLength;
    private IThreadDownloader threadDownloadListener;
    //额外的请求头
    private Map<String, String> requestPropertyMap = new HashMap<>();
    //当前线程的暂停状态
    private volatile boolean isPause = false;
    //当前下载状态
    private volatile DownloadState downloadState = DownloadState.START;

    public boolean isPause() {
        return isPause;
    }

    private void setPause(boolean pause) {
        isPause = pause;
        downloadState = DownloadState.PAUSE;
        threadDownloadListener.onPause();
    }

    public DownloadState getDownloadState() {
        return downloadState;
    }

    @Override
    public void interrupt() {
        setPause(true);
        super.interrupt();
    }


    /**
     * @param context 上下文对象
     * @param url 文件链接地址
     * @param startIndex 分片下载起始位置
     * @param endIndex 分片下载终止位置
     * @param fileName 需要保存到本地的文件名
     * @param filePath 需要保存到本地的路径
     * @param requestPropertyMap 额外的请求头
     * @param threadDownloadListener 文件下载回调接口
     */
    public DownloadThread(Context context, String url, String fileName, String filePath, int startIndex, int endIndex,
                          Map<String, String> requestPropertyMap, IThreadDownloader
            threadDownloadListener) {
        this.context = context;
        this.url = url;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.fileName = fileName;
        this.filePath = filePath;
        this.requestPropertyMap = requestPropertyMap;
        this.threadDownloadListener = threadDownloadListener;
        this.fileLength = endIndex - startIndex + 1;
        this.filePath = filePath + (fileName == null ? RemoteFileUtil
                .getRemoteFileName(this.url) : fileName);
        mDatabaseHelper = JustDownloader.getDatabaseHelper(context);
        //mDatabaseHelper = new DatabaseHelper(context);
        //getFileLength(url);
    }

//    public DownloadThread(Context context, String url, int threadId, int startIndex, int
//            endIndex) {
//        this.context = context;
//        this.url = url;
//        this.threadId = threadId;
//        this.startIndex = startIndex;
//        this.endIndex = endIndex;
//        //mDatabaseHelper = new DatabaseHelper(context);
//        //getFileLength(url);
//        //checkDBRecord();
//    }
//
//    public DownloadThread(Context context, String url, String filePath, int threadId, long
//            startIndex, long
//                                  endIndex, Map<String, String> requestPropertyMap,
//                          ThreadDownloadListener
//                                  threadDownloadListener) {
//        this(context, url, filePath, threadId, startIndex, endIndex, threadDownloadListener);
//        this.requestPropertyMap = requestPropertyMap;
//    }
//
//    public DownloadThread(Context context, String url, String filePath, int threadId, long
//            startIndex, long
//                                  endIndex, ThreadDownloadListener threadDownloadListener) {
//        this(context, url, threadId, startIndex, endIndex, threadDownloadListener);
//        this.filePath = filePath;
//    }
//
//    public DownloadThread(Context context, String url, String filePath, String fileName, int
//            threadId, long
//                                  startIndex, long endIndex, ThreadDownloadListener
//                                  threadDownloadListener) {
//        this(context, url, filePath, threadId, startIndex, endIndex, threadDownloadListener);
//        this.fileName = fileName;
//    }
//
//    public DownloadThread(Context context, String url, int threadId, long startIndex, long endIndex,
//                          ThreadDownloadListener downloadListener) {
//        //this(context, url, threadId, startIndex, endIndex);
//        this.threadDownloadListener = downloadListener;
//    }

    private int getFileLength(String url) {
        try {
            int length = 0;
            URL mUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) mUrl.openConnection();
            conn.getHeaderFields();
            conn.setConnectTimeout(5 * 1000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("Referer", url);
            conn.setRequestProperty("Charset", "UTF-8");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.connect();
            int responseCode = conn.getResponseCode();
            // 判断请求是否成功处理
            if (responseCode == 200) {
                length = conn.getContentLength();
            }
            return length;
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "[getFileLength IOException]");
        }
        return 0;
    }

    private void checkDBRecord() {
        DownloadInfo info = mDatabaseHelper.getDownloadInfo(url, filePath, startIndex,Process.myTid());
        File file = new File(filePath);
        if (info == null) {
            if (file.exists()) {
                file.delete();
            }
            // 数据库无记录，初次保存长度
            mDatabaseHelper.insert(url, filePath, fileLength, startIndex);
        } else if (fileLength != info.getSize() || !file.exists()) {
            /* 由于下载过程断电，允许实际文件大小与数据库存储有一个buffer的误差 */
                //|| file.length() - info.getReceive() < 0
                //|| file.length() - info.getReceive() > MALLOC_COPY_BUFFER)
            if (file.exists()) {
                file.delete();
            }
            // 条件不符，更新长度和重置接受长度为0
            mDatabaseHelper.updateFileLength(url, filePath, fileLength, startIndex);
            mDatabaseHelper.updateReceive(url, filePath, 0, startIndex);
        } else {
            receive = info.getReceive();
            if (receive == fileLength) {
                threadDownloadListener.onProgress(0);
                threadDownloadListener.onFinish(fileLength);
                downloadState = DownloadState.FINISH;
            }
        }
    }

    @Override
    public void run() {
        checkDBRecord();
        if (downloadState == DownloadState.FINISH) {
            return;
        }
        InputStream inputStream = null;
        RandomAccessFile randomAccessFile = null;
        long total = 0;//记录下载的总量
        HttpURLConnection connection;
        int responseCode;
        try {
            URL urlURL = new URL(this.url);
            connection = (HttpURLConnection) urlURL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10 * 1000);
            //添加额外头部参数
            if (requestPropertyMap.size() > 0) {
                for (Map.Entry<String, String> stringStringEntry : requestPropertyMap.entrySet()) {
                    connection.setRequestProperty(stringStringEntry.getKey(), stringStringEntry
                            .getValue());
                }
            }
            connection.setRequestProperty("Connection", "Keep-Alive");
            //加上这个头部，则可以防止getContentLength()为-1
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Accept-Ranges", "bytes");
            //Range start
            int start = startIndex + receive;
            Log.d(TAG, "Range bytes=" + start + "-" + endIndex);
            //设置分段下载的头信息  Range:做分段
            connection.setRequestProperty("Range", "bytes=" + start + "-" + endIndex);
            connection.connect();
            responseCode = connection.getResponseCode();
            File file = new File(filePath);
            // 不支持断点续传，则从第一个字节的位置读写
            if (responseCode == 200) {
                if (file.exists()) {
                    file.delete();
                }
                receive = 0;
                mDatabaseHelper.updateReceive(url, filePath, 0, startIndex);
            }
            if (responseCode == 206 || responseCode == 200) {
                inputStream = connection.getInputStream();
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                //String totalPath = filePath + (fileName == null ? RemoteFileUtil
                //        .getRemoteFileName(this.url) : fileName);
                randomAccessFile = new RandomAccessFile(filePath, "rw");
                //设置该分段的文件起点位置
                randomAccessFile.seek(startIndex+receive);
                byte[] rcvBuffer = new byte[MALLOC_REVEIVE_BUFFER];
                byte[] cpyBuffer = new byte[MALLOC_COPY_BUFFER];
                int rcvLength = 0;
                int cpyLength = 0;
                int curLength = 0;
                // 下载前初始化已下载的长度（从数据库读取）
                int totalLength = receive;
                int percentRecord = -1;
                //threadDownloadListener.onProgress(receive);
                while ((rcvLength = inputStream.read(rcvBuffer)) != -1 && !isPause) {
                    //Log.i(TAG, "[getFile] rcvLength = " + rcvLength);
                    System.arraycopy(rcvBuffer, 0, cpyBuffer,
                            cpyLength, rcvLength);
                    cpyLength += rcvLength;
                    curLength += rcvLength;
                    // 本线程已下载的长度
                    totalLength += rcvLength;
                    // 由于不希望写入太频繁加入缓存写入机制
                    if (cpyLength >= MALLOC_COPY_BUFFER
                            - MALLOC_REVEIVE_BUFFER) {
                        //Log.i(TAG, "[getFile] randomAccessFile.write  cpyLength[" +
                        //        cpyLength + "]curLength[" + curLength + "]");
                        randomAccessFile.write(cpyBuffer, 0, cpyLength);
                        receive = totalLength;
                        // 更新数据库（已下载的长度）
                        mDatabaseHelper.updateReceive(url, filePath, receive, startIndex);
                        threadDownloadListener.onProgress(cpyLength);
                        cpyLength = 0;
                    }
                    // 百分比改变才会上报
                    /*int curPer = (int) ((long) curLength * 100 / fileLength);
                    if (curPer != percentRecord) {
                        percentRecord = curPer;
                        threadDownloadListener.onProgress(percentRecord);
                    }*/
                }
                if (!isPause) {
                    if (endIndex - startIndex + 1 != totalLength) {
                        threadDownloadListener.onFail();
                        downloadState = DownloadState.FAIL;
                        return;
                    }
                    randomAccessFile.write(cpyBuffer, 0, cpyLength);
                    //mDatabaseHelper.delete(url, filePath, startIndex);
                    mDatabaseHelper.updateReceive(url, filePath, totalLength, startIndex);
                    //callbackProgress(curLength);
                    //callbackFinish(DOWNLOAD_SUCCESS);
                    threadDownloadListener.onProgress(cpyLength);
                    threadDownloadListener.onFinish(totalLength);
                    downloadState = DownloadState.FINISH;
                } else {
                    //stop
                    threadDownloadListener.onPause();
                    connection.disconnect();
                }
            } else {
                threadDownloadListener.onFail();
                downloadState = DownloadState.FAIL;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "[getFile IOException]");
            threadDownloadListener.onFail();
            downloadState = DownloadState.FAIL;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
