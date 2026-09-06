package com.fund.guguji.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.fund.guguji.R;

/**
 * 水墨雅致风格空状态组件，支持初始引导状态与无结果状态动态切换
 */
public class Empty extends LinearLayout {

    public interface OnTagClickListener {
        void onTagClick(String keyword);
    }

    public interface OnClearClickListener {
        void onClearClick();
    }

    private ImageView ivEmpty;
    private TextView tvTitle;
    private TextView tvHint;
    private TextView btnClear;
    private View layoutSuggestions;

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
        ivEmpty = findViewById(R.id.iv_empty);
        tvTitle = findViewById(R.id.tv_empty);
        tvHint = findViewById(R.id.tv_empty_hint);
        btnClear = findViewById(R.id.btn_clear_search);
        layoutSuggestions = findViewById(R.id.layout_search_suggestions);
    }

    /**
     * 设置大标题文本
     */
    public void setEmptyTitle(CharSequence title) {
        if (tvTitle != null) {
            tvTitle.setText(title);
        }
    }

    /**
     * 设置副说明文本
     */
    public void setEmptyHint(CharSequence hint) {
        if (tvHint != null) {
            tvHint.setText(hint);
        }
    }

    /**
     * 展示进入搜索时的初始探索状态
     */
    public void showInitialState(OnTagClickListener tagListener) {
        if (tvTitle != null) {
            tvTitle.setText(R.string.search_initial_title);
        }
        if (tvHint != null) {
            tvHint.setText(R.string.search_initial_hint);
        }
        if (btnClear != null) {
            btnClear.setVisibility(GONE);
        }
        if (layoutSuggestions != null) {
            layoutSuggestions.setVisibility(VISIBLE);
        }
        bindTags(tagListener);
    }

    /**
     * 展示搜索无结果时的空状态
     */
    public void showNoResultState(String keyword, OnClearClickListener clearListener, OnTagClickListener tagListener) {
        if (tvTitle != null) {
            tvTitle.setText(R.string.search_empty_title);
        }
        if (tvHint != null) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                tvHint.setText("未检索到与“" + keyword.trim() + "”相关的基金\n请检查拼写或尝试代码、拼音首字母");
            } else {
                tvHint.setText(R.string.search_empty_hint);
            }
        }
        if (btnClear != null) {
            btnClear.setVisibility(VISIBLE);
            btnClear.setOnClickListener(v -> {
                if (clearListener != null) {
                    clearListener.onClearClick();
                }
            });
        }
        if (layoutSuggestions != null) {
            layoutSuggestions.setVisibility(VISIBLE);
        }
        bindTags(tagListener);
    }

    /**
     * 绑定热门推荐标签点击事件
     */
    private void bindTags(OnTagClickListener listener) {
        int[] tagIds = new int[]{
                R.id.tag_hs300,
                R.id.tag_yfd,
                R.id.tag_nasdaq,
                R.id.tag_zg,
                R.id.tag_dividend,
                R.id.tag_gold
        };
        for (int id : tagIds) {
            TextView tv = findViewById(id);
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onTagClick(tv.getText().toString().trim());
                    }
                });
            }
        }
    }
}
