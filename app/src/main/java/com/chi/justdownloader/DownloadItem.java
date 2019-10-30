package com.chi.justdownloader;

/**
 * Created by askilledhand on 2019/9/6.
 *
 * DownloadItem记录下载链接/下载进度/当前下载状态
 */

public class DownloadItem {

    private String url;
    private int progress;
    private String status;

    public DownloadItem(String url, int progress, String status) {
        this.url = url;
        this.progress = progress;
        this.status = status;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
