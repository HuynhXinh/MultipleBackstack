package com.example.multiplebackstack.feature.card.details

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseFragment
import com.example.multiplebackstack.nav.destination.FeatureIdentifier

private const val ARG_CARD_NAME = "ARG_CARD_NAME"

class CardDetailsViewFragment : BaseFragment(R.layout.fragment_card_details) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardName = requireArguments().getString(ARG_CARD_NAME)
        val tvCardDetails = view.findViewById<TextView>(R.id.tv_card_details)
        tvCardDetails.text = cardName

        view.findViewById<View>(R.id.btn_go_to_accounts).setOnClickListener {
            navigate(
                rootIdentifier = FeatureIdentifier.ACCOUNTS.featureName,
                navDestination = FeatureIdentifier.ACCOUNTS.symbolicDestination.navDestination,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_account_details).setOnClickListener {
            navigate(
                rootIdentifier = FeatureIdentifier.ACCOUNTS.featureName,
                navDestination = FeatureIdentifier.ACCOUNT_DETAILS.symbolicDestination.navDestination,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_pay_list).setOnClickListener {
            navigate(
                navDestination = FeatureIdentifier.PAY.symbolicDestination.navDestination,
                args = null
            )
        }

        view.findViewById<View>(R.id.btn_go_to_pay_details).setOnClickListener {
            navigate(
                navDestination = FeatureIdentifier.PAY_DETAILS.symbolicDestination.navDestination,
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