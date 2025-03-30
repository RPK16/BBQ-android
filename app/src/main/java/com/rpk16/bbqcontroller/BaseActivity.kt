package com.rpk16.bbqcontroller

import android.content.pm.ActivityInfo
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("settings_prefs", MODE_PRIVATE)
    }

    override fun onResume() {
        super.onResume()
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
