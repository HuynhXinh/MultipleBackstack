package com.example.multiplebackstack.bottomnavigation.menu

class BottomMenu(
    val children: List<BottomItemGroup>,
    featureTag: String,
    titleRes: Int,
    titleA11Res: Int,
    icon: Int,
    badge: Badge,
    id: Int,
    isAvailable: Boolean,
) : BottomNavigableItem(
    featureTag = featureTag,
    fragmentClass = null,
    activityClass = null,
    titleRes = titleRes,
    titleA11Res = titleA11Res,
    icon = icon,
    badge = badge,
    id = id,
    isAvailable = isAvailable
)