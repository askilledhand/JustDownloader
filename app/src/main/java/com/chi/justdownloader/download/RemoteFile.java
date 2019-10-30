package com.chi.justdownloader.download;

/**
 * Created on 2019/8/22.
 *
 * @author askilledhand
 *
 * 下载链接对应的类，包含链接里包含的文件名、文件大小等信息
 */

public class RemoteFile {

    //是否支持断点续传的响应码
    public static final int SUPPORT_RANGE = 206;
    //是否支持断点续传
    private boolean supportRange = false;
    // 文件长度
    private int length;

    public RemoteFile(int code, int length) {
        this.supportRange = (code == SUPPORT_RANGE);
        this.length = length;
    }

    public RemoteFile() {
    }

    public RemoteFile(boolean supportRange, int length) {
        this.supportRange = supportRange;
        this.length = length;
    }

    public boolean isSupportRange() {
        return supportRange;
    }

    public void setSupportRange(boolean supportRange) {
        this.supportRange = supportRange;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
