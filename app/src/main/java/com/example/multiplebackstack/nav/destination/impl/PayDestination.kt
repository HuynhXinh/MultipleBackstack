package com.example.multiplebackstack.nav.destination.impl

import com.example.multiplebackstack.feature.pay.PayListActivity
import com.example.multiplebackstack.nav.destination.NavDestination.ActivityDestination

class PayDestination : ActivityDestination(
    identifier = "PAY",
    targetActivity = PayListActivity::class.java
) {
    override fun isActivity(): Boolean {
        return false
    }
}