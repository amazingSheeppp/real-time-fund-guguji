package com.fund.guguji.ui.circle;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fund.guguji.R;
import com.fund.guguji.data.api.GugujiApi;
import com.fund.guguji.data.api.model.CircleModels;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 入圈审批抽屉:圈主对待审批申请逐条通过/拒绝。
 */
public class AuditDialog {

    public interface OnAuditedListener {
        void onAudited();
    }

    public static void show(Context context, GugujiApi api, long circleId,
                            OnAuditedListener listener) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Theme_Guguji_BottomSheetDialog);
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_audit, null);
        dialog.setContentView(root);

        TextView tvEmpty = root.findViewById(R.id.tv_audit_empty);
        RecyclerView recycler = root.findViewById(R.id.recycler_audit);
        recycler.setLayoutManager(new LinearLayoutManager(context));

        CompositeDisposable disposables = new CompositeDisposable();

        // 用数组持有 adapter 引用,规避 lambda 捕获未初始化变量
        final AuditAdapter[] adapterRef = new AuditAdapter[1];
        AuditAdapter adapter = new AuditAdapter((applicationId, action) -> {
            api.auditCircle(circleId, applicationId, action)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            resp -> {
                                Toast.makeText(context,
                                        "approve".equals(action) ? "已通过" : "已拒绝", Toast.LENGTH_SHORT).show();
                                loadAuditList(api, circleId, context, adapterRef[0], tvEmpty, disposables, listener);
                            },
                            throwable -> Toast.makeText(context,
                                    throwable.getMessage(), Toast.LENGTH_LONG).show()
                    );
        });
        adapterRef[0] = adapter;
        recycler.setAdapter(adapter);

        dialog.setOnDismissListener(d -> disposables.clear());
        dialog.show();

        loadAuditList(api, circleId, context, adapter, tvEmpty, disposables, listener);
    }

    /** 拉取待审批申请列表 */
    private static void loadAuditList(GugujiApi api, long circleId, Context context,
                                      AuditAdapter adapter, TextView tvEmpty,
                                      CompositeDisposable disposables, OnAuditedListener listener) {
        disposables.add(
                api.getAuditList(circleId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                items -> {
                                    adapter.submitList(items);
                                    tvEmpty.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
                                    if (adapter.getItemCount() == 0 && listener != null) {
                                        // 审批完自动通知外层刷新圈子信息
                                        listener.onAudited();
                                    }
                                },
                                throwable -> Toast.makeText(context,
                                        throwable.getMessage(), Toast.LENGTH_LONG).show()
                        )
        );
    }

    /** 待审批申请适配器 */
    private static class AuditAdapter extends RecyclerView.Adapter<AuditAdapter.Holder> {

        public interface OnAuditActionListener {
            void onAudit(int applicationId, String action);
        }

        private final List<CircleModels.CircleAuditItem> items = new ArrayList<>();
        private final OnAuditActionListener listener;

        AuditAdapter(OnAuditActionListener listener) {
            this.listener = listener;
        }

        void submitList(List<CircleModels.CircleAuditItem> list) {
            items.clear();
            if (list != null) {
                items.addAll(list);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_audit, parent, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            CircleModels.CircleAuditItem item = items.get(position);
            holder.tvName.setText(item.getNickname());
            holder.tvTime.setText(formatTime(item.getAppliedAt()));
            holder.btnApprove.setOnClickListener(v -> listener.onAudit(Math.toIntExact(item.getApplicationId()), "approve"));
            holder.btnReject.setOnClickListener(v -> listener.onAudit(Math.toIntExact(item.getApplicationId()), "reject"));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class Holder extends RecyclerView.ViewHolder {
            final TextView tvName;
            final TextView tvTime;
            final TextView btnApprove;
            final TextView btnReject;

            Holder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_audit_name);
                tvTime = itemView.findViewById(R.id.tv_audit_time);
                btnApprove = itemView.findViewById(R.id.btn_audit_approve);
                btnReject = itemView.findViewById(R.id.btn_audit_reject);
            }
        }
    }

    private static String formatTime(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        try {
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            Date date = iso.parse(raw);
            SimpleDateFormat out = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return "申请于 " + out.format(date);
        } catch (Exception e) {
            return raw;
        }
    }
}