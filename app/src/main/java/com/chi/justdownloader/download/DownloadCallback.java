package com.chi.justdownloader.download;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public interface DownloadCallback {
    /**
     * 下载成功
     *
     * @param url 下载链接
     * @param filePath 保存到本地的文件路径
     */
    void onSuccess(String url, String filePath);

    /**
     * 下载失败
     *
     * @param url 下载链接
     */
    void onFailure(String url);

    /**
     * 暂停
     *
     * @param url 下载链接
     */
    void onPause(String url);

    /**
     * 下载进度
     *
     * @param url 下载链接
     * @param progress 下载进度
     */
    void onProgress(String url, int progress);

    /**
     * 下载速度
     *
     * @param url 下载链接
     * @param speed 下载速度
     */
    void onSpeed(String url, double speed);
}
