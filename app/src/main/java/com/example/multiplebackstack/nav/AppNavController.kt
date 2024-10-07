package com.example.multiplebackstack.nav

import com.example.multiplebackstack.nav.destination.NavDestination

interface AppNavController : Navigator {
    fun setRootIdentifiers(rootIdentifiers: List<String>)

    fun setOnDestinationChangedListener(listener: OnDestinationChangedListener)

    fun isHomeRoot(): Boolean

    interface OnDestinationChangedListener {
        fun onDestinationChanged(rootIdentifier: String?, navDestination: NavDestination)
    }
}