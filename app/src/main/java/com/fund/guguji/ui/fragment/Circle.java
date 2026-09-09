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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fund.guguji.R;
import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.data.api.AuthSession;
import com.fund.guguji.data.api.GugujiApi;
import com.fund.guguji.data.api.model.CircleModels;
import com.fund.guguji.data.api.model.HoldingModels;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.repository.LocalFundRepository;
import com.fund.guguji.ui.circle.CircleAdapter;
import com.fund.guguji.ui.circle.CircleDetailActivity;
import com.fund.guguji.ui.dialog.ConfirmDialog;
import com.fund.guguji.ui.dialog.InkInputDialog;
import com.fund.guguji.ui.login.LoginActivity;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 圈子页(二期:邮箱登录 + 朋友持仓分组展示)
 * 未登录展示登录引导;登录后展示我加入的圈子列表,并同步本地自选为服务端持仓。
 */
public class Circle extends Fragment implements CircleAdapter.OnCircleClickListener {

    private final CompositeDisposable disposables = new CompositeDisposable();

    private GugujiApi gugujiApi;
    private AuthSession authSession;
    private LocalFundRepository localFundRepository;

    private View layoutNotLogin;
    private View layoutLoggedIn;
    private TextView btnLogout;
    private TextView tvGreeting;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerCircles;
    private View layoutCirclesEmpty;
    private CircleAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_circle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RealTimeFundApp app = (RealTimeFundApp) requireActivity().getApplication();
        gugujiApi = app.getGugujiApi();
        authSession = app.getAuthSession();
        localFundRepository = app.getLocalFundRepository();

        layoutNotLogin = view.findViewById(R.id.layout_not_login);
        layoutLoggedIn = view.findViewById(R.id.layout_logged_in);
        btnLogout = view.findViewById(R.id.btn_logout);
        tvGreeting = view.findViewById(R.id.tv_circle_greeting);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        recyclerCircles = view.findViewById(R.id.recycler_circles);
        layoutCirclesEmpty = view.findViewById(R.id.layout_circles_empty);

