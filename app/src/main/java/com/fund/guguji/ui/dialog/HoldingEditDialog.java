package com.fund.guguji.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.fund.guguji.R;
import com.fund.guguji.data.db.entity.HoldingEntity;

/**
 * 持仓编辑弹窗
 */
public class HoldingEditDialog {

    public interface OnSaveListener {
        void onSave(HoldingEntity holding);
    }

    public static void show(Context context, String fundCode, String fundName,
                            HoldingEntity existing, OnSaveListener listener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_holding_edit, null);
        EditText etShares = view.findViewById(R.id.et_shares);
        EditText etCost = view.findViewById(R.id.et_cost);

        if (existing != null) {
            if (existing.getShare() > 0) {
                etShares.setText(String.valueOf(existing.getShare()));
            }
            if (existing.getCost() > 0) {
                etCost.setText(String.valueOf(existing.getCost()));
            }
        }

        new AlertDialog.Builder(context)
                .setTitle("编辑持仓 - " + fundName)
                .setView(view)
                .setPositiveButton("保存", (dialog, which) -> {
                    HoldingEntity holding = new HoldingEntity(fundCode);
                    try {
                        String sharesStr = etShares.getText().toString().trim();
                        holding.setShare(sharesStr.isEmpty() ? 0.0 : Double.parseDouble(sharesStr));
                    } catch (NumberFormatException e) {
                        holding.setShare(0.0);
                    }
                    try {
                        String costStr = etCost.getText().toString().trim();
                        holding.setCost(costStr.isEmpty() ? 0.0 : Double.parseDouble(costStr));
                    } catch (NumberFormatException e) {
                        holding.setCost(0.0);
                    }
                    if (listener != null) listener.onSave(holding);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
