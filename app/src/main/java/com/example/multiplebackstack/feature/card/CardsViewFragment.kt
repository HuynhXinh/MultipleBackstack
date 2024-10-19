package com.example.multiplebackstack.feature.card

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.example.multiplebackstack.R
import com.example.multiplebackstack.base.BaseFragment
import com.example.multiplebackstack.feature.card.details.CardDetailsViewFragment
import com.example.multiplebackstack.bottomnavigation.destination.FeatureIdentifier

class CardsViewFragment : BaseFragment(R.layout.fragment_cards) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val items = Array(20) { "Card #$it" }
        val adapter = CardsAdapter(items) { card ->
            navigate(
                identifier = FeatureIdentifier.CARD_DETAILS.featureName,
                args = CardDetailsViewFragment.data(card)
            )
        }
        // Set the adapter
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_cards)
        recyclerView.adapter = adapter
    }
}