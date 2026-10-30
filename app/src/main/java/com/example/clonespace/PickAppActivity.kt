package com.example.clonespace

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class PickAppActivity : AppCompatActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var adapter: AppAdapter
    private lateinit var progress: ProgressBar
    private var blocking: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pick)
        progress = findViewById(R.id.progress)
        adapter = AppAdapter(false, onClick = ::onPick)
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@PickAppActivity); adapter = this@PickAppActivity.adapter
        }
        load()
    }

    private fun load() = io.execute {
        val pm = packageManager
        val rows = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName }
            .map { ai: ApplicationInfo ->
                val reason = EngineBridge.checkSupport(this, ai.packageName)
                val note = when {
                    reason != null -> "Unsupported: $reason"
                    EngineBridge.isCloned(ai.packageName) -> "Already cloned"
                    else -> ai.packageName
                }
                Row(ai.packageName, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai), note, reason != null)
            }.sortedWith(compareBy({ it.noteIsError }, { it.label.lowercase() }))
        runOnUiThread { adapter.rows = rows; progress.visibility = View.GONE }
    }

    private fun onPick(row: Row) {
        if (row.noteIsError) return error(row, row.note!!.removePrefix("Unsupported: "))
        if (EngineBridge.isCloned(row.pkg)) { Toast.makeText(this, "Already cloned", Toast.LENGTH_SHORT).show(); return }
        blocking = AlertDialog.Builder(this).setMessage("Cloning ${row.label}…").setCancelable(false).show()
        io.execute {
            val r = EngineBridge.clone(this, row.pkg)
            runOnUiThread {
                blocking?.dismiss()
                if (r.ok) { Toast.makeText(this, "${row.label} cloned", Toast.LENGTH_SHORT).show(); finish() }
                else error(row, r.message ?: "Unknown error")
            }
        }
    }

    private fun error(row: Row, msg: String) {
        AlertDialog.Builder(this).setTitle("Can't clone ${row.label}")
            .setMessage(msg).setPositiveButton("OK", null).show()
    }
}
