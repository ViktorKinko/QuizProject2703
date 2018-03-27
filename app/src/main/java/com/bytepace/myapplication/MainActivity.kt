package com.bytepace.myapplication

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initTextView()
        startTimer()
    }

    private fun initTextView() {
        val textView = findViewById<TextView>(R.id.text)
        textView.text = BuildConfig.BUILD_TYPE
    }

    private fun startTimer() {
        val fiveMinutes = 300000L
        startService(CountdownService.newIntent(this, fiveMinutes))
    }
}
