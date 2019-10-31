package com.chi.justdownloader;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chi.justdownloader.download.DownloadCallback;
import com.chi.justdownloader.download.DownloadTask;
import com.chi.justdownloader.download.DownloadThread;
import com.chi.justdownloader.download.JustDownloader;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.function.Consumer;



/**
 * Created by Administrator on 2019/8/23.
 */

public class MainActivity extends Activity implements DownloadCallback {

    private static final String TAG = "MainActivity";
    String url_0 = "http://pic2.zhimg.com/80/v2-4bd879d9876f90c1db0bd98ffdee17f0_hd.jpg";
    String url_1 = "http://www.sxotu.com/u/20180509/09154140.gif";
    String url_2 = "http://pic1.win4000.com/wallpaper/2017-10-11/59dde2bca944f.jpg";
    String url_3 = "http://gdown.baidu.com/data/wisegame/d2fbbc8e64990454/wangyiyunyinle_87.apk";
    String url_4 = "https://desktop.githubusercontent.com/releases/2.2.2-5a1cfa2d/GitHubDesktopSetup.exe";

    private Button start;
    private Button pause;
    private ProgressBar progressBar;
    private TextView progressTV;

    private DownloadFragment downloadFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //JustDownloader.getInstance().startDownload(this, "", "", "", this);

        init();
    }

    private void init() {
        callRxPermission();
        downloadFragment = (DownloadFragment) getFragmentManager().findFragmentByTag("fragment");
        start = findViewById(R.id.start);
        pause = findViewById(R.id.pause);
        progressBar = findViewById(R.id.progress);
        progressTV = findViewById(R.id.progress_tv);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFragment.addTask(url_4, "GitHubDesktopSetup.exe");
                //startDownload();
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFragment.addTask(url_0, "test.jpg");
                //pauseDonload();
            }
        });
    }

    private void startDownload() {
        Log.d(TAG, "startDownload: " + Environment.getExternalStorageDirectory());
        JustDownloader.with(this)
                .withUrl(url_3)
                //.allowBackgroundDownload(true)
                .threadCount(3)
                .filePath(Environment.getExternalStorageDirectory() + File.separator + "Download")
                .fileName("music.apk")
                //.addExtraRequestProperty("", "")
                //.refreshTime(1000)
                .setDownloadListener(this)
                .download();
    }

    private void pauseDonload() {
        LinkedHashMap<String, DownloadTask> downloadTaskLinkedHashMap = JustDownloader
                .getInstance().getDownloadTaskLinkedHashMap();
        if (downloadTaskLinkedHashMap != null && downloadTaskLinkedHashMap.size() > 0) {
            DownloadTask downloadTask = JustDownloader.getInstance().getDownloadTaskLinkedHashMap
                    ().get(url_3);
            if (downloadTask != null) {
                downloadTask.pause();
            }
        }
    }

    @Override
    public void onSuccess(String filePath) {
        Log.d(TAG, "onSuccess: " + filePath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "FINISH", Toast.LENGTH_LONG).show();
            }
        });
        if (new File(filePath).exists()) {
            Log.d(TAG, "onSuccess: exists");
        }
        installApk(this, filePath);
    }

    @Override
    public void onFailure() {
        Log.d(TAG, "onFailure: ");
    }

    @Override
    public void onProgress(String url, int progress) {
        Log.d(TAG, "onProgress1: " + progress);
        int i = JustDownloader.getInstance().getDownloadTaskLinkedHashMap().get(url_4).mLastProgress;
        Log.d(TAG, "onProgress2: " + i);
        progressBar.setProgress(progress);
        progressTV.setText(progress + "%");
        final int p = progress;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //progressTV.setText(p + "%");
            }
        });
    }

    @Override
    public void onSpeed(double speed) {
        Log.d(TAG, "onSpeed: " + speed);
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    public static void installApk(Context context, String apkPath) {
        if (context == null || TextUtils.isEmpty(apkPath)) {
            return;
        }
        File file = new File(apkPath);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        //判读版本是否在7.0以上
        if (Build.VERSION.SDK_INT >= 24) {
            Log.v(TAG,"7.0以上，正在安装apk...");
            //provider authorities
            Uri apkUri = FileProvider.getUriForFile(context, "com.chi.justdownloader.fileprovider", file);
            //Granting Temporary Permissions to a URI
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        } else {
            Log.v(TAG,"7.0以下，正在安装apk...");
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }


    /**
    * RxPermission
    * https://github.com/tbruyelle/RxPermissions
    * */
    private void callRxPermission(){
        RxPermissions rxPermissions = new RxPermissions(this); // where this is an Activity instance
        rxPermissions.requestEach(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(new io.reactivex.functions.Consumer<Permission>() {
                    @Override
                    public void accept(Permission permission) throws Exception {
                        if (permission.granted) {
                            // 用户已经同意该权限
                            Log.d(TAG, permission.name + " is granted.");
                        } else if (permission.shouldShowRequestPermissionRationale) {
                            // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框
                            Log.d(TAG, permission.name + " is denied. More info should be provided.");
                        } else {
                            // 用户拒绝了该权限，并且选中『不再询问』
                            Log.d(TAG, permission.name + " is denied.");
                        }
                    }
                });
    }
}
