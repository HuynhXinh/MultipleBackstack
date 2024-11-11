package com.example.multiplebackstack.bottomnavigation.impl

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

class BottomNavigationManagerImplV5(
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

    /**
     * Check if can navigate up basing on FragmentManager's backstack
     * @see BottomNavigationManagerImplV5.getCurrentDestination
     */
    override fun canNavigateUp(): Boolean {
        val currentDestinationIdentifier = getCurrentDestination()?.featureTag ?: return false
        return currentDestinationIdentifier != homeBottomItemGroup.featureTag
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

        initRootForEachTabs()
    }

    private fun initRootForEachTabs() {
        // init empty backstack for each tab
        this.presetBottomItemGroups.filter { it.fragmentClass != null }.forEach { bottomItemGroup ->
            commitDestination(bottomItemGroup, null)

            this.fragmentManager?.saveBackStack(bottomItemGroup.featureTag)

            this.lastDestinationOfRoot[bottomItemGroup.featureTag] = bottomItemGroup
            rootTabInitial.add(bottomItemGroup.featureTag)
        }
    }

    /**
     *
     *  When user tapped on tab in BottomNavigationView then switch root
     *
     *    Step 1: Validate destination before perform navigate
     *      determine root of this destination if null return
     *
     *    Step 2: Get current destination basing on FragmentManager's backstack
     *      if backstack is empty then return null then fallback to home
     *      determine root of this destination if null then fallback to home
     *      Save current backstack
     *
     *    Step 3: Restore backstack of new root
     *
     *  If there is no nested destination on top and root of this destination is not exist
     *      then add root to backstack
     *      add root to rootTabInitial
     *      reset current destination
     *  Else
     *      Restore last destination of root and set current destination to it
     *
     *  Trigger callback destination changed
     *
     */
    override fun switchRoot(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        printCurrentBackStack("Begin switchRoot: ${bottomNavigableItem.featureTag}")

        // Validate destination before perform navigate
        val newDestinationIdentifier = bottomNavigableItem.featureTag
        val newRootDestination = determineRootDestinationByIdentifier(newDestinationIdentifier)

        if (!isValidDestination(bottomNavigableItem) || newRootDestination == null) {
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
        // Save current backstack
        fragmentManager?.saveBackStack(currentRootDestination.featureTag)

        postDelay {
            // Restore backstack for new root
            fragmentManager?.restoreBackStack(newRootDestination.featureTag)

            postDelay {
                printCurrentBackStack("After restoreBackStack - switchRoot")

                // If there is no nested destination on top and root of this destination is not exist
                //
                // Could NOT check these condition basing on FragmentManager's backstack
                // The result backstack is till belong to previous tab after restored backstack
                // Example:
                //  Given previous tab Accounts: Accounts -> Account details: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
                //  And in tab Cards: Cards -> Card details: backStackCount=2, backStackEntries=[CARDS, CARD_DETAILS]
                //  When switch to tab Accounts
                //  Then the backstack is still belong to tab Cards: backStackCount=2, backStackEntries=[CARDS, CARD_DETAILS]
                //  => Expected backstack is tab Accounts
                // So, we have to use the last destination of root is NULL and new root is not contain in rootTabInitial

                if (getLastDestinationOfRoot(newRootDestination.featureTag) == null &&
                    !isRootInit(newRootDestination.featureTag)
                ) {
                    // then add new root to backstack
                    commitDestination(bottomNavigableItem, args)

                    // add root to rootTabInitial
                    rootTabInitial.add(newRootDestination.featureTag)

                    // reset current destination
                    currentBottomNavigableItem = bottomNavigableItem

                    // save last destination of root
                    lastDestinationOfRoot[newRootDestination.featureTag] =
                        currentBottomNavigableItem
                } else {
                    // Restore last destination of root and set current destination to it

                    // Note: the case last destination is NOT NULL but root is not init
                    // App link -> direct navigate to a nested destination
                    getLastDestinationOfRoot(newRootDestination.featureTag)?.let { lastDestination ->
                        currentBottomNavigableItem = lastDestination
                    }
                }

                // Trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)

                printCurrentBackStack("End switchRoot")
            }
        }
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

    private fun determineBottomNavigableItemByIdentifier(identifier: String): BottomNavigableItem? {
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
     *  Step 1: Validate destination before perform navigate
     *        determine root of this destination if null return
     *
     *  Step 2: Enable back press callback
     *
     *  Step 3: Get current destination basing on FragmentManager's backstack
     *              if backstack is empty then return null then fallback to home
     *              determine root of this destination if null then fallback to home
     *          Save current backstack
     *
     *  Step 4: Restore backstack for new root
     *
     *  If destination navigate to root
     *      then pop the backstack to the root destination with excluded itself: flags = 0
     *      fragmentManager?.popBackStackImmediate(rootOfBottomNavigableItemIdentifier, 0)
     *
     *      If there is no nested destination on top and root of this destination is not exist
     *          then add root to backstack
     *          add root to rootTabInitial
     *      Else
     *          do nothing
     *
     *  Else - assuming navigate to a nested destination
     *      then add root to backstack
     *      reset current destination
     *
     *  reset current destination
     *
     *  trigger callback destination changed
     *
     *  Final: add current destination to last destination of root
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

        postDelay {
            // Restore backstack for new root
            fragmentManager?.restoreBackStack(newRootDestination.featureTag)

            postDelay {
                printCurrentBackStack("After restoreBackStack - navigateToFragment")

                // If this destination is root
                if (isRoot(newDestinationIdentifier)) {
                    // then pop the backstack to the root destination with excluded itself: flags = 0
                    // Before backstack: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
                    fragmentManager?.popBackStackImmediate(newRootDestination.featureTag, 0)
                    // After backstack: backStackCount=1, backStackEntries=[ACCOUNTS]

                    printCurrentBackStack("After popBackStackImmediate - navigateToFragment")

                    // If there is no nested destination on top and root of this destination is not exist
                    if (getBackStackCount() <= 1 && !isRootExisting()) {
                        // then add new root to backstack
                        commitDestination(bottomNavigableItem, args)

                        // add new root to rootTabInitial
                        rootTabInitial.add(newRootDestination.featureTag)
                    } else {
                        // do nothing
                    }

                } else { // assuming navigate to a nested destination
                    // then add root to backstack
                    commitDestination(bottomNavigableItem, args)
                }

                // reset current destination
                currentBottomNavigableItem = bottomNavigableItem

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)

                // add current destination to last destination of root
                lastDestinationOfRoot[newRootDestination.featureTag] = currentBottomNavigableItem

                printCurrentBackStack("End navigateToFragment")
            }
        }
    }

    private fun postDelay(delay: Long = 0, block: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed(
            { block() },
            delay
        )
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
    private fun isRootExisting(): Boolean {
        if (getBackStackCount() < 1) return false

        val rootIdentifier = fragmentManager?.getBackStackEntryAt(0)?.name ?: return false

        return isRoot(rootIdentifier)
    }

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
     *      Step 1: Get current destination basing on FragmentManager's backstack
     *          If backstack is empty then return null then fallback to home
     *          Compare current destination with home again if they are the same then return
     *          If there is remaining nested destination
     *          Then pop the backstack
     *              Check the backstack after pop
     *              If there is no nested destination on top and root of this destination is not exist
     *                  Then add root to backstack
     *                  Add root to rootTabInitial
     *                  Reset current destination
     *                  Save last destination of root
     *              Else
     *                  Assume that there is a nested destination on top
     *                  Reset current destination
     *                  Save last destination of root
     *
     *  Scenario 3: This current destination is root and different with home
     *      => then switching to home
     *
     */
    private fun navigateUp() {
        // Scenario 1: This current destination is home
        if (!canNavigateUp()) return

        val currentDestination = getCurrentDestination() ?: homeBottomItemGroup
        val currentRootDestination =
            determineRootDestinationByIdentifier(currentDestination.featureTag)
                ?: homeBottomItemGroup

        if (currentDestination.featureTag == homeBottomItemGroup.featureTag) {
            // Scenario 1: This current destination is home
            return
        }

        printCurrentBackStack("Begin navigateUp - ${currentRootDestination.featureTag}")

        // Scenario 2: There is remaining nested destination
        // Current backstack: backStackCount=2, backStackEntries=[ACCOUNTS, ACCOUNT_DETAILS]
        if (getBackStackCount() > 1) {
            fragmentManager?.popBackStackImmediate()
            // After backstack: backStackCount=1, backStackEntries=[ACCOUNTS]

            printCurrentBackStack("After popBackStackImmediate - navigateUp")

            initRootIfNeeded(currentRootDestination)
        } else {
            if (currentRootDestination.featureTag != homeBottomItemGroup.featureTag) {
                // Scenario 3: This current destination is root and different with home
                switchRoot(homeBottomItemGroup)
            } else {
                // Case direct to Account details and pressing back
                // Then should init home root
                initRootIfNeeded(currentRootDestination)
            }
        }
        printCurrentBackStack("End navigateUp")
    }

    private fun initRootIfNeeded(currentRootDestination: BottomNavigableItem) {
        // If there is no nested destination on top and root of this destination is not exist
        if (getBackStackCount() <= 1 && !isRootExisting()) {
            // then add root to backstack
            commitDestination(currentRootDestination, null)

            // add root to rootTabInitial
            rootTabInitial.add(currentRootDestination.featureTag)

            // reset current destination
            currentBottomNavigableItem = currentRootDestination

            // save last destination of root
            lastDestinationOfRoot[currentRootDestination.featureTag] = currentBottomNavigableItem
        } else {
            val currentDestinationAfterPop = getCurrentDestination()
            if (currentDestinationAfterPop != null) {
                // reset current destination
                currentBottomNavigableItem = currentDestinationAfterPop

                // save last destination of root
                lastDestinationOfRoot[currentRootDestination.featureTag] =
                    currentBottomNavigableItem
            }
        }
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

    /**
     * Get last destination of root
     */
    private fun getLastDestinationOfRoot(identifier: String): BottomNavigableItem? {
        return lastDestinationOfRoot[identifier]
    }
}