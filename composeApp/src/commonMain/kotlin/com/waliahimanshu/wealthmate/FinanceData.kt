package com.waliahimanshu.wealthmate

import kotlinx.serialization.Serializable

@Serializable
data class FinanceData(
    val salary: Double = 0.0,
    val outgoings: List<Outgoing> = emptyList(),
    val savings: List<SavingsAccount> = emptyList(),
    val mortgage: MortgageInfo? = null
) {
    val totalOutgoings: Double get() = outgoings.sumOf { it.amount }
    val totalSavings: Double get() = savings.sumOf { it.balance }
    val netMonthly: Double get() = salary - totalOutgoings - (mortgage?.monthlyPayment ?: 0.0)
}

@Serializable
data class Outgoing(
    val id: String,
    val name: String,
    val amount: Double,
    val category: OutgoingCategory
)

@Serializable
enum class OutgoingCategory {
    BILLS,
    GROCERIES,
    TRANSPORT,
    ENTERTAINMENT,
    SUBSCRIPTIONS,
    OTHER
}

@Serializable
data class SavingsAccount(
    val id: String,
    val name: String,
    val provider: String,
    val balance: Double,
    val interestRate: Double,
    val accountType: UKAccountType
)

@Serializable
enum class UKAccountType {
    ISA,
    LISA,
    REGULAR_SAVER,
    EASY_ACCESS,
    FIXED_TERM,
    PREMIUM_BONDS,
    OTHER
}

@Serializable
data class MortgageInfo(
    val provider: String,
    val remainingBalance: Double,
    val monthlyPayment: Double,
    val interestRate: Double,
    val termRemainingMonths: Int
)
