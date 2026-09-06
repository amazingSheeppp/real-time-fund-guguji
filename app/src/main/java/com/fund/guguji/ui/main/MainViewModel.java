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

    private final MutableLiveData<Boolean> refreshing = new MutableLiveData<>(false);
    private final MutableLiveData<Event<String>> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> lastRefreshTime = new MutableLiveData<>();
    private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());

    public MainViewModel(@NonNull Application application) {
        super(application);
        RealTimeFundApp app = (RealTimeFundApp) application;
        this.fundRepository = app.getFundRepository();
        this.localFundRepository = app.getLocalFundRepository();
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

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
