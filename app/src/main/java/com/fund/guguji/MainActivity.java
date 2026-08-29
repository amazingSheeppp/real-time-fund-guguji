package com.fund.guguji;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fund.guguji.ui.component.BottomTabBar;
import com.fund.guguji.ui.fragment.Circle;
import com.fund.guguji.ui.fragment.Home;
import com.fund.guguji.ui.fragment.Setting;

public class MainActivity extends AppCompatActivity {

    private BottomTabBar bottomTabBar;
    
    // 缓存 Fragment 实例
    private Fragment homeFragment;
    private Fragment circleFragment;

    private Fragment settingsFragment;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        
        // 初始化默认显示首页
        if (savedInstanceState == null) {
            homeFragment = new Home();
            showFragment(homeFragment);
        }
    }

    private void initViews() {
        bottomTabBar = findViewById(R.id.bottom_tab_bar);
        bottomTabBar.setOnTabSelectedListener(index -> {
            switch (index) {
                case 0:
                    // 自选
                    if (homeFragment == null) homeFragment = new Home();
                    showFragment(homeFragment);
                    break;
                case 1:
                    // 圈子
                    if (circleFragment == null) circleFragment = new Circle();
                    showFragment(circleFragment);
                    break;
                case 2:
                    // 我的(设置)
                    if (settingsFragment == null) settingsFragment = new Setting();
                    showFragment(settingsFragment);
                    break;
            }
        });
    }

    /**
     * 使用 show/hide 模式切换 Fragment，保留 Fragment 状态且不重复创建实例
     */
    private void showFragment(Fragment fragment) {
        if (currentFragment == fragment) return;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();

        // 如果当前有正在显示的 Fragment，先隐藏它
        if (currentFragment != null) {
            transaction.hide(currentFragment);
        }

        // 如果目标 Fragment 还没有被添加到管理器中，则 add
        if (!fragment.isAdded()) {
            transaction.add(R.id.nav_host_fragment, fragment);
        } else {
            // 如果已经添加过了，直接 show 出来
            transaction.show(fragment);
        }

        currentFragment = fragment;
        transaction.commit();
    }
}
