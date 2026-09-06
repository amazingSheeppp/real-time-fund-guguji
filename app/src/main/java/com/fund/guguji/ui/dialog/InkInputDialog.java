package com.fund.guguji.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;

import com.fund.guguji.R;

/**
 * 水墨纸质卡片风格通用单行文本输入弹窗
 */
public class InkInputDialog {

    public interface OnInputConfirmedListener {
        void onInputConfirmed(String text);
    }

    public static class Builder {
        private final Context context;
        private CharSequence title;
        private CharSequence subtitle;
        private CharSequence hint;
        private CharSequence initialText;
        private int maxLength = -1;
        private int inputType = -1;
        private boolean allowEmpty = false;
        private CharSequence emptyErrorMessage = "内容不能为空";
        private CharSequence positiveText = "确定";
        private CharSequence negativeText = "取消";
        private OnInputConfirmedListener onInputConfirmedListener;
        private View.OnClickListener onCancelListener;
        private boolean autoShowKeyboard = true;
        private boolean cancelable = true;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder setTitle(@StringRes int titleRes) {
            this.title = context.getString(titleRes);
            return this;
        }

        public Builder setSubtitle(CharSequence subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder setSubtitle(@StringRes int subtitleRes) {
            this.subtitle = context.getString(subtitleRes);
            return this;
        }

        public Builder setHint(CharSequence hint) {
            this.hint = hint;
            return this;
        }

        public Builder setHint(@StringRes int hintRes) {
            this.hint = context.getString(hintRes);
            return this;
        }

        public Builder setInitialText(CharSequence initialText) {
            this.initialText = initialText;
            return this;
        }

        public Builder setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder setInputType(int inputType) {
            this.inputType = inputType;
            return this;
        }

        public Builder setAllowEmpty(boolean allowEmpty) {
            this.allowEmpty = allowEmpty;
            return this;
        }

        public Builder setEmptyErrorMessage(CharSequence msg) {
            this.emptyErrorMessage = msg;
            return this;
        }

        public Builder setEmptyErrorMessage(@StringRes int msgRes) {
            this.emptyErrorMessage = context.getString(msgRes);
            return this;
        }

        public Builder setPositiveButton(CharSequence text, OnInputConfirmedListener listener) {
            this.positiveText = text;
            this.onInputConfirmedListener = listener;
            return this;
        }

        public Builder setPositiveButton(@StringRes int textRes, OnInputConfirmedListener listener) {
            this.positiveText = context.getString(textRes);
            this.onInputConfirmedListener = listener;
            return this;
        }

        public Builder setNegativeButton(CharSequence text, View.OnClickListener listener) {
            this.negativeText = text;
            this.onCancelListener = listener;
            return this;
        }

        public Builder setAutoShowKeyboard(boolean autoShowKeyboard) {
            this.autoShowKeyboard = autoShowKeyboard;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Dialog create() {
            Dialog dialog = new Dialog(context, R.style.Theme_Guguji_Dialog);
            View view = LayoutInflater.from(context).inflate(R.layout.dialog_ink_input, null);
            dialog.setContentView(view);
            dialog.setCancelable(cancelable);

            TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
            TextView tvSubtitle = view.findViewById(R.id.tv_dialog_subtitle);
            EditText editInput = view.findViewById(R.id.edit_dialog_input);
            TextView btnCancel = view.findViewById(R.id.btn_dialog_cancel);
            TextView btnConfirm = view.findViewById(R.id.btn_dialog_confirm);

            if (title != null) {
                tvTitle.setText(title);
            }
            if (subtitle != null) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(subtitle);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            if (hint != null) {
                editInput.setHint(hint);
            }

            if (maxLength > 0) {
                editInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
            }

            if (inputType != -1) {
                editInput.setInputType(inputType);
            }

            if (!TextUtils.isEmpty(initialText)) {
                editInput.setText(initialText);
                editInput.setSelection(initialText.length());
            }

            if (negativeText != null) {
                btnCancel.setText(negativeText);
            }
            if (positiveText != null) {
                btnConfirm.setText(positiveText);
            }

            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();
                if (onCancelListener != null) {
                    onCancelListener.onClick(v);
                }
            });

            btnConfirm.setOnClickListener(v -> {
                String input = editInput.getText().toString().trim();
                if (!allowEmpty && TextUtils.isEmpty(input)) {
                    Toast.makeText(context, emptyErrorMessage, Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                if (onInputConfirmedListener != null) {
                    onInputConfirmedListener.onInputConfirmed(input);
                }
            });

            InkDialogHelper.applyInkDialogWidth(context, dialog);

            if (autoShowKeyboard) {
                InkDialogHelper.showKeyboard(editInput);
            }

            return dialog;
        }

        public Dialog show() {
            Dialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
