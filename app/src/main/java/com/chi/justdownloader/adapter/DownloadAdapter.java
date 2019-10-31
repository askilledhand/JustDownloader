package com.chi.justdownloader.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.chi.justdownloader.DownloadItem;
import com.chi.justdownloader.R;
import com.chi.justdownloader.download.DownloadTask;

import java.util.List;

/**
 * Created by askilledhand on 2019/9/6.
 */

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.MyHolder> {

    private static final String TAG = "DownloadAdapter";
    private Context context;
    private List<DownloadTask> list;
    private MyClickListener myClickListener;

    public DownloadAdapter(Context context, List<DownloadTask> list) {
        this.context = context;
        this.list = list;
    }

    public void setMyClickListener(MyClickListener myClickListener) {
        this.myClickListener = myClickListener;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.download_item,
                parent, false);
        MyHolder holder = new MyHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(MyHolder holder, int position, List<Object> payloads) {
        Log.d(TAG, "onBindViewHolder: -----------------------1" + payloads.size() + "   /    " + payloads.get(0));
        if (payloads.isEmpty()){
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        DownloadTask downloadTask = list.get(position);
        //循环得到payloads里面的参数
        for (Object payload:payloads) {
            switch (String.valueOf(payload)){
                case "progress":
                    holder.progressBar.setProgress(downloadTask.mLastProgress);
                    break;
                case "speed":
                    //holder.speedView.setText(downloadTask.mDownloadSpeed);
                    break;
                case "state":
                    //holder.stateView.setText(bean.getAge()+"");
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: -----------------------2");
        Log.d(TAG, "onBindViewHolder: " + position);
        DownloadTask downloadTask = list.get(position);
        Log.d(TAG, "onBindViewHolder: " + downloadTask);
        if (downloadTask != null) {
            holder.progressBar.setProgress(downloadTask.mLastProgress);
        }
        holder.button.setText("start");
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.button.getText().toString().equalsIgnoreCase("start")) {
                    holder.button.setText("pause");
                    myClickListener.setOnPauseListener(position);
                } else if (holder.button.getText().toString().equalsIgnoreCase("pause")) {
                    holder.button.setText("start");
                    myClickListener.setOnStartListener(position);
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class MyHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;
        Button button;

        public MyHolder(View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progress_item);
            button = itemView.findViewById(R.id.button_item);
        }
    }

    //事件监听
    public interface MyClickListener{
        void setOnClickListener(int i);
        void setOnStartListener(int i);
        void setOnPauseListener(int i);
    }
}
