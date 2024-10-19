package com.example.multiplebackstack.bottomnavigation

import android.os.Bundle
import com.example.multiplebackstack.bottomnavigation.menu.BottomNavigableItem

interface BottomNavigator {
    fun navigate(
        bottomNavigableItem: BottomNavigableItem,
        args: Bundle? = null,
    )

    fun navigate(
        identifier: String,
        args: Bundle? = null,
    )
}