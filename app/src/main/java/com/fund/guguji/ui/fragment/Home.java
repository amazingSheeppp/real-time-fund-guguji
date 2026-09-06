package com.fund.guguji.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.ui.dialog.FundGroupDialogs;
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

    // 分组栏视图
    private LinearLayout layoutGroups;
    private HorizontalScrollView hsvGroups;
    private View btnAddGroup;

    // 空状态动态文案与操作视图
    private TextView tvEmptyTitle;
    private TextView tvEmptyHint;
    private TextView tvEmptyBtnText;
    private ImageView ivEmptyBtnIcon;

    private List<GroupEntity> latestGroups = new ArrayList<>();

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

        // 分组栏初始化
        layoutGroups = view.findViewById(R.id.layout_groups);
        hsvGroups = view.findViewById(R.id.hsv_groups);
        btnAddGroup = view.findViewById(R.id.btn_add_group);
        if (btnAddGroup != null) {
            btnAddGroup.setOnClickListener(v ->
                    FundGroupDialogs.showCreateGroupDialog(requireContext(), name ->
                            viewModel.createGroup(name, group -> viewModel.setSelectedGroupId(group.getId()))));
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_funds);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FundListAdapter(
                fund -> {
                    // 一期无详情页,点击暂无操作
                },
                fund -> {
                    String currentGroupId = viewModel.getSelectedGroupId().getValue();
                    boolean isSpecificGroup = currentGroupId != null && !MainViewModel.GROUP_ID_ALL.equals(currentGroupId);
                    viewModel.getGroupIdsByFund(fund.getCode(), currentGroupIds -> {
                        FundGroupDialogs.showFundActionDialog(requireContext(), fund, latestGroups, currentGroupIds, isSpecificGroup, new FundGroupDialogs.OnFundActionListener() {
                            @Override
                            public void onManageGroups() {
                                FundGroupDialogs.showSelectGroupsDialog(requireContext(), fund, latestGroups, currentGroupIds,
                                        selectedGroupIds -> {
                                            viewModel.updateFundGroups(fund.getCode(), selectedGroupIds);
                                            Toast.makeText(getContext(), "已更新所属分组", Toast.LENGTH_SHORT).show();
                                        },
                                        () -> FundGroupDialogs.showCreateGroupDialog(requireContext(),
                                                name -> viewModel.createGroup(name, null))
                                );
                            }

                            @Override
                            public void onRemoveFromCurrentGroup() {
                                if (isSpecificGroup) {
                                    viewModel.removeFundFromGroup(fund.getCode(), currentGroupId);
                                    Toast.makeText(getContext(), "已从当前分组移出", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onDeleteFund() {
                                viewModel.deleteFund(fund);
                            }
                        });
                    });
                }
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshValuations());

        emptyState = view.findViewById(R.id.empty_state);
        tvEmptyTitle = emptyState.findViewById(R.id.tv_empty_title);
        tvEmptyHint = emptyState.findViewById(R.id.tv_empty_hint);
        tvEmptyBtnText = emptyState.findViewById(R.id.tv_empty_btn_text);
        ivEmptyBtnIcon = emptyState.findViewById(R.id.iv_empty_btn_icon);

        // 空状态操作按钮（根据当前分组自适应行为：全局时跳转搜索，自定义分组时支持勾选添加入组）
        View btnEmptyAdd = emptyState.findViewById(R.id.btn_empty_add);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v -> onEmptyStateAction());
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

    /**
     * 空状态主操作按钮点击逻辑
     */
    private void onEmptyStateAction() {
        String currentGroupId = viewModel.getSelectedGroupId().getValue();
        if (currentGroupId == null || MainViewModel.GROUP_ID_ALL.equals(currentGroupId)) {
            startActivity(new Intent(getActivity(), SearchActivity.class));
        } else {
            GroupEntity group = findGroupById(currentGroupId);
            if (group == null) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
                return;
            }
            List<FundEntity> allFunds = viewModel.getAllFunds().getValue();
            viewModel.getFundCodesInGroup(currentGroupId, existingCodes -> {
                if (allFunds == null || allFunds.isEmpty()) {
                    startActivity(new Intent(getActivity(), SearchActivity.class));
                } else {
                    FundGroupDialogs.showAddFundsToGroupDialog(requireContext(), group, allFunds, existingCodes,
                            selectedCodes -> {
                                viewModel.addFundsToGroup(selectedCodes, group.getId());
                                Toast.makeText(getContext(), "已添加所选基金到分组", Toast.LENGTH_SHORT).show();
                            });
                }
            });
        }
    }

    private GroupEntity findGroupById(String groupId) {
        if (latestGroups == null || groupId == null) return null;
        for (GroupEntity g : latestGroups) {
            if (groupId.equals(g.getId())) return g;
        }
        return null;
    }

    /**
     * 动态渲染分组 Tab 栏
     */
    private void renderGroupTabs(List<GroupEntity> groups, String selectedId) {
        if (layoutGroups == null) return;
        View addBtn = layoutGroups.findViewById(R.id.btn_add_group);
        layoutGroups.removeAllViews();

        String finalSelectedId = selectedId != null ? selectedId : MainViewModel.GROUP_ID_ALL;
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // 1. 添加“全部”
        View allTabView = inflater.inflate(R.layout.item_group_tab, layoutGroups, false);
        TextView tvAll = allTabView.findViewById(R.id.tv_group_name);
        tvAll.setText(R.string.group_all);
        boolean allSelected = MainViewModel.GROUP_ID_ALL.equals(finalSelectedId);
        tvAll.setSelected(allSelected);
        tvAll.setOnClickListener(v -> viewModel.setSelectedGroupId(MainViewModel.GROUP_ID_ALL));
        layoutGroups.addView(allTabView);

        // 2. 依次添加用户自定义分组
        if (groups != null) {
            for (GroupEntity group : groups) {
                View tabView = inflater.inflate(R.layout.item_group_tab, layoutGroups, false);
                TextView tv = tabView.findViewById(R.id.tv_group_name);
                tv.setText(group.getName());
                boolean isSelected = group.getId().equals(finalSelectedId);
                tv.setSelected(isSelected);

                tv.setOnClickListener(v -> viewModel.setSelectedGroupId(group.getId()));

                tv.setOnLongClickListener(v -> {
                    FundGroupDialogs.showGroupActionDialog(requireContext(), group, new FundGroupDialogs.OnGroupActionListener() {
                        @Override
                        public void onRename() {
                            FundGroupDialogs.showRenameGroupDialog(requireContext(), group, newName ->
                                    viewModel.renameGroup(group.getId(), newName));
                        }

                        @Override
                        public void onDelete() {
                            viewModel.deleteGroup(group.getId());
                        }
                    });
                    return true;
                });

                layoutGroups.addView(tabView);
            }
        }

        // 3. 末尾保留“+ 新建分组”按钮
        if (addBtn != null) {
            layoutGroups.addView(addBtn);
        }
    }

    /**
     * 根据当前分组动态更新空状态文案与按钮
     */
    private void updateEmptyStateUI(boolean empty, String selectedId) {
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) return;

        boolean isAll = selectedId == null || MainViewModel.GROUP_ID_ALL.equals(selectedId);
        if (isAll) {
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.empty_title);
            if (tvEmptyHint != null) tvEmptyHint.setText(R.string.empty_hint);
            if (tvEmptyBtnText != null) tvEmptyBtnText.setText(R.string.empty_btn_search);
            if (ivEmptyBtnIcon != null) ivEmptyBtnIcon.setImageResource(R.drawable.ic_search);
        } else {
            GroupEntity group = findGroupById(selectedId);
            String groupName = group != null ? group.getName() : "当前分组";
            if (tvEmptyTitle != null) tvEmptyTitle.setText("「" + groupName + "」暂无基金");
            if (tvEmptyHint != null) tvEmptyHint.setText(R.string.group_empty_hint);
            if (tvEmptyBtnText != null) tvEmptyBtnText.setText(R.string.group_btn_add_fund);
            if (ivEmptyBtnIcon != null) ivEmptyBtnIcon.setImageResource(R.drawable.ic_add_small);
        }
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
        // 观察分组列表变动
        viewModel.getAllGroups().observe(getViewLifecycleOwner(), groups -> {
            latestGroups = groups != null ? groups : new ArrayList<>();
            String currentSelectedId = viewModel.getSelectedGroupId().getValue();
            if (currentSelectedId != null && !MainViewModel.GROUP_ID_ALL.equals(currentSelectedId)) {
                boolean exists = false;
                for (GroupEntity g : latestGroups) {
                    if (g.getId().equals(currentSelectedId)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    viewModel.setSelectedGroupId(MainViewModel.GROUP_ID_ALL);
                }
            }
            renderGroupTabs(latestGroups, viewModel.getSelectedGroupId().getValue());
        });

        // 观察当前选中的分组切换
        viewModel.getSelectedGroupId().observe(getViewLifecycleOwner(), selectedId -> {
            renderGroupTabs(latestGroups, selectedId);
            boolean empty = latestFunds == null || latestFunds.isEmpty();
            updateEmptyStateUI(empty, selectedId);
        });

        // 观察当前分组下的基金数据流
        viewModel.getDisplayFunds().observe(getViewLifecycleOwner(), funds -> {
            boolean empty = funds == null || funds.isEmpty();
            latestFunds = funds;
            checkAndFixOrderIndices(funds);
            List<FundEntity> sorted = sortFunds(funds != null ? new ArrayList<>(funds) : new ArrayList<>());
            adapter.submitList(sorted, () -> adapter.notifyDataSetChanged());
            updateEmptyStateUI(empty, viewModel.getSelectedGroupId().getValue());
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
