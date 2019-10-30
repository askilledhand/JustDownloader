package com.chi.justdownloader.download;

/**
 * Created by jxsong on 2019/8/22.
 *
 * @author jxsong
 */

public class DownloadInfo {
	private String mUrl;
	private String mPath;
	private int mSize;
	private int mReceive;
	private int mStartIndex;

	public String getUrl() {
		return mUrl;
	}

	public String getPath() {
		return mPath;
	}

	public int getSize() {
		return mSize;
	}

	public int getReceive() {
		return mReceive;
	}

	public void setUrl(String url) {
		mUrl = url;
	}

	public void setPath(String path) {
		mPath = path;
	}

	public void setStartIndex(int startIndex) {
		mStartIndex = startIndex;
	}

	public void setSize(int size) {
		mSize = size;
	}

	public void setReceive(int receive) {
		mReceive = receive;
	}
}
