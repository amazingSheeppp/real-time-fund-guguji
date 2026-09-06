package com.fund.guguji.ui.search;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.R;
import com.fund.guguji.data.api.FundSearchApi;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.model.FundSearchResult;
import com.fund.guguji.data.repository.LocalFundRepository;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 基金搜索页面
 */
public class SearchActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 20;

    private FundSearchApi searchApi;
    private LocalFundRepository localRepo;
    private com.fund.guguji.data.repository.FundRepository fundRepo;
    private SearchResultsAdapter adapter;
    private CompositeDisposable disposables = new CompositeDisposable();
    private com.fund.guguji.ui.component.Empty emptyState;
    private LinearLayoutManager layoutManager;

    // 分页状态
    private String currentKeyword = "";   // 当前搜索词,换词即重置分页
    private int nextPageIndex = 0;        // 下一页页码
    private boolean loading = false;      // 是否正在加载(防重复触发)
    private boolean hasMore = true;       // 是否还有下一页

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        RealTimeFundApp app = (RealTimeFundApp) getApplication();
        searchApi = app.getFundSearchApi();
        localRepo = app.getLocalFundRepository();
        fundRepo = app.getFundRepository();

        // 设置返回按钮的点击事件，返回上一页
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // 初始化空状态视图并绑定初始引导探索状态
        emptyState = findViewById(R.id.empty_state);
        emptyState.showInitialState(this::triggerQuickSearch);

        RecyclerView recyclerView = findViewById(R.id.recycler_search_results);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new SearchResultsAdapter(item -> {
            FundEntity fund = new FundEntity(item.getCode(), item.getName());
            // 写入本地数据库并立即拉取首轮实时估值(不绑定到当前即将销毁的页面生命周期，确保后台执行完整)
            Observable.fromCallable(() -> {
                localRepo.insertFund(fund);
                return fund;
            })
            .flatMap(entity -> fundRepo.refreshSingleValuation(entity))
            .subscribeOn(Schedulers.io())
            .subscribe(
                refreshed -> { /* 估值写入数据库后，Room LiveData 自动刷新首页 */ },
                error -> { /* 异常已在 Repository 内部标记 noValuation 兜底 */ }
            );

            Toast.makeText(this, "已添加: " + item.getName(), Toast.LENGTH_SHORT).show();
            finish();
        });
        recyclerView.setAdapter(adapter);

        // 滚动到底部前预触发加载下一页
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy <= 0) return; // 只在向下滚动时判断
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                int total = adapter.getItemCount();
                // footer 占 1 位;提前 5 个条目触发
                if (lastVisible >= total - 6) {
                    loadNextPage();
                }
            }
        });

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

        // 监听搜索框输入，当被清空时自动复位至初始探索空状态
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (s == null || s.toString().trim().isEmpty()) {
                    adapter.submitList(null);
                    adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);
                    emptyState.setVisibility(View.VISIBLE);
                    emptyState.showInitialState(SearchActivity.this::triggerQuickSearch);
                }
            }
        });
    }

    /**
     * 快捷点击标签触发搜索
     */
    private void triggerQuickSearch(String keyword) {
        EditText searchInput = findViewById(R.id.edit_search);
        if (searchInput != null) {
            searchInput.setText(keyword);
            searchInput.setSelection(keyword.length());
        }
        doSearch(keyword);
    }

    private void doSearch(String keyword) {
        if (keyword.length() < 2) {
            Toast.makeText(this, "请输入至少2个字符", Toast.LENGTH_SHORT).show();
            return;
        }

        // 换搜索词:重置分页状态,整体替换结果
        currentKeyword = keyword;
        nextPageIndex = 0;
        hasMore = true;

        Toast.makeText(this, "正在搜索...", Toast.LENGTH_SHORT).show();
        emptyState.setVisibility(View.GONE); // 新搜索开始前，默认隐藏空状态
        adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);

        disposables.add(
                searchApi.searchFundsByPage(keyword, 0, PAGE_SIZE)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onPageLoaded, this::onSearchError)
        );
    }

    /**
     * 滚动触发:加载下一页
     */
    private void loadNextPage() {
        if (loading || !hasMore || nextPageIndex <= 0) return;
        loading = true;
        adapter.setFooterState(SearchResultsAdapter.FOOTER_LOADING);
        disposables.add(
                searchApi.searchFundsByPage(currentKeyword, nextPageIndex, PAGE_SIZE)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onPageLoaded, this::onSearchError)
        );
    }

    /**
     * 一页数据返回:首页整体替换、后续页追加;不足一页或返回空则视为没有更多
     */
    private void onPageLoaded(List<FundSearchResult.FundSearchItem> results) {
        loading = false;

        if (nextPageIndex == 0) {
            // 首页
            if (results == null || results.isEmpty()) {
                adapter.submitList(null); // 清空历史搜索结果
                adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);
                emptyState.setVisibility(View.VISIBLE); // 显示无结果空状态
                emptyState.showNoResultState(currentKeyword, () -> {
                    EditText searchInput = findViewById(R.id.edit_search);
                    if (searchInput != null) {
                        searchInput.setText("");
                    }
                }, this::triggerQuickSearch);
                hasMore = false;
                return;
            }
            emptyState.setVisibility(View.GONE);
            adapter.submitList(results);
        } else {
            // 追加页:按代码去重,防止接口偶发返回重复数据
            if (results == null || results.isEmpty()) {
                hasMore = false;
                adapter.setFooterState(SearchResultsAdapter.FOOTER_NO_MORE);
                return;
            }
            java.util.Set<String> existing = new java.util.HashSet<>();
            for (FundSearchResult.FundSearchItem item : adapter.getItems()) {
                existing.add(item.getCode());
            }
            List<FundSearchResult.FundSearchItem> fresh = new ArrayList<>();
            for (FundSearchResult.FundSearchItem item : results) {
                if (existing.add(item.getCode())) {
                    fresh.add(item);
                }
            }
            if (fresh.isEmpty()) {
                hasMore = false;
                adapter.setFooterState(SearchResultsAdapter.FOOTER_NO_MORE);
                return;
            }
            adapter.appendList(fresh);
        }

        // 不足一页说明已到末尾;否则准备下一页
        if (results.size() < PAGE_SIZE) {
            hasMore = false;
            adapter.setFooterState(adapter.getItemCount() > 0
                    ? SearchResultsAdapter.FOOTER_NO_MORE : SearchResultsAdapter.FOOTER_NONE);
        } else {
            nextPageIndex++;
            adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);
        }
    }

    private void onSearchError(Throwable throwable) {
        loading = false;
        if (nextPageIndex == 0) {
            // 首页失败:清空并显示空态
            adapter.submitList(null);
            adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyState.showNoResultState(currentKeyword, () -> {
                EditText searchInput = findViewById(R.id.edit_search);
                if (searchInput != null) {
                    searchInput.setText("");
                }
            }, this::triggerQuickSearch);
        } else {
            // 追加页失败:停在当前列表,footer 复位,滚动可重试
            adapter.setFooterState(SearchResultsAdapter.FOOTER_NONE);
        }
        Toast.makeText(this, "搜索失败: " + throwable.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