        recyclerCircles.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new CircleAdapter(this);
        recyclerCircles.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::onPullRefresh);

        view.findViewById(R.id.btn_login).setOnClickListener(v ->
                startActivity(new Intent(requireContext(), LoginActivity.class)));
        btnLogout.setOnClickListener(v -> confirmLogout());
        view.findViewById(R.id.btn_create_circle).setOnClickListener(v -> onCreateCircleClicked());
        view.findViewById(R.id.btn_join_circle).setOnClickListener(v -> onJoinCircleClicked());

        renderLoginState();
    }

    /**
     * 每次回到圈子 Tab 时刷新登录态与圈子列表,并同步本地自选为服务端持仓
     */
    @Override
    public void onResume() {
        super.onResume();
        renderLoginState();
        if (authSession.isLoggedIn()) {
            refreshCircles(true);
        }
    }

    private void renderLoginState() {
        boolean loggedIn = authSession.isLoggedIn();
        layoutNotLogin.setVisibility(loggedIn ? View.GONE : View.VISIBLE);
        layoutLoggedIn.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        btnLogout.setVisibility(loggedIn ? View.VISIBLE : View.GONE);
        swipeRefresh.setEnabled(loggedIn);

        if (loggedIn) {
            String nickname = authSession.getNickname();
            String email = authSession.getEmail();
            tvGreeting.setText((nickname == null || nickname.isEmpty() ? "" : nickname + " · ") + email);
        } else {
            adapter.submitList(null);
            layoutCirclesEmpty.setVisibility(View.GONE);
        }
    }

    private void onPullRefresh() {
        refreshCircles(false);
    }

    /**
     * 刷新圈子列表;syncHoldings=true 时顺带把本地自选同步为服务端持仓。
     */
    private void refreshCircles(boolean syncHoldings) {
        swipeRefresh.setRefreshing(true);

        if (syncHoldings) {
            syncLocalHoldings();
        }

        disposables.add(
                gugujiApi.getMyCircles()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                circles -> {
                                    swipeRefresh.setRefreshing(false);
                                    adapter.submitList(circles);
                                    layoutCirclesEmpty.setVisibility(
                                            circles == null || circles.isEmpty() ? View.VISIBLE : View.GONE);
                                },
                                throwable -> {
                                    swipeRefresh.setRefreshing(false);
                                    Toast.makeText(getContext(), throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                        )
        );
    }

    /**
     * 把本地自选(FundEntity)全量同步为服务端持仓,静默失败不打断圈子浏览。
     */
    private void syncLocalHoldings() {
        disposables.add(
                io.reactivex.rxjava3.core.Single.fromCallable(() -> {
                            List<FundEntity> funds = localFundRepository.getAllFundsSync();
                            List<HoldingModels.HoldingItem> holdings = new ArrayList<>();
                            if (funds != null) {
                                for (FundEntity fund : funds) {
                                    holdings.add(new HoldingModels.HoldingItem(fund.getCode(), fund.getName()));
                                }
                            }
                            return holdings;
                        })
                        .flatMapObservable(gugujiApi::batchSyncHoldings)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                resp -> { /* 静默同步成功 */ },
                                throwable -> { /* 同步失败不阻断圈子浏览 */ }
                        )
        );
    }

    private void onCreateCircleClicked() {
        new InkInputDialog.Builder(requireContext())
                .setTitle(R.string.circle_create_title)
                .setSubtitle("圈子创建后自动生成唯一圈子码")
                .setHint(R.string.circle_name_hint)
                .setMaxLength(32)
                .setEmptyErrorMessage(R.string.circle_name_empty)
                .setPositiveButton(R.string.confirm, name ->
                        promptCircleDescription(name.trim()))
                .show();
    }

    private void promptCircleDescription(String name) {
        new InkInputDialog.Builder(requireContext())
                .setTitle(R.string.circle_create_title)
                .setSubtitle("圈子简介(选填)")
                .setHint(R.string.circle_desc_hint)
                .setMaxLength(200)
                .setAllowEmpty(true)
                .setPositiveButton(R.string.confirm, description ->
                        doCreateCircle(name, description))
                .show();
    }

    private void doCreateCircle(String name, String description) {
        disposables.add(
                gugujiApi.createCircle(name, description)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                circle -> {
                                    Toast.makeText(getContext(),
                                            getString(R.string.circle_created, circle.getCircleCode()),
                                            Toast.LENGTH_LONG).show();
                                    refreshCircles(false);
                                },
                                throwable -> Toast.makeText(getContext(),
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    private void onJoinCircleClicked() {
        new InkInputDialog.Builder(requireContext())
                .setTitle(R.string.circle_join_title)
                .setSubtitle(R.string.circle_join_hint)
                .setHint(R.string.circle_join_hint)
                .setMaxLength(32)
                .setEmptyErrorMessage(R.string.circle_join_code_empty)
                .setPositiveButton(R.string.circle_join_apply, code -> doSearchAndApply(code.trim()))
                .show();
    }

    private void doSearchAndApply(String code) {
        disposables.add(
                gugujiApi.searchCircle(code)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                circle -> {
                                    if ("approved".equals(circle.getMyStatus())) {
                                        Toast.makeText(getContext(), "您已是该圈子成员", Toast.LENGTH_SHORT).show();
                                        refreshCircles(false);
                                    } else {
                                        doApplyCircle(circle.getCircleId());
                                    }
                                },
                                throwable -> Toast.makeText(getContext(),
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    private void doApplyCircle(long circleId) {
        disposables.add(
                gugujiApi.applyCircle(circleId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                resp -> Toast.makeText(getContext(),
                                        "已提交申请，请等待圈主审批", Toast.LENGTH_SHORT).show(),
                                throwable -> Toast.makeText(getContext(),
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    private void confirmLogout() {
        ConfirmDialog.show(requireContext(), "退出登录",
                "退出后本次登录态将被清除，确定退出吗？", "退出", this::doLogout);
    }

    private void doLogout() {
        authSession.clear();
        renderLoginState();
        Toast.makeText(getContext(), "已退出登录", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCircleClick(CircleModels.MyCircleItem circle) {
        Intent intent = new Intent(requireContext(), CircleDetailActivity.class);
        intent.putExtra(CircleDetailActivity.EXTRA_CIRCLE_ID, circle.getCircleId());
        intent.putExtra(CircleDetailActivity.EXTRA_CIRCLE_NAME, circle.getName());
        intent.putExtra(CircleDetailActivity.EXTRA_MY_ROLE, circle.getMyRole());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disposables.clear();
    }
}