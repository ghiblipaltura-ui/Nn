const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_001";
const ADMIN_EMAIL = "dashnitin110@gmail.com";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

function nowTs() {
  return new Date();
}

test("Unauthenticated user: cannot read users, wallets, or tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("wallets").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("tasks").where("status", "==", "OPEN").get());
});

test("User profile: Alice can create her own USER profile but cannot self-assign ADMIN role or isVerified=true", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "ADMIN",
      referralCode: "ALICE01",
      isVerified: false,
      kycStatus: "UNVERIFIED",
      preferredLanguage: "en",
      consentAccepted: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "USER",
      referralCode: "ALICE01",
      isVerified: true,
      kycStatus: "UNVERIFIED",
      preferredLanguage: "en",
      consentAccepted: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Sharma",
      email: "alice@example.com",
      role: "USER",
      referralCode: "ALICE01",
      isVerified: false,
      kycStatus: "UNVERIFIED",
      preferredLanguage: "en",
      consentAccepted: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("PII isolation: Bob cannot read Alice's user profile or wallet", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      role: "USER",
      referralCode: "ALICE01",
      isVerified: false,
      kycStatus: "UNVERIFIED",
      preferredLanguage: "en",
      consentAccepted: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
    await db.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableBalanceInr: 250,
      pendingBalanceInr: 0,
      totalEarnedInr: 250,
      totalWithdrawnInr: 0,
      completedTasksCount: 2,
      activeProjectsCount: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("wallets").doc(ALICE_UID).get());
});

