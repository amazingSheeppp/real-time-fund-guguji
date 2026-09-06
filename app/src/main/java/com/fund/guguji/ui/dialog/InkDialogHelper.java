package com.fund.guguji.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

/**
 * 水墨弹窗通用辅助工具类
 */
public class InkDialogHelper {

    /**
     * 统一设置居中卡片弹窗的标准宽度 (屏幕宽度的 88%)
     */
    public static void applyInkDialogWidth(Context context, Dialog dialog) {
        if (context == null || dialog == null) return;
        Window window = dialog.getWindow();
        if (window != null) {
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int dialogWidth = (int) (screenWidth * 0.88);
            window.setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * 自动弹出软键盘
     */
    public static void showKeyboard(View view) {
        if (view == null) return;
        view.postDelayed(() -> {
            view.requestFocus();
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 120);
    }
}
