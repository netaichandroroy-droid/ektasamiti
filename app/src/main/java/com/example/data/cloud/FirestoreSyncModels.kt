package com.example.data.cloud

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object FirestoreDocIds {
    private val INVALID_CHARS = Regex("[^a-zA-Z0-9_\\-]")

    fun sanitizeId(raw: String): String {
        val cleaned = raw.trim().replace(INVALID_CHARS, "_")
        return cleaned.ifEmpty { "doc_${System.currentTimeMillis()}" }.take(128)
    }

    fun memberId(memberCode: String): String = sanitizeId(memberCode)

    fun savingsId(receiptNumber: String): String = sanitizeId(receiptNumber)

    fun loanKey(memberCode: String, disbursedDate: Long): String =
        sanitizeId("LN_${memberCode}_$disbursedDate")

    fun installmentId(receiptNumber: String): String = sanitizeId(receiptNumber)
}

@IgnoreExtraProperties
data class CloudMember(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberCode: String = "",
    val name: String = "",
    val mobile: String = "",
    val fatherOrHusbandName: String = "",
    val nid: String = "",
    val address: String = "",
    val memberType: String = "WEEKLY",
    val nomineeName: String = "",
    val nomineeRelation: String = "",
    val photoUri: String? = null,
    val admissionFee: Double = 100.0,
    val savingsBalance: Double = 0.0,
    val activeLoanCount: Int = 0,
    val totalLoanDue: Double = 0.0,
    val registrationDate: Long = 0L,
    val isActive: Boolean = true,
    val registeredBy: String = "Admin",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toRoomMember(existingLocalId: Long = 0L): Member = Member(
        id = existingLocalId,
        memberCode = memberCode,
        name = name,
        mobile = mobile,
        fatherOrHusbandName = fatherOrHusbandName,
        nid = nid,
        address = address,
        memberType = memberType,
        nomineeName = nomineeName,
        nomineeRelation = nomineeRelation,
        photoUri = photoUri,
        admissionFee = admissionFee,
        savingsBalance = savingsBalance,
        activeLoanCount = activeLoanCount,
        totalLoanDue = totalLoanDue,
        registrationDate = if (registrationDate > 0L) registrationDate else System.currentTimeMillis(),
        isActive = isActive,
        registeredBy = registeredBy
    )
}

