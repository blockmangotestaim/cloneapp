package com.example.clonespace

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var adapter: AppAdapter
    private lateinit var empty: TextView
    private var pendingLaunch: String? = null

    // Cloned apps ask the host for runtime permissions, so we pre-request the common ones.
    private val runtimePerms = listOf(
        Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,
        Manifest.permission.GET_ACCOUNTS, Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.CALL_PHONE
    )
    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        pendingLaunch?.let { doLaunch(it) }; pendingLaunch = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "CloneSpace"
        supportActionBar?.subtitle = EngineBridge.deviceInfo()
        empty = findViewById(R.id.empty)
        adapter = AppAdapter(true, onClick = { launch(it.pkg) }, onMenu = { v, r -> showMenu(v, r) })
        findViewById<RecyclerView>(R.id.list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity); adapter = this@MainActivity.adapter
        }
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            startActivity(Intent(this, PickAppActivity::class.java))
        }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() = io.execute {
        val pm = packageManager
        val rows = EngineBridge.listClonedPackages().map { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                Row(pkg, pm.getApplicationLabel(ai).toString(), pm.getApplicationIcon(ai))
            } catch (_: Exception) {
                Row(pkg, pkg, pm.defaultActivityIcon, "Original app was uninstalled", true)
            }
        }.sortedBy { it.label.lowercase() }
        runOnUiThread {
            adapter.rows = rows
            empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun launch(pkg: String) {
        val missing = runtimePerms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty() && !askedOnce) {
            askedOnce = true; pendingLaunch = pkg
            permLauncher.launch(missing.toTypedArray())
        } else doLaunch(pkg)
    }

    private fun doLaunch(pkg: String) {
        val r = EngineBridge.launch(pkg)
        if (!r.ok) AlertDialog.Builder(this).setTitle("Can't launch clone")
            .setMessage(r.message).setPositiveButton("OK", null).show()
    }

    private fun showMenu(anchor: View, row: Row) {
        PopupMenu(this, anchor).apply {
            menu.add("Add shortcut to home screen"); menu.add("Delete clone")
            setOnMenuItemClickListener { item ->
                if (item.title.startsWith("Add")) {
                    Toast.makeText(this@MainActivity,
                        if (Shortcuts.add(this@MainActivity, row)) "Shortcut requested" else "Your launcher doesn't support pinned shortcuts",
                        Toast.LENGTH_SHORT).show()
                } else confirmDelete(row)
                true
            }
        }.show()
    }

    private fun confirmDelete(row: Row) {
        AlertDialog.Builder(this).setTitle("Delete clone of ${row.label}?")
            .setMessage("All data inside the clone (accounts, chats, saves) will be erased. The original app is not affected.")
            .setPositiveButton("Delete") { _, _ ->
                io.execute {
                    val r = EngineBridge.delete(row.pkg)
                    runOnUiThread {
                        if (!r.ok) Toast.makeText(this, r.message, Toast.LENGTH_LONG).show()
                        refresh()
                    }
                }
            }.setNegativeButton("Cancel", null).show()
    }

    companion object { private var askedOnce = false }
}
