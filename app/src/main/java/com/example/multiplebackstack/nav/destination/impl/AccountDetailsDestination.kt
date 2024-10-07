package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.account.details.AccountDetailsViewFragment
import com.example.multiplebackstack.nav.destination.NavDestination.*

class AccountDetailsDestination : FragmentDestination(
    identifier = "ACCOUNT_DETAILS",
    fragmentClass = AccountDetailsViewFragment::class.java
) {
    override fun isActivity(): Boolean {
        return true
    }
}