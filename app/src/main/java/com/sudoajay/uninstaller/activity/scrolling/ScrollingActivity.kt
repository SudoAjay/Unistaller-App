package com.sudoajay.uninstaller.activity.scrolling

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.sudoajay.uninstaller.R
import com.sudoajay.uninstaller.activity.BaseActivity
import com.sudoajay.uninstaller.activity.main.PagingAppRecyclerAdapter
import com.sudoajay.uninstaller.activity.main.database.App
import com.sudoajay.uninstaller.activity.main.database.AppDao
import com.sudoajay.uninstaller.activity.main.database.AppRepository
import com.sudoajay.uninstaller.activity.main.database.AppRoomDatabase
import com.sudoajay.uninstaller.databinding.ActivityScrollingBinding
import com.sudoajay.uninstaller.helper.FileSize
import kotlinx.android.synthetic.main.content_scrolling.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class ScrollingActivity : BaseActivity() {
    private lateinit var binding: ActivityScrollingBinding
    lateinit var app: App
    private var isDarkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isDarkTheme = isDarkMode(applicationContext)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_scrolling)
        binding.activity = this
        changeStatusBarColor()

        val appDao: AppDao =
            AppRoomDatabase.getDatabase(applicationContext).appDao()
        val appRepository = AppRepository(applicationContext, appDao)
        if (!intent.action.isNullOrEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                app = appRepository.getAppFromId(intent.action.toString().toInt())

            }
        }


    }

    override fun onResume() {
        super.onResume()


        setSupportActionBar(binding.toolbar)
        binding.openAppFloating.setOnClickListener { openApp() }

        binding.appImageImageView.setImageDrawable(
            PagingAppRecyclerAdapter.getApplicationsIcon(
                app.icon,
                applicationContext.packageManager
            )
        )

        binding.toolbar.setNavigationIcon(R.drawable.ic_back)

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        passTheValueInTextView()
    }


    private fun passTheValueInTextView() {
        val info = packageManager.getPackageInfo(app.packageName, 0)

        val sdf = SimpleDateFormat(" h:mm a , d MMM yyyy ", Locale.getDefault())

        versionNameInfo_TextView.text = info.versionName
        versionCodeInfo_TextView.text = info.versionCode.toString()
        firstInstallInfo_TextView.text = sdf.format(info.firstInstallTime)
        lastUpdateInfo_TextView.text = sdf.format(info.lastUpdateTime)
        packageNameInfo_TextView.text = info.packageName
        apkPathInfo_TextView.text = info.applicationInfo.sourceDir
        dataPathInfo_TextView.text = info.applicationInfo.dataDir
        apkSizeInfo_TextView.text = FileSize.convertIt(app.size)

        minSdkInfo_TextView.text =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) info.applicationInfo.minSdkVersion.toString() else getString(
                R.string.unspecified_text
            )

        targetSdkInfo_TextView.text = info.applicationInfo.targetSdkVersion.toString()
        installerInfo_TextView.text = info.applicationInfo.processName

        val permission = packageManager.getPackageInfo(
            app.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requestedPermissions = permission.requestedPermissions
        val builder = StringBuilder()
        if (!requestedPermissions.isNullOrEmpty())
            for (per in requestedPermissions) {
                builder.append(per).append("\n")
            }


        permissionInfo_TextView.text = builder.toString()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_scrolling, menu)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_applicationSetting ->
                openAppSetting()

            R.id.action_openAppInGooglePlayStore -> openAppInStore()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun openApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
        launchIntent?.let { startActivity(it) }
    }

    private fun openAppSetting() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${app.packageName}")
        startActivity(intent)
    }

    private fun openAppInStore() {
        val appPackageName =
            app.packageName // getPackageName() from Context or Activity object

        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$appPackageName")
                )
            )
        } catch (anfe: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")
                )
            )
        }
    }

    /**
     * Making notification bar transparent
     */
    private fun changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isDarkTheme) {
                val window = window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                window.statusBarColor = Color.TRANSPARENT
            }
        }
    }
}