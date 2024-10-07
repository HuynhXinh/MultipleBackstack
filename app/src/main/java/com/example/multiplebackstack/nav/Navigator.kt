package com.example.multiplebackstack.nav

import android.os.Bundle
import com.example.multiplebackstack.nav.destination.NavDestination

interface Navigator {
    fun navigate(
        rootIdentifier: String,
        navDestination: NavDestination,
        args: Bundle? = null,
    )

    fun navigate(
        navDestination: NavDestination,
        args: Bundle? = null,
    )
}