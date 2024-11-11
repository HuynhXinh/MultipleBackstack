package com.example.multiplebackstack.bottomnavigation

import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem

interface BottomNavigationManager : BottomNavigator {
    fun presetBottomItemGroups(bottomItemGroups: List<BottomItemGroup>)

    fun setOnDestinationChangedListener(listener: OnDestinationChangedListener?)

    fun canNavigateUp(): Boolean

    interface OnDestinationChangedListener {
        fun onDestinationChanged(bottomNavigableItem: BottomNavigableItem)
    }


}