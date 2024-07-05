package com.flamyoad.sdkphotos34activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    private lateinit var requestPhotosPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val configs: Configs by lazy { Configs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btn_request).setOnClickListener {
            if (hasFullyObtainedPhotosPermission()) {
                Toast.makeText(this, "Full permission was granted", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (shouldShowPermissionPopup()) {
                requestPhotoPermission()
            } else {
                openSettings()
            }
        }

        // ADR 14:
        //      Able to keep requesting. shouldShowRequestRationale should not be used here.

        // ADR 13 and below:
        //      1st try - Deny (shouldShowRequestRationale = false)
        //      2nd try Deny (shouldShowRequestRationale = true)
        //      3rd try Cannot show request popup anymore (shouldShowRequestRationale = false)
        requestPhotosPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                val userDenied = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // If user selected 0 photos, below will be false, else true
                    it[READ_MEDIA_VISUAL_USER_SELECTED] != true
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    it[READ_MEDIA_IMAGES] != true
                } else {
                   it[READ_EXTERNAL_STORAGE] != true
                }
                if (userDenied) {
                    configs.setBool("has_denied_photos", true)
                }
            }
    }

    private fun requestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPhotosPermissionLauncher.launch(
                arrayOf(
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPhotosPermissionLauncher.launch(arrayOf(READ_MEDIA_IMAGES))
        } else {
            requestPhotosPermissionLauncher.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }

    private fun hasFullyObtainedPhotosPermission(): Boolean {
        // If true, means Full access on Android 13 (API level 33) or higher
        // On Android 14 (API level 34), partial photo permission is not counted as full access here.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PERMISSION_GRANTED
            // Full access up to Android 12 (API level 32)
        } else {
            ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED
        }
    }

    private fun shouldShowPermissionPopup(): Boolean {
        return configs.getBool("has_denied_photos", true)
//        val hasDeniedPermission = configs.getBool("has_denied_photos", true)
//        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
//            hasDeniedPermission || ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED
//        } else {
//            hasDeniedPermission
//        }
    }

    /**
     * ADR 14, if user has keep selecting Dont Allow for twice, permission popup cant be shown anymore.
     */
    private fun shouldShowRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityCompat.shouldShowRequestPermissionRationale(this, READ_MEDIA_VISUAL_USER_SELECTED)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.shouldShowRequestPermissionRationale(this, READ_MEDIA_IMAGES)
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)
        }
    }

    // User has granted partial photo permission. But app should still let user re-select allowed photos
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun hasGrantedPartialPhotosPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED
    }

    private fun openSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
}