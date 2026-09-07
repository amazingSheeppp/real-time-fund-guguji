package com.fund.guguji.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.model.HoldingsResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 前十大持仓列表适配器
 * 每行:排名 + 名称/代码占比 + 当日涨跌徽标(墨分五色:涨=浓墨实心,跌=空心描边,无行情=虚线)
 */
public class HoldingsAdapter extends RecyclerView.Adapter<HoldingsAdapter.HoldingViewHolder> {

    private final List<HoldingsResult.HoldingStock> items = new ArrayList<>();

    public void submitList(List<HoldingsResult.HoldingStock> stocks) {
        items.clear();
        if (stocks != null) {
            items.addAll(stocks);
        }
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    @NonNull
    @Override
    public HoldingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holding, parent, false);
        return new HoldingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HoldingViewHolder holder, int position) {
        HoldingsResult.HoldingStock stock = items.get(position);
        holder.bind(stock, position + 1);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HoldingViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRank;
        private final TextView tvName;
        private final TextView tvCode;
        private final TextView tvChange;

        HoldingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvName = itemView.findViewById(R.id.tv_stock_name);
            tvCode = itemView.findViewById(R.id.tv_stock_code);
            tvChange = itemView.findViewById(R.id.tv_stock_change);
        }

        void bind(HoldingsResult.HoldingStock stock, int rank) {
            tvRank.setText(String.valueOf(rank));
            tvName.setText(stock.getName());
            String percentText = stock.getPercent() > 0
                    ? String.format("占净值 %.2f%%", stock.getPercent())
                    : "";
            tvCode.setText(String.format("%s · %s", stock.getCode(), percentText));

            Double change = stock.getChangePercent();
            if (change == null) {
                // 无行情(非沪深港股标的):虚线灰
                tvChange.setText("--");
                tvChange.setBackgroundResource(R.drawable.bg_badge_flat);
                tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ink_faint));
            } else if (change > 0) {
                tvChange.setText(String.format("↑ +%.2f%%", change));
                tvChange.setBackgroundResource(R.drawable.bg_badge_up);
                tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.card));
            } else if (change < 0) {
                tvChange.setText(String.format("↓ %.2f%%", change));
                tvChange.setBackgroundResource(R.drawable.bg_badge_down);
                tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ink));
            } else {
                tvChange.setText("0.00%");
                tvChange.setBackgroundResource(R.drawable.bg_badge_flat);
                tvChange.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.ink_faint));
            }
        }
    }
}
