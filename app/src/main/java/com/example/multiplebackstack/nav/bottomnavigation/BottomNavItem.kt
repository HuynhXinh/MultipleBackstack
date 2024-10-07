package com.example.multiplebackstack.nav.bottomnavigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

data class BottomNavItem(
    val id: Int,
    val featureTag: String,
    @StringRes val titleRes: Int,
    @StringRes val titleA11Res: Int,
    @DrawableRes val icon: Int,
    val isAvailable: Boolean = true
)