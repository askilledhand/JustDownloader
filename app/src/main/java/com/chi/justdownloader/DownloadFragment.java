package com.chi.justdownloader;

import android.app.Fragment;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.chi.justdownloader.adapter.DownloadAdapter;
import com.chi.justdownloader.download.DownloadCallback;
import com.chi.justdownloader.download.DownloadTask;
import com.chi.justdownloader.download.JustDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2019/9/6.
 */

public class DownloadFragment extends Fragment implements DownloadCallback, DownloadAdapter.MyClickListener{

    private static final String TAG = "DownloadFragment";
    //同时最多下载数
    private int MOSTDOWNLOAD = 5;
    private RecyclerView recyclerView;
    private DownloadAdapter downloadAdapter;
    private List<DownloadItem> list = new ArrayList<>();
    private List<DownloadTask> taskList = new ArrayList<>();
    private HashMap<String, DownloadItem> urlTaskList= new HashMap<>();
    //private List<String> allUrl = new ArrayList<>();
    private Map<String, String> allUrl = new HashMap<>();
    //正在执行
    //private List<String> downloadUrl = new ArrayList<>();
    private Map<String, String> downloadUrl = new HashMap<>();
    //暂停执行
    //private List<String> pauseUrl = new ArrayList<>();
    private Map<String, String> pauseUrl = new HashMap<>();
    //等待执行
    //private List<String> waitUrl = new ArrayList<>();
    private Map<String, String> waitUrl = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle
            savedInstanceState) {
        View view = inflater.inflate(R.layout.download, container, false);
        recyclerView = view.findViewById(R.id.download_recycler);
        downloadAdapter = new DownloadAdapter(getActivity(), taskList);
        downloadAdapter.setMyClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager
                .VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(downloadAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL));
        return view;
    }

    public void addTask(String url, String fileName) {
        if (allUrl.containsKey(url)) {
            Toast.makeText(getActivity(), "Task is exist!", Toast.LENGTH_SHORT).show();
            return;
        } else {
            allUrl.put(url, fileName);
        }
        if (downloadUrl.size() > MOSTDOWNLOAD) {
            waitUrl.put(url, fileName);
        } else {
            startDownload(url, fileName);
        }
    }

    private void startDownload(String url, String fileName) {
        taskList.clear();
        Log.d(TAG, "startDownload: " + url);
        Log.d(TAG, "startDownload: " + JustDownloader.getInstance().getDownloadTaskLinkedHashMap().size());
        downloadUrl.put(url, fileName);
        JustDownloader.with(getActivity())
                .withUrl(url)
                //.allowBackgroundDownload(true)
                .threadCount(3)
                .filePath(Environment.getExternalStorageDirectory() + File.separator + "Download")
                .fileName(fileName)
                //.addExtraRequestProperty("", "")
                //.refreshTime(1000)
                .setDownloadListener(this)
                .download();
        Log.d(TAG, "startDownload: " + JustDownloader.getInstance().getDownloadTaskLinkedHashMap().size());
        DownloadTask downloadTask = JustDownloader.getInstance().getDownloadTaskLinkedHashMap().get(url);
        Log.d(TAG, "startDownload: " + downloadTask);
        taskList.add(downloadTask);
        downloadAdapter.notifyItemInserted(taskList.size());//通知演示插入动画
        downloadAdapter.notifyItemRangeChanged(taskList.size(), 1);//通知数据与界面重新绑定

    }

    public void pause() {
        taskList.get(0).pause();
    }

    @Override
    public void onSuccess(String url, String filePath) {
        allUrl.remove(url);
    }

    @Override
    public void onPause(String url) {

    }

    @Override
    public void onFailure(String url) {

    }

    @Override
    public void onProgress(String url, int progress) {
        Log.d(TAG, "onProgress: " + progress);
        DownloadTask downloadTask = JustDownloader.getInstance().getDownloadTaskLinkedHashMap().get(url);
        downloadAdapter.notifyItemChanged(getUrlPosition(allUrl, url), "progress");
    }

    @Override
    public void onSpeed(String url, String speed) {

    }

    @Override
    public void setOnClickListener(int i) {

    }

    @Override
    public void setOnStartListener(int i) {
        String start = list.get(i).getUrl();
    }

    @Override
    public void setOnPauseListener(int i) {
        String pause = list.get(i).getUrl();
        //if (downloadTask != null) {
        //    downloadTask.pause();
        //}
    }

    private int getUrlPosition(Map<String, String> map, String url) {
        int position = 0;
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String key = (String) entry.getKey();
            // Object val = entry.getValue();
            if (key.equals(url)) {
                return position;
            }
            position ++;
        }
        return position;
    }
}
