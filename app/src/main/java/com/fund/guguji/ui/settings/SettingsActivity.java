package com.fund.guguji.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fund.guguji.R;

/**
 * 设置页面
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btn_clear_data).setOnClickListener(v ->
                Toast.makeText(this, "功能开发中", Toast.LENGTH_SHORT).show());

        findViewById(R.id.btn_feedback).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "实时估值 App 反馈");
            startActivity(intent);
        });

        findViewById(R.id.btn_about).setOnClickListener(v ->
                Toast.makeText(this, "实时估值 v1.0", Toast.LENGTH_SHORT).show());

        TextView tvVersion = findViewById(R.id.tv_version);
        tvVersion.setText("v1.0");
    }
}
