package com.fund.guguji.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 持仓记录实体
 * 存储用户持有的基金份额和成本价
 */
@Entity(tableName = "holdings")
public class HoldingEntity {
    @PrimaryKey
    @NonNull
    private String fundCode;   // 基金代码
    private Double share;      // 持有份额
    private Double cost;       // 持仓成本价

    public HoldingEntity(@NonNull String fundCode) {
        this.fundCode = fundCode;
    }

    @NonNull
    public String getFundCode() { return fundCode; }
    public void setFundCode(@NonNull String fundCode) { this.fundCode = fundCode; }

    public Double getShare() { return share; }
    public void setShare(Double share) { this.share = share; }

    public Double getCost() { return cost; }
    public void setCost(Double cost) { this.cost = cost; }
}
