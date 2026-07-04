package com.fund.guguji.ui.search;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.R;
import com.fund.guguji.data.api.FundSearchApi;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.repository.LocalFundRepository;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 基金搜索页面
 */
public class SearchActivity extends AppCompatActivity {

    private FundSearchApi searchApi;
    private LocalFundRepository localRepo;
    private SearchResultsAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private View emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        RealTimeFundApp app = (RealTimeFundApp) getApplication();
        searchApi = app.getFundSearchApi();
        localRepo = app.getLocalFundRepository();

        // 设置返回按钮的点击事件，返回上一页
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 初始化空状态视图并绑定预设的搜索空状态文案
        emptyState = findViewById(R.id.empty_state);
        TextView tvEmpty = emptyState.findViewById(R.id.tv_empty);
        tvEmpty.setText(R.string.search_empty_title);

        RecyclerView recyclerView = findViewById(R.id.recycler_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SearchResultsAdapter(item -> {
            FundEntity fund = new FundEntity(item.getCode(), item.getName());
            // 数据库写入必须在后台线程
            disposables.add(
                    Observable.fromCallable(() -> {
                        localRepo.insertFund(fund);
                        return true;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            success -> {
                                Toast.makeText(this, "已添加: " + item.getName(), Toast.LENGTH_SHORT).show();
                                finish();
                            },
                            throwable -> Toast.makeText(this,
                                    "添加失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                    )
            );
        });
        recyclerView.setAdapter(adapter);

        EditText searchInput = findViewById(R.id.edit_search);
        Button btnSearch = findViewById(R.id.btn_search);

        btnSearch.setOnClickListener(v -> doSearch(searchInput.getText().toString().trim()));

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                doSearch(searchInput.getText().toString().trim());
                return true;
            }
            return false;
        });
    }

    private void doSearch(String keyword) {
        if (keyword.length() < 2) {
            Toast.makeText(this, "请输入至少2个字符", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "正在搜索...", Toast.LENGTH_SHORT).show();
        emptyState.setVisibility(View.GONE); // 新搜索开始前，默认隐藏空状态

        disposables.add(
                searchApi.searchFunds(keyword)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                results -> {
                                    if (results == null || results.isEmpty()) {
                                        adapter.submitList(null); // 清空历史搜索结果
                                        emptyState.setVisibility(View.VISIBLE); // 显示无结果空状态
                                    } else {
                                        emptyState.setVisibility(View.GONE);
                                        adapter.submitList(results);
                                    }
                                },
                                throwable -> {
                                    adapter.submitList(null);
                                    emptyState.setVisibility(View.VISIBLE);
                                    Toast.makeText(this,
                                            "搜索失败: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                        )
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
