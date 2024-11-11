package com.example.multiplebackstack.feature.account.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseFragment
import com.example.multiplebackstack.feature.card.details.CardDetailsViewFragment
import com.example.multiplebackstack.bottomnavigation.destination.FeatureIdentifier

private const val ARG_ACCOUNT_NAME = "ARG_ACCOUNT_NAME"

class AccountDetailsViewFragment : BaseFragment(R.layout.fragment_account_details) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val accountName = arguments?.getString(ARG_ACCOUNT_NAME) ?: "Unknown Account"
        val tvAccountDetails = view.findViewById<TextView>(R.id.tv_account_details)
        tvAccountDetails.text = accountName

        view.findViewById<View>(R.id.btn_go_to_cards).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.CARDS.featureName,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_card_details).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.CARD_DETAILS.featureName,
                args = CardDetailsViewFragment.data("Card #1")
            )
        }
    }

    companion object {
        fun data(accountName: String) = bundleOf(
            ARG_ACCOUNT_NAME to accountName
        )
    }
}