package com.example.multiplebackstack

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.example.multiplebackstack.base.BaseActivity
import com.example.multiplebackstack.bottomnavigation.BottomNavigator
import com.example.multiplebackstack.bottomnavigation.destination.FeatureIdentifier
import com.example.multiplebackstack.bottomnavigation.impl.BottomNavViewBuilder
import com.example.multiplebackstack.bottomnavigation.impl.BottomNavViewCoordinator
import com.example.multiplebackstack.bottomnavigation.impl.BottomNavigationManagerImplV6
import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomMenuProvider
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem
import com.example.multiplebackstack.bottomnavigation.menu.impl.BottomMenuProviderImpl
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : BaseActivity(R.layout.activity_main), BottomNavigator {

    private val bottomNavigationView by lazy { findViewById<BottomNavigationView>(R.id.bottom_nav) }

    private val bottomMenuProvider: BottomMenuProvider by lazy {
        BottomMenuProviderImpl()
    }

    private val bottomNavigationManager by lazy {
        BottomNavigationManagerImplV6(
            activity = this,
            containerId = R.id.fragment_layout_container
        )
    }

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (bottomNavigationManager.canNavigateUp()) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        val bottomItemGroups = bottomMenuProvider.getBottomNavItems()

        // Setup items for the bottom navigation view
        setupBottomNavigationView(bottomItemGroups)

        // Set up the root identifiers for the AppNavController
        setupNavController(bottomItemGroups)

        // Set up bottom navigation view coordinator
        setupBottomNavViewCoordinator()

        if (savedInstanceState == null) {
            // Set the initial root identifier
            bottomNavigationManager.navigate(FeatureIdentifier.CARD_DETAILS.featureName)
        }
    }

    override fun switchRoot(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        bottomNavigationManager.switchRoot(bottomNavigableItem, args)
    }

    override fun navigate(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        bottomNavigationManager.navigate(bottomNavigableItem, args)
    }

    override fun navigate(identifier: String, args: Bundle?) {
        bottomNavigationManager.navigate(identifier, args)
    }

    private fun setupBottomNavViewCoordinator() {
        BottomNavViewCoordinator(
            activity = this,
            containerId = R.id.fragment_layout_container,
            bottomNavigationView = bottomNavigationView,
            bottomItemGroups = bottomMenuProvider.getBottomNavItems(),
            bottomNavigationManager = bottomNavigationManager,
            onBottomNavItemSelectedListener = object :
                BottomNavViewCoordinator.OnBottomNavItemSelectedListener {
                override fun onBottomNavItemSelected(navigableItem: BottomItemGroup) {
                    // Handle bottom navigation item selection
                }
            }
        ).setup()
    }

    private fun setupNavController(bottomItemGroups: List<BottomItemGroup>) {
        bottomNavigationManager.presetBottomItemGroups(bottomItemGroups)
    }

    private fun setupBottomNavigationView(bottomItemGroups: List<BottomItemGroup>) {
        BottomNavViewBuilder(
            bottomNavigationView = bottomNavigationView,
            items = bottomItemGroups
        ).build()
    }
}