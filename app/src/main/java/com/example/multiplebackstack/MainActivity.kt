package com.example.multiplebackstack

import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import com.example.multiplebackstack.base.BaseActivity
import com.example.multiplebackstack.nav.Navigator
import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItem
import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItemsProvider
import com.example.multiplebackstack.nav.bottomnavigation.impl.BottomNavItemsProviderImpl
import com.example.multiplebackstack.nav.bottomnavigation.impl.BottomNavViewBuilder
import com.example.multiplebackstack.nav.bottomnavigation.impl.BottomNavViewCoordinator
import com.example.multiplebackstack.nav.destination.FeatureIdentifier
import com.example.multiplebackstack.nav.destination.NavDestination
import com.example.multiplebackstack.nav.impl.AppNavControllerImpl
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity(R.layout.activity_main), Navigator {

    private val bottomNavigationView by lazy { findViewById<BottomNavigationView>(R.id.bottom_nav) }

    private val bottomNavItemsProvider: BottomNavItemsProvider by lazy {
        BottomNavItemsProviderImpl()
    }

    private val appNavController by lazy {
        AppNavControllerImpl(
            activity = this,
            containerId = R.id.fragment_layout_container
        )
    }

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (appNavController.isHomeRoot()) {
                finish()
            }
        }
    }

    private val backInvokedCallback: OnBackInvokedCallback = OnBackInvokedCallback {
        if (appNavController.isHomeRoot()) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                backInvokedCallback
            )
        } else {
            onBackPressedDispatcher.addCallback(this, backPressedCallback)
        }

        // Setup items for the bottom navigation view
        setupBottomNavigationView()

        // Set up the root identifiers for the AppNavController
        setupNavController()

        // Set up bottom navigation view coordinator
        setupBottomNavViewCoordinator()

        if (savedInstanceState == null) {
            // Set the initial root identifier
            appNavController.navigate(
                FeatureIdentifier.ACCOUNTS.featureName,
                FeatureIdentifier.ACCOUNTS.symbolicDestination.navDestination,
                null
            )
        }
    }

    override fun navigate(rootIdentifier: String, navDestination: NavDestination, args: Bundle?) {
        appNavController.navigate(rootIdentifier, navDestination, args)
    }

    override fun navigate(navDestination: NavDestination, args: Bundle?) {
        appNavController.navigate(navDestination, args)
    }

    private fun setupBottomNavViewCoordinator() {
        BottomNavViewCoordinator(
            activity = this,
            containerId = R.id.fragment_layout_container,
            bottomNavigationView = bottomNavigationView,
            bottomNavItems = bottomNavItemsProvider.getBottomNavItems(),
            appNavController = appNavController,
            onBottomNavItemSelectedListener = object :
                BottomNavViewCoordinator.OnBottomNavItemSelectedListener {
                override fun onBottomNavItemSelected(navigableItem: BottomNavItem) {
                    // Handle bottom navigation item selection
                }
            }
        ).setup()
    }

    private fun setupNavController() {
        val rootIdentifiers =
            bottomNavItemsProvider.getBottomNavItems().map { it.featureTag }
        appNavController.setRootIdentifiers(rootIdentifiers)
    }

    private fun setupBottomNavigationView() {
        val bottomNavItems = bottomNavItemsProvider.getBottomNavItems()
        BottomNavViewBuilder(
            bottomNavigationView = bottomNavigationView,
            items = bottomNavItems
        ).build()
    }
}