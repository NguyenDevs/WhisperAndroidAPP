package com.nguyendevs.stkh.manager

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nguyendevs.stkh.util.showToast

/**
 * PermissionManager - Quản lý xin quyền runtime.
 * Migration: Java PermissionManager → Kotlin.
 */
class PermissionManager(private val context: Context) {

    private val activity: Activity get() = context as Activity
    private var onWritePermissionGranted: (() -> Unit)? = null

    companion object {
        const val REQUEST_RECORD_AUDIO = 1
        const val REQUEST_READ_STORAGE = 2
        const val REQUEST_WRITE_STORAGE = 3
    }

    fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO
            )
        }
    }

    fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.READ_MEDIA_AUDIO),
                REQUEST_READ_STORAGE
            )
        }
    }

    fun checkWriteStoragePermission(onPermissionGranted: () -> Unit) {
        this.onWritePermissionGranted = onPermissionGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: không cần quyền WRITE_EXTERNAL_STORAGE
            onPermissionGranted()
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_STORAGE
            )
        } else {
            onPermissionGranted()
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
        onStorageGranted: (() -> Unit)? = null
    ) {
        when (requestCode) {
            REQUEST_RECORD_AUDIO -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    context.showToast("Quyền ghi âm bị từ chối!")
                }
            }
            REQUEST_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onStorageGranted?.invoke()
                } else {
                    context.showToast("Quyền truy cập bộ nhớ bị từ chối!")
                }
            }
            REQUEST_WRITE_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onWritePermissionGranted?.invoke()
                } else {
                    context.showToast("Quyền ghi bộ nhớ bị từ chối!")
                }
            }
        }
    }
}
