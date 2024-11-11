package com.example.multiplebackstack.bottomnavigation.impl

import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.multiplebackstack.base.ScrollableFragment
import com.example.multiplebackstack.bottomnavigation.BottomNavigationManager
import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import java.lang.ref.WeakReference

class BottomNavViewCoordinator(
    activity: FragmentActivity,
    private val containerId: Int,
    bottomNavigationView: BottomNavigationView,
    bottomNavigationManager: BottomNavigationManager,
    private val bottomItemGroups: List<BottomItemGroup>,
    private val onBottomNavItemSelectedListener: OnBottomNavItemSelectedListener?
) : DefaultLifecycleObserver {

    private val activityWeakReference = WeakReference(activity)
    private val bottomNavigationViewWeakReference = WeakReference(bottomNavigationView)
    private val bottomNavigationManagerWeakReference = WeakReference(bottomNavigationManager)

    private val fragmentManager: FragmentManager? =
        activityWeakReference.get()?.supportFragmentManager

    init {
        activityWeakReference.get()?.lifecycle?.addObserver(this)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        bottomNavigationViewWeakReference.get()?.setOnItemSelectedListener(null)
        bottomNavigationViewWeakReference.get()?.setOnItemReselectedListener(null)
        bottomNavigationManagerWeakReference.get()?.setOnDestinationChangedListener(null)
    }

    private val onItemSelectedListener = NavigationBarView.OnItemSelectedListener { item ->
        val bottomNavigableItem = bottomItemGroups.firstOrNull { it.id == item.itemId }
        if (bottomNavigableItem != null) {
            val dispatcher = dispatcherDestination(bottomNavigableItem)

            // dispatcher == true => tab selected
            if (dispatcher) {
                onBottomNavItemSelectedListener?.onBottomNavItemSelected(bottomNavigableItem)
            }
            dispatcher
        } else {
            false
        }
    }

    fun setup() {
        bottomNavigationViewWeakReference.get()?.setOnItemSelectedListener(onItemSelectedListener)

        bottomNavigationViewWeakReference.get()?.setOnItemReselectedListener {
            val scrollableFragment =
                fragmentManager?.findFragmentById(containerId) as? ScrollableFragment
            scrollableFragment?.scrollToTop()
        }

        bottomNavigationManagerWeakReference.get()?.setOnDestinationChangedListener(object :
            BottomNavigationManager.OnDestinationChangedListener {
            override fun onDestinationChanged(bottomNavigableItem: BottomNavigableItem) {
                val currentRootBottomMenuItemSelected =
                    bottomNavigationViewWeakReference.get()?.selectedItemId
                val newRootBottomMenuItemSelected =
                    bottomItemGroups.find {
                        it.featureTag == bottomNavigableItem.featureTag ||
                                it.children.any { it.featureTag == bottomNavigableItem.featureTag }
                    }?.id

                if (newRootBottomMenuItemSelected != null && currentRootBottomMenuItemSelected != newRootBottomMenuItemSelected) {
                    bottomNavigationViewWeakReference.get()?.setOnItemSelectedListener(null)
                    bottomNavigationViewWeakReference.get()?.selectedItemId =
                        newRootBottomMenuItemSelected
                    bottomNavigationViewWeakReference.get()
                        ?.setOnItemSelectedListener(onItemSelectedListener)
                }
            }
        })
    }

    private fun dispatcherDestination(bottomNavigableItem: BottomNavigableItem): Boolean {
        bottomNavigationManagerWeakReference.get()?.switchRoot(bottomNavigableItem)
        return bottomNavigableItem.fragmentClass != null
    }

    interface OnBottomNavItemSelectedListener {
        fun onBottomNavItemSelected(navigableItem: BottomItemGroup)
    }
}