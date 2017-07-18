package com.werb.mediautilsdemo.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.werb.mediautilsdemo.MediaUtils;
import com.werb.mediautilsdemo.R;
import com.werb.mediautilsdemo.widget.SendView;
import com.werb.mediautilsdemo.widget.VideoProgressBar;

import java.util.UUID;

/**
 * Created by fylder on 2017/7/18.
 */

public class AhhRecorderActivity extends AppCompatActivity {

    private MediaUtils mediaUtils;
    private boolean isCancel;
    private VideoProgressBar progressBar;
    private int mProgress;
    private TextView btnInfo, btn;
    private TextView view;
    private SendView send;
    private RelativeLayout recordLayout, switchLayout;


    Handler mHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ahh);
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.main_surface_view);
        // setting
        mediaUtils = new MediaUtils(this);
        mediaUtils.setRecorderType(MediaUtils.MEDIA_VIDEO);
        mediaUtils.setTargetDir(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
        mediaUtils.setTargetName(UUID.randomUUID() + ".mp4");
        mediaUtils.setSurfaceView(surfaceView);

        // btn
        send = (SendView) findViewById(R.id.view_send);
//        view = (TextView) findViewById(R.id.view);
        btnInfo = (TextView) findViewById(R.id.tv_info);
        btn = (TextView) findViewById(R.id.main_press_control);
        btn.setOnTouchListener(btnTouch);
        findViewById(R.id.btn_close).setOnClickListener(v -> finish());
        send.backLayout.setOnClickListener(backClick);
        send.selectLayout.setOnClickListener(selectClick);
        recordLayout = (RelativeLayout) findViewById(R.id.record_layout);
        switchLayout = (RelativeLayout) findViewById(R.id.btn_switch);
        switchLayout.setOnClickListener(v -> mediaUtils.switchCamera());
        // progress
        progressBar = (VideoProgressBar) findViewById(R.id.main_progress_bar);
        progressBar.setOnProgressEndListener(listener);
        progressBar.setCancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setCancel(true);
    }

    int STAT = 0;
    int type = 0;

    View.OnTouchListener btnTouch = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            boolean ret = false;
            float downY = 0;
            int action = event.getAction();

            switch (v.getId()) {
                case R.id.main_press_control: {
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            STAT = 1;
                            mHandler.postDelayed(() -> {
                                if (STAT == 1) {
                                    Log.w("ahh", "长按");
                                    mediaUtils.record();
                                    startView();
                                    type = 2;
                                } else {
                                    Log.w("ahh", "点击");
                                    stopView(true);
                                    type = 1;
                                }
                            }, 500);
                            ret = true;
                            break;
                        case MotionEvent.ACTION_UP:
                            STAT = 2;
                            if (type == 2) {
                                if (!isCancel) {
                                    if (mProgress == 0) {
                                        stopView(false);
                                        break;
                                    }
                                    if (mProgress < 10) {
                                        //时间太短不保存
                                        mediaUtils.stopRecordUnSave();
                                        Toast.makeText(AhhRecorderActivity.this, "时间太短", Toast.LENGTH_SHORT).show();
                                        stopView(false);
                                        break;
                                    }
                                    //停止录制
                                    mediaUtils.stopRecordSave();
                                    stopView(true);
                                } else {
                                    //现在是取消状态,不保存
                                    mediaUtils.stopRecordUnSave();
                                    Toast.makeText(AhhRecorderActivity.this, "取消保存", Toast.LENGTH_SHORT).show();
                                    stopView(false);
                                }
                            }
                            ret = false;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float currentY = event.getY();
                            isCancel = downY - currentY > 10;
                            moveView();
                            break;
                    }
                }

            }
            return ret;
        }
    };

    VideoProgressBar.OnProgressEndListener listener = new VideoProgressBar.OnProgressEndListener() {
        @Override
        public void onProgressEndListener() {
            progressBar.setCancel(true);
            mediaUtils.stopRecordSave();
        }
    };

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    progressBar.setProgress(mProgress);
                    if (mediaUtils.isRecording()) {
                        mProgress = mProgress + 1;
                        sendMessageDelayed(handler.obtainMessage(0), 100);
                    }
                    break;
            }
        }
    };

    private void startView() {
        startAnim();
        mProgress = 0;
        handler.removeMessages(0);
        handler.sendMessage(handler.obtainMessage(0));
    }

    private void moveView() {
        if (isCancel) {
            btnInfo.setText("松手取消");
        } else {
            btnInfo.setText("上滑取消");
        }
    }

    private void stopView(boolean isSave) {
        stopAnim();
        progressBar.setCancel(true);
        mProgress = 0;
        handler.removeMessages(0);
        btnInfo.setText("双击放大");
        if (isSave) {
            recordLayout.setVisibility(View.GONE);
            send.startAnim();
        }
    }

    private void startAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 1, 0.5f),
                ObjectAnimator.ofFloat(btn, "scaleY", 1, 0.5f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1, 1.3f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1, 1.3f)
        );
        set.setDuration(250).start();
    }

    private void stopAnim() {
        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(btn, "scaleX", 0.5f, 1f),
                ObjectAnimator.ofFloat(btn, "scaleY", 0.5f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleX", 1.3f, 1f),
                ObjectAnimator.ofFloat(progressBar, "scaleY", 1.3f, 1f)
        );
        set.setDuration(250).start();
    }

    private View.OnClickListener backClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            send.stopAnim();
            recordLayout.setVisibility(View.VISIBLE);
            if (STAT == 1) {
                mediaUtils.deleteTargetFile();
            }
        }
    };

    private View.OnClickListener selectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (STAT == 1) {
                String path = mediaUtils.getTargetFilePath();
                Toast.makeText(AhhRecorderActivity.this, "文件以保存至：" + path, Toast.LENGTH_SHORT).show();
            }
            send.stopAnim();
            recordLayout.setVisibility(View.VISIBLE);
        }
    };
}
