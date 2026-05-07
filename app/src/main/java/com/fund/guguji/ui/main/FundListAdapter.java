package com.fund.guguji.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.FundEntity;

/**
 * 基金列表适配器
 */
public class FundListAdapter extends ListAdapter<FundEntity, FundViewHolder> {

    private final OnFundClickListener listener;
    private final OnFundLongClickListener longListener;

    public FundListAdapter(OnFundClickListener listener, OnFundLongClickListener longListener) {
        super(new DiffUtil.ItemCallback<FundEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull FundEntity oldItem, @NonNull FundEntity newItem) {
                return oldItem.getCode().equals(newItem.getCode());
            }

            @Override
            public boolean areContentsTheSame(@NonNull FundEntity oldItem, @NonNull FundEntity newItem) {
                return oldItem.getGsz() != null ? oldItem.getGsz().equals(newItem.getGsz()) : newItem.getGsz() == null
                        && oldItem.getGszzl() != null ? oldItem.getGszzl().equals(newItem.getGszzl()) : newItem.getGszzl() == null
                        && oldItem.getName() != null ? oldItem.getName().equals(newItem.getName()) : newItem.getName() == null;
            }
        });
        this.listener = listener;
        this.longListener = longListener;
    }

    @NonNull
    @Override
    public FundViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fund_card, parent, false);
        return new FundViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FundViewHolder holder, int position) {
        FundEntity fund = getItem(position);
        holder.bind(fund);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onFundClick(fund);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longListener != null) {
                longListener.onFundLongClick(fund);
                return true;
            }
            return false;
        });
    }

    public interface OnFundClickListener {
        void onFundClick(FundEntity fund);
    }

    public interface OnFundLongClickListener {
        void onFundLongClick(FundEntity fund);
    }
}
