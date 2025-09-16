package com.nguyendevs.stkh.Manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.nguyendevs.stkh.MainActivity;
import com.nguyendevs.stkh.R;

public class PermissionManager {
    private final Context context;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    private static final int REQUEST_READ_STORAGE_PERMISSION = 2;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 3;
    private Runnable onWritePermissionGranted; // Callback for file export

    public PermissionManager(Context context) {
        this.context = context;
    }
    public void checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        }
    }
    public void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.READ_MEDIA_AUDIO}, REQUEST_READ_STORAGE_PERMISSION);
        }
    }
    public void checkWriteStoragePermission(Runnable onPermissionGranted) {
        this.onWritePermissionGranted = onPermissionGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (onWritePermissionGranted != null) {
                onWritePermissionGranted.run();
            }
            return;
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
        } else {
            if (onWritePermissionGranted != null) {
                onWritePermissionGranted.run();
            }
        }
    }
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Quyền ghi âm bị từ chối!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_READ_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                String selectedMode = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("selectedMode", "Offline");
                if (selectedMode.equals("Online")) {
                    ((MainActivity) context).findViewById(R.id.imageView4).performClick();
                }
            } else {
                Toast.makeText(context, "Quyền truy cập bộ nhớ bị từ chối!", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (onWritePermissionGranted != null) {
                    onWritePermissionGranted.run();
                }
            } else {
                Toast.makeText(context, "Quyền ghi bộ nhớ bị từ chối!", Toast.LENGTH_LONG).show();
            }
        }
    }
}