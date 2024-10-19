package com.example.multiplebackstack.bottomnavigation.menu

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

open class BottomMenuDecoration(
    @StringRes val titleRes: Int,
    @StringRes val titleA11Res: Int,
    @DrawableRes val icon: Int,
    val badge: Badge = Badge.None,
    id: Int,
    isAvailable: Boolean,
) : BaseBottomMenu(id, isAvailable)