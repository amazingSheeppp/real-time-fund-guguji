package com.fund.guguji.ui.component;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fund.guguji.R;

public class BottomTabBar extends LinearLayout {

    private OnTabSelectedListener listener;
    private View[] tabs;
    private ImageView[] icons;
    private TextView[] texts;
    private int currentTab = 0;

    private int originalPaddingBottom;

    public interface OnTabSelectedListener {
        void onTabSelected(int index);
    }

    public BottomTabBar(Context context) {
        this(context, null);
    }

    public BottomTabBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BottomTabBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_bottom_tab_bar, this, true);
        
        originalPaddingBottom = getPaddingBottom();

        tabs = new View[]{
                findViewById(R.id.tab_home),
                findViewById(R.id.tab_analysis),
                findViewById(R.id.tab_settings)
        };

        icons = new ImageView[]{
                findViewById(R.id.iv_home),
                findViewById(R.id.iv_analysis),
                findViewById(R.id.iv_settings)
        };

        texts = new TextView[]{
                findViewById(R.id.tv_home),
                findViewById(R.id.tv_analysis),
                findViewById(R.id.tv_settings)
        };

        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            tabs[i].setOnClickListener(v -> selectTab(index));
        }

        // Handle navigation bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), 
                    originalPaddingBottom + systemBars.bottom);
            return insets;
        });
        
        updateTabUI();
    }

    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.listener = listener;
    }

    public void selectTab(int index) {
        if (currentTab == index) return;
        currentTab = index;
        updateTabUI();
        if (listener != null) {
            listener.onTabSelected(index);
        }
    }

    private void updateTabUI() {
        for (int i = 0; i < tabs.length; i++) {
            boolean isActive = (i == currentTab);
            // System will automatically apply the color from selector based on 'selected' state
            tabs[i].setSelected(isActive);
            icons[i].setSelected(isActive);
            texts[i].setSelected(isActive);
        }
    }
}
