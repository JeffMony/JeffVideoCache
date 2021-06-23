package com.jeffmony.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText mVideoUrlEditText;
    private Button mVideoPlayBtn;
    private CheckBox mLocalProxyBox;
    private CheckBox mUseOkHttpBox;

    private RadioGroup mRadioGroup;
    private RadioButton mExoBtn;
    private RadioButton mIjkBtn;

    private boolean mIsExoSelected;
    private boolean mIsIjkSelected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoUrlEditText = findViewById(R.id.video_url_edit_text);
        mVideoPlayBtn = findViewById(R.id.video_play_btn);
        mLocalProxyBox = findViewById(R.id.local_proxy_box);
        mUseOkHttpBox = findViewById(R.id.okhttp_box);
        mRadioGroup = findViewById(R.id.player_group);
        mExoBtn = findViewById(R.id.exo_play_btn);
        mIjkBtn = findViewById(R.id.ijk_play_btn);
        mExoBtn.setChecked(true);
        mIsExoSelected = mExoBtn.isChecked();
        mIsIjkSelected = mIjkBtn.isChecked();

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                mIsExoSelected = mExoBtn.isChecked();
                mIsIjkSelected = mIjkBtn.isChecked();
            }
        });

        mVideoPlayBtn.setOnClickListener(view -> {
            String videoUrl = mVideoUrlEditText.getText().toString();
            if (TextUtils.isEmpty(videoUrl)) {
                Toast.makeText(MainActivity.this, "The video url is empty", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(MainActivity.this, VideoPlayActivity.class);
                intent.putExtra("video_url", videoUrl);
                intent.putExtra("local_proxy_enable", mLocalProxyBox.isChecked());
                intent.putExtra("use_okttp_enable", mUseOkHttpBox.isChecked());
                int type;
                if (mIsExoSelected) {
                    type = 1;
                } else {
                    type = 2;
                }
                intent.putExtra("player_type", type);
                startActivity(intent);
            }
        });
    }
}