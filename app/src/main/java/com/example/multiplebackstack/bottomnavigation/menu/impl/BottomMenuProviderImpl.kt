package com.example.multiplebackstack.bottomnavigation.menu.impl

import com.example.multiplebackstack.bottomnavigation.menu.BottomItemGroup
import com.example.multiplebackstack.bottomnavigation.menu.BottomMenuProvider

class BottomMenuProviderImpl : BottomMenuProvider {

    override fun getBottomNavItems(): List<BottomItemGroup> {
        return BottomMenuBuilder().buildBottomMenu().children.filter { it.isAvailable }
    }
}