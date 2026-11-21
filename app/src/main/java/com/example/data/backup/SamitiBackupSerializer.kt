package com.example.data.backup

import com.example.data.model.InstallmentPayment
import com.example.data.model.Loan
import com.example.data.model.Member
import com.example.data.model.SavingsTransaction
import org.json.JSONArray
import org.json.JSONObject

data class SamitiBackupSnapshot(
    val schemaVersion: Int = 1,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val members: List<Member>,
    val savingsTransactions: List<SavingsTransaction>,
    val loans: List<Loan>,
    val installmentPayments: List<InstallmentPayment>
) {
    val totalRecords: Int
        get() = members.size + savingsTransactions.size + loans.size + installmentPayments.size
}

object SamitiBackupSerializer {
    private const val CURRENT_SCHEMA_VERSION = 1
    private const val APP_IDENTIFIER = "EktaSamiti"

    fun encode(snapshot: SamitiBackupSnapshot): String {
        val root = JSONObject().apply {
            put("schemaVersion", snapshot.schemaVersion)
            put("app", APP_IDENTIFIER)
            put("exportedAtMillis", snapshot.exportedAtMillis)
            put("members", JSONArray().apply {
                snapshot.members.forEach { put(memberToJson(it)) }
            })
            put("savingsTransactions", JSONArray().apply {
                snapshot.savingsTransactions.forEach { put(savingsToJson(it)) }
            })
            put("loans", JSONArray().apply {
                snapshot.loans.forEach { put(loanToJson(it)) }
            })
            put("installmentPayments", JSONArray().apply {
                snapshot.installmentPayments.forEach { put(installmentToJson(it)) }
            })
        }
        return root.toString(2)
    }

    fun decode(rawJson: String): Result<SamitiBackupSnapshot> {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) {
                return Result.failure(IllegalArgumentException("ব্যাকআপ ফাইলটি খালি"))
            }
            val root = JSONObject(trimmed)
            val app = root.optString("app", "")
            if (app != APP_IDENTIFIER) {
                return Result.failure(IllegalArgumentException("এটি একতা সমিতির বৈধ ব্যাকআপ ফাইল নয়"))
            }
            val schemaVersion = root.optInt("schemaVersion", CURRENT_SCHEMA_VERSION)
            if (schemaVersion <= 0) {
                return Result.failure(IllegalArgumentException("অসমর্থিত ব্যাকআপ সংস্করণ"))
            }

            val exportedAt = root.optLong("exportedAtMillis", System.currentTimeMillis())
            val membersArray = root.optJSONArray("members") ?: JSONArray()
            val savingsArray = root.optJSONArray("savingsTransactions") ?: JSONArray()
            val loansArray = root.optJSONArray("loans") ?: JSONArray()
            val installmentsArray = root.optJSONArray("installmentPayments") ?: JSONArray()

            val members = buildList(membersArray.length()) {
                for (i in 0 until membersArray.length()) {
                    add(jsonToMember(membersArray.getJSONObject(i)))
                }
            }
            val savings = buildList(savingsArray.length()) {
                for (i in 0 until savingsArray.length()) {
                    add(jsonToSavings(savingsArray.getJSONObject(i)))
                }
            }
            val loans = buildList(loansArray.length()) {
                for (i in 0 until loansArray.length()) {
                    add(jsonToLoan(loansArray.getJSONObject(i)))
                }
            }
            val installments = buildList(installmentsArray.length()) {
                for (i in 0 until installmentsArray.length()) {
                    add(jsonToInstallment(installmentsArray.getJSONObject(i)))
                }
            }

