package com.fund.guguji.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fund.guguji.R;

/**
 * 圈子页(二期:邮箱登录 + 朋友持仓分组展示)
 * 一期为占位页
 */
public class Circle extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_circle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView tvTitle = view.findViewById(R.id.tv_circle_title);
        TextView tvHint = view.findViewById(R.id.tv_circle_hint);
        tvTitle.setText("圈子");
        tvHint.setText("敬请期待\n邮箱登录 + 朋友持仓分组展示");
    }
}
