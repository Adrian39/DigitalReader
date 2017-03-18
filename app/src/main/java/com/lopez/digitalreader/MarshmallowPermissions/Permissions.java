package com.lopez.digitalreader.MarshmallowPermissions;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Created by JacoboAdrian on 3/7/2017.
 */

public class Permissions {
    public static final int EXTERNAL_STORATE_PERMISSION_REQUEST_CODE = 1;
    Activity activity;

    public Permissions(Activity activity) {
        this.activity = activity;
    }

    public boolean checkPermissionsForExternalStorage() {
        int result = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    public void requestPermissionForExternalStorage(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            Toast.makeText(activity,
                    "External Storage permission needed. Please allow in App Settings for additional functionality.",
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    EXTERNAL_STORATE_PERMISSION_REQUEST_CODE);
        }
    }
}
