package com.fund.guguji.ui.circle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.api.model.CircleModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 圈子列表适配器
 * 每行:圈子名 + 角色徽标 + 圈子码 + 成员数 + (圈主)待审批徽标。
 */
public class CircleAdapter extends RecyclerView.Adapter<CircleAdapter.CircleViewHolder> {

    public interface OnCircleClickListener {
        void onCircleClick(CircleModels.MyCircleItem circle);
    }

    private final List<CircleModels.MyCircleItem> items = new ArrayList<>();
    private final OnCircleClickListener listener;

    public CircleAdapter(OnCircleClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<CircleModels.MyCircleItem> circles) {
        items.clear();
        if (circles != null) {
            items.addAll(circles);
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public CircleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_circle, parent, false);
        return new CircleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CircleViewHolder holder, int position) {
        CircleModels.MyCircleItem circle = items.get(position);
        holder.bind(circle);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCircleClick(circle);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CircleViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvRole;
        private final TextView tvCode;
        private final TextView tvMeta;
        private final TextView tvPending;

        CircleViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_circle_name);
            tvRole = itemView.findViewById(R.id.tv_circle_role);
            tvCode = itemView.findViewById(R.id.tv_circle_code);
            tvMeta = itemView.findViewById(R.id.tv_circle_meta);
            tvPending = itemView.findViewById(R.id.tv_circle_pending);
        }

        void bind(CircleModels.MyCircleItem circle) {
            tvName.setText(circle.getName());
            tvCode.setText("圈子码 " + circle.getCircleCode());
            tvMeta.setText(String.format(Locale.getDefault(),
                    itemView.getContext().getString(R.string.circle_member_count), circle.getMemberCount()));

            boolean isOwner = "owner".equals(circle.getMyRole());
            if (isOwner) {
                tvRole.setText(R.string.circle_role_owner);
                tvRole.setBackgroundResource(R.drawable.bg_badge_up);
                tvRole.setTextColor(itemView.getContext().getColor(R.color.card));
            } else {
                tvRole.setText(R.string.circle_role_member);
                tvRole.setBackgroundResource(R.drawable.bg_badge_flat);
                tvRole.setTextColor(itemView.getContext().getColor(R.color.ink_mid));
            }

            if (isOwner && circle.getPendingAuditCount() > 0) {
                tvPending.setVisibility(View.VISIBLE);
                tvPending.setText(String.format(Locale.getDefault(),
                        itemView.getContext().getString(R.string.circle_pending), circle.getPendingAuditCount()));
            } else {
                tvPending.setVisibility(View.GONE);
            }
        }
    }
}