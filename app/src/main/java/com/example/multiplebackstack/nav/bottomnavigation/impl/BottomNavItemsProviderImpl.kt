package com.example.multiplebackstack.nav.bottomnavigation.impl

import android.view.View
import com.example.multiplebackstack.R
import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItem
import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItemsProvider

class BottomNavItemsProviderImpl : BottomNavItemsProvider {
    private val items = listOf(
        BottomNavItem(
            id = View.generateViewId(),
            featureTag = "ACCOUNTS",//FeatureIdentifier.ACCOUNTS.featureName,
            titleRes = R.string.title_accounts,
            titleA11Res = R.string.title_accounts,
            icon = R.drawable.ic_home
        ),
        BottomNavItem(
            id = View.generateViewId(),
            featureTag = "PAY",//FeatureIdentifier.PAY.featureName,
            titleRes = R.string.title_pay_list,
            titleA11Res = R.string.title_pay_list,
            icon = R.drawable.ic_pay_list
        ),
        BottomNavItem(
            id = View.generateViewId(),
            featureTag = "CARDS",//FeatureIdentifier.CARDS.featureName,
            titleRes = R.string.title_cards,
            titleA11Res = R.string.title_cards,
            icon = R.drawable.ic_cards
        ),
        BottomNavItem(
            id = View.generateViewId(),
            featureTag = "CARDS",//FeatureIdentifier.CARDS.featureName,
            titleRes = R.string.title_profile,
            titleA11Res = R.string.title_profile,
            icon = R.drawable.ic_profile,
            isAvailable = false
        )
    )

    override fun getBottomNavItems() = items
}