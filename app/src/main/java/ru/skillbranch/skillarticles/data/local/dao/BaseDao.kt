package ru.skillbranch.skillarticles.data.local.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update

interface BaseDao<T : Any> {

    // IGNORE: если запись с таким первичным ключом (id) уже есть в базе,
    // то запись, предложенная к вставке, отбрасывается (игнорируется).
    // Возвращаемый объект - список из значений столбца rowId каждой
    // вставленной записи (это НЕ список первичных ключей)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(list: List<T>): List<Long>

    // https://developer.android.com/training/data-storage/room/accessing-data#convenience-insert
    // If the @Insert method receives only 1 parameter, it can return a long,
    // which is the new rowId for the inserted item.
    // Возвращаемое значение (идетнификатор табличной записи: rowId) не является
    // значением первичного ключа этой записи (https://www.sqlite.org/rowidtable.html).
    // Это число - unique, non-NULL, signed 64-bit integer.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(obj: T): Long

    @Update
    suspend fun update(list: List<T>)

    @Update
    suspend fun update(obj: T)

    @Delete
    suspend fun delete(obj: T)
}