fun Member.toFirestoreCreateMap(userId: String): Map<String, Any> {
    val raw = mutableMapOf<String, Any?>(
        "userId" to userId,
        "memberCode" to memberCode.ifBlank { "ES-$id" },
        "name" to name.ifBlank { "সদস্য" },
        "mobile" to mobile,
        "fatherOrHusbandName" to fatherOrHusbandName,
        "nid" to nid,
        "address" to address,
        "memberType" to if (memberType in listOf("WEEKLY", "DAILY", "MONTHLY")) memberType else "WEEKLY",
        "nomineeName" to nomineeName,
        "nomineeRelation" to nomineeRelation,
        "photoUri" to photoUri,
        "admissionFee" to admissionFee.coerceAtLeast(0.0),
        "savingsBalance" to savingsBalance.coerceAtLeast(0.0),
        "activeLoanCount" to activeLoanCount.coerceAtLeast(0),
        "totalLoanDue" to totalLoanDue.coerceAtLeast(0.0),
        "registrationDate" to if (registrationDate > 0L) registrationDate else System.currentTimeMillis(),
        "isActive" to isActive,
        "registeredBy" to registeredBy.ifBlank { "Admin" },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return raw.filterValues { it != null } as Map<String, Any>
}

fun Member.toFirestoreUpdateMap(): Map<String, Any> {
    val raw = mutableMapOf<String, Any?>(
        "name" to name.ifBlank { "সদস্য" },
        "mobile" to mobile,
        "fatherOrHusbandName" to fatherOrHusbandName,
        "nid" to nid,
        "address" to address,
        "memberType" to if (memberType in listOf("WEEKLY", "DAILY", "MONTHLY")) memberType else "WEEKLY",
        "nomineeName" to nomineeName,
        "nomineeRelation" to nomineeRelation,
        "photoUri" to photoUri,
        "admissionFee" to admissionFee.coerceAtLeast(0.0),
        "savingsBalance" to savingsBalance.coerceAtLeast(0.0),
        "activeLoanCount" to activeLoanCount.coerceAtLeast(0),
        "totalLoanDue" to totalLoanDue.coerceAtLeast(0.0),
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    @Suppress("UNCHECKED_CAST")
    return raw.filterValues { it != null } as Map<String, Any>
}

@IgnoreExtraProperties
data class CloudSavingsTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val memberCode: String = "",
    val memberName: String = "",
    val type: String = "DEPOSIT",
    val savingsPlan: String = "WEEKLY",
    val amount: Double = 0.0,
    val balanceAfter: Double = 0.0,
    val date: Long = 0L,
    val note: String = "",
    val collectedBy: String = "Admin",
    val collectedByRole: String = "ADMIN",
    val receiptNumber: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toRoomTransaction(resolvedMemberId: Long, existingLocalId: Long = 0L): SavingsTransaction =
        SavingsTransaction(
            id = existingLocalId,
            memberId = resolvedMemberId,
            memberCode = memberCode,
            memberName = memberName,
            type = type,
            savingsPlan = savingsPlan,
            amount = amount,
            balanceAfter = balanceAfter,
            date = if (date > 0L) date else System.currentTimeMillis(),
            note = note,
            collectedBy = collectedBy,
            collectedByRole = collectedByRole,
            receiptNumber = receiptNumber.ifBlank { id }
        )
}

fun SavingsTransaction.toFirestoreCreateMap(userId: String): Map<String, Any> {
    val safeReceipt = receiptNumber.ifBlank { "REC-SAV-${id}_${date}" }
    return mapOf(
        "userId" to userId,
        "memberCode" to memberCode.ifBlank { "ES-$memberId" },
        "memberName" to memberName.ifBlank { "সদস্য" },
        "type" to if (type == "WITHDRAWAL") "WITHDRAWAL" else "DEPOSIT",
        "savingsPlan" to savingsPlan.ifBlank { "WEEKLY" },
        "amount" to amount.coerceAtLeast(0.01),
        "balanceAfter" to balanceAfter.coerceAtLeast(0.0),
        "date" to if (date > 0L) date else System.currentTimeMillis(),
        "note" to note.take(300),
        "collectedBy" to collectedBy.ifBlank { "Admin" },
        "collectedByRole" to if (collectedByRole == "COLLECTOR") "COLLECTOR" else "ADMIN",
        "receiptNumber" to safeReceipt,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@IgnoreExtraProperties
data class CloudLoan(
    @DocumentId val id: String = "",
    val userId: String = "",
    val loanKey: String = "",
    val memberCode: String = "",
    val memberName: String = "",
    val memberMobile: String = "",
    val principalAmount: Double = 0.0,
    val interestRate: Double = 0.0,
    val chargeAmount: Double = 0.0,
    val totalPayable: Double = 0.0,
    val totalPaid: Double = 0.0,
    val remainingDue: Double = 0.0,
    val totalInstallments: Int = 20,
    val paidInstallments: Int = 0,
    val perInstallmentAmount: Double = 0.0,
    val frequency: String = "WEEKLY",
    val disbursedDate: Long = 0L,
    val disbursedBy: String = "Admin",
    val status: String = "ACTIVE",
    val purpose: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toRoomLoan(resolvedMemberId: Long, existingLocalId: Long = 0L): Loan = Loan(
        id = existingLocalId,
        memberId = resolvedMemberId,
        memberCode = memberCode,
        memberName = memberName,
        memberMobile = memberMobile,
        principalAmount = principalAmount,
        interestRate = interestRate,
        chargeAmount = chargeAmount,
        totalPayable = totalPayable,
        totalPaid = totalPaid,
        remainingDue = remainingDue,
        totalInstallments = totalInstallments.coerceAtLeast(1),
        paidInstallments = paidInstallments.coerceAtLeast(0),
        perInstallmentAmount = perInstallmentAmount,
        frequency = frequency,
        disbursedDate = if (disbursedDate > 0L) disbursedDate else System.currentTimeMillis(),
        disbursedBy = disbursedBy,
        status = status,
        purpose = purpose
    )
}

fun Loan.toFirestoreCreateMap(userId: String): Map<String, Any> {
    val safeDisbursedDate = if (disbursedDate > 0L) disbursedDate else System.currentTimeMillis()
    val safeKey = FirestoreDocIds.loanKey(memberCode, safeDisbursedDate)
    val safePrincipal = principalAmount.coerceAtLeast(1.0)
    val safeCharge = chargeAmount.coerceAtLeast(0.0)
    val safePayable = totalPayable.coerceAtLeast(safePrincipal)
    return mapOf(
        "userId" to userId,
        "loanKey" to safeKey,
        "memberCode" to memberCode.ifBlank { "ES-$memberId" },
        "memberName" to memberName.ifBlank { "সদস্য" },
        "memberMobile" to memberMobile,
        "principalAmount" to safePrincipal,
        "interestRate" to interestRate.coerceAtLeast(0.0),
        "chargeAmount" to safeCharge,
        "totalPayable" to safePayable,
        "totalPaid" to totalPaid.coerceAtLeast(0.0),
        "remainingDue" to remainingDue.coerceAtLeast(0.0),
        "totalInstallments" to totalInstallments.coerceAtLeast(1),
        "paidInstallments" to paidInstallments.coerceAtLeast(0),
        "perInstallmentAmount" to perInstallmentAmount.coerceAtLeast(0.01),
        "frequency" to if (frequency == "MONTHLY") "MONTHLY" else "WEEKLY",
        "disbursedDate" to safeDisbursedDate,
        "disbursedBy" to disbursedBy.ifBlank { "Admin" },
        "status" to if (status == "CLOSED") "CLOSED" else "ACTIVE",
        "purpose" to purpose.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

fun Loan.toFirestoreUpdateMap(): Map<String, Any> = mapOf(
    "totalPaid" to totalPaid.coerceAtLeast(0.0),
    "remainingDue" to remainingDue.coerceAtLeast(0.0),
    "paidInstallments" to paidInstallments.coerceAtLeast(0),
    "status" to if (status == "CLOSED") "CLOSED" else "ACTIVE",
    "updatedAt" to FieldValue.serverTimestamp()
)

@IgnoreExtraProperties
data class CloudInstallmentPayment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val loanKey: String = "",
    val memberCode: String = "",
    val memberName: String = "",
    val installmentNumber: Int = 1,
    val paidAmount: Double = 0.0,
    val penaltyAmount: Double = 0.0,
    val paymentDate: Long = 0L,
    val remainingAfterPayment: Double = 0.0,
    val collectedBy: String = "Admin",
    val receiptNumber: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toRoomPayment(
        resolvedLoanId: Long,
        resolvedMemberId: Long,
        existingLocalId: Long = 0L
    ): InstallmentPayment = InstallmentPayment(
        id = existingLocalId,
        loanId = resolvedLoanId,
        memberId = resolvedMemberId,
        memberCode = memberCode,
        memberName = memberName,
        installmentNumber = installmentNumber.coerceAtLeast(1),
        paidAmount = paidAmount,
        penaltyAmount = penaltyAmount,
        paymentDate = if (paymentDate > 0L) paymentDate else System.currentTimeMillis(),
        remainingAfterPayment = remainingAfterPayment,
        collectedBy = collectedBy,
        receiptNumber = receiptNumber.ifBlank { id },
        notes = notes
    )
}

fun InstallmentPayment.toFirestoreCreateMap(userId: String, loanKey: String): Map<String, Any> {
    val safeReceipt = receiptNumber.ifBlank { "REC-INS-${id}_${paymentDate}" }
    return mapOf(
        "userId" to userId,
        "loanKey" to loanKey,
        "memberCode" to memberCode.ifBlank { "ES-$memberId" },
        "memberName" to memberName.ifBlank { "সদস্য" },
        "installmentNumber" to installmentNumber.coerceAtLeast(1),
        "paidAmount" to paidAmount.coerceAtLeast(0.01),
        "penaltyAmount" to penaltyAmount.coerceAtLeast(0.0),
        "paymentDate" to if (paymentDate > 0L) paymentDate else System.currentTimeMillis(),
        "remainingAfterPayment" to remainingAfterPayment.coerceAtLeast(0.0),
        "collectedBy" to collectedBy.ifBlank { "Admin" },
        "receiptNumber" to safeReceipt,
        "notes" to notes.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}
