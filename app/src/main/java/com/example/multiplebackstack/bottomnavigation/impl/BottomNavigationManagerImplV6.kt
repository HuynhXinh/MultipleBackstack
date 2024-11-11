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

class BottomNavigationManagerImplV6(
    activity: FragmentActivity,
    private val containerId: Int
) : BottomNavigationManager, DefaultLifecycleObserver {

    private val activityWeakReference = WeakReference(activity)
    private val fragmentManager: FragmentManager? =
        activityWeakReference.get()?.supportFragmentManager

    private var presetBottomItemGroups: List<BottomItemGroup> = emptyList()
    private lateinit var homeBottomItemGroup: BottomItemGroup

    private var listener: BottomNavigationManager.OnDestinationChangedListener? = null

    private val rootTabInitial = mutableListOf<String>()

    private val backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            navigateUp()
        }
    }

    init {
        // Use the regular back press handling for older Android versions
        activityWeakReference.get()?.apply {
            onBackPressedDispatcher.addCallback(this, backPressedCallback)
        }
    }

    /**
     * Check if can navigate up basing on FragmentManager's backstack
     * @see BottomNavigationManagerImplV6.getCurrentDestination
     */
    override fun canNavigateUp(): Boolean {
        val currentDestinationIdentifier = getCurrentDestination()?.featureTag ?: return false
        return currentDestinationIdentifier != homeBottomItemGroup.featureTag
    }

    override fun presetBottomItemGroups(bottomItemGroups: List<BottomItemGroup>) {
        this.presetBottomItemGroups = bottomItemGroups
        this.homeBottomItemGroup = bottomItemGroups.first()
    }

    /**
     *
     *  When user tapped on tab in BottomNavigationView then switch root
     *
     *  Scenario 1: The root is not exist
     *      => then init root
     *
     *  Scenario 2: The root is existing
     *      => save current backstack and restore backstack of new root
     *
     *  Step 1: Validate destination before perform navigate
     *      check the destination is defined in the bottom menu
     *      and assert the destination is root
     *
     *  Step 2: Get current destination basing on FragmentManager's backstack
     *      if backstack is empty then return null then fallback to home
     *      determine root of this destination if null then fallback to home
     *
     *  Step 3: Check if the current root is the same as the new root
     *      then do nothing
     *
     *  Step 4: Save current backstack
     *
     *  Step 5: Check if new root is not exist
     *      then init root
     *    else restore backstack for new root
     *
     *  Step 6: Trigger callback destination changed
     *
     */
    override fun switchRoot(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        printCurrentBackStack("Begin switchRoot: ${bottomNavigableItem.featureTag}")

        // Validate destination before perform navigate
        val rootDestinationIdentifier = bottomNavigableItem.featureTag

        if (!isValidDestination(bottomNavigableItem) || !isRoot(rootDestinationIdentifier)) {
            // Need to check the destination is defined in the bottom menu before perform navigate
            return
        }

        // Get current destination
        // if backstack is empty then return null
        // then fallback to home
        val currentDestination = getCurrentDestination() ?: homeBottomItemGroup
        val currentRootDestination =
            determineRootDestinationByIdentifier(currentDestination.featureTag)
                ?: homeBottomItemGroup

        if (currentRootDestination.featureTag == rootDestinationIdentifier) {
            // If the current root is the same as the new root, do nothing
            return
        }

        // Save current backstack
        fragmentManager?.saveBackStack(currentRootDestination.featureTag)

        if (isRootInit(rootDestinationIdentifier)) {
            // Restore backstack for new root
            fragmentManager?.restoreBackStack(rootDestinationIdentifier)
        } else {
            initRoot(bottomNavigableItem, args)
        }

        // Trigger callback destination changed
        listener?.onDestinationChanged(bottomNavigableItem)

        printCurrentBackStack("End switchRoot")
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
        determineBottomNavigableItemByIdentifier(identifier)?.let { bottomNavigableItem ->
            navigate(bottomNavigableItem, args)
        }
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

    private fun determineBottomNavigableItemByIdentifier(identifier: String): BottomNavigableItem? {
        return presetBottomItemGroups.find { it.featureTag == identifier }
            ?: presetBottomItemGroups.flatMap { it.children }.find { it.featureTag == identifier }
    }

    private fun initRoot(rootDestination: BottomNavigableItem, args: Bundle?) {
        // then add root to backstack
        commitDestination(rootDestination, args)
        // add root to rootTabInitial
        rootTabInitial.add(rootDestination.featureTag)
    }

    private fun commitDestination(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        fragmentManager?.commit {
            setReorderingAllowed(true)
            replace(
                /* containerViewId = */ containerId,
                /* fragmentClass = */ bottomNavigableItem.fragmentClass!!,
                /* args = */ args,
                /* tag = */ bottomNavigableItem.featureTag
            )
            addToBackStack(bottomNavigableItem.featureTag)
        }
    }

    private fun isRootInit(featureTag: String): Boolean {
        return rootTabInitial.contains(featureTag)
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
     *  Navigate to a destination
     *
     *  Scenario 1: Clear all top nested destinations if existing when navigate to root:
     *  If navigate to root and existing top nested destinations then pop the backstack to the root destination.
     *
     *  Scenario 2: Keep all top nested destinations when navigate to another nested destination
     *  If navigate to a nested destination then put to backstack with new instance even it existing.
     *
     *  Step 1: Validate destination before perform navigate
     *        determine root of this destination if null return
     *
     *  Step 2: Enable back press callback
     *
     *  Step 3: Get current destination basing on FragmentManager's backstack
     *              if backstack is empty then return null then fallback to home
     *              determine root of this destination if null then fallback to home
     *          save current backstack
     *
     *  Step 4: Check if new root is not exist
     *          then init root
     *      else restore backstack for new root
     *
     *  Step 5: Check if destination navigate to root
     *          then pop the backstack to the root destination with excluded itself: flags = 0
     *          fragmentManager?.popBackStackImmediate(rootOfBottomNavigableItemIdentifier, 0)
     *      else - assuming navigate to a nested destination
     *          then add root to backstack
     *
     *  Step 6: trigger callback destination changed
     *
     */
    private fun navigateToFragment(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // Validate destination before perform navigate
        val newDestinationIdentifier = bottomNavigableItem.featureTag
        val newRootDestination = determineRootDestinationByIdentifier(newDestinationIdentifier)

        if (!isValidDestination(bottomNavigableItem) || newRootDestination == null) {
            // Need to check the destination is defined in the bottom menu before perform navigate
            return
        }

        // Enable back press callback
        enableBackPress()

        printCurrentBackStack("Begin navigateToFragment - $newDestinationIdentifier")

        // Get current destination
        // if backstack is empty then return null
        // then fallback to home
        val currentDestination = getCurrentDestination() ?: homeBottomItemGroup
        val currentRootDestination =
            determineRootDestinationByIdentifier(currentDestination.featureTag)
                ?: homeBottomItemGroup
        // Save current backstack
        fragmentManager?.saveBackStack(currentRootDestination.featureTag)

        if (isRootInit(newRootDestination.featureTag)) {
            // Restore backstack for new root
            fragmentManager?.restoreBackStack(newRootDestination.featureTag)
        } else {
            // Init root
            initRoot(newRootDestination, args)
        }

        // If this destination is root
        if (isRoot(newDestinationIdentifier)) {
            // then pop the backstack to the root destination with excluded itself: flags = 0
            // Before backstack: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
            fragmentManager?.popBackStackImmediate(newRootDestination.featureTag, 0)
            // After backstack: backStackCount=1, backStackEntries=[ACCOUNTS]

            printCurrentBackStack("After popBackStackImmediate - navigateToFragment")

        } else { // assuming navigate to a nested destination
            // then add root to backstack
            commitDestination(bottomNavigableItem, args)
        }

        // trigger callback destination changed
        listener?.onDestinationChanged(bottomNavigableItem)

        printCurrentBackStack("End navigateToFragment")
    }

    private fun isValidDestination(bottomNavigableItem: BottomNavigableItem): Boolean {
        return presetBottomItemGroups.any { it.featureTag == bottomNavigableItem.featureTag } ||
                presetBottomItemGroups.flatMap { it.children }
                    .any { it.featureTag == bottomNavigableItem.featureTag }
    }

    /**
     *
     *  Get current destination basing on FragmentManager's backstack
     *  Example backstack of each tab:
     *
     *  Init: backStackCount=0, backStackEntries=[]
     *
     *  Tab Accounts: Accounts -> Account details: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
     *
     *  Switch to tab Cards: Cards -> Card details: backStackCount=2, backStackEntries=[CARDS, CARD_DETAILS]
     *
     *  From Card details to Account details:
     *      Accounts's backstack: backStackCount=3, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS (#1), ACCOUNT_DETAILS (#2)]
     *      Cards's backstack: backStackCount=2, backStackEntries=[CARDS, CARD_DETAILS]
     *
     */
    private fun getCurrentDestination(): BottomNavigableItem? {
        val backStackCount = fragmentManager?.backStackEntryCount ?: 0

        // If backstack is empty then return null
        if (backStackCount < 1) return null

        val identifier = fragmentManager?.getBackStackEntryAt(backStackCount - 1)?.name ?: ""

        return determineBottomNavigableItemByIdentifier(identifier)
    }

    /**
     *
     *  Check root existing basing on FragmentManager's backstack
     *  Example backstack:
     *
     *      Init: backStackCount=0, backStackEntries=[]
     *      => Root is not existing
     *
     *      Tab Accounts: Accounts -> Account details: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
     *      => Root is existing: ACCOUNTS at index 0
     *
     *      Tab Accounts: App link -> Account details: backStackCount=2, backStackEntries=[ACCOUNT_DETAILS]
     *      => Root is not existing
     *
     */

    private fun getBackStackCount(): Int {
        return fragmentManager?.backStackEntryCount ?: 0
    }

    /**
     *  Check an identifier is root or not basing on preset bottom item groups
     */
    private fun isRoot(identifier: String): Boolean {
        return presetBottomItemGroups.find { it.featureTag == identifier } != null
    }

    private fun determineRootDestinationByIdentifier(identifier: String): BottomNavigableItem? {
        return presetBottomItemGroups.find { bottomItemGroup ->
            bottomItemGroup.featureTag == identifier ||
                    bottomItemGroup.children.any { child -> child.featureTag == identifier }
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

    /**
     *
     *  Scenario 1: This current destination is home
     *      => then do nothing
     *
     *  Scenario 2: There is remaining nested destination
     *      => then pop the backstack and pre-check whether need to init root (if it is not exist)
     *
     *  Scenario 3: This current destination is root and different with home
     *      => then switching to home
     *
     */
    private fun navigateUp() {
        // Scenario 1: This current destination is home
        if (!canNavigateUp()) return

        val currentDestination = getCurrentDestination() ?: homeBottomItemGroup

        printCurrentBackStack("Begin navigateUp - ${currentDestination.featureTag}")

        // Scenario 2: There is remaining nested destination
        // Current backstack: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
        if (getBackStackCount() > 1) {
            fragmentManager?.popBackStackImmediate()
            // After backstack: backStackCount=1, backStackEntries=[ACCOUNTS]

            printCurrentBackStack("After popBackStackImmediate - navigateUp")
        } else {
            // Scenario 3: This current destination is root and different with home
            switchRoot(homeBottomItemGroup)
        }
        printCurrentBackStack("End navigateUp")
    }

    private fun printCurrentBackStack(tag: String) {
        val backStackCount = fragmentManager?.backStackEntryCount ?: 0
        val backStackEntries = (0 until backStackCount).map {
            fragmentManager?.getBackStackEntryAt(it)?.toString()
        }
        println("Xinh - $tag - Previous: backStackCount=$backStackCount, backStackEntries=$backStackEntries")
        val findFragmentId = fragmentManager?.findFragmentById(containerId)?.tag
        println("Xinh - $tag - Previous: findFragmentId=$findFragmentId")
    }

}