package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.updates.AppUpdater
import com.dialcadev.dialcash.core.updates.UpdateState
import com.dialcadev.dialcash.features.onboarding.presentation.ui.OnboardingActivity
import com.dialcadev.dialcash.features.register.presentation.ui.RegisterActivity
import com.dialcadev.dialcash.features.settings.presentation.ui.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userDataStore: UserDataStore
    @Inject
    lateinit var appUpdater: AppUpdater
    private var progressDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val userPreferences = userDataStore.getUserData().first()
            AppCompatDelegate.setDefaultNightMode(userPreferences.themeMode)
            val isRegistered = userPreferences.isRegistered
            val hasSeenOnboarding = userDataStore.isOnboardingSeen().first()
            if (!hasSeenOnboarding) {
                val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return@launch
            }
            if (!isRegistered) {
                val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return@launch
            }


            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)

            setContentView(R.layout.activity_main)

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
                insets
            }

            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            addMenuProvider(object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.main_menu, menu)
                }

                override fun onPrepareMenu(menu: Menu) {
                    val typedValue = TypedValue()
                    val attRes = com.google.android.material.R.attr.colorOnSurface
                    if (theme.resolveAttribute(attRes, typedValue, true)) {
                        val color = typedValue.data
                        for (i in 0 until menu.size) {
                            menu.getItem(i).icon?.mutate()?.setTint(color)
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_settings -> {
                            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                            startActivity(intent)
                            true
                        }

                        else -> false
                    }
                }
            }, this@MainActivity, Lifecycle.State.RESUMED)

            setupNavigation()
            handleBottomNavigationInsets()
            observeUpdates()
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = destination.label
        }
    }
    private fun observeUpdates() {
        appUpdater.checkForUpdates()
        lifecycleScope.launch {
            appUpdater.state.collect { state ->
                when (state) {
                    is UpdateState.UpdateAvailable -> {
                        val apkUrl = state.release.assets.firstOrNull { it.name.endsWith(".apk") }?.browser_download_url
                        if (apkUrl != null) {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle(R.string.update_available)
                                .setMessage("${getString(R.string.new_version)} ${state.release.tag_name}\n\n${state.release.name}")
                                .setPositiveButton(R.string.update) { _, _ -> appUpdater.downloadUpdate(apkUrl) }
                                .setNegativeButton(R.string.later) { _, _ -> appUpdater.resetState() }
                                .show()
                        }
                    }
                    is UpdateState.Downloading -> {
                        if (progressDialog == null) {
                            progressDialog = AlertDialog.Builder(this@MainActivity)
                                .setTitle(R.string.downloading_update)
                                .setMessage("0%")
                                .setCancelable(false)
                                .create()
                            progressDialog?.show()
                        }
                        progressDialog?.setMessage("${state.progress}%")
                    }
                    is UpdateState.ReadyToInstall -> {
                        progressDialog?.dismiss()
                        progressDialog = null

                        AlertDialog.Builder(this@MainActivity)
                            .setTitle(R.string.download_complete)
                            .setMessage(R.string.install_update_message)
                            .setPositiveButton(R.string.install) { _, _ ->
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    this@MainActivity,
                                    "${packageName}.provider",
                                    state.apkFile
                                )
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                startActivity(intent)
                                appUpdater.resetState()
                            }
                            .setNegativeButton(R.string.later) { _, _ -> appUpdater.resetState() }
                            .show()
                    }
                    is UpdateState.Error -> {
                        progressDialog?.dismiss()
                        progressDialog = null
                        android.widget.Toast.makeText(this@MainActivity, state.message, android.widget.Toast.LENGTH_LONG).show()
                        appUpdater.resetState()
                    }
                    else -> {}
                }
            }
        }
    }
    private fun handleBottomNavigationInsets() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                0
            )

            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.bottomMargin = navigationBars.bottom
            view.layoutParams = layoutParams

            insets
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}