            Result.success(
                SamitiBackupSnapshot(
                    schemaVersion = schemaVersion,
                    exportedAtMillis = exportedAt,
                    members = members,
                    savingsTransactions = savings,
                    loans = loans,
                    installmentPayments = installments
                )
            )
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("ব্যাকআপ ফাইল পড়া যায়নি: ${e.message ?: "ত্রুটি"}", e))
        }
    }

    private fun memberToJson(m: Member): JSONObject = JSONObject().apply {
        put("id", m.id)
        put("memberCode", m.memberCode)
        put("name", m.name)
        put("mobile", m.mobile)
        put("fatherOrHusbandName", m.fatherOrHusbandName)
        put("nid", m.nid)
        put("address", m.address)
        put("memberType", m.memberType)
        put("nomineeName", m.nomineeName)
        put("nomineeRelation", m.nomineeRelation)
        put("photoUri", m.photoUri ?: JSONObject.NULL)
        put("admissionFee", m.admissionFee)
        put("savingsBalance", m.savingsBalance)
        put("activeLoanCount", m.activeLoanCount)
        put("totalLoanDue", m.totalLoanDue)
        put("registrationDate", m.registrationDate)
        put("isActive", m.isActive)
        put("registeredBy", m.registeredBy)
    }

    private fun jsonToMember(obj: JSONObject): Member = Member(
        id = obj.optLong("id", 0L),
        memberCode = obj.getString("memberCode"),
        name = obj.getString("name"),
        mobile = obj.optString("mobile", ""),
        fatherOrHusbandName = obj.optString("fatherOrHusbandName", ""),
        nid = obj.optString("nid", ""),
        address = obj.optString("address", ""),
        memberType = obj.optString("memberType", "WEEKLY"),
        nomineeName = obj.optString("nomineeName", ""),
        nomineeRelation = obj.optString("nomineeRelation", ""),
        photoUri = obj.optString("photoUri", "").takeIf { !obj.isNull("photoUri") && it.isNotBlank() },
        admissionFee = obj.optDouble("admissionFee", 0.0),
        savingsBalance = obj.optDouble("savingsBalance", 0.0),
        activeLoanCount = obj.optInt("activeLoanCount", 0),
        totalLoanDue = obj.optDouble("totalLoanDue", 0.0),
        registrationDate = obj.optLong("registrationDate", System.currentTimeMillis()),
        isActive = obj.optBoolean("isActive", true),
        registeredBy = obj.optString("registeredBy", "Admin")
    )

    private fun savingsToJson(s: SavingsTransaction): JSONObject = JSONObject().apply {
        put("id", s.id)
        put("memberId", s.memberId)
        put("memberCode", s.memberCode)
        put("memberName", s.memberName)
        put("type", s.type)
        put("savingsPlan", s.savingsPlan)
        put("amount", s.amount)
        put("balanceAfter", s.balanceAfter)
        put("date", s.date)
        put("note", s.note)
        put("collectedBy", s.collectedBy)
        put("collectedByRole", s.collectedByRole)
        put("receiptNumber", s.receiptNumber)
    }

    private fun jsonToSavings(obj: JSONObject): SavingsTransaction = SavingsTransaction(
        id = obj.optLong("id", 0L),
        memberId = obj.optLong("memberId", 0L),
        memberCode = obj.getString("memberCode"),
        memberName = obj.getString("memberName"),
        type = obj.optString("type", "DEPOSIT"),
        savingsPlan = obj.optString("savingsPlan", "WEEKLY"),
        amount = obj.getDouble("amount"),
        balanceAfter = obj.optDouble("balanceAfter", 0.0),
        date = obj.optLong("date", System.currentTimeMillis()),
        note = obj.optString("note", ""),
        collectedBy = obj.optString("collectedBy", "Admin"),
        collectedByRole = obj.optString("collectedByRole", "ADMIN"),
        receiptNumber = obj.optString("receiptNumber", "")
    )

    private fun loanToJson(l: Loan): JSONObject = JSONObject().apply {
        put("id", l.id)
        put("memberId", l.memberId)
        put("memberCode", l.memberCode)
        put("memberName", l.memberName)
        put("memberMobile", l.memberMobile)
        put("principalAmount", l.principalAmount)
        put("interestRate", l.interestRate)
        put("chargeAmount", l.chargeAmount)
        put("totalPayable", l.totalPayable)
        put("totalPaid", l.totalPaid)
        put("remainingDue", l.remainingDue)
        put("totalInstallments", l.totalInstallments)
        put("paidInstallments", l.paidInstallments)
        put("perInstallmentAmount", l.perInstallmentAmount)
        put("frequency", l.frequency)
        put("disbursedDate", l.disbursedDate)
        put("disbursedBy", l.disbursedBy)
        put("status", l.status)
        put("purpose", l.purpose)
    }

    private fun jsonToLoan(obj: JSONObject): Loan = Loan(
        id = obj.optLong("id", 0L),
        memberId = obj.optLong("memberId", 0L),
        memberCode = obj.getString("memberCode"),
        memberName = obj.getString("memberName"),
        memberMobile = obj.optString("memberMobile", ""),
        principalAmount = obj.getDouble("principalAmount"),
        interestRate = obj.optDouble("interestRate", 0.0),
        chargeAmount = obj.optDouble("chargeAmount", 0.0),
        totalPayable = obj.getDouble("totalPayable"),
        totalPaid = obj.optDouble("totalPaid", 0.0),
        remainingDue = obj.optDouble("remainingDue", obj.getDouble("totalPayable")),
        totalInstallments = obj.optInt("totalInstallments", 20),
        paidInstallments = obj.optInt("paidInstallments", 0),
        perInstallmentAmount = obj.optDouble("perInstallmentAmount", 0.0),
        frequency = obj.optString("frequency", "WEEKLY"),
        disbursedDate = obj.optLong("disbursedDate", System.currentTimeMillis()),
        disbursedBy = obj.optString("disbursedBy", "Admin"),
        status = obj.optString("status", "ACTIVE"),
        purpose = obj.optString("purpose", "")
    )

    private fun installmentToJson(i: InstallmentPayment): JSONObject = JSONObject().apply {
        put("id", i.id)
        put("loanId", i.loanId)
        put("memberId", i.memberId)
        put("memberCode", i.memberCode)
        put("memberName", i.memberName)
        put("installmentNumber", i.installmentNumber)
        put("paidAmount", i.paidAmount)
        put("penaltyAmount", i.penaltyAmount)
        put("paymentDate", i.paymentDate)
        put("remainingAfterPayment", i.remainingAfterPayment)
        put("collectedBy", i.collectedBy)
        put("receiptNumber", i.receiptNumber)
        put("notes", i.notes)
    }

    private fun jsonToInstallment(obj: JSONObject): InstallmentPayment = InstallmentPayment(
        id = obj.optLong("id", 0L),
        loanId = obj.optLong("loanId", 0L),
        memberId = obj.optLong("memberId", 0L),
        memberCode = obj.getString("memberCode"),
        memberName = obj.getString("memberName"),
        installmentNumber = obj.optInt("installmentNumber", 1),
        paidAmount = obj.getDouble("paidAmount"),
        penaltyAmount = obj.optDouble("penaltyAmount", 0.0),
        paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
        remainingAfterPayment = obj.optDouble("remainingAfterPayment", 0.0),
        collectedBy = obj.optString("collectedBy", "Admin"),
        receiptNumber = obj.optString("receiptNumber", ""),
        notes = obj.optString("notes", "")
    )
}
