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
    private final TextView tvValuationTime;

    public FundViewHolder(View itemView) {
        super(itemView);
        tvName = itemView.findViewById(R.id.tv_fund_name);
        tvCode = itemView.findViewById(R.id.tv_fund_code);
        tvValuation = itemView.findViewById(R.id.tv_valuation);
        tvChangePercent = itemView.findViewById(R.id.tv_change_percent);
        tvValuationTime = itemView.findViewById(R.id.tv_valuation_time);
    }

    public void bind(FundEntity fund) {
        tvName.setText(fund.getName() != null ? fund.getName() : fund.getCode());
        tvCode.setText(fund.getCode());

        // 估值时间
        if (fund.getGztime() != null && !fund.getGztime().isEmpty()) {
            tvValuationTime.setText("估值 " + fund.getGztime());
        } else {
            tvValuationTime.setText("暂无估值");
        }

        // 估值显示
        if (fund.getGsz() != null) {
            tvValuation.setText(fund.getGsz());
            tvValuation.setVisibility(View.VISIBLE);
        } else {
            tvValuation.setText("--");
            tvValuation.setVisibility(View.VISIBLE);
        }

        // 涨跌幅:涨=焦墨实心徽标(白字),跌=淡墨空心徽标(描边),停牌=虚线灰
        if (fund.getGszzl() != null && !fund.isNoValuation()) {
            tvChangePercent.setText(String.format("%+.2f%%", fund.getGszzl()));
            tvChangePercent.setVisibility(View.VISIBLE);
            if (fund.getGszzl() > 0) {
                tvChangePercent.setTextColor(itemView.getContext().getColor(R.color.card));
                tvChangePercent.setBackgroundResource(R.drawable.bg_badge_up);
            } else if (fund.getGszzl() < 0) {
                tvChangePercent.setTextColor(itemView.getContext().getColor(R.color.ink));
                tvChangePercent.setBackgroundResource(R.drawable.bg_badge_down);
            } else {
                tvChangePercent.setTextColor(itemView.getContext().getColor(R.color.ink_faint));
                tvChangePercent.setBackgroundResource(R.drawable.bg_badge_flat);
            }
        } else {
            // 无实时估值/停牌
            tvChangePercent.setText("--");
            tvChangePercent.setVisibility(View.VISIBLE);
            tvChangePercent.setTextColor(itemView.getContext().getColor(R.color.ink_faint));
            tvChangePercent.setBackgroundResource(R.drawable.bg_badge_flat);
        }
    }
}
