package com.example.multiplebackstack.feature.pay

import android.content.Intent
import android.os.Bundle
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseActivity

class PayDetailsActivity : BaseActivity(R.layout.fragment_pay_details) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(Intent(this, PayListActivity::class.java))
    }
}