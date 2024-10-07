package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.card.CardsViewFragment
import com.example.multiplebackstack.nav.destination.NavDestination.FragmentDestination

class CardsDestination : FragmentDestination(
    identifier = "CARDS",
    fragmentClass = CardsViewFragment::class.java
) {
    override fun isActivity(): Boolean {
        return false
    }
}