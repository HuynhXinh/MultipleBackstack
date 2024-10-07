package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.pay.PayDetailsActivity
import com.example.multiplebackstack.nav.destination.NavDestination

class PayDetailDestination : NavDestination.ActivityDestination(
    identifier = "PAY_DETAILS",
    targetActivity = PayDetailsActivity::class.java
) {
    override fun isActivity(): Boolean {
        return false
    }
}