package com.sirjpagdi.mcqscanner

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoadTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_load_test)
        refreshList()
    }

    private fun refreshList() {
        val container = findViewById<LinearLayout>(R.id.containerTests)
        container.removeAllViews()
        val names = Prefs.listTestNames(this)

        if (names.isEmpty()) {
            container.addView(TextView(this).apply {
                text = "No saved tests yet. Use \"Save Test\" on the home screen first."
                setTextColor(0xFF888888.toInt())
                textSize = 14f
                setPadding(0, 20, 0, 0)
            })
            return
        }

        for (name in names) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
            }
            val loadBtn = Button(this).apply {
                text = name
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener {
                    setResult(Activity.RESULT_OK, Intent().putExtra("test_name", name))
                    finish()
                }
            }
            val deleteBtn = Button(this).apply {
                text = "✕"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = 8 }
                setOnClickListener {
                    Prefs.deleteTest(this@LoadTestActivity, name)
                    Toast.makeText(this@LoadTestActivity, "Deleted \"$name\"", Toast.LENGTH_SHORT).show()
                    refreshList()
                }
            }
            row.addView(loadBtn)
            row.addView(deleteBtn)
            container.addView(row)
        }
    }
}
