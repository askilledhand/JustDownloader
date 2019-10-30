package com.chi.justdownloader.download;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.chi.justdownloader.db.DatabaseHelper;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public class JustDownloader {
    private static final String TAG = "JustDownloader";
    private static volatile JustDownloader sJustDownloader;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int THREAD_SIZE = Math.max(3, Math.min(CPU_COUNT - 1, 5));
    private static int CORE_POOL_SIZE = THREAD_SIZE;//核心线程数
    private ExecutorService mExecutorService;//线程池
    public static DatabaseHelper mDatabaseHelper;
    //private final Deque<DownloadTask> readyTasks = new ArrayDeque<>();
    private final Deque<DownloadTask> runningTasks = new ArrayDeque<>();
    //private final Deque<DownloadTask> stopTasks = new ArrayDeque<>();

    public LinkedHashMap<String, DownloadTask> downloaderList = new LinkedHashMap<>();


    private JustDownloader() {
    }

    public static JustDownloader getInstance() {
        if (sJustDownloader == null) {
            synchronized (JustDownloader.class) {
                if (sJustDownloader == null) {
                    sJustDownloader = new JustDownloader();
                }
            }
        }
        return sJustDownloader;
    }

    /**
     * 创建线程池
     *
     * @return mExecutorService
     */
    public synchronized ExecutorService executorService() {
        if (mExecutorService == null) {
            mExecutorService = new ThreadPoolExecutor(CORE_POOL_SIZE, Integer.MAX_VALUE, 60,
                    TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new ThreadFactory() {
                @Override
                public Thread newThread(@NonNull Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setDaemon(false);
                    return thread;
                }
            });
        }
        return mExecutorService;
    }

    public static DatabaseHelper getDatabaseHelper(Context context) {
        if (mDatabaseHelper == null) {
            synchronized (JustDownloader.class) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new DatabaseHelper(context);
                }
            }
        }
        return mDatabaseHelper;
    }

    public LinkedHashMap<String, DownloadTask> getDownloadTaskLinkedHashMap() {
        return downloaderList;
    }

    /**
     * 添加下载任务
     * @param downloadTask 下载任务
     */
    public void addDownloader(DownloadTask downloadTask) {
        Log.d(TAG, "addDownloader: " + downloadTask);
        if (downloaderList == null) {
            downloaderList = new LinkedHashMap<>();
        }
        downloaderList.put(downloadTask.url, downloadTask);
    }

    /**
     * 暂停下载
     * @param downloadTask 下载任务
     * @return JustDownloader对象
     */
    public JustDownloader pauseDownloader(@NonNull DownloadTask downloadTask) {
        if (downloadTask.getDownloadState() == DownloadState.START) {
            downloadTask.pause();
        }
        return this;
    }


    /**
     * @param context  上下文对象
     * @param url      下载的地址
     * @param name     文件名
     * @param path     文件保存路径
     * @param callBack 回调接口
     */
    public void startDownload(Context context, String url, String name, String path, final DownloadCallback
            callBack) {
        //getDatabaseHelper(context);
        DownloadTask downloadTask = new DownloadTask(context, url, name, path, CORE_POOL_SIZE,
                0, callBack);
        downloadTask.addExtraRequestProperty("", "").download();
        runningTasks.add(downloadTask);
    }

    /**
     * @param context 上下文对象
     * @return 下载任务
     */
    public static DownloadTask with(Context context) {
        //getDatabaseHelper(context);
        DownloadTask downloadTask = new DownloadTask(context);
        return downloadTask;
    }

    /*private static void getDatabaseHelper(Context context) {
        if (mDatabaseHelper == null) {
            synchronized (JustDownloader.class) {
                if (mDatabaseHelper == null) {
                    mDatabaseHelper = new DatabaseHelper(context);
                }
            }
        }
    }*/

    /**
     * @param downLoadTask 下载任务
     */
    public void recyclerTask(DownloadTask downLoadTask) {
        runningTasks.remove(downLoadTask);
        //参考OkHttp的Dispatcher()的源码
        //readyTasks.
    }

    public void stopDownLoad(String url) {
        //这个停止是不是这个正在下载的
    }
}
