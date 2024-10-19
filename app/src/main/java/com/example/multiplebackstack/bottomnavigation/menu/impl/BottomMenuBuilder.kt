package com.example.multiplebackstack.bottomnavigation.menu.impl

import com.example.multiplebackstack.R
import com.example.multiplebackstack.bottomnavigation.destination.FeatureIdentifier
import com.example.multiplebackstack.bottomnavigation.menu.Badge
import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomMenu
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem
import com.example.multiplebackstack.feature.account.AccountsViewFragment
import com.example.multiplebackstack.feature.account.details.AccountDetailsViewFragment
import com.example.multiplebackstack.feature.card.CardsViewFragment
import com.example.multiplebackstack.feature.card.details.CardDetailsViewFragment
import com.example.multiplebackstack.feature.pay.PayDetailsActivity
import com.example.multiplebackstack.feature.pay.PayListActivity

class BottomMenuBuilder {
    fun buildBottomMenu(): BottomMenu {
        val children = listOf(
            buildAccountsBottomItemGroup(),
            buildPayBottomItemGroup(),
            buildCardsBottomItemGroup(),
        )
        return BottomMenu(
            children = children,
            featureTag = "ROOT",
            titleRes = -1,
            titleA11Res = -1,
            icon = -1,
            badge = Badge.None,
            id = -1,
            isAvailable = false,
        )
    }

    private fun buildCardsBottomItemGroup(): BottomItemGroup {
        val children = listOf(
            BottomNavigableItem(
                id = 5,
                isAvailable = true,
                featureTag = FeatureIdentifier.CARD_DETAILS.featureName,
                fragmentClass = CardDetailsViewFragment::class.java,
                titleRes = R.string.title_cards,
                titleA11Res = R.string.title_cards,
                icon = R.drawable.ic_cards,
                badge = Badge.None
            )
        )

        return BottomItemGroup(
            children = children,
            id = 2,
            isAvailable = true,
            featureTag = FeatureIdentifier.CARDS.featureName,
            fragmentClass = CardsViewFragment::class.java,
            titleRes = R.string.title_cards,
            titleA11Res = R.string.title_cards,
            icon = R.drawable.ic_cards,
            badge = Badge.New
        )
    }

    private fun buildPayBottomItemGroup(): BottomItemGroup {
        val children = listOf(
            BottomNavigableItem(
                id = 4,
                isAvailable = true,
                featureTag = FeatureIdentifier.PAY_DETAILS.featureName,
                activityClass = PayDetailsActivity::class.java,
                titleRes = R.string.title_pay_list,
                titleA11Res = R.string.title_pay_list,
                icon = R.drawable.ic_pay_list,
                badge = Badge.None
            )
        )
        return BottomItemGroup(
            children = children,
            id = 1,
            isAvailable = true,
            featureTag = FeatureIdentifier.PAY.featureName,
            activityClass = PayListActivity::class.java,
            titleRes = R.string.title_pay_list,
            titleA11Res = R.string.title_pay_list,
            icon = R.drawable.ic_pay_list,
            badge = Badge.Notification
        )
    }

    private fun buildAccountsBottomItemGroup(): BottomItemGroup {
        val children = listOf(
            BottomNavigableItem(
                id = 3,
                isAvailable = true,
                featureTag = FeatureIdentifier.ACCOUNT_DETAILS.featureName,
                fragmentClass = AccountDetailsViewFragment::class.java,
                titleRes = R.string.title_accounts,
                titleA11Res = R.string.title_accounts,
                icon = R.drawable.ic_home,
                badge = Badge.None
            )
        )
        return BottomItemGroup(
            children = children,
            fragmentClass = AccountsViewFragment::class.java,
            id = 0,
            isAvailable = true,
            featureTag = FeatureIdentifier.ACCOUNTS.featureName,
            titleRes = R.string.title_accounts,
            titleA11Res = R.string.title_accounts,
            icon = R.drawable.ic_home,
            badge = Badge.Number(99)
        )
    }
}