package com.example.multiplebackstack.feature.card.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseFragment
import com.example.multiplebackstack.bottomnavigation.destination.FeatureIdentifier
import com.example.multiplebackstack.feature.account.details.AccountDetailsViewFragment

private const val ARG_CARD_NAME = "ARG_CARD_NAME"

class CardDetailsViewFragment : BaseFragment(R.layout.fragment_card_details) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardName = arguments?.getString(ARG_CARD_NAME) ?: "Unknown Account"
        val tvCardDetails = view.findViewById<TextView>(R.id.tv_card_details)
        tvCardDetails.text = cardName

        view.findViewById<View>(R.id.btn_go_to_accounts).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.ACCOUNTS.featureName,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_account_details).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.ACCOUNT_DETAILS.featureName,
                args = AccountDetailsViewFragment.data("Open from Card Details: Account #999")
            )
        }

        view.findViewById<View>(R.id.btn_go_to_pay_list).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.PAY.featureName,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_pay_details).setOnClickListener {
            navigate(
                identifier = FeatureIdentifier.PAY_DETAILS.featureName,
                args = null
            )
        }
    }

    companion object {
        fun data(card: String) = bundleOf(
            ARG_CARD_NAME to card
        )
    }
}