package com.fund.guguji.ui.main;

import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.FundEntity;

/**
 * 基金列表 ViewHolder
 */
public class FundViewHolder extends RecyclerView.ViewHolder {

    private final TextView tvName;
    private final TextView tvCode;
    private final TextView tvValuation;
    private final TextView tvChangePercent;
    private final TextView tvProfitToday;
    private final TextView tvProfitTotal;
    private final TextView tvNavInfo;

    public FundViewHolder(View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tv_fund_name);
        tvCode = itemView.findViewById(R.id.tv_fund_code);
        tvValuation = itemView.findViewById(R.id.tv_valuation);
        tvChangePercent = itemView.findViewById(R.id.tv_change_percent);
        tvProfitToday = itemView.findViewById(R.id.tv_profit_today);
        tvProfitTotal = itemView.findViewById(R.id.tv_profit_total);
        tvNavInfo = itemView.findViewById(R.id.tv_nav_info);
    }

    public void bind(FundEntity fund) {
        tvName.setText(fund.getName() != null ? fund.getName() : fund.getCode());
        tvCode.setText(fund.getCode());

        // 估值显示
        if (fund.getGsz() != null) {
            tvValuation.setText(fund.getGsz());
            tvValuation.setVisibility(View.VISIBLE);
        } else {
            tvValuation.setVisibility(View.GONE);
        }

        // 涨跌幅
        if (fund.getGszzl() != null) {
            String percentText = String.format("%+.2f%%", fund.getGszzl());
            tvChangePercent.setText(percentText);
            tvChangePercent.setVisibility(View.VISIBLE);

            int color = fund.getGszzl() >= 0
                    ? itemView.getContext().getColor(R.color.price_up)
                    : itemView.getContext().getColor(R.color.price_down);
            tvChangePercent.setTextColor(color);
        } else {
            tvChangePercent.setVisibility(View.GONE);
        }

        // 今日收益
        if (fund.getProfitToday() != 0) {
            String profitText = String.format("%+.2f", fund.getProfitToday());
            tvProfitToday.setText(profitText);
            tvProfitToday.setVisibility(View.VISIBLE);
            tvProfitToday.setTextColor(fund.getProfitToday() >= 0
                    ? itemView.getContext().getColor(R.color.price_up)
                    : itemView.getContext().getColor(R.color.price_down));
        } else {
            tvProfitToday.setVisibility(View.GONE);
        }

        // 总收益
        if (fund.getProfitTotal() != 0) {
            String totalText = String.format("%+.2f", fund.getProfitTotal());
            tvProfitTotal.setText(totalText);
            tvProfitTotal.setVisibility(View.VISIBLE);
            tvProfitTotal.setTextColor(fund.getProfitTotal() >= 0
                    ? itemView.getContext().getColor(R.color.price_up)
                    : itemView.getContext().getColor(R.color.price_down));
        } else {
            tvProfitTotal.setVisibility(View.GONE);
        }

        // 净值信息
        String navInfo = "";
        if (fund.getJzrq() != null && fund.getDwjz() != null) {
            navInfo = fund.getJzrq() + " 净值 " + fund.getDwjz();
        }
        if (fund.getGztime() != null) {
            navInfo += " 估值 " + fund.getGztime();
        }
        tvNavInfo.setText(navInfo.trim());
    }
}
