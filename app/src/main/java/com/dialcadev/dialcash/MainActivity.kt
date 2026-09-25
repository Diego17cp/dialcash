package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.dialcadev.dialcash.core.datastore.UserDataStore
import com.dialcadev.dialcash.core.theme.AppTypography
import com.dialcadev.dialcash.core.ui.UiChromeViewModel
import com.dialcadev.dialcash.core.ui.components.AppBarAction
import com.dialcadev.dialcash.core.ui.components.FloatingBottomNav
import com.dialcadev.dialcash.core.ui.components.FragmentNavHost
import com.dialcadev.dialcash.core.ui.components.LiquidAppBar
import com.dialcadev.dialcash.core.updates.AppUpdater
import com.dialcadev.dialcash.core.updates.UpdateState
import com.dialcadev.dialcash.features.onboarding.presentation.ui.OnboardingActivity
import com.dialcadev.dialcash.features.register.presentation.ui.RegisterActivity
import com.dialcadev.dialcash.features.settings.presentation.ui.SettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
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

    private val currentDestinationId = mutableStateOf<Int?>(null)
    private val currentTitle = mutableStateOf("DialCash")
    private val canGoBack = mutableStateOf(false)
    private var navController: NavController? = null
    private val chromeViewModel: UiChromeViewModel by viewModels()
    private lateinit var navFragmentContainer: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        var isReady = false
        splashScreen.setKeepOnScreenCondition { !isReady }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            insets
        }

        setupFragmentContainer()
        setupNavigation()

        lifecycleScope.launch {
            val userPreferences = userDataStore.getUserData().first()
            val hasSeenOnboarding = userDataStore.isOnboardingSeen().first()

            if (!hasSeenOnboarding) {
                val intent = Intent(this@MainActivity, OnboardingActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return@launch
            }
            if (!userPreferences.isRegistered) {
                val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                return@launch
            }

            observeUpdates()
            isReady = true
        }
    }

    private fun setupFragmentContainer() {
        val root = findViewById<ViewGroup>(R.id.main)

        navFragmentContainer = FragmentContainerView(this).apply {
            id = R.id.fragment_nav_host_container
        }
        root.addView(
            navFragmentContainer,
            0,
            android.widget.LinearLayout.LayoutParams(0, 0)
        )

        val existing =
            supportFragmentManager.findFragmentById(R.id.fragment_nav_host_container) as? NavHostFragment
        val navHostFragment = existing ?: NavHostFragment.create(R.navigation.nav_graph).also {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_nav_host_container, it, "nav_host_fragment")
                .setPrimaryNavigationFragment(it)
                .commitNow()
        }
        navHostFragment.navController.addOnDestinationChangedListener { controller, destination, _ ->
            currentDestinationId.value = destination.id
            currentTitle.value = destination.label?.toString() ?: "DialCash"
            canGoBack.value = controller.previousBackStackEntry != null
        }
        this.navController = navHostFragment.navController
    }

    private fun setupNavigation() {
        val composeRoot = findViewById<ComposeView>(R.id.compose_content_root)
        val hazeState = chromeViewModel.hazeState
        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.transactionsFragment,
            R.id.accountsFragment,
            R.id.incomesFragment,
            R.id.blogFragment
        )
        val showBackButton = canGoBack.value && currentDestinationId.value !in topLevelDestinations
        composeRoot.setContent {
            MaterialTheme(
                typography = AppTypography
            ) {
                Box(Modifier.fillMaxSize()) {
                    FragmentNavHost(
                        modifier = Modifier.hazeSource(hazeState),
                        container = navFragmentContainer
                    )
                    val extraActions by chromeViewModel.extraActions.collectAsState()
                    LiquidAppBar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .onGloballyPositioned {
                                chromeViewModel.reportTopBarHeight(it.size.height)
                            },
                        hazeState = hazeState,
                        title = currentTitle.value,
                        onBackClick = if (showBackButton) {
                            { navController?.popBackStack() }
                        } else null,
                        actions = listOf(
                            AppBarAction(
                                iconRes = R.drawable.ic_settings_fill,
                                contentDescription = stringResource(R.string.settings),
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            SettingsActivity::class.java
                                        )
                                    )
                                })
                        ) + extraActions
                    )
                    FloatingBottomNav(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .zIndex(1f)
                            .onGloballyPositioned {
                                chromeViewModel.reportBottomBarHeight(it.size.height)
                            },
                        hazeState = hazeState,
                        currentDestinationId = currentDestinationId.value
                    ) { selectedItem ->
                        val nav = navController ?: return@FloatingBottomNav
                        if (nav.currentDestination?.id != selectedItem.routeId) {
                            val options = NavOptions.Builder()
                                .setPopUpTo(nav.graph.startDestinationId, false, saveState = true)
                                .setLaunchSingleTop(true)
                                .setRestoreState(true)
                                .build()
                            nav.navigate(resId = selectedItem.routeId, null, options)
                        }
                    }
                }
            }
        }
    }

    private fun observeUpdates() {
        appUpdater.checkForUpdates()
        lifecycleScope.launch {
            appUpdater.state.collect { state ->
                when (state) {
                    is UpdateState.UpdateAvailable -> {
                        val apkUrl =
                            state.release.assets.firstOrNull { it.name.endsWith(".apk") }?.browser_download_url
                        if (apkUrl != null) {
                            AlertDialog.Builder(this@MainActivity)
                                .setTitle(R.string.update_available)
                                .setMessage("${getString(R.string.new_version)} ${state.release.tag_name}\n\n${state.release.name}")
                                .setPositiveButton(R.string.update) { _, _ ->
                                    appUpdater.downloadUpdate(
                                        apkUrl
                                    )
                                }
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
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            state.message,
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        appUpdater.resetState()
                    }

                    else -> {}
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}