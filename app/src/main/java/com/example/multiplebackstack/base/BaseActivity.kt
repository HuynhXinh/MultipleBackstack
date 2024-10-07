package com.example.multiplebackstack.base

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity(layoutId: Int) : AppCompatActivity(layoutId) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(
            this::class.simpleName,
            "onCreate - instance: ${hashCode()} - savedInstanceState: $savedInstanceState"
        )
    }

    override fun onStart() {
        super.onStart()
        Log.d(this::class.simpleName, "onStart - instance: ${hashCode()}")
    }

    override fun onResume() {
        super.onResume()
        Log.d(this::class.simpleName, "onResume - instance: ${hashCode()}")
    }

    override fun onPause() {
        super.onPause()
        Log.d(this::class.simpleName, "onPause - instance: ${hashCode()}")
    }

    override fun onStop() {
        super.onStop()
        Log.d(this::class.simpleName, "onStop - instance: ${hashCode()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(this::class.simpleName, "onDestroy - instance: ${hashCode()}")
    }
}