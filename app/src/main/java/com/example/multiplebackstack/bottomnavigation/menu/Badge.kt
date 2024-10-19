package com.example.multiplebackstack.bottomnavigation.menu

sealed class Badge {
    data class Number(val number: Int) : Badge()
    data object New : Badge()
    data object Notification : Badge()
    data object None : Badge()
}