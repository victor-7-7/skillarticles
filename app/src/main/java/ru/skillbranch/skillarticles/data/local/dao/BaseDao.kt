package ru.skillbranch.skillarticles.data.local.dao

import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T : Any> {

    // IGNORE: если запись с таким ключом (id) уже есть в базе,
    // то запись, предложенная к вставке, отбрасывается (игнорируется)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(list: List<T>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(obj: T): Long

    @Update
    fun update(list: List<T>)

    @Update
    fun update(obj: T)
}