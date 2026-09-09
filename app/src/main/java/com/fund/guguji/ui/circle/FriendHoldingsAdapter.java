package com.fund.guguji.ui.circle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.api.model.HoldingModels;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 圈子内朋友持仓适配器
 * 每个朋友一张卡片,展开其真实持仓基金列表。
 */
public class FriendHoldingsAdapter extends RecyclerView.Adapter<FriendHoldingsAdapter.FriendViewHolder> {

    private final List<HoldingModels.FriendHoldingItem> items = new ArrayList<>();

    public void submitList(List<HoldingModels.FriendHoldingItem> friends) {
        items.clear();
        if (friends != null) {
            items.addAll(friends);
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_holding, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvOwnerBadge;
        private final TextView tvCount;
        private final LinearLayout layoutHoldings;
        private final TextView tvNoHolding;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_friend_name);
            tvOwnerBadge = itemView.findViewById(R.id.tv_owner_badge);
            tvCount = itemView.findViewById(R.id.tv_friend_count);
            layoutHoldings = itemView.findViewById(R.id.layout_holdings);
            tvNoHolding = itemView.findViewById(R.id.tv_no_holding);
        }

        void bind(HoldingModels.FriendHoldingItem friend) {
            tvName.setText(friend.getNickname());
            tvOwnerBadge.setVisibility(friend.isOwner() ? View.VISIBLE : View.GONE);

            List<HoldingModels.HoldingItem> holdings = friend.getHoldings();
            int count = holdings == null ? 0 : holdings.size();
            tvCount.setText(String.format(Locale.getDefault(), "持有 %d 只基金", count));

            layoutHoldings.removeAllViews();
            boolean hasHoldings = holdings != null && !holdings.isEmpty();
            if (hasHoldings) {
                LayoutInflater inflater = LayoutInflater.from(itemView.getContext());
                for (HoldingModels.HoldingItem holding : holdings) {
                    View row = inflater.inflate(R.layout.item_friend_holding_row, layoutHoldings, false);
                    TextView tvFundName = row.findViewById(R.id.tv_friend_fund_name);
                    TextView tvFundCode = row.findViewById(R.id.tv_friend_fund_code);
                    if (tvFundName != null) tvFundName.setText(holding.fundName);
                    if (tvFundCode != null) tvFundCode.setText(holding.fundCode);
                    layoutHoldings.addView(row);
                }
            }
            tvNoHolding.setVisibility(hasHoldings ? View.GONE : View.VISIBLE);
        }
    }
}