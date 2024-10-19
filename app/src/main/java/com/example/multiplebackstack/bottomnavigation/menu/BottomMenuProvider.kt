package com.example.multiplebackstack.bottomnavigation.menu

interface BottomMenuProvider {
    fun getBottomNavItems(): List<BottomItemGroup>
}