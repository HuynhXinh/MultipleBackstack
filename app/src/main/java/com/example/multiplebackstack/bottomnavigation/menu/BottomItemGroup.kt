package com.example.multiplebackstack.bottomnavigation.menu

import android.app.Activity
import androidx.fragment.app.Fragment

class BottomItemGroup(
    val children: List<BottomNavigableItem>,
    fragmentClass: Class<out Fragment>? = null,
    activityClass: Class<out Activity>? = null,
    featureTag: String,
    titleRes: Int,
    titleA11Res: Int,
    icon: Int,
    badge: Badge,
    id: Int,
    isAvailable: Boolean,
) : BottomNavigableItem(
    featureTag = featureTag,
    fragmentClass = fragmentClass,
    activityClass = activityClass,
    titleRes = titleRes,
    titleA11Res = titleA11Res,
    icon = icon,
    badge = badge,
    id = id,
    isAvailable = isAvailable
)