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

class BottomNavigationManagerImplV3(
    activity: FragmentActivity,
    private val containerId: Int
) : BottomNavigationManager, DefaultLifecycleObserver {

    private val activityWeakReference = WeakReference(activity)
    private val fragmentManager: FragmentManager? =
        activityWeakReference.get()?.supportFragmentManager

    private var presetBottomItemGroups: List<BottomItemGroup> = emptyList()
    private lateinit var homeBottomItemGroup: BottomItemGroup
    private lateinit var currentBottomNavigableItem: BottomNavigableItem

    private var listener: BottomNavigationManager.OnDestinationChangedListener? = null

    private val rootTabInitial = mutableListOf<String>()

    /**
     *  Issue: Can NOT get correct current fragment tag after restored backstack
     *
     *  Give home Accounts -> tag: Accounts
     *  And current fragment is Cards list -> tag: Cards
     *  When save current backstack: fragmentManager?.saveBackStack(Cards)
     *  Then restore backstack: fragmentManager?.restoreBackStack(Accounts)
     *  The current fragment tag is still Cards - the expectation is Accounts
     *
     *  => So, we have to custom a map of last destination for case save and restore backstack
     */
    private val lastDestinationOfRoot = mutableMapOf<String, BottomNavigableItem>()

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateUp()
        }
    }

    override fun canNavigateUp(): Boolean {
        return fragmentManager?.findFragmentById(containerId)?.tag != homeBottomItemGroup.featureTag
    }

    init {
        // Use the regular back press handling for older Android versions
        activityWeakReference.get()?.apply {
            onBackPressedDispatcher.addCallback(this, backPressedCallback)
        }
    }

    override fun presetBottomItemGroups(bottomItemGroups: List<BottomItemGroup>) {
        this.presetBottomItemGroups = bottomItemGroups
        this.homeBottomItemGroup = bottomItemGroups.first()
        this.currentBottomNavigableItem = homeBottomItemGroup
        this.lastDestinationOfRoot[homeBottomItemGroup.featureTag] = currentBottomNavigableItem

        // init empty backstack for each root
        this.presetBottomItemGroups.forEach { bottomItemGroup ->
            this.fragmentManager?.saveBackStack(bottomItemGroup.featureTag)
        }
    }

    /**
     *  Save current backstack
     *  Save last destination of root
     *
     *  Restore backstack of new root
     *  If root of bottomNavigableItem is not exist && there is no nested destination on top
     *      then add root to backstack
     *      add root to rootTabInitial
     *      reset current destination
     *      save last destination of root
     *  Else
     *      Restore last destination of root and set current destination to it
     *
     *  Trigger callback destination changed
     *
     */
    override fun switchRoot(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // Save current backstack
        val rootOfCurrentDestinationIdentifier =
            lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag).featureTag
        fragmentManager?.saveBackStack(rootOfCurrentDestinationIdentifier)
        // Save last destination of root
        lastDestinationOfRoot[rootOfCurrentDestinationIdentifier] = currentBottomNavigableItem

        val identifier = bottomNavigableItem.featureTag
        val fragmentClass = bottomNavigableItem.fragmentClass ?: error("Fragment class is null")
        val rootOfBottomNavigableItemIdentifier =
            lookupRootOfBottomNavigableItem(identifier).featureTag

        // Restore backstack of new root
        fragmentManager?.restoreBackStack(rootOfBottomNavigableItemIdentifier)

        if (!isRootExisting(rootOfBottomNavigableItemIdentifier) &&
            getLastDestinationOfRoot(rootOfBottomNavigableItemIdentifier) == null
        ) {
            // then add root to backstack
            fragmentManager?.commit {
                setReorderingAllowed(true)
                replace(
                    /* containerViewId = */ containerId,
                    /* fragmentClass = */ fragmentClass,
                    /* args = */ args,
                    /* tag = */ identifier
                )
                addToBackStack(identifier)
            }

            // add root to rootTabInitial
            rootTabInitial.add(rootOfBottomNavigableItemIdentifier)

            // reset current destination
            currentBottomNavigableItem = bottomNavigableItem

            // save last destination of root
            lastDestinationOfRoot[rootOfBottomNavigableItemIdentifier] = currentBottomNavigableItem
        } else {
            // Restore last destination of root and set current destination to it
            getLastDestinationOfRoot(rootOfBottomNavigableItemIdentifier)?.let { lastDestination ->
                currentBottomNavigableItem = lastDestination
            }
        }

        // Trigger callback destination changed
        listener?.onDestinationChanged(currentBottomNavigableItem)
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
        return presetBottomItemGroups.find { it.featureTag == identifier }
            ?: presetBottomItemGroups.flatMap { it.children }.find { it.featureTag == identifier }
    }

    override fun setOnDestinationChangedListener(listener: BottomNavigationManager.OnDestinationChangedListener?) {
        this.listener = listener
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        // clear all things possible
        backPressedCallback.remove()
        listener = null
        rootTabInitial.clear()
        presetBottomItemGroups = emptyList()
    }

    private fun navigateToActivity(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        activityWeakReference.get()?.let { activity ->
            val intent = Intent(activity, bottomNavigableItem.activityClass).apply {
                args?.let { putExtras(it) }
            }
            activity.startActivity(intent)
        }
    }

    /**
     *
     *  Scenario 1: Clear all top nested destinations if existing when navigate to root:
     *  If navigate to root and existing top nested destinations then pop the backstack to the root destination.
     *
     *  Scenario 2: Keep all top nested destinations when navigate to another nested destination
     *  If navigate to a nested destination then put to backstack with new instance even it existing.
     *
     *  Save current backstack
     *  Save last destination of root
     *
     *  Lookup root of bottomNavigableItem
     *  Restore backstack of root
     *
     *  If bottomNavigableItem navigate to root
     *      then pop the backstack to the root destination with excluded itself: flags = 0
     *      fragmentManager?.popBackStackImmediate(rootOfBottomNavigableItemIdentifier, 0)
     *
     *      If root of bottomNavigableItem is not exist
     *          then add root to backstack
     *          add root to rootTabInitial
     *      Else
     *          do nothing
     *
     *     reset current destination
     *
     *  Else - assuming navigate to a nested destination
     *      then add root to backstack
     *      reset current destination
     *
     *  trigger callback destination changed
     *
     *  Final: add current destination to last destination of root
     *
     */
    private fun navigateToFragment(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // Enable back press
        enableBackPress()

        // Save current backstack
        val rootOfCurrentDestinationIdentifier =
            lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag).featureTag
        fragmentManager?.saveBackStack(rootOfCurrentDestinationIdentifier)
        // Save last destination of root
        lastDestinationOfRoot[rootOfCurrentDestinationIdentifier] = currentBottomNavigableItem

        // Lookup root of bottomNavigableItem
        val identifier = bottomNavigableItem.featureTag
        val fragmentClass = bottomNavigableItem.fragmentClass ?: error("Fragment class is null")
        val rootOfBottomNavigableItemIdentifier =
            lookupRootOfBottomNavigableItem(identifier).featureTag
        // Restore backstack of root
        fragmentManager?.restoreBackStack(rootOfBottomNavigableItemIdentifier)

        // If bottomNavigableItem navigate to root
        if (isNavigateToRoot(identifier)) {
            // then pop the backstack to the root destination with excluded itself: flags = 0
            fragmentManager?.popBackStackImmediate(rootOfBottomNavigableItemIdentifier, 0)

            // If root of bottomNavigableItem is not exist
            if (!isRootExisting(rootOfBottomNavigableItemIdentifier)) {
                // then add root to backstack
                fragmentManager?.commit {
                    setReorderingAllowed(true)
                    replace(
                        /* containerViewId = */ containerId,
                        /* fragmentClass = */ fragmentClass,
                        /* args = */ args,
                        /* tag = */ identifier
                    )
                    addToBackStack(identifier)
                }

                // add root to rootTabInitial
                rootTabInitial.add(rootOfBottomNavigableItemIdentifier)
            } else {
                // do nothing
            }

            // reset current destination
            currentBottomNavigableItem = bottomNavigableItem
        } else { // assuming navigate to a nested destination
            // then add root to backstack
            fragmentManager?.commit {
                setReorderingAllowed(true)
                replace(
                    /* containerViewId = */ containerId,
                    /* fragmentClass = */ fragmentClass,
                    /* args = */ args,
                    /* tag = */ identifier
                )
                addToBackStack(identifier)
            }

            // reset current destination
            currentBottomNavigableItem = bottomNavigableItem
        }

        // trigger callback destination changed
        listener?.onDestinationChanged(currentBottomNavigableItem)

        // add current destination to last destination of root
        lastDestinationOfRoot[rootOfBottomNavigableItemIdentifier] = currentBottomNavigableItem
    }

    private fun isRootExisting(rootIdentifier: String): Boolean {
        return this.rootTabInitial.contains(rootIdentifier)
    }

    private fun getCurrentRootIdentifier(): String {
        return getCurrentRoot().featureTag
    }

    private fun getCurrentRoot(): BottomNavigableItem {
        return lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag)
    }

    private fun isNavigateToRoot(identifier: String): Boolean {
        return presetBottomItemGroups.find { it.featureTag == identifier } != null
    }

    /**
     * Default: homeBottomItemGroup
     */
    private fun lookupRootOfBottomNavigableItem(identifier: String): BottomNavigableItem {
        return presetBottomItemGroups.find { rootBottomItemGroup ->
            rootBottomItemGroup.featureTag == identifier ||
                    rootBottomItemGroup.children.any { child -> child.featureTag == identifier }
        } ?: homeBottomItemGroup
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

    /**
     *
     *  Scenario 1: If current destination is home then do nothing
     *
     *  Scenario 2: If current destination is nested destination then pop the backstack
     *      Pre-check next destination is root - fragmentManager?.findFragmentById(containerId)?.tag
     *      If next destination NOT null
     *          lookup next destination by tag
     *          reset current destination
     *          save last destination of root
     *          trigger callback destination changed
     *
     *      Else - assuming next destination is root and null
     *
     *
     *  Scenario 3: If current destination is root and different with home then switching to home
     *
     *  Trigger callback destination changed
     *
     *  Final: save last destination of root
     *
     */
    private fun navigateUp() {
        // Scenario 1: If current destination is home then do nothing
        if (!canNavigateUp()) return

        // Scenario 2: If current destination is nested destination then pop the backstack
        val currentRootDestination = getCurrentRoot()
        if (currentRootDestination.featureTag != currentBottomNavigableItem.featureTag) {
            fragmentManager?.popBackStackImmediate()

            val nextDestinationIdentifier = fragmentManager?.findFragmentById(containerId)?.tag // Card details???

            // Issue: when direct navigate to Account details (#1)
            // And navigate to Card details
            // And navigate Account details (#2)
            // When pressing back then back to tab Cards
            // Expected focusing tab Accounts and back to previous Account details (#1)

            if (nextDestinationIdentifier != null) {
                // reset current destination
                currentBottomNavigableItem = lookupBottomNavigableItem(nextDestinationIdentifier)
                    ?: error("Fragment not found")

                // save last destination of root
                lastDestinationOfRoot[getCurrentRootIdentifier()] = currentBottomNavigableItem

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)

            } else { // assuming next destination is root and null
                val identifier = currentRootDestination.featureTag
                val fragmentClass =
                    currentRootDestination.fragmentClass ?: error("Fragment class is null")

                // then add root to backstack
                fragmentManager?.commit {
                    setReorderingAllowed(true)
                    replace(
                        /* containerViewId = */ containerId,
                        /* fragmentClass = */ fragmentClass,
                        /* args = */ null,
                        /* tag = */ identifier
                    )
                    addToBackStack(identifier)
                }

                // reset current destination
                currentBottomNavigableItem = currentRootDestination

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)
            }
        } else {
            // Scenario 3: If current destination is root and different with home then switching to home
            switchRoot(homeBottomItemGroup)
        }
    }

    /**
     * Get last destination of root
     */
    private fun getLastDestinationOfRoot(identifier: String): BottomNavigableItem? {
        return lastDestinationOfRoot[identifier]
    }
}