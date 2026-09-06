package com.fund.guguji.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.fund.guguji.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 水墨底部操作抽屉通用组件 (BottomSheet)
 * 支持设置头部信息及动态链式添加操作项
 */
public class InkActionSheet {

    public interface OnActionClickListener {
        void onClick();
    }

    public static class ActionItem {
        private final int iconRes;
        private final CharSequence title;
        private final boolean hasChevron;
        private final boolean isDestructive;
        private final OnActionClickListener listener;

        public ActionItem(@DrawableRes int iconRes, CharSequence title, boolean hasChevron, boolean isDestructive, OnActionClickListener listener) {
            this.iconRes = iconRes;
            this.title = title;
            this.hasChevron = hasChevron;
            this.isDestructive = isDestructive;
            this.listener = listener;
        }
    }

    public static class Builder {
        private final Context context;
        private CharSequence title;
        private CharSequence subtitle;
        private CharSequence extraInfo;
        private final List<ActionItem> actions = new ArrayList<>();
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

        public Builder setExtraInfo(CharSequence extraInfo) {
            this.extraInfo = extraInfo;
            return this;
        }

        public Builder addAction(@DrawableRes int iconRes, CharSequence title, OnActionClickListener listener) {
            return addAction(iconRes, title, false, false, listener);
        }

        public Builder addAction(@DrawableRes int iconRes, @StringRes int titleRes, OnActionClickListener listener) {
            return addAction(iconRes, context.getString(titleRes), false, false, listener);
        }

        public Builder addAction(@DrawableRes int iconRes, CharSequence title, boolean isDestructive, OnActionClickListener listener) {
            return addAction(iconRes, title, false, isDestructive, listener);
        }

        public Builder addAction(@DrawableRes int iconRes, @StringRes int titleRes, boolean isDestructive, OnActionClickListener listener) {
            return addAction(iconRes, context.getString(titleRes), false, isDestructive, listener);
        }

        public Builder addAction(@DrawableRes int iconRes, CharSequence title, boolean hasChevron, boolean isDestructive, OnActionClickListener listener) {
            actions.add(new ActionItem(iconRes, title, hasChevron, isDestructive, listener));
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public BottomSheetDialog create() {
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_Guguji_BottomSheetDialog);
            View root = LayoutInflater.from(context).inflate(R.layout.dialog_ink_action_sheet, null);
            dialog.setContentView(root);
            dialog.setCancelable(cancelable);

            View layoutHeader = root.findViewById(R.id.layout_sheet_header);
            View dividerHeader = root.findViewById(R.id.divider_sheet_header);
            TextView tvTitle = root.findViewById(R.id.tv_sheet_title);
            TextView tvSubtitle = root.findViewById(R.id.tv_sheet_subtitle);
            TextView tvExtra = root.findViewById(R.id.tv_sheet_extra);
            LinearLayout layoutActions = root.findViewById(R.id.layout_actions_container);

            boolean hasHeader = false;
            if (!TextUtils.isEmpty(title)) {
                tvTitle.setText(title);
                tvTitle.setVisibility(View.VISIBLE);
                hasHeader = true;
            } else {
                tvTitle.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(subtitle)) {
                tvSubtitle.setText(subtitle);
                tvSubtitle.setVisibility(View.VISIBLE);
                hasHeader = true;
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(extraInfo)) {
                tvExtra.setText(extraInfo);
                tvExtra.setVisibility(View.VISIBLE);
                hasHeader = true;
            } else {
                tvExtra.setVisibility(View.GONE);
            }

            if (!hasHeader) {
                layoutHeader.setVisibility(View.GONE);
                dividerHeader.setVisibility(View.GONE);
            } else {
                layoutHeader.setVisibility(View.VISIBLE);
                dividerHeader.setVisibility(View.VISIBLE);
            }

            // 动态构建操作列表项
            LayoutInflater inflater = LayoutInflater.from(context);
            for (ActionItem action : actions) {
                View itemView = inflater.inflate(R.layout.item_ink_action, layoutActions, false);
                ImageView ivIcon = itemView.findViewById(R.id.iv_action_icon);
                TextView tvActionTitle = itemView.findViewById(R.id.tv_action_title);
                ImageView ivChevron = itemView.findViewById(R.id.iv_action_chevron);

                if (action.iconRes != 0) {
                    ivIcon.setImageResource(action.iconRes);
                    ivIcon.setVisibility(View.VISIBLE);
                } else {
                    ivIcon.setVisibility(View.GONE);
                }

                tvActionTitle.setText(action.title);

                if (action.hasChevron) {
                    ivChevron.setVisibility(View.VISIBLE);
                } else {
                    ivChevron.setVisibility(View.GONE);
                }

                if (action.isDestructive) {
                    tvActionTitle.setTextColor(ContextCompat.getColor(context, R.color.ink));
                    tvActionTitle.setTypeface(tvActionTitle.getTypeface(), android.graphics.Typeface.BOLD);
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (action.listener != null) {
                        action.listener.onClick();
                    }
                });

                layoutActions.addView(itemView);
            }

            return dialog;
        }

        public BottomSheetDialog show() {
            BottomSheetDialog dialog = create();
            dialog.show();
            return dialog;
        }
    }
}
