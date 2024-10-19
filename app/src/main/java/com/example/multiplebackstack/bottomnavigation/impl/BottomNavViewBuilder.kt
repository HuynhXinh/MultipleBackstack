package com.example.multiplebackstack.bottomnavigation.impl

import com.example.multiplebackstack.bottomnavigation.menu.Badge
import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.lang.ref.WeakReference

class BottomNavViewBuilder(
    private val bottomNavigationView: BottomNavigationView,
    private val items: List<BottomItemGroup>
) {

    private val weakReference = WeakReference(bottomNavigationView)

    fun build() {
        val bottomNavigationView = weakReference.get() ?: return

        bottomNavigationView.menu.clear()

        items.forEachIndexed { index, item ->
            bottomNavigationView.menu.add(0, item.id, index, item.titleRes)
                .setIcon(item.icon)
                .setContentDescription(bottomNavigationView.context.getString(item.titleA11Res))

            showBadge(item)
        }
    }

    private fun showBadge(item: BottomItemGroup) {
        when (val badge = item.badge) {
            is Badge.Number -> {
                bottomNavigationView.getOrCreateBadge(item.id).number = badge.number
            }

            is Badge.New -> {
                // show text new
                bottomNavigationView.getOrCreateBadge(item.id).text = "NEW"
            }

            is Badge.Notification -> {
                // show dot
                bottomNavigationView.getOrCreateBadge(item.id)
            }

            is Badge.None -> {
                bottomNavigationView.removeBadge(item.id)
            }
        }
    }
}