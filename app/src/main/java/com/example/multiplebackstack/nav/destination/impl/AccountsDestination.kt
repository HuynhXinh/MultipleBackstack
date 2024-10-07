package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.account.AccountsViewFragment
import com.example.multiplebackstack.nav.destination.NavDestination.FragmentDestination

class AccountsDestination : FragmentDestination(
    identifier = "ACCOUNTS",
    fragmentClass = AccountsViewFragment::class.java
) {
    override fun isActivity(): Boolean {
        return true
    }
}