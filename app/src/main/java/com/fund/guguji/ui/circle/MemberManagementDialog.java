package com.fund.guguji.ui.circle;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.api.GugujiApi;
import com.fund.guguji.data.api.model.CircleModels;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 成员管理抽屉:列出圈子成员,圈主可移出成员(不能移出自己)。
 */
public class MemberManagementDialog {

    public interface OnMembersChangedListener {
        void onMembersChanged();
    }

    public static void show(Context context, GugujiApi api, long circleId, long myUserId,
                            OnMembersChangedListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_Guguji_BottomSheetDialog);
        View root = LayoutInflater.from(context)
                .inflate(R.layout.dialog_member_management, null);
        dialog.setContentView(root);

        TextView tvTitle = root.findViewById(R.id.tv_member_title);
        TextView tvEmpty = root.findViewById(R.id.tv_member_empty);
        RecyclerView recycler = root.findViewById(R.id.recycler_members);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        CompositeDisposable disposables = new CompositeDisposable();
        MemberAdapter adapter = new MemberAdapter(myUserId, (member, isOwnerAction) -> {
            if (!isOwnerAction) return;
            api.kickMember(circleId, member.getUserId())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            v -> {
                                Toast.makeText(context, "已移出该成员", Toast.LENGTH_SHORT).show();
                                if (listener != null) listener.onMembersChanged();
                                dialog.dismiss();
                            },
                            throwable -> Toast.makeText(context,
                                    throwable.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
        recycler.setAdapter(adapter);

        disposables.add(
                api.getCircleMembers(circleId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                resp -> {
                                    List<CircleModels.CircleMemberItem> members =
                                            resp != null ? resp.getMembers() : new ArrayList<>();
                                    adapter.submitList(members);
                                    tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                                },
                                throwable -> Toast.makeText(context,
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );

        dialog.setOnDismissListener(d -> disposables.clear());
        dialog.show();
    }

    /** 成员列表适配器:圈主排在前面,普通成员可被移出 */
    private static class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.Holder> {

        private final long myUserId;
        private final List<CircleModels.CircleMemberItem> items = new ArrayList<>();
        private final java.util.function.BiConsumer<CircleModels.CircleMemberItem, Boolean> onAction;

        MemberAdapter(long myUserId, java.util.function.BiConsumer<CircleModels.CircleMemberItem, Boolean> onAction) {
            this.myUserId = myUserId;
            this.onAction = onAction;
        }

        void submitList(List<CircleModels.CircleMemberItem> members) {
            items.clear();
            if (members != null) {
                // 圈主置顶
                items.addAll(members);
                items.sort((a, b) -> {
                    boolean aOwner = "owner".equals(a.getRole());
                    boolean bOwner = "owner".equals(b.getRole());
                    if (aOwner != bOwner) return aOwner ? -1 : 1;
                    return Long.compare(a.getUserId(), b.getUserId());
                });
            }
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_member, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CircleModels.CircleMemberItem member = items.get(position);
            boolean isOwner = "owner".equals(member.getRole());
            boolean isMe = member.getUserId() == myUserId;

            holder.tvName.setText(isOwner ? member.getNickname() + " (圈主)" : member.getNickname());
            String joined = member.getJoinedAt() != null ? member.getJoinedAt() : "";
            holder.tvSub.setText(isOwner ? "圈主" : (isMe ? "我" : "成员") + (joined.isEmpty() ? "" : (" · " + joined)));
            holder.tvSub.setTextColor(holder.itemView.getContext().getColor(R.color.ink_faint));

            // 圈主/自己不能移出
            boolean canKick = !isOwner && !isMe;
            holder.btnAction.setVisibility(canKick ? View.VISIBLE : View.GONE);
            if (canKick) {
                holder.btnAction.setText("移出");
                holder.btnAction.setOnClickListener(v -> onAction.accept(member, true));
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvSub;
            final TextView btnAction;

            Holder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_member_name);
                tvSub = itemView.findViewById(R.id.tv_member_sub);
                btnAction = itemView.findViewById(R.id.btn_member_action);
            }
        }
    }
}