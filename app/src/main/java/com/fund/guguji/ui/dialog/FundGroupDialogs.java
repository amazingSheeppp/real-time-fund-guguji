package com.fund.guguji.ui.dialog;

import android.content.Context;
import android.widget.Toast;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;

import java.util.List;

/**
 * 基金分组业务弹窗门面 (Facade)
 * 组合复用通用水墨组件：InkInputDialog、InkActionSheet、InkSelectSheet、ConfirmDialog
 */
public class FundGroupDialogs {

    public interface OnGroupCreatedListener {
        void onGroupCreated(String name);
    }

    public interface OnGroupRenamedListener {
        void onGroupRenamed(String newName);
    }

    public interface OnGroupActionListener {
        void onRename();
        void onDelete();
    }

    public interface OnFundActionListener {
        void onManageGroups();
        void onRemoveFromCurrentGroup();
        void onDeleteFund();
    }

    public interface OnGroupsSelectedListener {
        void onGroupsSelected(List<String> selectedGroupIds);
    }

    public interface OnFundsSelectedListener {
        void onFundsSelected(List<String> selectedFundCodes);
    }

    /**
     * 新建分组：调用通用输入框组件
     */
    public static void showCreateGroupDialog(Context context, OnGroupCreatedListener listener) {
        new InkInputDialog.Builder(context)
                .setTitle(R.string.group_add)
                .setSubtitle(R.string.group_name_hint)
                .setHint(R.string.group_name_hint)
                .setMaxLength(12)
                .setEmptyErrorMessage(R.string.group_name_empty)
                .setPositiveButton(R.string.confirm, text -> {
                    if (listener != null) {
                        listener.onGroupCreated(text);
                    }
                })
                .show();
    }

    /**
     * 重命名分组：调用通用输入框组件
     */
    public static void showRenameGroupDialog(Context context, GroupEntity group, OnGroupRenamedListener listener) {
        new InkInputDialog.Builder(context)
                .setTitle(R.string.group_rename)
                .setSubtitle("请输入新的分组名称")
                .setHint(R.string.group_name_hint)
                .setInitialText(group.getName())
                .setMaxLength(12)
                .setEmptyErrorMessage(R.string.group_name_empty)
                .setPositiveButton(R.string.confirm, text -> {
                    if (listener != null) {
                        listener.onGroupRenamed(text);
                    }
                })
                .show();
    }

    /**
     * 长按分组标签弹出的操作菜单（重命名 / 删除）：调用通用底部操作抽屉
     */
    public static void showGroupActionDialog(Context context, GroupEntity group, OnGroupActionListener listener) {
        new InkActionSheet.Builder(context)
                .setTitle("分组管理")
                .setSubtitle("当前分组: " + group.getName())
                .addAction(R.drawable.ic_tag_ink, context.getString(R.string.group_rename), () -> {
                    if (listener != null) listener.onRename();
                })
                .addAction(R.drawable.ic_delete_ink, context.getString(R.string.group_delete), true, () -> {
                    String msg = context.getString(R.string.group_delete_confirm, group.getName());
                    ConfirmDialog.show(context, context.getString(R.string.group_delete), msg, "删除", () -> {
                        if (listener != null) listener.onDelete();
                    });
                })
                .show();
    }

    /**
     * 长按基金卡片：调用通用底部操作抽屉
     */
    public static void showFundActionDialog(Context context, FundEntity fund,
                                           List<GroupEntity> allGroups,
                                           List<String> currentGroupIds,
                                           boolean isSpecificGroup,
                                           OnFundActionListener listener) {
        // 计算已归属的分组名称预览
        StringBuilder sb = new StringBuilder();
        if (currentGroupIds != null && allGroups != null) {
            for (GroupEntity g : allGroups) {
                if (currentGroupIds.contains(g.getId())) {
                    if (sb.length() > 0) sb.append(" · ");
                    sb.append(g.getName());
                }
            }
        }
        String extra = sb.length() > 0 ? ("已归属: " + sb.toString()) : "未加入任何自定义分组";

        InkActionSheet.Builder builder = new InkActionSheet.Builder(context)
                .setTitle(fund.getName())
                .setSubtitle(fund.getCode())
                .setExtraInfo(extra)
                .addAction(R.drawable.ic_tag_ink, context.getString(R.string.group_set), true, false, () -> {
                    if (listener != null) listener.onManageGroups();
                });

        if (isSpecificGroup) {
            builder.addAction(R.drawable.ic_remove_ink, context.getString(R.string.group_remove_from_current), () -> {
                String msg = context.getString(R.string.group_remove_confirm, fund.getName());
                ConfirmDialog.show(context, context.getString(R.string.group_remove_from_current), msg, "移出", () -> {
                    if (listener != null) listener.onRemoveFromCurrentGroup();
                });
            });
        }

        builder.addAction(R.drawable.ic_delete_ink, "删除自选", true, () -> {
            ConfirmDialog.show(context, "删除自选", "确定删除 " + fund.getName() + " 吗？", "删除", () -> {
                if (listener != null) listener.onDeleteFund();
            });
        });

        builder.show();
    }

    /**
     * 设置基金所属分组：调用通用多选抽屉
     */
    public static void showSelectGroupsDialog(Context context, FundEntity fund, List<GroupEntity> allGroups,
                                             List<String> currentGroupIds,
                                             OnGroupsSelectedListener listener,
                                             Runnable onNewGroupClick) {
        new InkSelectSheet.Builder<GroupEntity>(context)
                .setTitle(context.getString(R.string.group_set) + " · " + fund.getName())
                .setSubtitle("请勾选归属分组，可多选")
                .setEmptyHint("暂无自定义分组，可点击下方新建")
                .setSecondaryButton(R.string.group_add, onNewGroupClick)
                .setPrimaryButtonText(R.string.save)
                .setItems(allGroups, new InkSelectSheet.ItemConverter<GroupEntity>() {
                    @Override
                    public String getId(GroupEntity item) {
                        return item.getId();
                    }

                    @Override
                    public String getTitle(GroupEntity item) {
                        return item.getName();
                    }
                }, currentGroupIds)
                .setOnSelectedListener((selectedItems, selectedIds) -> {
                    if (listener != null) {
                        listener.onGroupsSelected(selectedIds);
                    }
                })
                .show();
    }

    /**
     * 向指定分组批量添加已有基金：调用通用多选抽屉
     */
    public static void showAddFundsToGroupDialog(Context context, GroupEntity group,
                                                List<FundEntity> allFunds,
                                                List<String> existingFundCodes,
                                                OnFundsSelectedListener listener) {
        if (allFunds == null || allFunds.isEmpty()) {
            Toast.makeText(context, "当前暂无自选基金，请先搜索添加基金", Toast.LENGTH_SHORT).show();
            return;
        }

        new InkSelectSheet.Builder<FundEntity>(context)
                .setTitle("添加基金到「" + group.getName() + "」")
                .setSubtitle("从自选列表中勾选基金批量加入本分组")
                .setEmptyHint("暂无自选基金")
                .setPrimaryButtonText("确认添加")
                .setItems(allFunds, new InkSelectSheet.ItemConverter<FundEntity>() {
                    @Override
                    public String getId(FundEntity item) {
                        return item.getCode();
                    }

                    @Override
                    public String getTitle(FundEntity item) {
                        return item.getName() + " (" + item.getCode() + ")";
                    }
                }, existingFundCodes)
                .setOnSelectedListener((selectedItems, selectedIds) -> {
                    if (listener != null) {
                        listener.onFundsSelected(selectedIds);
                    }
                })
                .show();
    }
}
