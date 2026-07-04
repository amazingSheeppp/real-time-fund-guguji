package com.fund.guguji.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
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
import com.fund.guguji.ui.dialog.ConfirmDialog;
import com.fund.guguji.ui.dialog.HoldingEditDialog;
import com.fund.guguji.ui.main.FundListAdapter;
import com.fund.guguji.ui.main.MainViewModel;
import com.fund.guguji.ui.search.SearchActivity;

import java.util.Locale;

public class Home extends Fragment {

    private MainViewModel viewModel;
    private FundListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    
    private TextView tvTodayPL;
    private TextView tvTotalHoldings;
    private TextView tvTodayYield;

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
    }

    private void initViews(View view) {
        tvTodayPL = view.findViewById(R.id.tv_today_pl);
        tvTotalHoldings = view.findViewById(R.id.tv_total_holdings);
        tvTodayYield = view.findViewById(R.id.tv_today_yield);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_funds);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FundListAdapter(
                fund -> {
                    viewModel.loadHoldingByCode(fund.getCode(), existing ->
                            HoldingEditDialog.show(requireActivity(), fund.getCode(), fund.getName(), existing,
                                    holding -> viewModel.saveHolding(holding)));
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

        // 设置刷新按钮点击事件，执行估值刷新
        view.findViewById(R.id.btn_refresh).setOnClickListener(v -> viewModel.refreshValuations());

        view.findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), SearchActivity.class)));
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
            adapter.submitList(funds);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            
            updateSummary(funds);
        });

        viewModel.isRefreshing().observe(getViewLifecycleOwner(), refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
            }
        });

        viewModel.getErrorMessage().observe(getViewLifecycleOwner(), event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) {
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateSummary(java.util.List<com.fund.guguji.data.db.entity.FundEntity> funds) {
        if (funds == null || funds.isEmpty()) {
            tvTodayPL.setText("0.00");
            tvTotalHoldings.setText("0.00");
            tvTodayYield.setText("0.00%");
            return;
        }

        double totalPL = 0;
        double totalHoldings = 0;
        // Basic calculation for summary (needs more robust data from holding entities)
        // For now, just showing placeholders or simple sum if available
        tvTodayPL.setText(String.format(Locale.getDefault(), "+%.2f", totalPL));
        tvTotalHoldings.setText(String.format(Locale.getDefault(), "%.2f", totalHoldings));
        tvTodayYield.setText(String.format(Locale.getDefault(), "+%.2f%%", 0.0));
    }
}
