package com.fund.guguji.ui.detail;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.model.HoldingsResult;
import com.fund.guguji.data.model.NavResult;
import com.fund.guguji.ui.detail.FundDetailViewModel.ChartData;
import com.fund.guguji.ui.detail.FundDetailViewModel.ChartRange;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.chip.ChipGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 基金详情页
 * 展示:实时估值头部 + 业绩走势(近7日/近1月/近半年/近1年,单位净值) + 前十大持仓(含当日涨跌)
 */
public class FundDetailActivity extends AppCompatActivity {

    public static final String EXTRA_FUND_CODE = "extra_fund_code";
    public static final String EXTRA_FUND_NAME = "extra_fund_name";

    private String fundCode;
    private String fundName;

    private FundDetailViewModel viewModel;
    private HoldingsAdapter holdingsAdapter;

    private TextView tvValuation;
    private TextView tvValuationTime;
    private TextView tvChangePercent;
    private TextView tvRangeChange;
    private TextView tvChartLoading;
    private View layoutChartError;
    private TextView tvChartError;
    private TextView tvReportDate;
    private TextView tvHoldingsLoading;
    private View layoutHoldingsError;
    private TextView tvHoldingsError;
    private com.github.mikephil.charting.charts.LineChart lineChart;

    private static final SimpleDateFormat X_LABEL_FMT = new SimpleDateFormat("MM-dd", Locale.US);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fund_detail);

        String code = getIntent().getStringExtra(EXTRA_FUND_CODE);
        String name = getIntent().getStringExtra(EXTRA_FUND_NAME);
        if (code == null || code.isEmpty()) {
            Toast.makeText(this, "缺少基金代码", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        fundCode = code;
        fundName = name;

        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends androidx.lifecycle.ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new FundDetailViewModel(getApplication());
            }
        }).get(FundDetailViewModel.class);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        bindViews();
        setupChart();
        setupChips();

        observeFund();
        observeChart();
        observeHoldings();

        viewModel.init(new FundEntity(code, name));
    }

    private void bindViews() {
        // 滚动内容顶部的基金名称/代码
        TextView tvFundName = findViewById(R.id.tv_fund_name);
        TextView tvFundCode = findViewById(R.id.tv_fund_code);
        tvFundName.setText(fundName != null && !fundName.isEmpty() ? fundName : fundCode);
        tvFundCode.setText(fundCode);

        tvValuation = findViewById(R.id.tv_valuation);
        tvValuationTime = findViewById(R.id.tv_valuation_time);
        tvChangePercent = findViewById(R.id.tv_change_percent);
        tvRangeChange = findViewById(R.id.tv_range_change);
        tvChartLoading = findViewById(R.id.tv_chart_loading);
        layoutChartError = findViewById(R.id.layout_chart_error);
        tvChartError = findViewById(R.id.tv_chart_error);
        tvReportDate = findViewById(R.id.tv_report_date);
        tvHoldingsLoading = findViewById(R.id.tv_holdings_loading);
        layoutHoldingsError = findViewById(R.id.layout_holdings_error);
        tvHoldingsError = findViewById(R.id.tv_holdings_error);
        lineChart = findViewById(R.id.line_chart);

        holdingsAdapter = new HoldingsAdapter();
        RecyclerView recyclerHoldings = findViewById(R.id.recycler_holdings);
        recyclerHoldings.setLayoutManager(new LinearLayoutManager(this));
        recyclerHoldings.setAdapter(holdingsAdapter);

        findViewById(R.id.btn_chart_retry).setOnClickListener(v ->
                viewModel.loadChart(currentRange()));
        findViewById(R.id.btn_holdings_retry).setOnClickListener(v -> viewModel.retryHoldings());
    }

    /** 初始化墨水屏风格图表:灰阶线条、隐藏交互高亮、等宽数字 */
    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setBackgroundColor(ContextCompat.getColor(this, R.color.card));
        lineChart.setNoDataText("");

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(ContextCompat.getColor(this, R.color.ink_faint));
        xAxis.setGridColor(ContextCompat.getColor(this, R.color.line_soft));
        xAxis.setAxisLineColor(ContextCompat.getColor(this, R.color.line_soft));
        xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextSize(10f);
        xAxis.setLabelCount(4, true);

        YAxis left = lineChart.getAxisLeft();
        left.setTextColor(ContextCompat.getColor(this, R.color.ink_faint));
        left.setGridColor(ContextCompat.getColor(this, R.color.line_soft));
        left.setAxisLineColor(ContextCompat.getColor(this, R.color.line_soft));
        left.setDrawAxisLine(false);
        left.setTextSize(10f);
        left.setLabelCount(4, true);

        YAxis right = lineChart.getAxisRight();
        right.setEnabled(false);
    }

    private void setupChips() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_range);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            ChartRange range;
            if (id == R.id.chip_7d) range = ChartRange.D7;
            else if (id == R.id.chip_6m) range = ChartRange.M6;
            else if (id == R.id.chip_1y) range = ChartRange.Y1;
            else range = ChartRange.M1;
            viewModel.loadChart(range);
        });
    }

    private ChartRange currentRange() {
        ChipGroup chipGroup = findViewById(R.id.chip_group_range);
        int id = chipGroup.getCheckedChipId();
        if (id == R.id.chip_7d) return ChartRange.D7;
        if (id == R.id.chip_6m) return ChartRange.M6;
        if (id == R.id.chip_1y) return ChartRange.Y1;
        return ChartRange.M1;
    }

    private void observeFund() {
        // DB 读取到最新估值(或种子数据)后渲染估值头部
        viewModel.getFund().observe(this, this::renderFund);
    }

    private void renderFund(FundEntity fund) {
        if (fund == null) return;

        // 收盘后官方净值已同步:展示官方数据;否则展示实时估值
        Double officialChange = fund.getOfficialNavChange();
        boolean hasOfficial = officialChange != null
                && fund.getOfficialNav() != null
                && fund.getOfficialNavDate() != null;
        Double gszzl = fund.getGszzl();
        Double change = hasOfficial ? officialChange : gszzl;

        if (hasOfficial) {
            tvValuation.setText(fund.getOfficialNav());
            tvValuationTime.setText(String.format("官方净值 · %s", fund.getOfficialNavDate()));
        } else if (fund.getGsz() != null && !fund.getGsz().isEmpty()) {
            tvValuation.setText(fund.getGsz());
            tvValuationTime.setText(String.format("估算净值 · 估值 %s", shortTime(fund.getGztime())));
        } else {
            tvValuation.setText(fund.getDwjz() != null && !fund.getDwjz().isEmpty() ? fund.getDwjz() : "--");
            tvValuationTime.setText(String.format("单位净值 · %s", fund.getJzrq() != null ? fund.getJzrq() : "--"));
        }

        if (change == null || (!hasOfficial && fund.isNoValuation())) {
            tvChangePercent.setText("--");
            tvChangePercent.setBackgroundResource(R.drawable.bg_badge_flat);
            tvChangePercent.setTextColor(ContextCompat.getColor(this, R.color.ink_faint));
        } else if (change > 0) {
            tvChangePercent.setText(String.format("↑ +%.2f%%", change));
            tvChangePercent.setBackgroundResource(R.drawable.bg_badge_up);
            tvChangePercent.setTextColor(ContextCompat.getColor(this, R.color.card));
        } else if (change < 0) {
            tvChangePercent.setText(String.format("↓ %.2f%%", change));
            tvChangePercent.setBackgroundResource(R.drawable.bg_badge_down);
            tvChangePercent.setTextColor(ContextCompat.getColor(this, R.color.ink));
        } else {
            tvChangePercent.setText("0.00%");
            tvChangePercent.setBackgroundResource(R.drawable.bg_badge_flat);
            tvChangePercent.setTextColor(ContextCompat.getColor(this, R.color.ink_faint));
        }
    }

    private void observeChart() {
        viewModel.getChartLoading().observe(this, loading -> {
            boolean show = loading != null && loading;
            tvChartLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                layoutChartError.setVisibility(View.GONE);
                tvRangeChange.setVisibility(View.GONE);
            }
        });

        viewModel.getChartError().observe(this, error -> {
            if (error != null) {
                tvChartError.setText(String.format("业绩走势加载失败: %s", error));
                layoutChartError.setVisibility(View.VISIBLE);
                tvRangeChange.setVisibility(View.GONE);
                lineChart.clear();
            } else {
                layoutChartError.setVisibility(View.GONE);
            }
        });

        viewModel.getChartData().observe(this, this::renderChart);
    }

    private void renderChart(ChartData data) {
        if (data == null || data.points.isEmpty()) {
            return;
        }
        tvRangeChange.setVisibility(View.VISIBLE);
        double change = data.rangeChangePercent;
        String rangeLabel = data.range.label;
        if (change > 0) {
            tvRangeChange.setText(String.format("%s +%.2f%%", rangeLabel, change));
            tvRangeChange.setTextColor(ContextCompat.getColor(this, R.color.ink));
        } else {
            tvRangeChange.setText(String.format("%s %.2f%%", rangeLabel, change));
            tvRangeChange.setTextColor(ContextCompat.getColor(this, R.color.ink_mid));
        }

        List<Entry> entries = new ArrayList<>(data.points.size());
        for (int i = 0; i < data.points.size(); i++) {
            entries.add(new Entry(i, (float) data.points.get(i).getNav()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "");
        dataSet.setColor(ContextCompat.getColor(this, R.color.ink));
        dataSet.setLineWidth(1.6f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setHighlightEnabled(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(ContextCompat.getColor(this, R.color.line_soft));
        dataSet.setFillAlpha(60);

        LineData lineData = new LineData(dataSet);
        lineData.setDrawValues(false);
        lineChart.setData(lineData);

        // X 轴:索引转日期标签
        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = Math.round(value);
                if (idx < 0 || idx >= data.points.size()) return "";
                return xLabel(data.points.get(idx).getDate());
            }
        });

        // 近7日点少,直接全部可见;其余区间默认全部展示,可缩放拖动
        lineChart.resetViewPortOffsets();
        lineChart.fitScreen();
        if (data.range == ChartRange.D7) {
            lineChart.setVisibleXRangeMaximum(8f);
        }
        lineChart.invalidate();
    }

    private void observeHoldings() {
        viewModel.getHoldingsLoading().observe(this, loading -> {
            boolean show = loading != null && loading;
            tvHoldingsLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                layoutHoldingsError.setVisibility(View.GONE);
            }
        });

        viewModel.getHoldingsError().observe(this, error -> {
            if (error != null) {
                tvHoldingsError.setText(String.format("持仓加载失败: %s", error));
                layoutHoldingsError.setVisibility(View.VISIBLE);
            } else {
                layoutHoldingsError.setVisibility(View.GONE);
            }
        });

        viewModel.getHoldings().observe(this, this::renderHoldings);
    }

    private void renderHoldings(HoldingsResult result) {
        if (result == null) return;
        if (result.getReportDate() != null) {
            tvReportDate.setText(String.format("报告期 %s", result.getReportDate()));
            tvReportDate.setVisibility(View.VISIBLE);
        } else {
            tvReportDate.setVisibility(View.GONE);
        }
        List<HoldingsResult.HoldingStock> stocks = result.getStocks();
        if (stocks != null && !stocks.isEmpty()) {
            holdingsAdapter.submitList(stocks);
        }
    }

    /** "yyyy-MM-dd HH:mm" → "MM-dd HH:mm";纯日期原样返回 */
    private String shortTime(String gztime) {
        if (gztime == null) return "--";
        return gztime.length() >= 16 ? gztime.substring(5, 16) : gztime;
    }

    private String xLabel(String date) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date);
            return d != null ? X_LABEL_FMT.format(d) : date;
        } catch (Exception e) {
            return date;
        }
    }
}
