package com.fund.guguji.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import com.fund.guguji.R;

public class Empty extends LinearLayout {

    public Empty(Context context) {
        this(context, null);
    }

    public Empty(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Empty(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_empty, this, true);
    }
}
