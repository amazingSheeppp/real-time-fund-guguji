package com.fund.guguji.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.repository.FundRepository;
import com.fund.guguji.data.repository.LocalFundRepository;
import com.fund.guguji.util.Event;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 主页面 ViewModel
 */
public class MainViewModel extends AndroidViewModel {

    private final FundRepository fundRepository;
    private final LocalFundRepository localFundRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static final String GROUP_ID_ALL = "ALL";

    private final MutableLiveData<String> selectedGroupId = new MutableLiveData<>(GROUP_ID_ALL);
    private final LiveData<List<FundEntity>> displayFunds;

    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> lastRefreshTime = new MutableLiveData<>();
    private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());

    public MainViewModel(@NonNull Application application) {
        super(application);
        RealTimeFundApp app = (RealTimeFundApp) application;
        this.fundRepository = app.getFundRepository();
        this.localFundRepository = app.getLocalFundRepository();

        this.displayFunds = androidx.lifecycle.Transformations.switchMap(selectedGroupId, groupId -> {
            if (groupId == null || GROUP_ID_ALL.equals(groupId)) {
                return localFundRepository.getAllFunds();
            } else {
                return localFundRepository.getFundsByGroup(groupId);
            }
        });
    }

    public LiveData<String> getSelectedGroupId() {
        return selectedGroupId;
    }

    public void setSelectedGroupId(String groupId) {
        if (groupId == null) groupId = GROUP_ID_ALL;
        selectedGroupId.setValue(groupId);
    }

    public LiveData<List<FundEntity>> getDisplayFunds() {
        return displayFunds;
    }

    public LiveData<List<FundEntity>> getAllFunds() {
        return localFundRepository.getAllFunds();
    }

    public LiveData<List<GroupEntity>> getAllGroups() {
        return localFundRepository.getAllGroups();
    }

    public LiveData<Boolean> isRefreshing() {
        return refreshing;
    }

    public LiveData<String> getLastRefreshTime() {
        return lastRefreshTime;
    }

    public LiveData<Event<String>> getErrorMessage() {
        return errorMessage;
    }

    private boolean pendingRefresh = false;

    /**
     * 刷新所有基金估值(正在刷新时记录 pendingRefresh，刷新完成后自动补刷)
     */
    public void refreshValuations() {
        if (Boolean.TRUE.equals(refreshing.getValue())) {
            pendingRefresh = true;
            return;
        }
        refreshing.setValue(true);
        disposables.add(
                fundRepository.refreshAllValuations()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                funds -> {
                                    lastRefreshTime.setValue(timeFormat.format(new java.util.Date()));
                                    onRefreshFinished();
                                },
                                throwable -> {
                                    onRefreshFinished();
                                    errorMessage.setValue(new Event<>("刷新失败: " + throwable.getMessage()));
                                }
                        )
        );
    }

    private void onRefreshFinished() {
        refreshing.setValue(false);
        if (pendingRefresh) {
            pendingRefresh = false;
            refreshValuations();
        }
    }

    /**
     * 异步删除基金
     */
    public void deleteFund(FundEntity fund) {
        disposables.add(
                Completable.fromAction(() -> {
                    localFundRepository.deleteFundByCode(fund.getCode());
                    localFundRepository.removeFundFromAllGroups(fund.getCode());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { /* LiveData 自动刷新 */ },
                        throwable -> errorMessage.setValue(new Event<>(
                                "删除失败: " + throwable.getMessage()))
                )
        );
    }

    /**
     * 异步更新基金实体（如修复 orderIndex）
     */
    public void updateFund(FundEntity fund) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.updateFund(fund))
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                                () -> { /* LiveData 自动刷新 */ },
                                throwable -> {}
                        )
        );
    }

    /**
     * 异步创建新分组
     */
    public void createGroup(String name, java.util.function.Consumer<GroupEntity> onSuccess) {
        if (name == null || name.trim().isEmpty()) {
            errorMessage.setValue(new Event<>("分组名称不能为空"));
            return;
        }
        final String finalName = name.trim();
        disposables.add(
                io.reactivex.rxjava3.core.Single.fromCallable(() -> {
                    if (localFundRepository.isGroupNameExists(finalName)) {
                        throw new IllegalArgumentException("已存在同名分组");
                    }
                    String id = java.util.UUID.randomUUID().toString();
                    GroupEntity group = new GroupEntity(id, finalName);
                    localFundRepository.insertGroup(group);
                    return group;
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        group -> {
                            if (onSuccess != null) {
                                onSuccess.accept(group);
                            }
                        },
                        throwable -> errorMessage.setValue(new Event<>(throwable.getMessage()))
                )
        );
    }

    /**
     * 异步重命名分组
     */
    public void renameGroup(String groupId, String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            errorMessage.setValue(new Event<>("分组名称不能为空"));
            return;
        }
        final String finalName = newName.trim();
        disposables.add(
                Completable.fromAction(() -> {
                    if (localFundRepository.isGroupNameExists(finalName)) {
                        throw new IllegalArgumentException("已存在同名分组");
                    }
                    localFundRepository.updateGroupName(groupId, finalName);
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> { /* LiveData 自动刷新 */ },
                        throwable -> errorMessage.setValue(new Event<>(throwable.getMessage()))
                )
        );
    }

    /**
     * 异步删除分组 (若当前选中该组，则重置为全部)
     */
    public void deleteGroup(String groupId) {
        if (groupId == null || GROUP_ID_ALL.equals(groupId)) return;
        if (groupId.equals(selectedGroupId.getValue())) {
            selectedGroupId.setValue(GROUP_ID_ALL);
        }
        disposables.add(
                Completable.fromAction(() -> localFundRepository.deleteGroupById(groupId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> { /* LiveData 自动刷新 */ },
                                throwable -> errorMessage.setValue(new Event<>("删除分组失败: " + throwable.getMessage()))
                        )
        );
    }

    /**
     * 异步更新基金所属的分组集合
     */
    public void updateFundGroups(String fundCode, List<String> groupIds) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.setFundGroups(fundCode, groupIds))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> { /* LiveData 自动刷新 */ },
                                throwable -> errorMessage.setValue(new Event<>("更新分组失败: " + throwable.getMessage()))
                        )
        );
    }

    /**
     * 异步将基金从某个具体分组移出
     */
    public void removeFundFromGroup(String fundCode, String groupId) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.removeFundFromGroup(fundCode, groupId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> { /* LiveData 自动刷新 */ },
                                throwable -> errorMessage.setValue(new Event<>("移出分组失败: " + throwable.getMessage()))
                        )
        );
    }

    /**
     * 批量将已有基金添加至某分组
     */
    public void addFundsToGroup(List<String> fundCodes, String groupId) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.addFundsToGroup(fundCodes, groupId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> { /* LiveData 自动刷新 */ },
                                throwable -> errorMessage.setValue(new Event<>("添加失败: " + throwable.getMessage()))
                        )
        );
    }

    /**
     * 异步获取某基金当前所属的分组 ID 列表
     */
    public void getGroupIdsByFund(String fundCode, java.util.function.Consumer<List<String>> callback) {
        disposables.add(
                io.reactivex.rxjava3.core.Single.fromCallable(() -> localFundRepository.getGroupIdsByFundSync(fundCode))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                ids -> {
                                    if (callback != null) callback.accept(ids);
                                },
                                throwable -> {
                                    if (callback != null) callback.accept(new java.util.ArrayList<>());
                                }
                        )
        );
    }

    /**
     * 异步获取某分组中已有的基金代码列表
     */
    public void getFundCodesInGroup(String groupId, java.util.function.Consumer<List<String>> callback) {
        disposables.add(
                io.reactivex.rxjava3.core.Single.fromCallable(() -> localFundRepository.getFundCodesInGroup(groupId))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                codes -> {
                                    if (callback != null) callback.accept(codes);
                                },
                                throwable -> {
                                    if (callback != null) callback.accept(new java.util.ArrayList<>());
                                }
                        )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
