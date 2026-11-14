const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
function serverTimestamp() {
  return new Date(Date.now() - 1000);
}

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validMemberPayload(uid) {
  return {
    userId: uid,
    memberCode: "ES-1001",
    name: "Md. Karim",
    mobile: "01711223344",
    memberType: "WEEKLY",
    admissionFee: 100.0,
    savingsBalance: 500.0,
    activeLoanCount: 0,
    totalLoanDue: 0.0,
    registrationDate: 1731570000000,
    isActive: true,
    registeredBy: "Admin",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validSavingsPayload(uid) {
  return {
    userId: uid,
    memberCode: "ES-1001",
    memberName: "Md. Karim",
    type: "DEPOSIT",
    savingsPlan: "WEEKLY",
    amount: 200.0,
    balanceAfter: 700.0,
    date: 1731570000000,
    note: "Weekly savings",
    collectedBy: "Admin",
    collectedByRole: "ADMIN",
    receiptNumber: "REC-SAV-1001",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validLoanPayload(uid, status = "ACTIVE") {
  return {
    userId: uid,
    loanKey: "LN_ES-1001_1731570000000",
    memberCode: "ES-1001",
    memberName: "Md. Karim",
    memberMobile: "01711223344",
    principalAmount: 10000.0,
    interestRate: 10.0,
    chargeAmount: 1000.0,
    totalPayable: 11000.0,
    totalPaid: 0.0,
    remainingDue: 11000.0,
    totalInstallments: 20,
    paidInstallments: 0,
    perInstallmentAmount: 550.0,
    frequency: "WEEKLY",
    disbursedDate: 1731570000000,
    disbursedBy: "Admin",
    status,
    purpose: "Business",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

function validInstallmentPayload(uid) {
  return {
    userId: uid,
    loanKey: "LN_ES-1001_1731570000000",
    memberCode: "ES-1001",
    memberName: "Md. Karim",
    installmentNumber: 1,
    paidAmount: 550.0,
    penaltyAmount: 0.0,
    paymentDate: 1731570000000,
    remainingAfterPayment: 10450.0,
    collectedBy: "Admin",
    receiptNumber: "REC-INS-1001",
    notes: "Week 1",
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
  };
}

test("Unauthenticated caller cannot read or write any Samiti collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("members").get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("ES-1001")
      .set(validMemberPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("loans").get());
});

test("Cross-user isolation: Bob cannot read or write Alice's Samiti data", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("ES-1001")
      .set(validMemberPayload(ALICE_UID))
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("members").doc("ES-1001").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("members").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("members")
      .doc("ES-1002")
      .set(validMemberPayload(BOB_UID))
  );
});

test("Owner CRUD succeeds on valid Member, Savings, Loan, and Installment documents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(userDoc.collection("members").doc("ES-1001").set(validMemberPayload(ALICE_UID)));
  await assertSucceeds(userDoc.collection("members").get());
  await assertSucceeds(
    userDoc.collection("members").doc("ES-1001").update({
      savingsBalance: 700.0,
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc
      .collection("savings_transactions")
      .doc("REC-SAV-1001")
      .set(validSavingsPayload(ALICE_UID))
  );

  await assertSucceeds(
    userDoc
      .collection("loans")
      .doc("LN_ES-1001_1731570000000")
      .set(validLoanPayload(ALICE_UID))
  );
  await assertSucceeds(
    userDoc.collection("loans").doc("LN_ES-1001_1731570000000").update({
      totalPaid: 550.0,
      remainingDue: 10450.0,
      paidInstallments: 1,
      status: "ACTIVE",
      updatedAt: serverTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc
      .collection("installment_payments")
      .doc("REC-INS-1001")
      .set(validInstallmentPayload(ALICE_UID))
  );
});

test("Shadow update with ghost field or closed loan mutation fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    userDoc.collection("members").doc("ES-1001").set({
      ...validMemberPayload(ALICE_UID),
      isAdminOverride: true,
    })
  );

  await assertSucceeds(
    userDoc
      .collection("loans")
      .doc("LN_ES-1001_1731570000000")
      .set(validLoanPayload(ALICE_UID, "CLOSED"))
  );

  await assertFails(
    userDoc.collection("loans").doc("LN_ES-1001_1731570000000").update({
      status: "ACTIVE",
      updatedAt: serverTimestamp(),
    })
  );
});

test("Invalid document ID or identity spoofing is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertFails(
    userDoc.collection("members").doc("bad id!").set(validMemberPayload(ALICE_UID))
  );
  await assertFails(
    userDoc.collection("members").doc("ES-1001").set(validMemberPayload(BOB_UID))
  );
});
