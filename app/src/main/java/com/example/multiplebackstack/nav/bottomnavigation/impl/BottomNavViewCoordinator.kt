package com.example.multiplebackstack.nav.bottomnavigation.impl

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.example.multiplebackstack.base.ScrollableFragment
import com.example.multiplebackstack.nav.AppNavController
import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItem
import com.example.multiplebackstack.nav.destination.FeatureIdentifier
import com.example.multiplebackstack.nav.destination.NavDestination
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

class BottomNavViewCoordinator(
    private val activity: FragmentActivity,
    private val containerId: Int,
    private val bottomNavigationView: BottomNavigationView,
    private val bottomNavItems: List<BottomNavItem>,
    private val appNavController: AppNavController,
    private val onBottomNavItemSelectedListener: OnBottomNavItemSelectedListener
) {
    private val fragmentManager: FragmentManager = activity.supportFragmentManager

    private val onItemSelectedListener = NavigationBarView.OnItemSelectedListener { item ->
        val navigableItem = bottomNavItems.firstOrNull { it.id == item.itemId }
        if (navigableItem != null) {
            val navDestination =
                FeatureIdentifier.fromFeatureName(navigableItem.featureTag)?.symbolicDestination?.navDestination
            if (navDestination != null) {
                val dispatcher = dispatcherDestination(navDestination)

                // dispatcher == true => tab selected
                if (dispatcher) {
                    onBottomNavItemSelectedListener.onBottomNavItemSelected(navigableItem)
                }

                dispatcher
            } else {
                false
            }
        } else {
            false
        }
    }

    fun setup() {
        bottomNavigationView.setOnItemSelectedListener(onItemSelectedListener)

        bottomNavigationView.setOnItemReselectedListener {
            val scrollableFragment =
                fragmentManager.findFragmentById(containerId) as? ScrollableFragment
            scrollableFragment?.scrollToTop()
        }

        appNavController.setOnDestinationChangedListener(object :
            AppNavController.OnDestinationChangedListener {
            override fun onDestinationChanged(
                rootIdentifier: String?,
                navDestination: NavDestination
            ) {
                val currentMenuItemSelected = bottomNavigationView.selectedItemId
                val newMenuItemSelected =
                    bottomNavItems.find { it.featureTag == rootIdentifier }?.id

                if (newMenuItemSelected != null && currentMenuItemSelected != newMenuItemSelected) {
                    bottomNavigationView.setOnItemSelectedListener(null)
                    bottomNavigationView.selectedItemId = newMenuItemSelected
                    bottomNavigationView.setOnItemSelectedListener(onItemSelectedListener)
                }
            }
        })
    }

    private fun dispatcherDestination(navDestination: NavDestination): Boolean {
        return when (navDestination) {
            is NavDestination.FragmentDestination -> {
                appNavController.navigate(
                    rootIdentifier = navDestination.identifier,
                    navDestination = navDestination,
                )
                true
            }

            is NavDestination.ActivityDestination -> {
                startActivity(navDestination)
                false
            }
        }
    }

    private fun startActivity(navDestination: NavDestination.ActivityDestination) {
        val intent = Intent(activity, navDestination.targetActivity)
        activity.startActivity(intent)
    }

    interface OnBottomNavItemSelectedListener {
        fun onBottomNavItemSelected(navigableItem: BottomNavItem)
    }
}