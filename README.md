// 示例
allprojects {<br>
	repositories {<br>
	...<br>
	maven { url 'https://jitpack.io' }<br>
    }<br>
}<br>

implementation 'com.github.askilledhand:JustDownloader:1.1.0' <br>

DownloadTask downloadTask = JustDownloader.with(context)<br>
	.withUrl(url) // 文件路径<br>
	.threadCount(3) // 线程数<br>
	.filePath(Environment.getExternalStorageDirectory() + File.separator + "Download") // 下载路径<br>
	.fileName(fileName) // 文件名<br>
	.setDownloadListener(this); // 下载状态反馈<br>
downloadTask.download();<br>
