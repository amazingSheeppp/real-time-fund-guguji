package com.fund.guguji.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;

/**
 * 通用确认弹窗
 */
public class ConfirmDialog {

    public interface OnConfirmListener {
        void onConfirm();
    }

    public static void show(Context context, String title, String message,
                            OnConfirmListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> {
                    if (listener != null) listener.onConfirm();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
