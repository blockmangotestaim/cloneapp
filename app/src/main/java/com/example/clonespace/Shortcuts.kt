package com.example.clonespace

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap

object Shortcuts {
    fun add(ctx: Context, row: Row): Boolean {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)) return false
        val intent = Intent(ctx, LaunchProxyActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(LaunchProxyActivity.EXTRA_PKG, row.pkg)
        val info = ShortcutInfoCompat.Builder(ctx, "clone_${row.pkg}")
            .setShortLabel(row.label + " (clone)")
            .setIcon(IconCompat.createWithBitmap(row.icon.toBitmap(192, 192)))
            .setIntent(intent)
            .build()
        return ShortcutManagerCompat.requestPinShortcut(ctx, info, null)
    }
}
