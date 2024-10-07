package com.example.multiplebackstack.nav.destination

enum class FeatureIdentifier(
    val featureName: String,
    val symbolicDestination: SymbolicDestination
) {
    ACCOUNTS("ACCOUNTS", SymbolicDestination.VIEW_ACCOUNTS),
    ACCOUNT_DETAILS("ACCOUNT_DETAILS", SymbolicDestination.VIEW_ACCOUNT_DETAILS),
    PAY("PAY", SymbolicDestination.VIEW_PAY),
    PAY_DETAILS("PAY_DETAILS", SymbolicDestination.VIEW_PAY_DETAILS),
    CARDS("CARDS", SymbolicDestination.VIEW_CARDS),
    CARD_DETAILS("CARD_DETAILS", SymbolicDestination.VIEW_CARD_DETAILS);

    companion object {
        fun fromFeatureName(featureName: String): FeatureIdentifier? {
            return entries.find { it.featureName == featureName }
        }
    }
}