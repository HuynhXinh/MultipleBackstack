package com.example.multiplebackstack.nav.destination

import com.example.multiplebackstack.nav.destination.impl.AccountDetailsDestination
import com.example.multiplebackstack.nav.destination.impl.AccountsDestination
import com.example.multiplebackstack.nav.destination.impl.CardDetailsDestination
import com.example.multiplebackstack.nav.destination.impl.CardsDestination
import com.example.multiplebackstack.nav.destination.impl.PayDestination
import com.example.multiplebackstack.nav.destination.impl.PayDetailDestination

enum class SymbolicDestination(val navDestination: NavDestination) {
    VIEW_ACCOUNTS(AccountsDestination()),
    VIEW_ACCOUNT_DETAILS(AccountDetailsDestination()),
    VIEW_PAY(PayDestination()),
    VIEW_PAY_DETAILS(PayDetailDestination()),
    VIEW_CARDS(CardsDestination()),
    VIEW_CARD_DETAILS(CardDetailsDestination());
}