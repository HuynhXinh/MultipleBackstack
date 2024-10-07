package com.example.multiplebackstack.feature.account

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseFragment
import com.example.multiplebackstack.feature.account.details.AccountDetailsViewFragment
import com.example.multiplebackstack.nav.destination.FeatureIdentifier

class AccountsViewFragment : BaseFragment(R.layout.fragment_accounts) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val items = Array(20) { "Account #$it" }
        val adapter = AccountsAdapter(items) { accountName ->
            navigate(
                rootIdentifier = FeatureIdentifier.ACCOUNTS.featureName,
                navDestination = FeatureIdentifier.ACCOUNT_DETAILS.symbolicDestination.navDestination,
                args = AccountDetailsViewFragment.data(accountName)
            )
        }
        val recyclerView = requireView().findViewById<RecyclerView>(R.id.rv_accounts)
        recyclerView.adapter = adapter
    }
}