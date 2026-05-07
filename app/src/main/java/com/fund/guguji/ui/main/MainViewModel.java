package com.fund.guguji.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.data.db.entity.FundEntity;
import com.fund.guguji.data.db.entity.GroupEntity;
import com.fund.guguji.data.db.entity.HoldingEntity;
import com.fund.guguji.data.repository.FundRepository;
import com.fund.guguji.data.repository.LocalFundRepository;
import com.fund.guguji.util.Event;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
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

    public MainViewModel(@NonNull Application application) {
        super(application);
        RealTimeFundApp app = (RealTimeFundApp) application;
        this.fundRepository = app.getFundRepository();
        this.localFundRepository = app.getLocalFundRepository();
    }

    public LiveData<List<FundEntity>> getAllFunds() {
        return localFundRepository.getAllFunds();
    }

    public LiveData<List<HoldingEntity>> getAllHoldings() {
        return localFundRepository.getAllHoldings();
    }

    public LiveData<List<GroupEntity>> getAllGroups() {
        return localFundRepository.getAllGroups();
    }

    public LiveData<Boolean> isRefreshing() {
        return refreshing;
    }

    public LiveData<Event<String>> getErrorMessage() {
        return errorMessage;
    }

    /**
     * 刷新所有基金估值
     */
    public void refreshValuations() {
        refreshing.setValue(true);
        disposables.add(
                fundRepository.refreshAllValuations()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                funds -> refreshing.setValue(false),
                                throwable -> {
                                    refreshing.setValue(false);
                                    errorMessage.setValue(new Event<>("刷新失败: " + throwable.getMessage()));
                                }
                        )
        );
    }

    /**
     * 异步加载持仓信息
     */
    public void loadHoldingByCode(String fundCode, OnHoldingLoaded listener) {
        disposables.add(
                Single.fromCallable(() -> localFundRepository.getHoldingByCode(fundCode))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                holding -> {
                                    if (listener != null) listener.onHoldingLoaded(holding);
                                },
                                throwable -> errorMessage.setValue(new Event<>(
                                        "加载持仓失败: " + throwable.getMessage()))
                        )
        );
    }

    public interface OnHoldingLoaded {
        void onHoldingLoaded(HoldingEntity holding);
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
     * 异步保存持仓信息
     */
    public void saveHolding(HoldingEntity holding) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.saveHolding(holding))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                () -> errorMessage.setValue(new Event<>("持仓已保存")),
                                throwable -> errorMessage.setValue(new Event<>(
                                        "保存持仓失败: " + throwable.getMessage()))
                        )
        );
    }

    public void deleteHolding(String fundCode) {
        disposables.add(
                Completable.fromAction(() -> localFundRepository.deleteHolding(fundCode))
                        .subscribeOn(Schedulers.io())
                        .subscribe()
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
