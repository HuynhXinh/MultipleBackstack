package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.card.details.CardDetailsViewFragment
import com.example.multiplebackstack.nav.destination.NavDestination.FragmentDestination

class CardDetailsDestination : FragmentDestination(
    identifier = "ACCOUNT_DETAILS",
    fragmentClass = CardDetailsViewFragment::class.java
) {
    override fun isActivity(): Boolean {
        return false
    }
}