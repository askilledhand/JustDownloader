package com.chi.justdownloader.download;

import android.app.Activity;
import android.content.Context;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.internal.operators.parallel.ParallelRunOn;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public class DownloadTask implements IDownloader{
    private static final String TAG = "DownloadTask";
    private Context context;
    //文件下载的url
    public String url;
    //文件的名称
    public String fileName;
    //文件的路径
    public String filePath;
    //文件的大小
    public int mContentLength;
    //下载文件的线程的个数
    public int mThreadSize;
    //线程下载成功的个数,变量加个volatile，多线程保证变量可见性
    public volatile int mSuccessNumber;
    //总进度=每个线程的进度的和
    public int mCurrentLength;
    //总进度=每个线程的进度的和
    public int mTotalLength;
    //最近一次进度，可用来对比当前进度，变化时才回调更新
    public int mLastProgress = 0;
    //是否显示进度
    public boolean showProgress = false;
    //周期计算下载速度开始时间
    public long start;
    //周期计算下载速度开始时间
    public long startTime;
    //周期计算下载速度期间下载的大小
    public long totalRead = 0;
    //下载速度
    public double mDownloadSpeed = 0;
    private Timer timer;
    private final double NANOS_PER_SECOND = 1000000000.0;//1秒=10亿nanoseconds
    private final double BYTES_PER_MIB = 1024 * 1024;//1M=1024*1024byte
    public volatile DownloadState downloadState = DownloadState.PAUSE;//下载状态,默认为暂停
    private Map<String, String> requestPropertyMap = new HashMap<>();
    private List<DownloadThread> mDownloadThreads = new ArrayList<>();
    private DownloadCallback mDownloadCallback;

    public DownloadTask(Context context) {
        this.context = context;
    }

    /**
     * @param context        上下文对象
     * @param url            下载的地址
     * @param name           文件名
     * @param path           文件保存路径
     * @param threadSize     该文件由几个线程下载
     * @param contentLength  文件长度
     * @param callBack       回调接口
     */
    public DownloadTask(Context context, String url, String name, String path, int threadSize,
                        int contentLength, DownloadCallback callBack) {
        this.context = context;
        this.url = url;
        this.fileName = name;
        this.filePath = path;
        this.mThreadSize = threadSize;
        this.mContentLength = contentLength;
        this.mDownloadCallback = callBack;
    }

    /**
     * 设置文件下载路径
     * @param url 文件下载路径
     * @return 下载任务
     */
    public DownloadTask withUrl(String url) {
        this.url = url;
        Log.d(TAG, "JustDownloader.getInstance().addDownloader(DownloadTask.this): ");
        JustDownloader.getInstance().addDownloader(DownloadTask.this);
        return this;
    }

    /**
     * 添加额外的请求头
     * @param key 请求头-键
     * @param value 请求头-值
     * @return DownloadTask
     */
    public DownloadTask addExtraRequestProperty(String key, String value) {
        requestPropertyMap.put(key, value);
        return this;
    }

    /**
     * 添加额外的请求头
     * @param map 请求头map
     * @return DownloadTask
     */
    public DownloadTask addExtraRequestPropertyMap(Map<String, String> map) {
        requestPropertyMap.putAll(map);
        return this;
    }

    /**
     * 设置文件下载的目录
     * @param filePath 文件下载的目录
     * @return DownloadTask
     */
    public DownloadTask filePath(String filePath) {
        if (!filePath.endsWith("/")) {
            filePath = filePath + File.separator;
        }
        this.filePath = filePath;
        return this;
    }

    /**
     * 设置文件名，非必须，未设置则用服务器文件名
     * @param fileName 文件名
     * @return DownloadTask
     */
    public DownloadTask fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * 设置下载线程数
     * @param mThreadSize 线程数 最大为5
     * @return DownloadTask
     */
    public DownloadTask threadCount(int mThreadSize) {
        //设置有效值
        if (mThreadSize <= 0) {
            throw new RuntimeException("threadCount must greater than 0!");
        }
        if (mThreadSize > 5) {
            mThreadSize = 5;
        }
        this.mThreadSize = mThreadSize;
        return this;
    }

    public DownloadTask setDownloadListener(DownloadCallback mDownloadCallback) {
        this.mDownloadCallback = mDownloadCallback;
        return this;
    }

    /**
     * 获取下载状态
     * @return 当前下载状态
     */
    public DownloadState getDownloadState() {
        return downloadState;
    }


    @Override
    public void download() {
        JustDownloader.getInstance().executorService().execute(new Runnable() {
            @Override
            public void run() {
                RemoteFile remoteFile = RemoteFileUtil.getRemoteFileLength(url);
                if (remoteFile.getLength() < 1) { // 获取文件长度失败
                    mDownloadCallback.onFailure(url);
                    return;
                } else {
                    mContentLength = remoteFile.getLength();
                }
                Log.d(TAG, "mContentLength: " + mContentLength);
                Log.d(TAG, "remoteFile.isSupportRange(): " + remoteFile.isSupportRange());
                if (!remoteFile.isSupportRange()) { //不支持断点续传
                    mThreadSize = 1;
                }
                for (int i = 0; i < mThreadSize; i++) {
                    //初始化的时候，需要读取数据库
                    //每个线程的下载的大小threadSize
                    int perThreadSize = mContentLength / mThreadSize;
                    //开始下载的位置
                    int start = i * perThreadSize;
                    //结束下载的位置
                    int end = start + perThreadSize - 1;
                    DownloadInfo info = JustDownloader.getDatabaseHelper(context).getDownloadInfo(url, filePath + (fileName == null ? RemoteFileUtil
                            .getRemoteFileName(url) : fileName), start, Process.myTid());
                    if (info != null) {
                        // 获取每个线程已经下载的大小
                        int received = info.getReceive();
                        mCurrentLength += received;
                        totalRead = mCurrentLength;
                    }
                    if (i == mThreadSize - 1) {
                        end = mContentLength - 1;
                        // 已获取所有子线程已经下载的大小，显示进度从已下载大小处递增(否则可能出现进度忽大忽小的情况)
                        showProgress = true;
                        getDownloadSpeed();
                    }
                    DownloadThread downloadThread = new DownloadThread(context, url, fileName, filePath, start, end,
                            requestPropertyMap, new IThreadDownloader() {

                        @Override
                        public void onProgress(int progress) {
                            mCurrentLength += progress;
                            long mcl = mCurrentLength;
                            final int mCurrentProgress = (int) (mcl * 100 / mContentLength);
                            if (showProgress) {
                                // 进度值发生变化
                                if (mCurrentProgress != mLastProgress) {
                                    mLastProgress = mCurrentProgress;
                                    ((Activity)context).runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDownloadCallback.onProgress(url, mLastProgress);
                                        }
                                    });
                                }
                            }
                            //计算下载速度
                            long start = System.nanoTime();   //开始时间
                            long totalRead = 0;  //总共下载了多少
                            final double NANOS_PER_SECOND = 1000000000.0;  //1秒=10亿nanoseconds
                            final double BYTES_PER_MIB = 1024 * 1024;    //1M=1024*1024byte
                            double speed = NANOS_PER_SECOND / BYTES_PER_MIB * totalRead /
                                    (System.nanoTime() - start + 1);
                        }

                        @Override
                        public void onDownload(long currentDownloadSize) {

                        }

                        @Override
                        public void onFinish(int total) {
                            mTotalLength += total;
                            if (mTotalLength == mContentLength) {
                                final String totalPath = filePath + (fileName == null ? RemoteFileUtil
                                        .getRemoteFileName(url) : fileName);
                                ((Activity)context).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mDownloadCallback.onSuccess(url, totalPath);
                                    }
                                });
                                cancelTimer();
                            }
                        }

                        @Override
                        public void onSuccess() {
                            cancelTimer();
                        }

                        @Override
                        public void onPause() {
                            ((Activity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mDownloadCallback.onPause(url);
                                }
                            });
                            cancelTimer();
                        }

                        @Override
                        public void onFail() {
                            ((Activity)context).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mDownloadCallback.onFailure(url);
                                }
                            });
                            cancelTimer();
                        }
                    });
                    JustDownloader.getInstance().executorService().execute(downloadThread);
                    mDownloadThreads.add(downloadThread);
                }
            }

            // 获取下载速度
            private void getDownloadSpeed() {
                startTime = System.nanoTime();//开始时间
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        long tmpTotalRead = mCurrentLength - totalRead;//期间下载的大小
                        final double speed = NANOS_PER_SECOND / BYTES_PER_MIB * tmpTotalRead /
                                (System.nanoTime() - startTime + 1);
                        mDownloadSpeed = speed;
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mDownloadCallback.onSpeed(url, speed);
                            }
                        });
                        totalRead = mContentLength;
                        startTime = System.nanoTime();
                    }
                },1000,1000);//每隔一秒使用handler发送一下消息,也就是每隔一秒执行一次,一直重复执行

            }
        });
    }

    /**
     * double转String,保留小数点后两位
     * @param num
     * @return
     */
    public static double doubleToString(double num) {
        //使用0.00不足位补0，#.##仅保留有效位
        String value = new DecimalFormat("0.00").format(num);
        return convertToDouble(value, 0);
    }

    //把String转化为double
    public static double convertToDouble(String number, double defaultValue) {
        if (TextUtils.isEmpty(number)) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(number);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private void cancelTimer() {
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    public void pause() {
        for (DownloadThread thread : mDownloadThreads) {
            thread.interrupt();
        }
        mDownloadThreads.clear();
        mCurrentLength = 0;
        cancelTimer();
    }

    @Override
    public void start() {
        //for (DownloadThread thread : mDownloadThreads) {
        //    JustDownloader.getInstance().executorService().execute(thread);
        //}
        //cancelTimer();
    }

    @Override
    public void recovery() {

    }

    @Override
    public void delete() {

    }

    @Override
    public boolean deleteCacheFile() {
        return false;
    }
}
