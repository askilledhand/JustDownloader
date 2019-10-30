package com.chi.justdownloader.download;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public interface IThreadDownloader {

    void onProgress(int progress);

    void onDownload(long currentDownloadSize);

    void onFinish(int total);

    void onSuccess();

    void onPause();

    void onFail();


}
