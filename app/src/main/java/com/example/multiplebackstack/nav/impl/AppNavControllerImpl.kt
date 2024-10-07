package com.example.multiplebackstack.nav.impl

import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.os.Bundle
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import com.example.multiplebackstack.nav.AppNavController
import com.example.multiplebackstack.nav.destination.FeatureIdentifier
import com.example.multiplebackstack.nav.destination.NavDestination
import com.example.multiplebackstack.nav.destination.NavDestination.*
import java.util.Stack

private const val HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE =
    "HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE"
private const val CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE =
    "CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE"

class AppNavControllerImpl(
    activity: FragmentActivity,
    private val containerId: Int
) : AppNavController {

    private val fragmentManager: FragmentManager = activity.supportFragmentManager

    private var homeRootDestinationIdentifier: String = HOME_ROOT_DESTINATION_IDENTIFIER_UNDEFINE
    private var currentRootDestinationIdentifier: String =
        CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE

    private var rootDestinationIdentifiers: List<String> = emptyList()

    private var listener: AppNavController.OnDestinationChangedListener? = null

    private val backStackMap = mutableMapOf<String, Stack<String>>()

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
        return fragmentManager.findFragmentById(containerId)?.tag == homeRootDestinationIdentifier
    }

    init {
        if (Build.VERSION.SDK_INT >= TIRAMISU) {
            activity.onBackInvokedDispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_OVERLAY) {
                // Handle the predictive back swipe gesture here
                if (isHomeRoot()) {
                    // do nothing
                } else {
                    handleBackStackChanged()
                }
            }
        } else {
            // Use the regular back press handling for older Android versions
            activity.onBackPressedDispatcher.addCallback(activity, backPressedCallback)
        }
    }

    override fun setRootIdentifiers(rootIdentifiers: List<String>) {
        this.rootDestinationIdentifiers = rootIdentifiers
        this.homeRootDestinationIdentifier = rootIdentifiers.first()
    }

    override fun navigate(
        rootIdentifier: String,
        navDestination: NavDestination,
        args: Bundle?,
    ) {
        if (navDestination !is FragmentDestination) {
            error("Unsupported destination type")
        }

        // Enable back press
        enableBackPress()

        // The first time do the navigation
        if (currentRootDestinationIdentifier == CURRENT_ROOT_DESTINATION_IDENTIFIER_UNDEFINE) {
            currentRootDestinationIdentifier = rootIdentifier
            fragmentManager.commit {
                setReorderingAllowed(true)
                replace(
                    /* containerViewId = */ containerId,
                    /* fragmentClass = */ navDestination.fragmentClass,
                    /* args = */ args,
                    /* tag = */ navDestination.identifier
                )

                // If direct navigation to root destination, set name for the back stack
                if (rootDestinationIdentifiers.contains(navDestination.identifier)) {
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
                if (navDestination.identifier == currentRootDestinationIdentifier) {
                    // do nothing
                } else {
                    // Assuming navigating to a nested destination, then replace the current fragment and add it to the back stack
                    fragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ navDestination.fragmentClass,
                            /* args = */ args,
                            /* tag = */ navDestination.identifier
                        )
                        addToBackStack(null)
                    }
                }
            } else { // Different root, then switch to the new root
                // Save the current tab's back stack before switching to a new one
                fragmentManager.saveBackStack(currentRootDestinationIdentifier)

                // Set the new current tab
                currentRootDestinationIdentifier = rootIdentifier

                // If the new root exists in the back stack, restore it
                if (backStackMap.contains(rootIdentifier)) {
                    fragmentManager.restoreBackStack(rootIdentifier)

                    if (rootDestinationIdentifiers.contains(navDestination.identifier)) {
                        // do nothing
                    } else {
                        // Assuming navigate to nested destination
                        fragmentManager.commit {
                            setReorderingAllowed(true)
                            replace(
                                /* containerViewId = */ containerId,
                                /* fragmentClass = */ navDestination.fragmentClass,
                                /* args = */ args,
                                /* tag = */ navDestination.identifier
                            )
                            addToBackStack(null)
                        }
                    }

                } else { // Haven't root
                    fragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ navDestination.fragmentClass,
                            /* args = */ args,
                            /* tag = */ navDestination.identifier
                        )

                        // If direct navigation to root destination, set name for the back stack
                        if (rootDestinationIdentifiers.contains(navDestination.identifier)) {
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
        listener?.onDestinationChanged(rootIdentifier, navDestination)
    }

    override fun navigate(
        navDestination: NavDestination,
        args: Bundle?,
    ) {
        navigate(currentRootDestinationIdentifier, navDestination, args)
    }

    override fun setOnDestinationChangedListener(listener: AppNavController.OnDestinationChangedListener) {
        this.listener = listener
    }

    private fun enableBackPress() {
        backPressedCallback.isEnabled = true
    }

    private fun handleBackStackChanged() {
        // Assuming the current root is not the home root
        val currentDestinationIdentifier = fragmentManager.findFragmentById(containerId)?.tag
            ?: error("Current destination is null")

        // If current destination is the root destination
        if (rootDestinationIdentifiers.contains(currentDestinationIdentifier)) {
            if (currentRootDestinationIdentifier == homeRootDestinationIdentifier) {
                // Home root destination, do nothing
            } else {
                // Navigate to the home root destination
                val homeRootDestination =
                    FeatureIdentifier.fromFeatureName(homeRootDestinationIdentifier)?.symbolicDestination?.navDestination
                if (homeRootDestination != null && homeRootDestination is FragmentDestination) {
                    navigate(homeRootDestinationIdentifier, homeRootDestination)
                } else {
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
                val currentRootDestination =
                    FeatureIdentifier.fromFeatureName(currentRootDestinationIdentifier)?.symbolicDestination?.navDestination
                if (currentRootDestination != null && currentRootDestination is FragmentDestination) {
                    fragmentManager.commit {
                        setReorderingAllowed(true)
                        replace(
                            /* containerViewId = */ containerId,
                            /* fragmentClass = */ currentRootDestination.fragmentClass,
                            /* args = */ null,
                            /* tag = */ currentRootDestination.identifier
                        )

                        // If direct navigation to root destination, set name for the back stack
                        addToBackStack(currentRootDestinationIdentifier)

                        // Keep track of the back stack for the root destination
                        backStackMap[currentRootDestinationIdentifier] = Stack()

                        listener?.onDestinationChanged(
                            currentRootDestinationIdentifier,
                            currentRootDestination
                        )
                    }
                } else {
                    error("Current root destination is null")
                }
            }
        }
    }
}