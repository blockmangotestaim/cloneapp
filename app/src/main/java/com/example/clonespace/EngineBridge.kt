package com.example.clonespace

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import java.util.zip.ZipFile

data class Row(
    val pkg: String,
    val label: String,
    val icon: Drawable,
    val note: String? = null,      // subtitle text
    val noteIsError: Boolean = false
)

data class Outcome(val ok: Boolean, val message: String? = null)

object EngineBridge {
    /** One clone per app lives in virtual user 0. (Use 1,2,... for extra clones of the same app.) */
    const val USER_ID = 0

    private val core get() = BlackBoxCore.get()

    // ---- lifecycle -------------------------------------------------------
    fun attach(base: Context) {
        core.doAttachBaseContext(base, object : ClientConfiguration() {
            override fun getHostPackageName(): String = base.packageName
        })
    }

    fun create() = core.doCreate()

    // ---- operations ------------------------------------------------------
    fun isCloned(pkg: String): Boolean = try { core.isInstalled(pkg, USER_ID) } catch (_: Throwable) { false }

    fun listClonedPackages(): List<String> = try {
        core.getInstalledApplications(0, USER_ID).map { it.packageName }
    } catch (_: Throwable) { emptyList() }

    fun clone(ctx: Context, pkg: String): Outcome {
        checkSupport(ctx, pkg)?.let { return Outcome(false, it) }
        return try {
            val r = core.installPackageAsUser(pkg, USER_ID)
            if (r.success) Outcome(true)
            else Outcome(false, "The engine could not install this app: ${r.msg ?: "unknown reason"}.\n" +
                    "It likely uses features the virtual engine doesn't support (split APKs, anti-tamper, or special system APIs).")
        } catch (t: Throwable) {
            Outcome(false, "Cloning failed: ${t.javaClass.simpleName}: ${t.message}")
        }
    }

    fun launch(pkg: String): Outcome = try {
        if (!isCloned(pkg)) Outcome(false, "This clone no longer exists.")
        else { core.launchApk(pkg, USER_ID); Outcome(true) }
    } catch (t: Throwable) {
        Outcome(false, "Couldn't launch the clone: ${t.message}")
    }

    fun delete(pkg: String): Outcome = try {
        core.uninstallPackageAsUser(pkg, USER_ID); Outcome(true)
    } catch (t: Throwable) { Outcome(false, "Couldn't delete the clone: ${t.message}") }

    // ---- 32-bit / 64-bit handling ---------------------------------------
    val hostIs64Bit: Boolean get() = if (Build.VERSION.SDK_INT >= 23) Process.is64Bit() else false

    /** Reads lib/<abi>/ folders of the base APK and its splits (no hidden APIs). */
    private fun nativeAbis(ai: ApplicationInfo): Set<String> {
        val paths = listOfNotNull(ai.sourceDir) + (ai.splitSourceDirs?.toList() ?: emptyList())
        val abis = HashSet<String>()
        for (p in paths) try {
            ZipFile(p).use { z ->
                for (e in z.entries()) {
                    val n = e.name
                    if (n.startsWith("lib/") && n.count { it == '/' } >= 2) abis += n.split('/')[1]
                }
            }
        } catch (_: Throwable) {}
        return abis
    }

    /** Returns a human-readable reason if the app can't be cloned by THIS build, else null. */
    fun checkSupport(ctx: Context, pkg: String): String? {
        if (pkg == ctx.packageName) return "CloneSpace can't clone itself."
        val ai = try { ctx.packageManager.getApplicationInfo(pkg, 0) }
        catch (_: PackageManager.NameNotFoundException) { return "App is not installed." }

        val sys = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updated = ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        if (sys && !updated) return "System apps can't be cloned."

        val abis = nativeAbis(ai)
        val has64 = abis.any { it == "arm64-v8a" || it == "x86_64" }
        val has32 = abis.any { it == "armeabi-v7a" || it == "armeabi" || it == "x86" }
        if (abis.isNotEmpty()) {
            if (hostIs64Bit && !has64)
                return "This app only ships 32-bit code. Install the 32-bit edition of CloneSpace (arm32 APK) to clone it."
            if (!hostIs64Bit && !has32)
                return "This app only ships 64-bit code. Use the 64-bit edition of CloneSpace (arm64 APK) to clone it."
        }
        return null
    }

    fun deviceInfo(): String =
        "Edition: ${if (hostIs64Bit) "64-bit" else "32-bit"} • Device ABIs: ${Build.SUPPORTED_ABIS.joinToString()}"
}
