package com.fund.guguji.ui.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.model.FundSearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果适配器
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.ViewHolder> {

    private final List<FundSearchResult.FundSearchItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public SearchResultsAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FundSearchResult.FundSearchItem item = items.get(position);
        holder.tvCode.setText(item.getCode());
        holder.tvName.setText(item.getName());
        holder.tvType.setText(item.getFoundType());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void submitList(List<FundSearchResult.FundSearchItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public interface OnItemClickListener {
        void onItemClick(FundSearchResult.FundSearchItem item);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvCode;
        final TextView tvName;
        final TextView tvType;

        ViewHolder(View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_fund_code);
            tvName = itemView.findViewById(R.id.tv_fund_name);
            tvType = itemView.findViewById(R.id.tv_fund_type);
        }
    }
}
