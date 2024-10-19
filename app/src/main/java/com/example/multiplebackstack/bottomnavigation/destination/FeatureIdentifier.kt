package com.example.multiplebackstack.bottomnavigation.destination

enum class FeatureIdentifier(
    val featureName: String
) {
    ACCOUNTS("ACCOUNTS"),
    ACCOUNT_DETAILS("ACCOUNT_DETAILS"),
    PAY("PAY"),
    PAY_DETAILS("PAY_DETAILS"),
    CARDS("CARDS"),
    CARD_DETAILS("CARD_DETAILS");

    companion object {
        fun fromFeatureName(featureName: String): FeatureIdentifier? {
            return entries.find { it.featureName == featureName }
        }
    }
}