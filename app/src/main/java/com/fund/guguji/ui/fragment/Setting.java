package com.fund.guguji.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fund.guguji.RealTimeFundApp;
import com.fund.guguji.R;
import com.fund.guguji.data.db.AppDatabase;
import com.fund.guguji.ui.dialog.ConfirmDialog;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class Setting extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btn_clear_data).setOnClickListener(v ->
                ConfirmDialog.show(requireActivity(), "清除所有数据",
                        "将清空全部自选基金与自定义分组，此操作不可恢复，确定继续吗？",
                        "清除",
                        this::clearAllData));

        view.findViewById(R.id.btn_about).setOnClickListener(v ->
                Toast.makeText(getContext(), "咕咕鸡 v1.0", Toast.LENGTH_SHORT).show());
    }

    private void clearAllData() {
        AppDatabase db = ((RealTimeFundApp) requireActivity().getApplication()).getDatabase();
        Completable.fromAction(() -> {
                    db.fundDao().deleteAll();
                    db.groupDao().deleteAllGroups();
                    db.groupDao().deleteAllCrossRefs();
                    db.clearAllTables();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Toast.makeText(getContext(), "已清除所有数据", Toast.LENGTH_SHORT).show(),
                        throwable -> Toast.makeText(getContext(),
                                "清除失败: " + throwable.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
