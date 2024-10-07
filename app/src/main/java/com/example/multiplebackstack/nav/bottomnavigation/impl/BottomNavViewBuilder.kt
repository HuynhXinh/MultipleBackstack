package com.example.multiplebackstack.nav.bottomnavigation.impl

import com.example.multiplebackstack.nav.bottomnavigation.BottomNavItem
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavViewBuilder(
    private val bottomNavigationView: BottomNavigationView,
    private val items: List<BottomNavItem>
) {
    fun build() {
        bottomNavigationView.menu.clear()

        items.forEachIndexed { index, item ->
            if (item.isAvailable) {
                bottomNavigationView.menu.add(0, item.id, index, item.titleRes)
                    .setIcon(item.icon)
                    .setContentDescription(bottomNavigationView.context.getString(item.titleA11Res))
            }
        }
    }
}