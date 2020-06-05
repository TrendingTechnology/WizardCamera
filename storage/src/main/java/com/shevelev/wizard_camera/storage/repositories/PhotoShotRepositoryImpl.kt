package com.shevelev.wizard_camera.storage.repositories

import android.net.Uri
import com.shevelev.wizard_camera.common_entities.entities.PhotoShot
import com.shevelev.wizard_camera.storage.core.DbCore
import com.shevelev.wizard_camera.storage.entities.PhotoShotDb
import javax.inject.Inject

class PhotoShotRepositoryImpl
@Inject
constructor(
    private val db: DbCore
): PhotoShotRepository {
    override fun create(shot: PhotoShot) = db.photoShot.create(shot.map())

    override fun readPaged(limit: Int, offset: Int): List<PhotoShot> = db.photoShot.readPaged(limit, offset).map { it.map() }

    override fun deleteById(id: Long) = db.photoShot.deleteById(id)

    private fun PhotoShot.map(): PhotoShotDb =
        PhotoShotDb(
            id = id,
            fileName = fileName,
            created = created,
            createdSort = created.toEpochSecond(),
            filter = filter,
            contentUri = contentUri?.toString()
        )

    private fun PhotoShotDb.map(): PhotoShot =
        PhotoShot(
            id = id,
            fileName = fileName,
            created = created,
            filter = filter,
            contentUri = contentUri?.let { Uri.parse(it)}
        )
}