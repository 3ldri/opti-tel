package com.optimphone.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.text.format.Formatter
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private data class AppInfo(
        val label: String,
        val packageName: String,
        val isSystem: Boolean,
        val icon: Drawable?,
        val launchIntent: Intent?
    )

    private val apps = mutableListOf<AppInfo>()
    private lateinit var listAdapter: BaseAdapter

    private val uninstallLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshApps()
            refreshStorage()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listAdapter = object : BaseAdapter() {
            override fun getCount() = apps.size
            override fun getItem(position: Int) = apps[position]
            override fun getItemId(position: Int) = position.toLong()

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.item_app, parent, false)
                val app = apps[position]
                view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(app.icon)
                view.findViewById<TextView>(R.id.appName).text = app.label
                view.findViewById<TextView>(R.id.appPackage).text =
                    if (app.isSystem) "${app.packageName} • système" else app.packageName
                view.findViewById<MaterialButton>(R.id.btnOpen).visibility =
                    if (app.launchIntent != null) View.VISIBLE else View.GONE
                view.findViewById<MaterialButton>(R.id.btnOpen).setOnClickListener {
                    startActivity(app.launchIntent)
                }
                view.findViewById<MaterialButton>(R.id.btnDisable).setOnClickListener {
                    openAppDetails(app.packageName)
                }
                view.findViewById<MaterialButton>(R.id.btnUninstall).setOnClickListener {
                    requestUninstall(app.packageName)
                }
                return view
            }
        }

        findViewById<ListView>(R.id.appList).adapter = listAdapter

        findViewById<MaterialButton>(R.id.btnStorage).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.btnBattery).setOnClickListener {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        findViewById<MaterialButton>(R.id.btnNotifications).setOnClickListener {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        findViewById<MaterialButton>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<MaterialButton>(R.id.btnAi).setOnClickListener {
            startActivity(Intent(this, AiActivity::class.java))
        }

        refreshStorage()
        refreshApps()
    }

    private fun refreshStorage() {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        val used = total - free
        val percent = if (total > 0) (used * 100 / total).toInt() else 0
        findViewById<TextView>(R.id.storageInfo).text = getString(
            R.string.storage_used,
            Formatter.formatFileSize(this, used),
            Formatter.formatFileSize(this, total)
        )
        findViewById<ProgressBar>(R.id.storageBar).progress = percent

        val mem = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        mem.getMemoryInfo(memInfo)
        val ramUsed = memInfo.totalMem - memInfo.availMem
        findViewById<TextView>(R.id.ramInfo).text = getString(
            R.string.ram_used,
            Formatter.formatFileSize(this, ramUsed),
            Formatter.formatFileSize(this, memInfo.totalMem)
        )

        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        findViewById<TextView>(R.id.batteryInfo).text =
            getString(R.string.battery_level, if (battery > 0) battery else -1)
    }

    private fun refreshApps() {
        val pm = packageManager
        val flags = PackageManager.GET_ACTIVITIES
        val launchables = mutableSetOf<String>()
        pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).forEach { launchables.add(it.activityInfo.packageName) }

        val loaded = pm.getInstalledPackages(flags).mapNotNull { info ->
            val appInfo: ApplicationInfo = info.applicationInfo ?: return@mapNotNull null
            AppInfo(
                label = pm.getApplicationLabel(appInfo).toString(),
                packageName = appInfo.packageName,
                isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                icon = pm.getApplicationIcon(appInfo),
                launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            )
        }

        apps.clear()
        apps.addAll(loaded.sortedWith(
            compareBy({ it.isSystem }, { it.label.lowercase() })
        ))
        listAdapter.notifyDataSetChanged()
    }

    private fun openAppDetails(packageName: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", packageName, null)))
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUninstall(packageName: String) {
        try {
            uninstallLauncher.launch(
                Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null))
            )
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
    }
}
