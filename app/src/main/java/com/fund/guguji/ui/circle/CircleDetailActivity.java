package com.fund.guguji.ui.circle;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fund.guguji.R;
import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.data.api.AuthSession;
import com.fund.guguji.data.api.GugujiApi;
import com.fund.guguji.data.api.model.HoldingModels;
import com.fund.guguji.ui.dialog.ConfirmDialog;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 圈子详情页:朋友持仓展示 + 圈主审批/成员管理/解散 + 成员退出。
 */
public class CircleDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CIRCLE_ID = "circle_id";
    public static final String EXTRA_CIRCLE_NAME = "circle_name";
    public static final String EXTRA_MY_ROLE = "my_role";

    private final CompositeDisposable disposables = new CompositeDisposable();

    private GugujiApi gugujiApi;
    private AuthSession authSession;

    private long circleId;
    private String myRole = "member";

    private TextView tvCircleName;
    private TextView tvMyRole;
    private TextView tvCircleMeta;
    private View layoutOwnerActions;
    private TextView btnManageMembers;
    private TextView btnAudit;
    private TextView btnLeave;
    private TextView btnDissolve;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recyclerFriends;
    private View layoutFriendsEmpty;
    private FriendHoldingsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_circle_detail);

        RealTimeFundApp app = (RealTimeFundApp) getApplication();
        gugujiApi = app.getGugujiApi();
        authSession = app.getAuthSession();

        circleId = getIntent().getLongExtra(EXTRA_CIRCLE_ID, -1);
        myRole = getIntent().getStringExtra(EXTRA_MY_ROLE);
        if (myRole == null) myRole = "member";

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getIntent().getStringExtra(EXTRA_CIRCLE_NAME));
        toolbar.setNavigationOnClickListener(v -> finish());

        tvCircleName = findViewById(R.id.tv_circle_name);
        tvMyRole = findViewById(R.id.tv_my_role);
        tvCircleMeta = findViewById(R.id.tv_circle_meta);
        layoutOwnerActions = findViewById(R.id.layout_owner_actions);
        btnManageMembers = findViewById(R.id.btn_manage_members);
        btnAudit = findViewById(R.id.btn_audit);
        btnLeave = findViewById(R.id.btn_leave);
        btnDissolve = findViewById(R.id.btn_dissolve);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        recyclerFriends = findViewById(R.id.recycler_friends);
        layoutFriendsEmpty = findViewById(R.id.layout_friends_empty);

        tvCircleName.setText(getIntent().getStringExtra(EXTRA_CIRCLE_NAME));

        boolean isOwner = "owner".equals(myRole);
        tvMyRole.setText(isOwner ? R.string.circle_role_owner : R.string.circle_role_member);
        tvMyRole.setBackgroundResource(isOwner ? R.drawable.bg_badge_up : R.drawable.bg_badge_flat);
        tvMyRole.setTextColor(getColor(isOwner ? R.color.card : R.color.ink_mid));
        layoutOwnerActions.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        btnLeave.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        btnDissolve.setVisibility(isOwner ? View.VISIBLE : View.GONE);

        recyclerFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendHoldingsAdapter();
        recyclerFriends.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> refresh(true));

        btnManageMembers.setOnClickListener(v ->
                MemberManagementDialog.show(this, gugujiApi, circleId, authSession.getUserId(),
                        this::refreshOnlyMeta));
        btnAudit.setOnClickListener(v ->
                AuditDialog.show(this, gugujiApi, circleId, this::refreshOnlyMeta));
        btnLeave.setOnClickListener(v -> confirmLeave());
        btnDissolve.setOnClickListener(v -> confirmDissolve());

        refresh(false);
    }

    private void refreshOnlyMeta() {
        refresh(true);
    }

    /**
     * 加载圈子聚合信息 + 朋友持仓。
     * 后端未单独提供圈子详情接口,信息通过「我的圈子」与「朋友持仓」两个接口拼装。
     */
    private void refresh(boolean showRefreshing) {
        if (showRefreshing) swipeRefresh.setRefreshing(true);

        disposables.add(
                Observable.zip(
                        gugujiApi.getMyCircles(),
                        gugujiApi.getCircleFriendHoldings(circleId),
                        (circles, friendHoldings) -> {
                            // 从我的圈子列表中匹配当前圈子的元信息
                            String code = "";
                            int memberCount = 0;
                            if (circles != null) {
                                for (com.fund.guguji.data.api.model.CircleModels.MyCircleItem item : circles) {
                                    if (item.getCircleId() == circleId) {
                                        code = item.getCircleCode();
                                        memberCount = item.getMemberCount();
                                        break;
                                    }
                                }
                            }
                            return new BundleData(code, memberCount, friendHoldings);
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                data -> {
                                    swipeRefresh.setRefreshing(false);
                                    bindData(data);
                                },
                                throwable -> {
                                    swipeRefresh.setRefreshing(false);
                                    Toast.makeText(this, throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                        )
        );
    }

    /** 拼装后的聚合数据简要载体 */
    private static final class BundleData {
        final String circleCode;
        final int memberCount;
        final HoldingModels.FriendHoldingsResponse friendHoldings;

        BundleData(String circleCode, int memberCount, HoldingModels.FriendHoldingsResponse friendHoldings) {
            this.circleCode = circleCode;
            this.memberCount = memberCount;
            this.friendHoldings = friendHoldings;
        }
    }

    private void bindData(BundleData data) {
        tvCircleMeta.setText(String.format(Locale.getDefault(),
                "圈子码 %s · %d 名成员", data.circleCode, data.memberCount));

        java.util.List<HoldingModels.FriendHoldingItem> friends =
                data.friendHoldings != null ? data.friendHoldings.getFriends() : null;
        adapter.submitList(friends);
        layoutFriendsEmpty.setVisibility(friends == null || friends.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void confirmLeave() {
        ConfirmDialog.show(this, getString(R.string.circle_detail_leave),
                "退出后将无法查看该圈子朋友持仓，确定退出吗？", getString(R.string.circle_detail_leave), () ->
                        disposables.add(gugujiApi.leaveCircle(circleId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(v -> {
                                    Toast.makeText(this, "已退出圈子", Toast.LENGTH_SHORT).show();
                                    finish();
                                }, throwable -> Toast.makeText(this,
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()))
        );
    }

    private void confirmDissolve() {
        ConfirmDialog.show(this, getString(R.string.circle_detail_dissolve),
                "解散后圈子与成员关系将被永久删除，无法恢复，确定解散吗？",
                getString(R.string.circle_detail_dissolve), () ->
                        disposables.add(gugujiApi.dissolveCircle(circleId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(v -> {
                                    Toast.makeText(this, "圈子已解散", Toast.LENGTH_SHORT).show();
                                    finish();
                                }, throwable -> Toast.makeText(this,
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()))
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}