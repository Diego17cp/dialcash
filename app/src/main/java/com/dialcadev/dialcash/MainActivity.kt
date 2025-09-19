package com.dialcadev.dialcash

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.size
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.dialcadev.dialcash.data.UserDataStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userDataStore: UserDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val isRegistered = userDataStore.isUserRegistered().first()
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
}