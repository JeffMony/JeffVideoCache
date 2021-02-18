package com.jeffmony.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText mVideoUrlEditText;
    private Button mVideoPlayBtn;
    private CheckBox mLocalProxyBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoUrlEditText = findViewById(R.id.video_url_edit_text);
        mVideoPlayBtn = findViewById(R.id.video_play_btn);
        mLocalProxyBox = findViewById(R.id.local_proxy_box);

        mVideoPlayBtn.setOnClickListener(view -> {
            String videoUrl = mVideoUrlEditText.getText().toString();
            if (TextUtils.isEmpty(videoUrl)) {
                Toast.makeText(MainActivity.this, "The video url is empty", Toast.LENGTH_LONG).show();
            } else {
                Intent intent = new Intent(MainActivity.this, VideoPlayActivity.class);
                intent.putExtra("video_url", videoUrl);
                intent.putExtra("local_proxy_enable", mLocalProxyBox.isChecked());
                startActivity(intent);
            }
        });
    }
}