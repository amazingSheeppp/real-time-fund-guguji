package com.fund.guguji;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fund.guguji.ui.dialog.ConfirmDialog;
import com.fund.guguji.ui.dialog.HoldingEditDialog;
import com.fund.guguji.ui.main.FundListAdapter;
import com.fund.guguji.ui.main.MainViewModel;
import com.fund.guguji.ui.search.SearchActivity;
import com.fund.guguji.ui.settings.SettingsActivity;

public class MainActivity extends AppCompatActivity {

    private MainViewModel viewModel;
    private FundListAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        initViews();
        observeData();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recycler_funds);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FundListAdapter(
                fund -> {
                    viewModel.loadHoldingByCode(fund.getCode(), existing ->
                            HoldingEditDialog.show(this, fund.getCode(), fund.getName(), existing,
                                    holding -> viewModel.saveHolding(holding)));
                },
                fund -> {
                    ConfirmDialog.show(this, "删除基金",
                            "确定删除 " + fund.getName() + " 吗？",
                            () -> viewModel.deleteFund(fund));
                }
        );
        recyclerView.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setOnRefreshListener(() -> viewModel.refreshValuations());

        emptyState = findViewById(R.id.empty_state);

        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
    }

    private boolean hasFunds = false;

    private void observeData() {
        viewModel.getAllFunds().observe(this, funds -> {
            boolean empty = funds == null || funds.isEmpty();
            if (!empty && !hasFunds) {
                // 首次加载到数据时自动刷新估值
                hasFunds = true;
                viewModel.refreshValuations();
            }
            hasFunds = !empty;
            adapter.submitList(funds);
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        });

        viewModel.isRefreshing().observe(this, refreshing -> {
            if (refreshing != null) {
                swipeRefresh.setRefreshing(refreshing);
            }
        });

        viewModel.getErrorMessage().observe(this, event -> {
            String msg = event.getContentIfNotHandled();
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        } else if (id == R.id.action_refresh) {
            viewModel.refreshValuations();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