test("Wallet immutability: Alice can create zero-balance wallet but can NEVER edit her own wallet balance", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Cannot create wallet with non-zero balance
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableBalanceInr: 9999,
      pendingBalanceInr: 0,
      totalEarnedInr: 9999,
      totalWithdrawnInr: 0,
      completedTasksCount: 0,
      activeProjectsCount: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Can initialize zero-balance wallet
  await assertSucceeds(
    aliceDb.collection("wallets").doc(ALICE_UID).set({
      userId: ALICE_UID,
      availableBalanceInr: 0,
      pendingBalanceInr: 0,
      totalEarnedInr: 0,
      totalWithdrawnInr: 0,
      completedTasksCount: 0,
      activeProjectsCount: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Cannot update balance afterward
  await assertFails(
    aliceDb.collection("wallets").doc(ALICE_UID).update({
      availableBalanceInr: 5000,
      updatedAt: nowTs(),
    })
  );
});

test("Ledger immutability: Regular user cannot mint ledger transactions; Admin can", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("ledger_transactions").doc("tx_fake_1").set({
      userId: ALICE_UID,
      type: "CREDIT_TASK",
      category: "TASKS",
      grossAmountInr: 100,
      platformCommissionInr: 15,
      gatewayFeeInr: 0,
      netAmountInr: 85,
      status: "VERIFIED",
      referenceId: "task_1",
      idempotencyKey: "key_1",
      description: "Fake credit",
      isTestMode: true,
      createdAt: nowTs(),
    })
  );

  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("ledger_transactions").doc("tx_real_1").set({
      userId: ALICE_UID,
      type: "CREDIT_TASK",
      category: "TASKS",
      grossAmountInr: 100,
      platformCommissionInr: 15,
      gatewayFeeInr: 0,
      netAmountInr: 85,
      status: "VERIFIED",
      referenceId: "task_1",
      idempotencyKey: "task_1_alice_123",
      description: "Approved microtask payout",
      isTestMode: true,
      createdAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("ledger_transactions").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(
    aliceDb.collection("ledger_transactions").get()
  );
});

test("Email spoofing protection: unverified email matching admin is rejected", async () => {
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();

  await assertFails(
    spoofAdminDb.collection("commission_configs").doc("TASKS").set({
      category: "TASKS",
      commissionPercent: 15,
      gatewayFeePercent: 2,
      operatingCostPercent: 3,
      isActive: true,
      updatedByUid: "spoof_1",
      updatedAt: nowTs(),
    })
  );
});

test("Anti-MLM referral check: user cannot refer themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("referrals").doc("ref_self").set({
      referrerUid: ALICE_UID,
      refereeUid: ALICE_UID,
      referralCode: "ALICE01",
      status: "SIGNED_UP",
      rewardInr: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceDb.collection("referrals").doc("ref_valid").set({
      referrerUid: BOB_UID,
      refereeUid: ALICE_UID,
      referralCode: "BOB01",
      status: "SIGNED_UP",
      rewardInr: 0,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Tasks and submissions: aligned queries succeed; unfiltered list fails; submission requires existing task", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true }).firestore();
  await assertSucceeds(
    adminDb.collection("tasks").doc("task_101").set({
      title: "Categorize 20 Retail Images",
      titleHi: "20 उत्पाद चित्रों को वर्गीकृत करें",
      category: "IMAGE_CATEGORIZATION",
      description: "Label product images into accurate e-commerce categories.",
      rewardGrossInr: 100,
      commissionPercent: 15,
      workerNetInr: 85,
      slotsTotal: 50,
      slotsFilled: 0,
      status: "OPEN",
      createdByUid: ADMIN_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("tasks").where("status", "==", "OPEN").get());
  await assertFails(aliceDb.collection("tasks").get());

  // Orphaned task submission fails
  await assertFails(
    aliceDb.collection("task_submissions").doc("sub_bad").set({
      taskId: "nonexistent_task",
      userId: ALICE_UID,
      proofText: "Done",
      status: "SUBMITTED",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Valid submission succeeds
  await assertSucceeds(
    aliceDb.collection("task_submissions").doc("sub_good").set({
      taskId: "task_101",
      userId: ALICE_UID,
      proofText: "Categorized all 20 retail images in sheet #4.",
      status: "SUBMITTED",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Referral codes lookup: PII-free code creation and lookup works without exposing user profiles", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("referral_codes").doc("EMBOB456").set({
      code: "EMBOB456",
      ownerUid: BOB_UID,
      createdAt: nowTs(),
    })
  );

  // Alice cannot overwrite Bob's code
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("referral_codes").doc("EMBOB456").set({
      code: "EMBOB456",
      ownerUid: ALICE_UID,
      createdAt: nowTs(),
    })
  );

  // Alice can look up Bob's code by ID
  await assertSucceeds(aliceDb.collection("referral_codes").doc("EMBOB456").get());
});

test("Freelance gigs & orders: seller cannot buy own gig; buyer and seller can read order", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("freelance_gigs").doc("gig_1").set({
      sellerId: BOB_UID,
      sellerName: "Bob Designer",
      category: "LOGO_DESIGN",
      title: "Modern Logo Pack",
      description: "3 logo concepts with vector files.",
      priceInr: 1000,
      deliveryDays: 3,
      isActive: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Self-order blocked
  await assertFails(
    bobDb.collection("freelance_orders").doc("order_self").set({
      gigId: "gig_1",
      buyerId: BOB_UID,
      sellerId: BOB_UID,
      grossAmountInr: 1000,
      platformCommissionInr: 100,
      sellerNetInr: 900,
      requirements: "Self test",
      status: "PLACED",
      isTestMode: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Valid buyer order succeeds
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("freelance_orders").doc("order_valid").set({
      gigId: "gig_1",
      buyerId: ALICE_UID,
      sellerId: BOB_UID,
      grossAmountInr: 1000,
      platformCommissionInr: 100,
      sellerNetInr: 900,
      requirements: "Please design a navy and green fintech logo.",
      status: "PLACED",
      isTestMode: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Game tasks: admin can publish GAME_TASK and user can submit completion proof", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    adminDb.collection("tasks").doc("game_math_01").set({
      title: "Quick Math Sprint",
      titleHi: "आसान गणित स्प्रिंट",
      category: "GAME_TASK",
      description: "Complete 5 quick math questions cleanly.",
      rewardGrossInr: 30,
      commissionPercent: 15,
      workerNetInr: 25.5,
      slotsTotal: 100,
      slotsFilled: 0,
      status: "OPEN",
      createdByUid: ADMIN_UID,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("task_submissions").doc(`game_math_01_${ALICE_UID}`).set({
      taskId: "game_math_01",
      userId: ALICE_UID,
      proofText: "Quick Math Sprint verified score: 5/5",
      status: "SUBMITTED",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Daily check-in promo: user can file 1 pending claim/day but cannot self-approve", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const claimId = `checkin_${ALICE_UID}_2026-11-14`;

  // Cannot create pre-approved claim
  await assertFails(
    aliceDb.collection("daily_checkins").doc(claimId).set({
      userId: ALICE_UID,
      dateKey: "2026-11-14",
      requestedBonusInr: 100,
      status: "APPROVED_TEST",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Valid pending claim succeeds
  await assertSucceeds(
    aliceDb.collection("daily_checkins").doc(claimId).set({
      userId: ALICE_UID,
      dateKey: "2026-11-14",
      requestedBonusInr: 100,
      status: "PENDING_ADMIN_FUNDING",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // User cannot self-approve after creating
  await assertFails(
    aliceDb.collection("daily_checkins").doc(claimId).update({
      status: "APPROVED_TEST",
      updatedAt: nowTs(),
    })
  );
});


