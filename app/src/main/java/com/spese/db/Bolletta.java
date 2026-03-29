package com.spese.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.UUID;

@Entity(tableName = "bollette",
        foreignKeys = @ForeignKey(
                entity = PurchaseType.class,
                parentColumns = "id",
                childColumns = "purchaseTypeId",
                onDelete = ForeignKey.RESTRICT),
        indices = {@Index("purchaseTypeId")})
public class Bolletta {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    private String purchaseTypeId;

    private double amount;
    private int month;
    private int year;
    private long createdAt;

    public Bolletta(@NonNull String purchaseTypeId, double amount, int month, int year) {
        this.id = UUID.randomUUID().toString();
        this.purchaseTypeId = purchaseTypeId;
        this.amount = amount;
        this.month = month;
        this.year = year;
        this.createdAt = System.currentTimeMillis();
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    @NonNull
    public String getPurchaseTypeId() { return purchaseTypeId; }
    public void setPurchaseTypeId(@NonNull String purchaseTypeId) { this.purchaseTypeId = purchaseTypeId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
