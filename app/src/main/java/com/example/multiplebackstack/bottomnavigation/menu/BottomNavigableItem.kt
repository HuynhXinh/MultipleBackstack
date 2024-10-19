package com.example.multiplebackstack.bottomnavigation.menu

import android.app.Activity
import androidx.fragment.app.Fragment

open class BottomNavigableItem(
    val featureTag: String,
    val fragmentClass: Class<out Fragment>? = null,
    val activityClass: Class<out Activity>? = null,
    titleRes: Int,
    titleA11Res: Int,
    icon: Int,
    badge: Badge,
    id: Int,
    isAvailable: Boolean,
) : BottomMenuDecoration(titleRes, titleA11Res, icon, badge, id, isAvailable)
