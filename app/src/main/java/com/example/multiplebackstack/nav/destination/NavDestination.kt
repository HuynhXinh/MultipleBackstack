package com.example.multiplebackstack.nav.destination

import android.app.Activity
import androidx.fragment.app.Fragment

sealed class NavDestination(val identifier: String) {
    open fun isActivity(): Boolean = true

    abstract class FragmentDestination(
        identifier: String,
        val fragmentClass: Class<out Fragment>
    ) : NavDestination(identifier)

    abstract class ActivityDestination(
        identifier: String,
        val targetActivity: Class<out Activity>
    ) : NavDestination(identifier)
}