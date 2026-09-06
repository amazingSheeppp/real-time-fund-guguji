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
import com.fund.guguji.util.MarketUtils;

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

        // 空状态“搜索添加”按钮点击跳转搜索页面
        View btnEmptyAdd = emptyState.findViewById(R.id.btn_empty_add);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v ->
                    startActivity(new Intent(getActivity(), SearchActivity.class)));
        }

        // 刷新按钮
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> refreshData());

        // 添加基金:右下角 FAB 进入搜索页
        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));

        // 排序切换
        TextView tvSortChange = view.findViewById(R.id.tv_sort_change);
        TextView tvSortTime = view.findViewById(R.id.tv_sort_time);
        tvSortChange.setOnClickListener(v -> {
            if (!sortByChange) {
                sortByChange = true;
                updateSortChips(tvSortChange, tvSortTime);
            }
        });
        tvSortTime.setOnClickListener(v -> {
            if (sortByChange) {
                sortByChange = false;
                updateSortChips(tvSortChange, tvSortTime);
            }
        });
        updateSortChips(tvSortChange, tvSortTime);
    }

    private void updateSortChips(TextView tvSortChange, TextView tvSortTime) {
        tvSortChange.setSelected(sortByChange);
        tvSortTime.setSelected(!sortByChange);
        // 重新排序当前列表并提交更新
        List<FundEntity> source = latestFunds != null ? latestFunds : adapter.getCurrentList();
        if (source != null && !source.isEmpty()) {
            List<FundEntity> sorted = sortFunds(new ArrayList<>(source));
            adapter.submitList(sorted, () -> {
                // 确保视图精准重绘并置顶
                adapter.notifyDataSetChanged();
                RecyclerView rv = getView() != null ? getView().findViewById(R.id.recycler_funds) : null;
                if (rv != null) {
                    rv.scrollToPosition(0);
                }
            });
        }
    }

    private List<FundEntity> sortFunds(List<FundEntity> list) {
        if (list == null) return new ArrayList<>();
        if (sortByChange) {
            // 按涨跌幅：从大到小（降序），涨幅高的在前，无估值(null)的置后；相同涨跌幅按添加时间倒序
            list.sort(Comparator.comparing(FundEntity::getGszzl,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Comparator.comparingInt(FundEntity::getOrderIndex).reversed()));
        } else {
            // 按添加时间：从新到旧（降序），最新添加的在最前；相同添加时间按代码倒序
            list.sort(Comparator.comparingInt(FundEntity::getOrderIndex).reversed()
                    .thenComparing(Comparator.comparing(FundEntity::getCode, Comparator.reverseOrder())));
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
    // 最近一次 LiveData 下发的基金列表,供刷新结束后复查是否仍有缺估值的基金
    private List<FundEntity> latestFunds = null;
    private boolean orderIndicesChecked = false;

    /**
     * 检查并为存量全部为 0 的 orderIndex 分配连续序号，确保添加时间排序生效
     */
    private void checkAndFixOrderIndices(List<FundEntity> funds) {
        if (orderIndicesChecked || funds == null || funds.size() <= 1) {
            return;
        }
        boolean allZero = true;
        for (FundEntity f : funds) {
            if (f.getOrderIndex() != 0) {
                allZero = false;
                break;
            }
        }
        if (allZero) {
            orderIndicesChecked = true;
            for (int i = 0; i < funds.size(); i++) {
                FundEntity fund = funds.get(i);
                fund.setOrderIndex(i + 1);
                viewModel.updateFund(fund);
            }
        }
    }

    private void observeData() {
        viewModel.getAllFunds().observe(getViewLifecycleOwner(), funds -> {
            boolean empty = funds == null || funds.isEmpty();
            latestFunds = funds;
            checkAndFixOrderIndices(funds);
            List<FundEntity> sorted = sortFunds(funds != null ? new ArrayList<>(funds) : new ArrayList<>());
            adapter.submitList(sorted, () -> adapter.notifyDataSetChanged());
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            updateStatusBar(funds);

            // 有基金还缺估值(如新添加的)时自动补刷一次
            if (!empty && hasPendingValuation(funds)) {
                viewModel.refreshValuations();
            }
            hasFunds = !empty;
        });

        viewModel.isRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
                if (!refreshing) {
                    // 刷新结束后复查:若期间新添加了基金(估值仍缺),立即补刷
                    if (latestFunds != null && !latestFunds.isEmpty()
                            && hasPendingValuation(latestFunds)) {
                        viewModel.refreshValuations();
                        return;
                    }
                    // 刷新结束(无论成败)后重启自动刷新计时
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

        // 监听最新刷新时间，刷新完成时即时更新状态条
        viewModel.getLastRefreshTime().observe(getViewLifecycleOwner(), time -> {
            if (latestFunds != null && !latestFunds.isEmpty() && time != null && !time.isEmpty()) {
                tvUpdateTime.setText("更新于 " + time);
            }
        });
    }

    /**
     * 判断列表中是否有基金还没拉到估值(新添加的基金 gsz 为空且未标记为无估值)
     */
    private boolean hasPendingValuation(List<FundEntity> funds) {
        if (funds == null) return false;
        for (FundEntity fund : funds) {
            if (!fund.isNoValuation() && (fund.getGsz() == null || fund.getGsz().isEmpty())) {
                return true;
            }
        }
        return false;
    }

    private void updateStatusBar(List<FundEntity> funds) {
        boolean isTrading = MarketUtils.isTradingTime();
        tvMarketStatus.setText(isTrading ? R.string.market_status_trading : R.string.market_status_closed);

        if (funds == null || funds.isEmpty()) {
            tvUpdateTime.setText("");
            return;
        }
        // 优先展示最近一次刷新的完成时间
        String refreshTime = viewModel.getLastRefreshTime().getValue();
        if (refreshTime != null && !refreshTime.isEmpty()) {
            tvUpdateTime.setText("更新于 " + refreshTime);
            return;
        }
        // 未刷新过时回退展示最新一条基金的行情估值时间
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
        updateStatusBar(latestFunds);
        // 恢复前台时若存在待估值基金，主动触发补刷
        if (latestFunds != null && !latestFunds.isEmpty() && hasPendingValuation(latestFunds)) {
            viewModel.refreshValuations();
        }
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
