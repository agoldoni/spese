package com.spese.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BollettaDao {

    @Insert
    void insert(Bolletta bolletta);

    @Update
    void update(Bolletta bolletta);

    @Delete
    void delete(Bolletta bolletta);

    @Query("SELECT * FROM bollette ORDER BY createdAt DESC")
    List<Bolletta> getAll();
}
