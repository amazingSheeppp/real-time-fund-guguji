package com.fund.guguji.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;

import com.fund.guguji.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 水墨底部单选/多选抽屉通用组件 (BottomSheet)
 * 支持泛型数据绑定、多选/单选模式切换、空状态占位以及自定义扩展按钮
 *
 * @param <T> 数据源实体类型
 */
public class InkSelectSheet<T> {

    public interface ItemConverter<T> {
        String getId(T item);
        String getTitle(T item);
        default String getSubtitle(T item) { return null; }
        default int getIconRes(T item) { return 0; }
    }

    public interface OnSelectedListener<T> {
        void onSelected(List<T> selectedItems, List<String> selectedIds);
    }

    public static class SelectItem<T> {
        private final String id;
        private final String title;
        private final String subtitle;
        private final int iconRes;
        private final T data;

        public SelectItem(String id, String title, String subtitle, int iconRes, T data) {
            this.id = id;
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
            this.data = data;
        }

        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public int getIconRes() { return iconRes; }
        public T getData() { return data; }
    }

    public static class Builder<T> {
        private final Context context;
        private CharSequence title;
        private CharSequence subtitle;
        private CharSequence emptyHint;
        private CharSequence secondaryButtonText;
        private Runnable secondaryButtonAction;
        private CharSequence primaryButtonText = "保存";
        private boolean isMultiSelect = true;
        private final List<SelectItem<T>> items = new ArrayList<>();
        private final Set<String> selectedIds = new HashSet<>();
        private OnSelectedListener<T> onSelectedListener;
        private boolean cancelable = true;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder<T> setTitle(CharSequence title) {
            this.title = title;
            return this;
        }

        public Builder<T> setTitle(@StringRes int titleRes) {
            this.title = context.getString(titleRes);
            return this;
        }

        public Builder<T> setSubtitle(CharSequence subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder<T> setSubtitle(@StringRes int subtitleRes) {
            this.subtitle = context.getString(subtitleRes);
            return this;
        }

        public Builder<T> setEmptyHint(CharSequence emptyHint) {
            this.emptyHint = emptyHint;
            return this;
        }

        public Builder<T> setSecondaryButton(CharSequence text, Runnable action) {
            this.secondaryButtonText = text;
            this.secondaryButtonAction = action;
            return this;
        }

        public Builder<T> setSecondaryButton(@StringRes int textRes, Runnable action) {
            this.secondaryButtonText = context.getString(textRes);
            this.secondaryButtonAction = action;
            return this;
        }

        public Builder<T> setPrimaryButtonText(CharSequence text) {
            this.primaryButtonText = text;
            return this;
        }

        public Builder<T> setPrimaryButtonText(@StringRes int textRes) {
            this.primaryButtonText = context.getString(textRes);
            return this;
        }

        public Builder<T> setMultiSelect(boolean multiSelect) {
            this.isMultiSelect = multiSelect;
            return this;
        }

        public Builder<T> setItems(List<T> rawItems, ItemConverter<T> converter, List<String> initialSelectedIds) {
            this.items.clear();
            if (rawItems != null && converter != null) {
                for (T item : rawItems) {
                    this.items.add(new SelectItem<>(
                            converter.getId(item),
                            converter.getTitle(item),
                            converter.getSubtitle(item),
                            converter.getIconRes(item),
                            item
                    ));
                }
            }
            this.selectedIds.clear();
            if (initialSelectedIds != null) {
                this.selectedIds.addAll(initialSelectedIds);
            }
            return this;
        }

        public Builder<T> setOnSelectedListener(OnSelectedListener<T> listener) {
            this.onSelectedListener = listener;
            return this;
        }

        public Builder<T> setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public BottomSheetDialog create() {
            BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_Guguji_BottomSheetDialog);
            View root = LayoutInflater.from(context).inflate(R.layout.dialog_ink_select_sheet, null);
            dialog.setContentView(root);
            dialog.setCancelable(cancelable);

            TextView tvTitle = root.findViewById(R.id.tv_select_sheet_title);
            TextView tvSubtitle = root.findViewById(R.id.tv_select_sheet_subtitle);
            LinearLayout layoutItems = root.findViewById(R.id.layout_select_items_container);
            TextView tvEmpty = root.findViewById(R.id.tv_select_empty_hint);
            TextView btnSecondary = root.findViewById(R.id.btn_select_secondary_action);
            TextView btnPrimary = root.findViewById(R.id.btn_select_primary_action);

            if (!TextUtils.isEmpty(title)) {
                tvTitle.setText(title);
                tvTitle.setVisibility(View.VISIBLE);
            } else {
                tvTitle.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(subtitle)) {
                tvSubtitle.setText(subtitle);
                tvSubtitle.setVisibility(View.VISIBLE);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(emptyHint)) {
                tvEmpty.setText(emptyHint);
            }

            if (!TextUtils.isEmpty(secondaryButtonText)) {
                btnSecondary.setText(secondaryButtonText);
                btnSecondary.setVisibility(View.VISIBLE);
                btnSecondary.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (secondaryButtonAction != null) {
                        secondaryButtonAction.run();
                    }
                });
            } else {
                btnSecondary.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(primaryButtonText)) {
                btnPrimary.setText(primaryButtonText);
            }

