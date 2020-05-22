package com.shevelev.wizard_camera.storage.core

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shevelev.wizard_camera.storage.dao.PhotoShotDao
import com.shevelev.wizard_camera.storage.entities.PhotoShotDb
import com.shevelev.wizard_camera.storage.type_converters.DateTimeTypeConverter
import com.shevelev.wizard_camera.storage.type_converters.FilterCodeConverter

@Database(
    entities = [
        PhotoShotDb::class
    ],
    version = 1)
@TypeConverters(
    DateTimeTypeConverter::class,
    FilterCodeConverter::class)
abstract class DbCore: RoomDatabase(), DbCoreDao, DbCoreRun {
    abstract override val photoShot: PhotoShotDao

    /**
     * Run some code without transaction
     */
    override fun <T>run(action: (DbCoreDao) -> T): T = action.invoke(this)

    /**
     * Run some code in transaction
     */
    override fun <T>runConsistent(action: (DbCoreDao) -> T): T =
        runInTransaction<T> {
            action(this)
        }
}