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

class BottomNavigationManagerImplV2(
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
     *  Issue: Can NOT get correct current fragment tag after restore backstack
     *
     *  Give home Accounts -> tag: Accounts
     *  And current fragment is Cards list -> tag: Cards
     *  When save current backstack: fragmentManager?.saveBackStack(Cards)
     *  Then restore backstack: fragmentManager?.restoreBackStack(Accounts)
     *  The current fragment tag is still Cards - the expectation is Accounts
     *
     *  => So, we have to custom a map of last destination of root to get the correct current destination
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

    override fun presetBottomItemGroups(rootBottomItemGroups: List<BottomItemGroup>) {
        this.presetBottomItemGroups = rootBottomItemGroups
        this.homeBottomItemGroup = rootBottomItemGroups.first()
        this.currentBottomNavigableItem = rootBottomItemGroups.first()
        this.lastDestinationOfRoot[homeBottomItemGroup.featureTag] = homeBottomItemGroup

        this.presetBottomItemGroups.forEach { bottomItemGroup ->
            this.fragmentManager?.saveBackStack(bottomItemGroup.featureTag)
            this.rootTabInitial.add(bottomItemGroup.featureTag)
        }
    }

    override fun switchRoot(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // save current backstack
        val rootOfCurrentDestination =
            lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag)
                ?: error("Root of current destination is null")
        /**
         *
         * Issue: the backstack will be lost when switching root and the current root is not init
         *
         * Give current root is Accounts -> tag: Accounts
         * When tapped on Account item
         * Then navigate to Account details -> tag: AccountDetails
         *
         * When tapped on Card details button (switching root to Cards)
         * Then navigate to Card details -> tag: CardDetails
         *
         * When tapped on tab Accounts again (switching root to Accounts and the Cards list root is not init) => the backstack is lost
         *      Then save current backstack: Cards => the backstack contains CardDetails will be lost because the Cards root is not init
         *      Anb switching root to Accounts -> tag: Accounts
         * Then navigate to Account details and focus tab Accounts
         *
         * When tapped on tab Cards again (switching root to Cards)
         * Then navigate to Cards list -> tag: Cards (expectation is show Card Details)
         *
         */
        fragmentManager?.saveBackStack(rootOfCurrentDestination.featureTag) // what happened when current root haven't init???

        // switching to new root
        val rootOfBottomNavigableItemIdentifier =
            lookupRootOfBottomNavigableItem(bottomNavigableItem.featureTag)?.featureTag
                ?: error("Root of destination is null")
        if (isRootInit(rootOfBottomNavigableItemIdentifier)) {
            fragmentManager?.restoreBackStack(rootOfBottomNavigableItemIdentifier)
            currentBottomNavigableItem =
                getLastDestinationOfRoot(rootOfBottomNavigableItemIdentifier)
        } else {
            // assuming root is not init
            // then add root to backstack
            val identifier = bottomNavigableItem.featureTag
            val fragmentClass = bottomNavigableItem.fragmentClass ?: error("Fragment class is null")
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
//            rootTabInitial.add(rootOfBottomNavigableItemIdentifier)

            // reset current destination
            currentBottomNavigableItem = bottomNavigableItem
        }

        // trigger callback destination changed
        listener?.onDestinationChanged(currentBottomNavigableItem)

        // add current destination to last destination of root
        lastDestinationOfRoot[rootOfBottomNavigableItemIdentifier] = currentBottomNavigableItem
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
     *  Scenario 1: Clear all top nested destinations when navigate to root:
     *  If navigate to root then pop the backstack to the root destination.
     *
     *  Scenario 2: Keep all top nested destinations when navigate to another nested destination
     *  If navigate to a nested destination then put to backstack with new instance even it existing.
     *
     *  if navigate to the same root
     *      if navigate to root
     *          if root init
     *              if current destination is root
     *                  then do nothing
     *              else
     *                  assuming current destination is a nested destination
     *                  then pop the backstack to the root destination (clear all top nested destinations)
     *                  reset current destination
     *                  trigger callback destination changed
     *          else
     *              then add root to backstack (identifier)
     *              add root to rootTabInitial
     *              reset current destination to root
     *              trigger callback destination changed
     *      else
     *          assuming navigate to nested destination
     *          add nested destination to backstack(null)
     *          reset current destination
     *          trigger callback destination changed
     *
     *  else - assuming switching tab
     *      save current backstack (what happened when current root haven't init??? -- direct navigate to nested destination without init root)
     *          saveBackStack(currentRoot)
     *
     *      switching current root to new root
     *      if the root of destination is init - then restore backstack first
     *          restoreBackStack(newRoot)
     *
     *      if navigate to root
     *          if root init
     *              if current destination is root
     *                  then do nothing
     *              else
     *                  assuming current destination is a nested destination
     *                  then pop the back stack to the root destination
     *                  reset current destination
     *                  trigger callback destination changed
     *          else
     *              then add root to backstack (identifier)
     *              reset current destination to root
     *              trigger callback destination changed
     *
     *      else - assuming navigate to nested destination
     *          add nested destination to backstack (null)
     *          reset current destination
     *          trigger callback destination changed
     *
     *  Final: add current destination to last destination of root
     */
    private fun navigateToFragment(bottomNavigableItem: BottomNavigableItem, args: Bundle?) {
        // Enable back press
        enableBackPress()

        val identifier = bottomNavigableItem.featureTag
        val rootOfBottomNavigableItemIdentifier =
            lookupRootOfBottomNavigableItem(identifier)?.featureTag
                ?: error("Root of identifier is null")
        val fragmentClass = bottomNavigableItem.fragmentClass ?: error("Fragment class is null")

        if (isNavigateToTheSameRoot(identifier)) {
            if (isNavigateToRoot(identifier)) {
                if (isRootInit(rootOfBottomNavigableItemIdentifier)) {
                    // current destination is root
                    if (identifier == rootOfBottomNavigableItemIdentifier) {
                        // do nothing
                    } else {
                        // assuming current destination is a nested destination
                        // then pop the back stack to the root destination
                        fragmentManager?.popBackStackImmediate(
                            rootOfBottomNavigableItemIdentifier,
                            0
                        )

                        // reset current destination
                        currentBottomNavigableItem = bottomNavigableItem

                        // trigger callback destination changed
                        listener?.onDestinationChanged(currentBottomNavigableItem)
                    }
                } else {
                    // assuming root is not init
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
//                    rootTabInitial.add(rootOfBottomNavigableItemIdentifier)

                    // reset current destination
                    currentBottomNavigableItem = bottomNavigableItem

                    // trigger callback destination changed
                    listener?.onDestinationChanged(currentBottomNavigableItem)
                }
            } else {
                // assuming navigate to nested destination
                // add nested destination to backstack
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

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)
            }
        } else { // assuming switching tab
            // save current backstack
            // saveBackStack(currentRoot)
            val currentRoot = getCurrentRootIdentifier()
            fragmentManager?.saveBackStack(currentRoot) // what happened when current root haven't init???

            // if new root init
            // restoreBackStack(newRoot)
            if (isRootInit(rootOfBottomNavigableItemIdentifier)) {
                fragmentManager?.restoreBackStack(rootOfBottomNavigableItemIdentifier)
            }
            // switching current root to new root
            if (isNavigateToRoot(identifier)) {
                if (isRootInit(rootOfBottomNavigableItemIdentifier)) {
                    // current destination is root
                    if (identifier == rootOfBottomNavigableItemIdentifier) {
                        // do nothing
                    } else {
                        // assuming current destination is a nested destination
                        // then pop the back stack to the root destination
                        fragmentManager?.popBackStackImmediate(
                            rootOfBottomNavigableItemIdentifier,
                            0
                        )

                        // reset current destination
                        currentBottomNavigableItem = bottomNavigableItem

                        // trigger callback destination changed
                        listener?.onDestinationChanged(currentBottomNavigableItem)
                    }
                } else {
                    // assuming root is not init
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
//                    rootTabInitial.add(rootOfBottomNavigableItemIdentifier)

                    // reset current destination
                    currentBottomNavigableItem = bottomNavigableItem

                    // trigger callback destination changed
                    listener?.onDestinationChanged(currentBottomNavigableItem)
                }
            } else {
                // assuming navigate to nested destination
                // add nested destination to backstack
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

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)
            }
        }

        // add current destination to last destination of root
        lastDestinationOfRoot[rootOfBottomNavigableItemIdentifier] = bottomNavigableItem
    }

    private fun getCurrentRootIdentifier(): String {
        return lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag)?.featureTag
            ?: error("Current root is null")
    }

    private fun isRootInit(rootIdentifier: String): Boolean {
        return (fragmentManager?.backStackEntryCount
            ?: 0) >= 1 && rootTabInitial.contains(rootIdentifier)
    }

    private fun isNavigateToRoot(identifier: String): Boolean {
        return presetBottomItemGroups.find { it.featureTag == identifier } != null
    }

    private fun isNavigateToTheSameRoot(identifier: String): Boolean {
        val rootOfBottomNavigableItemIdentifier =
            lookupRootOfBottomNavigableItem(identifier)?.featureTag
                ?: error("Root of identifier is null")

        val currentRoot =
            lookupRootOfBottomNavigableItem(currentBottomNavigableItem.featureTag)?.featureTag
                ?: error("Current root is null")

        return rootOfBottomNavigableItemIdentifier == currentRoot
    }

    private fun lookupRootOfBottomNavigableItem(identifier: String): BottomNavigableItem? {
        return presetBottomItemGroups.find { rootBottomItemGroup ->
            rootBottomItemGroup.featureTag == identifier ||
                    rootBottomItemGroup.children.any { child -> child.featureTag == identifier }
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
     *  if canGoBack == false return do nothing
     *
     *  current destination = get current fragment tag -> determine current destination
     *
     *  if current destination is root and different with home
     *      save current backstack
     *          saveBackStack(currentRoot)
     *      switching to home
     *          if home is init
     *              restoreBackStack(home)
     *              get last destination of home
     *              reset current destination
     *              trigger callback destination changed
     *
     *          else - assuming home is not init
     *              then add root to backstack
     *              add root to rootTabInitial
     *              reset current destination
     *              trigger callback destination changed
     *
     *  else - assuming current destination is nested destination
     *      then pop the backstack - popBackStackImmediate
     *
     *      pre-check root of current destination is init or not
     *      if backstack count after pop <= 1
     *          if root of current destination is init
     *              get current fragment tag -> determine current destination
     *              reset current destination
     *              trigger callback destination changed
     *
     *          else - assuming root of current destination is not init - in case direct navigate to nested destination previously
     *              then add root of current destination to backstack
     *              add root to rootTabInitial
     *              reset current destination
     *              trigger callback destination changed
     *      else - assuming current destination is still in a nested destination
     *          get current fragment tag -> determine current destination
     *          reset current destination
     *          trigger callback destination changed
     *
     *
     *  Final: add current destination to last destination of root
     *
     */
    private fun navigateUp() {
        // if canGoBack == false then do nothing
        if (!canNavigateUp()) return

        val currentDestinationIdentifier = currentBottomNavigableItem.featureTag
        val rootOfCurrentDestinationIdentifier =
            lookupRootOfBottomNavigableItem(currentDestinationIdentifier)?.featureTag
                ?: error("Root of current destination is null")

        // if current destination is root and different with home
        if (isRoot(currentDestinationIdentifier) && currentDestinationIdentifier != homeBottomItemGroup.featureTag) {
            // save current backstack
            fragmentManager?.saveBackStack(currentDestinationIdentifier)

            // switching to home
            // if home is init
            if (isRootInit(homeBottomItemGroup.featureTag)) {
                fragmentManager?.restoreBackStack(homeBottomItemGroup.featureTag)

                /**
                 *  Issue: Can NOT get correct current fragment tag after restore backstack
                 *
                 *  Give home Accounts -> tag: Accounts
                 *  And current fragment is Cards list -> tag: Cards
                 *  When save current backstack: fragmentManager?.saveBackStack(Cards)
                 *  Then restore backstack: fragmentManager?.restoreBackStack(Accounts)
                 *  The current fragment tag is still Cards - the expectation is Accounts
                 *
                 *  => So, we have to custom a map of last destination of root to get the correct current destination
                 */
                //reset current destination
                currentBottomNavigableItem =
                    getLastDestinationOfRoot(homeBottomItemGroup.featureTag)

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)

            } else { // assuming home is not init
                // then add root to backstack
                val fragmentClass =
                    homeBottomItemGroup.fragmentClass ?: error("Home fragment class is null")
                val identifier = homeBottomItemGroup.featureTag
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
                // add root to rootTabInitial
//                rootTabInitial.add(identifier)

                // reset current destination
                currentBottomNavigableItem = homeBottomItemGroup

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)
            }
        } else { // assuming current destination is nested destination
            // pop the backstack - popBackStackImmediate
            fragmentManager?.popBackStackImmediate()

            // pre-check root of current destination is init or not
            // if backstack count after pop <= 1
            if ((fragmentManager?.backStackEntryCount ?: 0) <= 1) {
                // if root of current destination is init
                if (isRootInit(rootOfCurrentDestinationIdentifier)) {
                    // reset current destination
                    currentBottomNavigableItem = getCurrentDestinationByFragmentTag()

                    // trigger callback destination changed
                    listener?.onDestinationChanged(currentBottomNavigableItem)
                } else { // assuming root of current destination is not init - in case direct navigate to nested destination previously
                    // then add root of current destination to backstack
                    val rootOfCurrentDestination =
                        lookupRootOfBottomNavigableItem(currentDestinationIdentifier)
                            ?: error("Root of current destination fragment class is null")

                    val fragmentClass =
                        rootOfCurrentDestination.fragmentClass
                            ?: error("Home fragment class is null")
                    val identifier = rootOfCurrentDestination.featureTag
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
                    // add root to rootTabInitial
//                    rootTabInitial.add(identifier)

                    // reset current destination
                    currentBottomNavigableItem = rootOfCurrentDestination
                }
            } else { // assuming current destination is nested destination
                // reset current destination
                currentBottomNavigableItem = getCurrentDestinationByFragmentTag()

                // trigger callback destination changed
                listener?.onDestinationChanged(currentBottomNavigableItem)
            }
        }

        // add current destination to last destination of root
        lastDestinationOfRoot[rootOfCurrentDestinationIdentifier] = currentBottomNavigableItem

    }

    /**
     * Get current fragment tag -> determine current destination
     * Default: homeBottomItemGroup
     */
    private fun getCurrentDestinationByFragmentTag(): BottomNavigableItem {
        val currentFragmentTag =
            fragmentManager?.findFragmentById(containerId)?.tag ?: return homeBottomItemGroup
        return lookupBottomNavigableItem(currentFragmentTag) ?: homeBottomItemGroup
    }

    /**
     * Get last destination of root
     * Default: homeBottomItemGroup
     */
    private fun getLastDestinationOfRoot(identifier: String): BottomNavigableItem {
        return lastDestinationOfRoot[identifier] ?: homeBottomItemGroup
    }

    private fun isRoot(identifier: String): Boolean {
        return presetBottomItemGroups.find { it.featureTag == identifier } != null
    }
}