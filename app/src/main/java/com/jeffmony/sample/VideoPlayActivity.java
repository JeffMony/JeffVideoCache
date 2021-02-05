package com.jeffmony.sample;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.os.Bundle;
import android.view.Surface;
import android.view.TextureView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jeffmony.playersdk.IPlayer;
import com.jeffmony.playersdk.JeffPlayer;
import com.jeffmony.videocache.utils.LogUtils;

public class VideoPlayActivity extends Activity {

    private static final String TAG = "VideoPlayActivity";

    private String mVideoUrl;
    private Surface mSurface;
    private JeffPlayer mPlayer;
    private Size mScreenSize;
    private int mVideoWidth;
    private int mVideoHeight;

    private TextureView mVideoView;
    private SeekBar mProgressView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_play);
        mScreenSize = ScreenUtils.getScreenSize(this.getApplicationContext());

        mVideoUrl = getIntent().getStringExtra("video_url");

        mVideoView = findViewById(R.id.video_textureview);
        mProgressView = findViewById(R.id.video_progress_view);

        float ratio = (mScreenSize.getWidth() > mScreenSize.getHeight()) ? mScreenSize.getHeight() * 1.0f / mScreenSize.getWidth() : mScreenSize.getWidth() * 1.0f / mScreenSize.getHeight();
        int width = mScreenSize.getWidth();
        int height = (int) (ratio * width);
        mVideoView.setLayoutParams(new LinearLayout.LayoutParams(width, height));

        mVideoView.setSurfaceTextureListener(mTextureListener);
    }

    private TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            mSurface = new Surface(surfaceTexture);
            initPlayerSettings();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };

    private void initPlayerSettings() {
        mPlayer = new JeffPlayer(this.getApplicationContext());
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

    private IPlayer.OnPreparedListener mOnPreparedListener = new IPlayer.OnPreparedListener() {
        @Override
        public void onPrepared() {
            mPlayer.start();
        }
    };

    private IPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener = new IPlayer.OnVideoSizeChangedListener() {
        @Override
        public void onVideoSizeChanged(int width, int height, int rotationDegree, float pixelRatio, float darRatio) {
            LogUtils.i(TAG, "onVideoSizeChanged width="+width+", height="+height);
            mVideoWidth = width;
            mVideoHeight = height;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPlayer.release();
    }
}
