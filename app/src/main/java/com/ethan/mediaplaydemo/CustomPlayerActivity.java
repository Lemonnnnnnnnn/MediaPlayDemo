package com.ethan.mediaplaydemo;

import static android.text.TextUtils.isEmpty;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.ethan.mediaplaydemo.utils.LogUtils;
import com.ethan.mediaplaydemo.utils.TimeUtils;
import com.ethan.player.activity.AudioPlayerActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;

public class CustomPlayerActivity extends AppCompatActivity implements MediaPlayer.EventListener, IVLCVout.OnNewVideoLayoutListener {

    private static final String ASSET_FILENAME = "love.mkv";
    private static final int MSG_UPDATE_PROGRESS = 0x1;

    private SurfaceView mSurfaceView;
    private SeekBar mSeekBar;
    private TextView mTimeView;

    private String mPlayerUrl;
    private LibVLC mLibVLC = null;
    private MediaPlayer mMediaPlayer = null;
    private int mVideoWidth = 0;
    private int mVideoHeight = 0;
    private boolean isTouching = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_UPDATE_PROGRESS) {
                updateVideoProgress();
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cutom_player);

        mPlayerUrl = getIntent().getStringExtra("playerUrl");
        LogUtils.i("CustomPlayerActivity playerUrl = " + mPlayerUrl);

        initViews();
        initPlayer();
    }

    public static void gotoAudioPlayerActivity(Context mContext, String videoUrl) {
        Intent intent = new Intent(mContext, CustomPlayerActivity.class);
        intent.putExtra("playerUrl", videoUrl);
        mContext.startActivity(intent);
    }

    private void initViews() {
        mSurfaceView = (SurfaceView) findViewById(R.id.video_surface_view);
        mSeekBar = (SeekBar) findViewById(R.id.video_progress_view);
        mTimeView = (TextView) findViewById(R.id.time_text_view);


        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTouching = true;
                mHandler.removeMessages(MSG_UPDATE_PROGRESS);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mMediaPlayer != null){
                    updateVideoProgress();
                    mMediaPlayer.setTime((long) (seekBar.getProgress() * 1.0f * mMediaPlayer.getLength() / 1000));
                    isTouching = false;
                }
            }
        });
    }

    private void initPlayer() {
        final ArrayList<String> args = new ArrayList<>();
        args.add("--vout=android-display");
        mLibVLC = new LibVLC(this, args);
        mMediaPlayer = new MediaPlayer(mLibVLC);
        mMediaPlayer.setEventListener(this);

        IVLCVout vlcVout = mMediaPlayer.getVLCVout();
        vlcVout.setVideoView(mSurfaceView);
        vlcVout.attachViews(this);

        try {
            Media media = null;
            if (TextUtils.isEmpty(mPlayerUrl)) {
                media = new Media(mLibVLC, getAssets().openFd(ASSET_FILENAME));
            } else {
                media = new Media(mLibVLC, mPlayerUrl);
            }
            mMediaPlayer.setMedia(media);
            media.release();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mMediaPlayer.play();

        mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
    }

    private void updateSurfaceFrame() {
        int sw = getWindow().getDecorView().getWidth();
        int sh = getWindow().getDecorView().getHeight();
        LogUtils.w("updateSurfaceFrame sw="+sw+", sh="+sh);

        int displayWidth = sw;
        int displayHeight = (int)(sw * mVideoHeight * 1.0f / mVideoWidth);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(displayWidth, displayHeight);
        mSurfaceView.setLayoutParams(params);
    }

    private void updateVideoProgress() {
        long currentPosition = mMediaPlayer.getTime();
        long duration = mMediaPlayer.getLength();
        int progress = (int)(currentPosition * 1.0f / duration * 1000);
        if (!isTouching){
            mSeekBar.setProgress(progress);
        }
        mTimeView.setText(TimeUtils.getVideoTimeString(currentPosition) + " / " + TimeUtils.getVideoTimeString(duration));
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 1000);
    }

    @Override
    public void onEvent(MediaPlayer.Event event) {
        if (event.type == MediaPlayer.Event.Playing) {
            mTimeView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        LogUtils.w("onNewVideoLayout width="+width+", height="+height
                +", sarNum="+sarNum+", sarDen="+sarDen);
        LogUtils.w("onNewVideoLayout visibleWidth="+visibleWidth+", visibleHeight="+visibleHeight);

        if (mVideoWidth != visibleWidth || mVideoHeight != visibleHeight) {
            mVideoWidth = visibleWidth;
            mVideoHeight = visibleHeight;
            updateSurfaceFrame();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(MSG_UPDATE_PROGRESS);
        mMediaPlayer.release();
        mMediaPlayer.getVLCVout().detachViews();
    }
}
