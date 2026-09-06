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
 * 支持分页追加:正常条目 + 底部加载状态 footer(加载中/没有更多)
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ITEM = 0;
    private static final int TYPE_FOOTER = 1;

    /** footer 状态:无 footer / 正在加载 / 没有更多 */
    public static final int FOOTER_NONE = 0;
    public static final int FOOTER_LOADING = 1;
    public static final int FOOTER_NO_MORE = 2;

    private final List<FundSearchResult.FundSearchItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private int footerState = FOOTER_NONE;

    public SearchResultsAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_FOOTER) {
            View view = inflater.inflate(R.layout.item_search_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(footerState);
            return;
        }
        FundSearchResult.FundSearchItem item = items.get(position);
        ViewHolder vh = (ViewHolder) holder;
        vh.tvCode.setText(item.getCode());
        vh.tvName.setText(item.getName());
        vh.tvType.setText(item.getFoundType());
        vh.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        // 有 footer 状态时多占一个位置
        return items.size() + (footerState == FOOTER_NONE ? 0 : 1);
    }

    @Override
    public int getItemViewType(int position) {
        // footer 永远在列表末尾;无 footer 时全部为普通条目
        return (footerState != FOOTER_NONE && position == items.size()) ? TYPE_FOOTER : TYPE_ITEM;
    }

    /** 新搜索:整体替换结果 */
    public void submitList(List<FundSearchResult.FundSearchItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /** 翻页:追加下一页数据并刷新 footer */
    public void appendList(List<FundSearchResult.FundSearchItem> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int start = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(start, newItems.size());
    }

    public List<FundSearchResult.FundSearchItem> getItems() {
        return items;
    }

    public void setFooterState(int state) {
        int old = footerState;
        footerState = state;
        if (old != state) {
            if (old == FOOTER_NONE) {
                notifyItemInserted(items.size());
            } else if (state == FOOTER_NONE) {
                notifyItemRemoved(items.size());
            } else {
                notifyItemChanged(items.size());
            }
        }
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

    /**
     * 加载状态 footer:加载中显示"加载中…",没有更多显示"没有更多了"
     */
    static class FooterViewHolder extends RecyclerView.ViewHolder {
        final TextView tvFooter;

        FooterViewHolder(View itemView) {
            super(itemView);
            tvFooter = itemView.findViewById(R.id.tv_footer);
        }

        void bind(int state) {
            tvFooter.setText(state == FOOTER_LOADING ? "加载中…" : "没有更多了");
        }
    }
}
