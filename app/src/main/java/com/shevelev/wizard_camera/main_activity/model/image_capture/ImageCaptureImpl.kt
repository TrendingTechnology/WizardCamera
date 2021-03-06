package com.shevelev.wizard_camera.main_activity.model.image_capture

import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import com.shevelev.wizard_camera.common_entities.entities.PhotoShot
import com.shevelev.wizard_camera.common_entities.enums.FilterCode
import com.shevelev.wizard_camera.main_activity.dto.ScreenOrientation
import com.shevelev.wizard_camera.shared.coroutines.DispatchersProvider
import com.shevelev.wizard_camera.shared.files.FilesHelper
import com.shevelev.wizard_camera.shared.media_scanner.MediaScanner
import com.shevelev.wizard_camera.storage.repositories.PhotoShotRepository
import com.shevelev.wizard_camera.utils.id.IdUtil
import kotlinx.coroutines.withContext
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.io.FileOutputStream
import java.io.OutputStream
import javax.inject.Inject

class ImageCaptureImpl
@Inject
constructor(
    private val dispatchersProvider: DispatchersProvider,
    private val photoShotRepository: PhotoShotRepository,
    private val filesHelper: FilesHelper,
    private val mediaScanner: MediaScanner
) : ImageCapture {

    override var inProgress: Boolean = false
        private set

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun capture(textureView: TextureView, activeFilter: FilterCode, screenOrientation: ScreenOrientation): Boolean {
        inProgress = true
        try {
            val rawBitmap = textureView.bitmap

            val bitmap = withContext(dispatchersProvider.calculationsDispatcher) {
                correctScreenOrientation(rawBitmap, screenOrientation)
            }

            val outputFile = withContext(dispatchersProvider.ioDispatcher) {
                filesHelper.createFileForShot(activeFilter).also { outputFile ->
                    FileOutputStream(outputFile).use { outputStream ->
                        compressBitmap(bitmap, outputStream)
                        outputStream.flush()
                    }
                }
            }

            val contentUri = mediaScanner.processNewShot(outputFile)

            withContext(dispatchersProvider.ioDispatcher) {
                saveToDb(outputFile.name, activeFilter, contentUri)
            }

            return true
        } catch (ex: Exception) {
            Timber.e(ex)
            return false
        } finally {
            inProgress = false
        }
    }

    private fun compressBitmap(bitmap: Bitmap, outputStream: OutputStream) =
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun convertScreenOrientationToDegrees(orientation: ScreenOrientation): Float =
        when (orientation) {
            ScreenOrientation.PORTRAIT -> 0f
            ScreenOrientation.LANDSCAPE -> 270f
            ScreenOrientation.REVERSED_LANDSCAPE -> 90f
            ScreenOrientation.REVERSED_PORTRAIT -> 180f
        }

    private fun correctScreenOrientation(bitmap: Bitmap, screenOrientation: ScreenOrientation): Bitmap {
        val degrees = convertScreenOrientationToDegrees(screenOrientation)
        return if (degrees != 0f) {
            rotate(bitmap, degrees)
        } else {
            bitmap
        }
    }

    private fun saveToDb(fileName: String, filter: FilterCode, contentUri: Uri?) =
        photoShotRepository.create(PhotoShot(IdUtil.generateLongId(), fileName, ZonedDateTime.now(), filter, contentUri))
}