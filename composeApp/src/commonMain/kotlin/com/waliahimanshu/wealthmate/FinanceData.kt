package com.waliahimanshu.wealthmate

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Root data structure for the entire household/family finances
 */
@Serializable
data class HouseholdFinances(
    val id: String = generateId(),
    val name: String = "Our Household",
    val members: List<HouseholdMember> = emptyList(),
    val sharedGoals: List<SharedGoal> = emptyList(),
    val sharedAccounts: List<SavingsAccount> = emptyList(), // Joint accounts
    val sharedOutgoings: List<Outgoing> = emptyList(), // Shared bills (rent, utilities)
    val mortgage: MortgageInfo? = null,
    val investments: List<Investment> = emptyList(), // All investments (personal + kids)
    val customOutgoingCategories: List<String> = emptyList(), // User-defined categories
    val customGoalCategories: List<String> = emptyList(), // User-defined goal categories
    val customInvestmentCategories: List<String> = emptyList(), // User-defined investment categories
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    // Consolidated views
    val totalHouseholdIncome: Double
        get() = members.sumOf { it.salary }

    val totalIndividualOutgoings: Double
        get() = members.sumOf { it.totalOutgoings }

    val totalSharedOutgoings: Double
        get() = sharedOutgoings.sumOf { it.amount }

    val totalOutgoings: Double
        get() = totalIndividualOutgoings + totalSharedOutgoings + (mortgage?.monthlyPayment ?: 0.0)

    val totalIndividualSavings: Double
        get() = members.sumOf { it.totalSavings }

    val totalSharedSavings: Double
        get() = sharedAccounts.sumOf { it.balance }

    val totalSavings: Double
        get() = totalIndividualSavings + totalSharedSavings

    val netMonthlyHousehold: Double
        get() = totalHouseholdIncome - totalOutgoings

    val totalGoalsTarget: Double
        get() = sharedGoals.sumOf { it.targetAmount }

    val totalGoalsSaved: Double
        get() = sharedGoals.sumOf { it.currentAmount }

    val goalsProgress: Double
        get() = if (totalGoalsTarget > 0) (totalGoalsSaved / totalGoalsTarget) * 100 else 0.0

    // Investment computed properties
    val totalMonthlyInvestments: Double
        get() = investments.sumOf { it.monthlyContribution }

    val totalPortfolioValue: Double
        get() = investments.sumOf { it.currentValue }

    val totalInvested: Double
        get() = investments.sumOf { it.totalContributed }

    val kidsInvestments: List<Investment>
        get() = investments.filter { it.isForKids }

    val adultInvestments: List<Investment>
        get() = investments.filter { !it.isForKids }

    // All savings combined (shared + individual members)
    val allSavings: List<SavingsAccount>
        get() = sharedAccounts + members.flatMap { it.savings }

    // Easy Access Savings - money you can withdraw immediately
    val easyAccessSavings: Double
        get() {
            val easyTypes = listOf(
                UKAccountType.EASY_ACCESS,
                UKAccountType.CURRENT_ACCOUNT,
                UKAccountType.CASH_ISA,
                UKAccountType.REGULAR_SAVER
            )
            return allSavings.filter { it.accountType in easyTypes }.sumOf { it.balance }
        }

    // Locked Savings - money in fixed terms or notice accounts
    val lockedSavings: Double
        get() {
            val lockedTypes = listOf(
                UKAccountType.FIXED_TERM,
                UKAccountType.NOTICE_ACCOUNT,
                UKAccountType.PREMIUM_BONDS
            )
            return allSavings.filter { it.accountType in lockedTypes }.sumOf { it.balance }
        }
}

/**
 * Individual household member (you, partner, etc.)
 */
@Serializable
data class HouseholdMember(
    val id: String = generateId(),
    val name: String,
    val color: String = "#4CAF50", // For UI differentiation
    val salary: Double = 0.0,
    val outgoings: List<Outgoing> = emptyList(),
    val savings: List<SavingsAccount> = emptyList(),
    val contributions: List<GoalContribution> = emptyList() // Track who contributed what to goals
) {
    val totalOutgoings: Double get() = outgoings.sumOf { it.amount }
    val totalSavings: Double get() = savings.sumOf { it.balance }
    val netMonthly: Double get() = salary - totalOutgoings
}

/**
 * Shared financial goal (house deposit, holiday, emergency fund, wedding, etc.)
 */
