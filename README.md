### 示例
```
allprojects {
	repositories {
	...
	maven { url 'https://jitpack.io' }
    }
}
```
```
implementation 'com.github.askilledhand:JustDownloader:1.1.0' 
```
```
DownloadTask downloadTask = JustDownloader.with(context)
	.withUrl(url) // 文件路径
	.threadCount(3) // 线程数
	.filePath(Environment.getExternalStorageDirectory() + File.separator + "Download") // 下载路径
	.fileName(fileName) // 文件名
	.setDownloadListener(this); // 下载状态反馈
downloadTask.download();
```