            // 数据列表渲染与状态管理
            if (items.isEmpty()) {
                tvEmpty.setVisibility(View.VISIBLE);
            } else {
                tvEmpty.setVisibility(View.GONE);
                LayoutInflater inflater = LayoutInflater.from(context);
                List<ImageView> checkViews = new ArrayList<>();

                for (int i = 0; i < items.size(); i++) {
                    SelectItem<T> item = items.get(i);
                    View itemView = inflater.inflate(R.layout.item_ink_select, layoutItems, false);
                    ImageView ivIcon = itemView.findViewById(R.id.iv_select_icon);
                    TextView tvItemTitle = itemView.findViewById(R.id.tv_select_item_title);
                    TextView tvItemSubtitle = itemView.findViewById(R.id.tv_select_item_subtitle);
                    ImageView ivCheck = itemView.findViewById(R.id.iv_select_check);
                    checkViews.add(ivCheck);

                    if (item.getIconRes() != 0) {
                        ivIcon.setImageResource(item.getIconRes());
                        ivIcon.setVisibility(View.VISIBLE);
                    } else {
                        ivIcon.setVisibility(View.GONE);
                    }

                    tvItemTitle.setText(item.getTitle());
                    if (!TextUtils.isEmpty(item.getSubtitle())) {
                        tvItemSubtitle.setText(item.getSubtitle());
                        tvItemSubtitle.setVisibility(View.VISIBLE);
                    } else {
                        tvItemSubtitle.setVisibility(View.GONE);
                    }

                    updateCheckIcon(ivCheck, selectedIds.contains(item.getId()));

                    final int index = i;
                    itemView.setOnClickListener(v -> {
                        String id = item.getId();
                        if (isMultiSelect) {
                            if (selectedIds.contains(id)) {
                                selectedIds.remove(id);
                                updateCheckIcon(ivCheck, false);
                            } else {
                                selectedIds.add(id);
                                updateCheckIcon(ivCheck, true);
                            }
                        } else {
                            selectedIds.clear();
                            selectedIds.add(id);
                            for (int j = 0; j < checkViews.size(); j++) {
                                updateCheckIcon(checkViews.get(j), j == index);
                            }
                        }
                    });

                    layoutItems.addView(itemView);
                }
            }

            btnPrimary.setOnClickListener(v -> {
                dialog.dismiss();
                if (onSelectedListener != null) {
                    List<T> selectedData = new ArrayList<>();
                    List<String> selectedIdList = new ArrayList<>(selectedIds);
                    for (SelectItem<T> item : items) {
                        if (selectedIds.contains(item.getId())) {
                            selectedData.add(item.getData());
                        }
                    }
                    onSelectedListener.onSelected(selectedData, selectedIdList);
                }
            });

            return dialog;
        }

        public BottomSheetDialog show() {
            BottomSheetDialog dialog = create();
            dialog.show();
            return dialog;
        }

        private static void updateCheckIcon(ImageView imageView, boolean isChecked) {
            if (isChecked) {
                imageView.setImageResource(R.drawable.ic_check_circle_filled);
            } else {
                imageView.setImageResource(R.drawable.ic_check_circle_empty);
            }
        }
    }
}
