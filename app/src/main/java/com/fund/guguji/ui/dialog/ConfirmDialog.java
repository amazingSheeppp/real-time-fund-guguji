package com.fund.guguji.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.fund.guguji.R;

/**
 * 水墨纸质卡片风格通用确认弹窗
 * 支持 Builder 链式调用及快捷静态方法
 */
public class ConfirmDialog {

    public interface OnConfirmListener {
        void onConfirm();
    }

    /**
     * 快捷调用：默认确定按钮
     */
    public static void show(Context context, String title, String message, OnConfirmListener listener) {
        new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", listener)
                .show();
    }

    /**
     * 快捷调用：自定义确定按钮文案
     */
    public static void show(Context context, String title, String message, String positiveText, OnConfirmListener listener) {
        new Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, listener)
                .show();
    }

    /**
     * 通用 Builder 构造器
     */
    public static class Builder {
        private final Context context;
        private CharSequence title;
        private CharSequence message;
        private CharSequence positiveText = "确定";
        private CharSequence negativeText = "取消";
        private OnConfirmListener onConfirmListener;
        private View.OnClickListener onCancelListener;
        private boolean cancelable = true;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setMessage(CharSequence message) {
            this.message = message;
            return this;
        }

        public Builder setPositiveButton(CharSequence text, OnConfirmListener listener) {
            this.positiveText = text;
            this.onConfirmListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, View.OnClickListener listener) {
            this.negativeText = text;
            this.onCancelListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Dialog create() {
            Dialog dialog = new Dialog(context, R.style.Theme_Guguji_Dialog);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_ink_confirm, null);
            dialog.setContentView(view);
            dialog.setCancelable(cancelable);

            TextView tvTitle = view.findViewById(R.id.tv_confirm_title);
            TextView tvMessage = view.findViewById(R.id.tv_confirm_message);
            TextView btnCancel = view.findViewById(R.id.btn_confirm_cancel);
            TextView btnPositive = view.findViewById(R.id.btn_confirm_positive);

            if (title != null) {
                tvTitle.setText(title);
            }
            if (message != null) {
                tvMessage.setText(message);
            }
            if (negativeText != null) {
                btnCancel.setText(negativeText);
            }
            if (positiveText != null) {
                btnPositive.setText(positiveText);
            }

            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();
                if (onCancelListener != null) {
                    onCancelListener.onClick(v);
                }
            });

            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (onConfirmListener != null) {
                    onConfirmListener.onConfirm();
                }
            });

            InkDialogHelper.applyInkDialogWidth(context, dialog);
            return dialog;
        }

        public Dialog show() {
            Dialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
