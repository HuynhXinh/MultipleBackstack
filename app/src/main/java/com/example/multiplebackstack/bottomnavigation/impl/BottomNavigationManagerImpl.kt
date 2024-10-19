package com.example.multiplebackstack.bottomnavigation.impl

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.multiplebackstack.bottomnavigation.BottomNavigationManager
import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem
import java.lang.ref.WeakReference
import java.util.Stack

private const val HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE =
    "HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE"
private const val CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE =
    "CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE"

class BottomNavigationManagerImpl(
    activity: FragmentActivity,
    private val containerId: Int
) : BottomNavigationManager, DefaultLifecycleObserver {

    private val activityWeakReference = WeakReference(activity)
    private val fragmentManager: FragmentManager? =
        activityWeakReference.get()?.supportFragmentManager

    private var homeRootDestinationIdentifier: String = HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE
    private var currentRootDestinationIdentifier: String =
        CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE

    private var rootBottomItemGroups: List<BottomItemGroup> = emptyList()
    private var homeRootBottomItemGroup: BottomItemGroup? = null

    private var listener: BottomNavigationManager.OnDestinationChangedListener? = null

    private val backStackMap = mutableMapOf<String, Stack<BottomNavigableItem>>()

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isHomeRoot()) {
                isEnabled = false
            } else {
                handleBackStackChanged()
            }
        }
    }

    override fun isHomeRoot(): Boolean {
        return fragmentManager?.findFragmentById(containerId)?.tag == homeRootDestinationIdentifier
    }

    init {
        // Use the regular back press handling for older Android versions
        activityWeakReference.get()?.apply {
            onBackPressedDispatcher.addCallback(this, backPressedCallback)
        }
    }

    override fun setRootBottomItemGroups(rootBottomItemGroups: List<BottomItemGroup>) {
        this.rootBottomItemGroups = rootBottomItemGroups
        this.homeRootBottomItemGroup = rootBottomItemGroups.first()
        this.homeRootDestinationIdentifier = rootBottomItemGroups.first().featureTag
    }

    override fun navigate(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        if (isFragmentDestination(bottomNavigableItem)) {
            navigateToFragment(bottomNavigableItem, args)
        } else if (isActivityDestination(bottomNavigableItem)) {
            navigateToActivity(bottomNavigableItem, args)
        } else {
            error("Unknown destination type")
        }
    }

    override fun navigate(identifier: String, args: Bundle?) {
        lookupBottomNavigableItem(identifier)?.let { bottomNavigableItem ->
            navigate(bottomNavigableItem, args)
        }
    }

    private fun lookupBottomNavigableItem(identifier: String): BottomNavigableItem? {
        return rootBottomItemGroups.find { it.featureTag == identifier } ?:
                rootBottomItemGroups.flatMap { it.children }.find { it.featureTag == identifier }
    }

    override fun setOnDestinationChangedListener(listener: BottomNavigationManager.OnDestinationChangedListener?) {
        this.listener = listener
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // clear all things possible
        backPressedCallback.remove()
        listener = null
        backStackMap.clear()
        rootBottomItemGroups = emptyList()
        homeRootDestinationIdentifier = HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE
        currentRootDestinationIdentifier = CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE
    }

    private fun navigateToActivity(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        activityWeakReference.get()?.let { activity ->
            val intent = Intent(activity, bottomNavigableItem.activityClass).apply {
                args?.let { putExtras(it) }
            }
            activity.startActivity(intent)
        }
    }

    private fun navigateToFragment(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // Enable back press
        enableBackPress()

        val rootIdentifier = lookupRootIdentifier(bottomNavigableItem)?.featureTag
            ?: error("Root identifier is null")
        val fragmentClass = bottomNavigableItem.fragmentClass ?: error("Fragment class is null")
        val identifier = bottomNavigableItem.featureTag

        // The first time do the navigation
        if (currentRootDestinationIdentifier == CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE) {
            currentRootDestinationIdentifier = rootIdentifier
            fragmentManager?.commit {
                setReorderingAllowed(true)
                replace(
                    /* containerViewId = */ containerId,
                    /* fragmentClass = */ fragmentClass,
                    /* args = */ args,
                    /* tag = */ identifier
                )

                // If direct navigation to root destination, set name for the back stack
                if (isRooDestination(identifier)) {
                    addToBackStack(rootIdentifier)

                    // Keep track of the back stack for the root destination
                    backStackMap[rootIdentifier] = Stack()
                } else {
                    // Case when navigating to a nested destination
                    // Then when pressing back, it should pop the back stack
                    addToBackStack(null)
                }
            }
        } else { // The seconds time do navigation
            // The same root
            if (rootIdentifier == currentRootDestinationIdentifier) {
                // If the current root is the same as the new root, then do nothing
                if (identifier == currentRootDestinationIdentifier) {
                    // do nothing
                } else {
                    // Assuming navigating to a nested destination, then replace the current fragment and add it to the back stack
                    fragmentManager?.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ fragmentClass,
                            /* args = */ args,
                            /* tag = */ identifier
                        )
                        addToBackStack(null)
                    }
                }
            } else { // Different root, then switch to the new root
                // Save the current tab's back stack before switching to a new one
                fragmentManager?.saveBackStack(currentRootDestinationIdentifier)

                // Set the new current tab
                currentRootDestinationIdentifier = rootIdentifier

                // If the new root exists in the back stack, restore it
                if (backStackMap.contains(rootIdentifier)) {
                    fragmentManager?.restoreBackStack(rootIdentifier)

                    if (isRooDestination(identifier)) {
                        // do nothing
                    } else {
                        // Assuming navigate to nested destination
                        fragmentManager?.commit {
                            setReorderingAllowed(true)
                            replace(
                                /* containerViewId = */ containerId,
                                /* fragmentClass = */ fragmentClass,
                                /* args = */ args,
                                /* tag = */ identifier
                            )
                            addToBackStack(null)
                        }
                    }

                } else { // Haven't root
                    fragmentManager?.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ fragmentClass,
                            /* args = */ args,
                            /* tag = */ identifier
                        )

                        // If direct navigation to root destination, set name for the back stack
                        if (isRooDestination(identifier)) {
                            addToBackStack(rootIdentifier)

                            // Keep track of the back stack for the root destination
                            backStackMap[rootIdentifier] = Stack()
                        } else {
                            // Case when navigating to a nested destination
                            // Then when pressing back, it should pop the back stack
                            addToBackStack(null)
                        }
                    }
                }
            }
        }

        // Notify the listener if needed
        listener?.onDestinationChanged(bottomNavigableItem)
    }

    private fun lookupRootIdentifier(bottomNavigableItem: BottomNavigableItem): BottomNavigableItem? {
        return rootBottomItemGroups.find { rootBottomItemGroup ->
            rootBottomItemGroup.featureTag == bottomNavigableItem.featureTag ||
                    rootBottomItemGroup.children.any { child -> child.featureTag == bottomNavigableItem.featureTag }
        }
    }

    private fun isActivityDestination(bottomNavigableItem: BottomNavigableItem): Boolean {
        return bottomNavigableItem.activityClass != null
    }

    private fun isFragmentDestination(bottomNavigableItem: BottomNavigableItem): Boolean {
        return bottomNavigableItem.fragmentClass != null
    }

    private fun enableBackPress() {
        backPressedCallback.isEnabled = true
    }

    private fun handleBackStackChanged() {
        // Get the current root is not the home root
        val currentDestinationIdentifier = fragmentManager?.findFragmentById(containerId)?.tag
            ?: error("Current destination is null")

        // If current destination is the root destination
        if (isRooDestination(currentDestinationIdentifier)) {
            if (currentRootDestinationIdentifier == homeRootDestinationIdentifier) {
                // Home root destination, do nothing
            } else {
                // Navigate to the home root destination
                homeRootBottomItemGroup?.let {
                    navigate(it)
                } ?: {
                    error("Home root destination is null")
                }
            }
        } else { // Assuming current destination is a nested destination
            var backStackCount = fragmentManager.backStackEntryCount
            fragmentManager.popBackStack()
            backStackCount--

            // Assuming the back stack is empty and the root destination is not in the back stack
            // then navigate to the root destination
            if (backStackCount == 0 && !backStackMap.contains(currentRootDestinationIdentifier)) {
                val currentRootDestination = lookupRootDestination(currentRootDestinationIdentifier)

                if (currentRootDestination?.fragmentClass != null) {
                    fragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ currentRootDestination.fragmentClass,
                            /* args = */ null,
                            /* tag = */ currentRootDestination.featureTag
                        )

                        // If direct navigation to root destination, set name for the back stack
                        addToBackStack(currentRootDestinationIdentifier)

                        // Keep track of the back stack for the root destination
                        backStackMap[currentRootDestinationIdentifier] = Stack()

                        listener?.onDestinationChanged(currentRootDestination)
                    }
                } else {
                    error("Current root destination is null")
                }
            }
        }
    }

    private fun lookupRootDestination(identifier: String): BottomNavigableItem? {
        return rootBottomItemGroups.find { it.featureTag == identifier }
    }

    private fun isRooDestination(identifier: String): Boolean {
        return rootBottomItemGroups.any { it.featureTag == identifier }
    }
}