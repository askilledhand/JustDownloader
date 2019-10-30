package com.chi.justdownloader.download;

import java.io.File;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public interface DownloadCallback {
    /**
     * 下载成功
     *
     * @param filePath 保存到本地的文件路径
     */
    void onSuccess(String filePath);

    /**
     * 下载失败
     */
    void onFailure();

    /**
     * 下载进度
     *
     * @param url 链接
     * @param progress 下载进度
     */
    void onProgress(String url, int progress);

    /**
     * 下载速度
     *
     * @param speed 下载速度
     */
    void onSpeed(double speed);
}