@Serializable
data class SharedGoal(
    val id: String = generateId(),
    val name: String,
    val description: String = "",
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long? = null, // Optional deadline
    val category: GoalCategory = GoalCategory.OTHER,
    val customCategory: String? = null, // User-defined category (takes precedence if set)
    val contributions: List<GoalContribution> = emptyList(),
    val linkedAccountId: String? = null, // Link to a savings account if desired
    val createdAt: Long = currentTimeMillis(),
    val isCompleted: Boolean = false,
    val icon: String = "" // emoji or icon name
) {
    val progressPercent: Double
        get() = if (targetAmount > 0) (currentAmount / targetAmount) * 100 else 0.0

    val remainingAmount: Double
        get() = (targetAmount - currentAmount).coerceAtLeast(0.0)

    val monthlyTargetToComplete: Double
        get() {
            if (targetDate == null) return 0.0
            val monthsRemaining = ((targetDate - currentTimeMillis()) / (30L * 24 * 60 * 60 * 1000)).toInt()
            return if (monthsRemaining > 0) remainingAmount / monthsRemaining else remainingAmount
        }

    val displayCategory: String
        get() = customCategory ?: category.name.replace("_", " ")
}

@Serializable
enum class GoalCategory {
    HOME_DEPOSIT,
    EMERGENCY_FUND,
    HOLIDAY,
    WEDDING,
    CAR,
    EDUCATION,
    RETIREMENT,
    RENOVATION,
    BABY,
    OTHER
}

/**
 * Track who contributed how much to a goal
 */
@Serializable
data class GoalContribution(
    val id: String = generateId(),
    val memberId: String,
    val memberName: String,
    val amount: Double,
    val date: Long = currentTimeMillis(),
    val note: String = ""
)

/**
 * Monthly outgoing expense
 */
@Serializable
data class Outgoing(
    val id: String = generateId(),
    val name: String,
    val amount: Double,
    val category: OutgoingCategory = OutgoingCategory.OTHER,
    val customCategory: String? = null, // User-defined category (takes precedence if set)
    val isRecurring: Boolean = true,
    val frequency: PaymentFrequency = PaymentFrequency.MONTHLY,
    val ownerId: String? = null, // null = shared, otherwise member ID
    val notes: String = ""
) {
    val displayCategory: String
        get() = customCategory ?: category.name.replace("_", " ")
}

@Serializable
enum class OutgoingCategory {
    // Housing
    RENT,
    MORTGAGE,
    COUNCIL_TAX,
    UTILITIES,
    HOME_INSURANCE,
    MAINTENANCE,

    // Living
    GROCERIES,
    HOUSEHOLD,

    // Transport
    CAR_PAYMENT,
    CAR_INSURANCE,
    FUEL,
    PUBLIC_TRANSPORT,
    PARKING,

    // Financial
    LOAN_REPAYMENT,
    CREDIT_CARD,
    CHILDCARE,

    // Lifestyle
    SUBSCRIPTIONS,
    ENTERTAINMENT,
    DINING_OUT,
    GYM,
    PERSONAL_CARE,

    // Communication
    MOBILE_PHONE,
    BROADBAND,

    // Insurance
    LIFE_INSURANCE,
    HEALTH_INSURANCE,

    // Other
    CHARITY,
    PETS,
    OTHER,
    KIDS_ACTIVITIES



}

@Serializable
enum class PaymentFrequency {
    WEEKLY,
    FORTNIGHTLY,
    MONTHLY,
    QUARTERLY,
    ANNUALLY;

    fun toMonthlyAmount(amount: Double): Double = when (this) {
        WEEKLY -> amount * 52 / 12
        FORTNIGHTLY -> amount * 26 / 12
        MONTHLY -> amount
        QUARTERLY -> amount / 3
        ANNUALLY -> amount / 12
    }
}

/**
 * Savings account (UK-specific types)
 */
@Serializable
data class SavingsAccount(
    val id: String = generateId(),
    val name: String,
    val provider: String,
    val balance: Double,
    val interestRate: Double,
    val accountType: UKAccountType,
    val ownerId: String? = null, // null = joint account
    val ownerName: String? = null, // For display
    val isJoint: Boolean = false,
    val linkedGoalId: String? = null, // If this account is for a specific goal
    val notes: String = ""
)

@Serializable
enum class UKAccountType {
    // ISAs
    CASH_ISA,
    STOCKS_SHARES_ISA,
    LIFETIME_ISA,
    INNOVATIVE_FINANCE_ISA,
    JUNIOR_ISA,

    // Regular savings
    REGULAR_SAVER,
    EASY_ACCESS,
    NOTICE_ACCOUNT,
    FIXED_TERM,

    // Other
    PREMIUM_BONDS,
    CURRENT_ACCOUNT,
    INVESTMENT_ACCOUNT,
    PENSION,
    WORKPLACE_PENSION,
    SIPP,
    GENERAL_INVESTMENT,
    CRYPTO,
    OTHER
}

/**
 * Mortgage and property details
 */
