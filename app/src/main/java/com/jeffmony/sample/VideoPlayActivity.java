package com.jeffmony.sample;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jeffmony.playersdk.IPlayer;
import com.jeffmony.playersdk.JeffPlayer;
import com.jeffmony.playersdk.common.PlayerSettings;
import com.jeffmony.playersdk.common.PlayerType;
import com.jeffmony.playersdk.common.SeekType;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.TimeUtils;

public class VideoPlayActivity extends Activity {

    private static final String TAG = "VideoPlayActivity";

    private final static int MSG_UPDATE_VIDEOSIZE = 1;
    private final static int MSG_UPDATE_VIDEOTIME = 2;

    private final static int MAX_PROGRESS = 1000;

    private String mVideoUrl;
    private int mPlayerType;
    private Surface mSurface;
    private JeffPlayer mPlayer;
    private Size mScreenSize;
    private int mVideoWidth;
    private int mVideoHeight;
    private boolean mLocalProxyEnable;

    private TextureView mVideoView;
    private SeekBar mProgressView;
    private TextView mTimeView;
    private ImageButton mVideoStatusBtn;
    private VideoStatus mVideoStatus;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_UPDATE_VIDEOSIZE) {
                updateVideoSurfaceSize(mVideoWidth, mVideoHeight);
            } else if (msg.what == MSG_UPDATE_VIDEOTIME) {
                updateVideoTimeInfo();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        mScreenSize = ScreenUtils.getScreenSize(this.getApplicationContext());

        mVideoUrl = getIntent().getStringExtra("video_url");
        mLocalProxyEnable = getIntent().getBooleanExtra("local_proxy_enable", false);
        mPlayerType = getIntent().getIntExtra("player_type", 1);

        mVideoView = findViewById(R.id.video_textureview);
        mProgressView = findViewById(R.id.video_progress_view);
        mTimeView = findViewById(R.id.video_time_view);
        mVideoStatusBtn = findViewById(R.id.video_status_btn);

        mVideoView.setSurfaceTextureListener(mTextureListener);
        mProgressView.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
        mVideoStatusBtn.setOnClickListener(mOnClickListener);
    }

    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            mSurface = new Surface(surfaceTexture);
            initPlayerSettings();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) { }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) { }
    };

    private void initPlayerSettings() {
        if (mPlayerType == 1) {
            mPlayer = new JeffPlayer(this.getApplicationContext(), PlayerType.EXO_PLAYER);
        } else {
            mPlayer = new JeffPlayer(this.getApplicationContext(), PlayerType.IJK_PLAYER);
        }
        mPlayer.setSeekType(SeekType.CLOSEST_SYNC);
        PlayerSettings playerSettings = new PlayerSettings();
        playerSettings.setLocalProxyEnable(mLocalProxyEnable);
        mPlayer.initPlayerSettings(playerSettings);

        mPlayer.setOnPreparedListener(mOnPreparedListener);
        mPlayer.setOnVideoSizeChangedListener(mOnVideoSizeChangedListener);
        mPlayer.setSurface(mSurface);
        try {
            mPlayer.setDataSource(this, Uri.parse(mVideoUrl), null);
        } catch (Exception e) {
            Toast.makeText(this, "player setDataSource failed", Toast.LENGTH_LONG).show();
            return;
        }
        mPlayer.prepareAsync();

    }

    private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) { }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mHandler.removeMessages(MSG_UPDATE_VIDEOTIME);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            long totalDuration = mPlayer.getDuration();
            if (totalDuration > 0) {
                int progress = seekBar.getProgress();
                mPlayer.seekTo((long) (progress * 1.0f / MAX_PROGRESS * totalDuration));
                if (progress == MAX_PROGRESS) {
                    mHandler.removeMessages(MSG_UPDATE_VIDEOTIME);
                } else {
                    mHandler.sendEmptyMessage(MSG_UPDATE_VIDEOTIME);
                }
            } else {
                mHandler.sendEmptyMessage(MSG_UPDATE_VIDEOTIME);
            }
        }
    };

    private IPlayer.OnPreparedListener mOnPreparedListener = new IPlayer.OnPreparedListener() {
        @Override
        public void onPrepared() {
            mHandler.sendEmptyMessage(MSG_UPDATE_VIDEOTIME);
            mPlayer.start();

            updatePlayerBtn();
        }
    };

    private IPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new IPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(int width, int height, int rotationDegree, float pixelRatio, float darRatio) {
            LogUtils.i(TAG, "onVideoSizeChanged width="+width+", height="+height);
            mVideoWidth = width;
            mVideoHeight = height;
            mHandler.sendEmptyMessage(MSG_UPDATE_VIDEOSIZE);
        }
    };

    private View.OnClickListener mOnClickListener = view -> {
        if (mVideoStatus == VideoStatus.PLAY) {
            mPlayer.pause();
            mHandler.removeMessages(MSG_UPDATE_VIDEOTIME);
        } else {
            mPlayer.start();
            mHandler.sendEmptyMessage(MSG_UPDATE_VIDEOTIME);
        }
        updatePlayerBtn();
    };

    private void updateVideoSurfaceSize(int width, int height) {
        int baseWidth = mScreenSize.getWidth();
        float ratio = (width > height) ? height * 1.0f / width : width * 1.0f / height;
        int tempWidth = baseWidth;
        int tempHeight = (width > height) ? (int)(tempWidth * ratio) : (int)(tempWidth / ratio);
        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(tempWidth, tempHeight));
    }

    private void updateVideoTimeInfo() {
        mTimeView.setVisibility(View.VISIBLE);
        long totalDuration = mPlayer.getDuration();
        long currentPosition = mPlayer.getCurrentPosition();
        mTimeView.setText(TimeUtils.getVideoTimeString(currentPosition) + "/" + TimeUtils.getVideoTimeString(totalDuration));

        int progress;
        if (totalDuration > 0) {
            progress = (int) (1.0f * currentPosition / totalDuration * MAX_PROGRESS);
            mProgressView.setProgress(progress);

            long bufferedPosition = mPlayer.getBufferedPosition();
            int bufferedProgress = (int) (1.0f * bufferedPosition / totalDuration * MAX_PROGRESS);
            mProgressView.setSecondaryProgress(bufferedProgress);
        }
        mHandler.removeMessages(MSG_UPDATE_VIDEOTIME);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_VIDEOTIME, 1000);
    }

    private void updatePlayerBtn() {
        if (mVideoStatusBtn.getVisibility() == View.GONE) {
            mVideoStatusBtn.setVisibility(View.VISIBLE);
        }
        if (mPlayer.isPlaying()) {
            mVideoStatusBtn.setBackgroundResource(R.drawable.pause_icon);
            mVideoStatus = VideoStatus.PLAY;
        } else {
            mVideoStatusBtn.setBackgroundResource(R.drawable.play_icon);
            mVideoStatus = VideoStatus.PAUSE;
        }
    }

    private void removeHandlerMsg() {
        mHandler.removeMessages(MSG_UPDATE_VIDEOSIZE);
        mHandler.removeMessages(MSG_UPDATE_VIDEOTIME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeHandlerMsg();
        mPlayer.release();
    }
}
