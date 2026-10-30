package com.example.clonespace

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/** Invisible activity used by home-screen shortcuts to start a clone. */
class LaunchProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PKG)
        if (pkg != null) {
            val r = EngineBridge.launch(pkg)
            if (!r.ok) Toast.makeText(this, r.message, Toast.LENGTH_LONG).show()
        }
        finish()
    }
    companion object { const val EXTRA_PKG = "pkg" }
}