@Serializable
data class MortgageInfo(
    val provider: String,
    val propertyValue: Double = 0.0,
    val purchasePrice: Double = 0.0,
    val purchaseDate: Long? = null,
    val originalMortgageAmount: Double = 0.0,
    val remainingBalance: Double,
    val monthlyPayment: Double,
    val interestRate: Double,
    val mortgageType: MortgageType = MortgageType.REPAYMENT,
    val dealDescription: String = "", // e.g. "2-year fixed", "5-year fixed", "Tracker"
    val fixedUntil: Long? = null, // When fixed rate deal ends
    val termRemainingMonths: Int,
    val totalTermMonths: Int = 0, // Original full term in months (e.g. 300 for 25 years)
    val overpaymentAllowancePercent: Double = 10.0, // Typical UK default is 10%
    val owners: List<String> = emptyList(), // Member IDs
    val notes: String = ""
) {
    val loanToValue: Double
        get() = if (propertyValue > 0) (remainingBalance / propertyValue) * 100 else 0.0

    val equity: Double
        get() = propertyValue - remainingBalance

    val equityPercent: Double
        get() = if (propertyValue > 0) (equity / propertyValue) * 100 else 0.0

    val mortgagePaidDown: Double
        get() = if (originalMortgageAmount > 0) originalMortgageAmount - remainingBalance else 0.0

    val termRemainingYears: Int
        get() = termRemainingMonths / 12

    val termRemainingExtraMonths: Int
        get() = termRemainingMonths % 12
}

@Serializable
enum class MortgageType {
    REPAYMENT,
    INTEREST_ONLY,
    PART_AND_PART
}

/**
 * Investment tracking - for ISAs, Trading 212, Junior ISAs, etc.
 */
@Serializable
data class Investment(
    val id: String = generateId(),
    val name: String,                         // "ISA 2024", "Trading Account"
    val fundName: String = "",                // "Vanguard S&P 500 ETF", "FTSE Global All Cap"
    val provider: String,                     // "Trading 212", "Vanguard", "Hargreaves Lansdown"
    val accountType: UKAccountType,           // STOCKS_SHARES_ISA, JUNIOR_ISA, GENERAL_INVESTMENT

    // Contribution tracking
    val contributionAmount: Double,           // How much you invest per period
    val frequency: InvestmentFrequency,       // MONTHLY, ONE_TIME, etc.

    // Portfolio value tracking (manually updated)
    val currentValue: Double = 0.0,           // Current portfolio value
    val totalContributed: Double = 0.0,       // Total amount invested over time

    // Ownership
    val ownerId: String? = null,              // Member ID (null = household)
    val ownerName: String? = null,
    val isForKids: Boolean = false,           // Junior ISA / kids investment

    // Metadata
    val assetClass: AssetClass = AssetClass.STOCKS,
    val startDate: Long = currentTimeMillis(),
    val lastUpdated: Long = currentTimeMillis(),
    val notes: String = ""
) {
    val monthlyContribution: Double
        get() = when (frequency) {
            InvestmentFrequency.ONE_TIME -> 0.0
            InvestmentFrequency.WEEKLY -> contributionAmount * 52 / 12
            InvestmentFrequency.MONTHLY -> contributionAmount
            InvestmentFrequency.QUARTERLY -> contributionAmount / 3
            InvestmentFrequency.ANNUALLY -> contributionAmount / 12
        }

    val gainLoss: Double
        get() = currentValue - totalContributed

    val gainLossPercent: Double
        get() = if (totalContributed > 0) ((currentValue - totalContributed) / totalContributed) * 100 else 0.0

    val displayFrequency: String
        get() = when (frequency) {
            InvestmentFrequency.ONE_TIME -> "One-time"
            InvestmentFrequency.WEEKLY -> "Weekly"
            InvestmentFrequency.MONTHLY -> "Monthly"
            InvestmentFrequency.QUARTERLY -> "Quarterly"
            InvestmentFrequency.ANNUALLY -> "Annually"
        }
}

@Serializable
enum class InvestmentFrequency {
    ONE_TIME, WEEKLY, MONTHLY, QUARTERLY, ANNUALLY
}

@Serializable
enum class AssetClass {
    STOCKS, BONDS, ETF, CRYPTO, CASH, PROPERTY, OTHER
}

// Utility functions
fun generateId(): String = Random.nextLong().toString(36)

expect fun currentTimeMillis(): Long

// ============================================
// BACKWARD COMPATIBILITY - Keep old FinanceData for migration
// ============================================

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

    // Migration helper - convert old data to new format
    fun toHouseholdFinances(memberName: String = "Me"): HouseholdFinances {
        val member = HouseholdMember(
            name = memberName,
            salary = salary,
            outgoings = outgoings,
            savings = savings
        )
        return HouseholdFinances(
            members = listOf(member),
            mortgage = mortgage
        )
    }
}
