package com.fund.guguji.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.ui.dialog.ConfirmDialog;
import com.fund.guguji.ui.main.FundListAdapter;
import com.fund.guguji.ui.main.MainViewModel;
import com.fund.guguji.ui.search.SearchActivity;
import com.fund.guguji.util.Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Home extends Fragment {

    private MainViewModel viewModel;
    private FundListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;

    private TextView tvMarketStatus;
    private TextView tvUpdateTime;

    private final Handler autoRefreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefreshRunnable = this::refreshData;

    // 排序模式:true = 按涨跌幅,false = 按添加时间
    private boolean sortByChange = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        initViews(view);
        observeData();
        startAutoRefresh();
    }

    private void initViews(View view) {
        tvMarketStatus = view.findViewById(R.id.tv_market_status);
        tvUpdateTime = view.findViewById(R.id.tv_update_time);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_funds);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FundListAdapter(
                fund -> {
                    // 一期无详情页,点击暂无操作
                },
                fund -> {
                    ConfirmDialog.show(requireActivity(), "删除基金",
                            "确定删除 " + fund.getName() + " 吗？",
                            () -> viewModel.deleteFund(fund));
                }
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshValuations());

        emptyState = view.findViewById(R.id.empty_state);

        // 刷新按钮
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> refreshData());

        // 添加基金:顶部搜索按钮 与 右下角 FAB
        view.findViewById(R.id.btn_search).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));
        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));

        // 排序切换
        TextView tvSortChange = view.findViewById(R.id.tv_sort_change);
        TextView tvSortTime = view.findViewById(R.id.tv_sort_time);
        tvSortChange.setOnClickListener(v -> {
            sortByChange = true;
            updateSortChips(tvSortChange, tvSortTime);
        });
        tvSortTime.setOnClickListener(v -> {
            sortByChange = false;
            updateSortChips(tvSortChange, tvSortTime);
        });
        updateSortChips(tvSortChange, tvSortTime);
    }

    private void updateSortChips(TextView tvSortChange, TextView tvSortTime) {
        tvSortChange.setSelected(sortByChange);
        tvSortTime.setSelected(!sortByChange);
        // 重新排序当前列表
        if (adapter.getCurrentList() != null) {
            adapter.submitList(sortFunds(new ArrayList<>(adapter.getCurrentList())));
        }
    }

    private List<FundEntity> sortFunds(List<FundEntity> list) {
        if (sortByChange) {
            list.sort(Comparator.comparing(FundEntity::getGszzl,
                    Comparator.nullsLast(Comparator.reverseOrder())));
        } else {
            list.sort(Comparator.comparingInt(FundEntity::getOrderIndex));
        }
        return list;
    }

    private void refreshData() {
        viewModel.refreshValuations();
    }

    private void startAutoRefresh() {
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        autoRefreshHandler.postDelayed(autoRefreshRunnable, Constants.REFRESH_INTERVAL_MS);
    }

    private boolean hasFunds = false;

    private void observeData() {
        viewModel.getAllFunds().observe(getViewLifecycleOwner(), funds -> {
            boolean empty = funds == null || funds.isEmpty();
            if (!empty && !hasFunds) {
                hasFunds = true;
                viewModel.refreshValuations();
            }
            hasFunds = !empty;
            adapter.submitList(sortFunds(funds != null ? new ArrayList<>(funds) : new ArrayList<>()));
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            updateStatusBar(funds);
        });

        viewModel.isRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
                // 刷新结束(无论成败)后重启自动刷新计时
                if (!refreshing) {
                    startAutoRefresh();
                }
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateStatusBar(List<FundEntity> funds) {
        if (funds == null || funds.isEmpty()) {
            tvMarketStatus.setText("盘中 · 实时估值");
            tvUpdateTime.setText("");
            return;
        }
        tvMarketStatus.setText("盘中 · 实时估值");
        // 展示最新一条的估值时间
        String time = null;
        for (FundEntity f : funds) {
            if (f.getGztime() != null && !f.getGztime().isEmpty()) {
                time = f.getGztime();
                break;
            }
        }
        tvUpdateTime.setText(time != null ? "更新于 " + time : "等待估值中…");
    }

    @Override
    public void onResume() {
        super.onResume();
        startAutoRefresh();
    }

    @Override
    public void onPause() {
        super.onPause();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
    }
}
