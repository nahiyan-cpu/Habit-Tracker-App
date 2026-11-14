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

test("Unauthenticated user cannot read or write habits or journals", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("journals").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set({ userId: ALICE_UID }));
});

test("Authenticated user can create and read own profile and reject ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = new Date(Date.now() - 1000);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      totalPoints: 0,
      level: 1,
      themeMode: "system",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(profileRef.get());

  // Shadow update / ghost field injection must fail
  await assertFails(
    profileRef.update({
      isAdmin: true,
      updatedAt: ts,
    })
  );
});

test("Cross-user isolation blocks Bob from reading or writing Alice's habits, completions, and journals", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").set({
      userId: ALICE_UID,
      name: "Morning Meditation",
      description: "10 mins mindfulness",
      categoryId: "cat_mindfulness",
      iconKey: "self_improvement",
      colorHex: "#6C63FF",
      startDate: "2026-11-14",
      frequencyType: "daily",
      targetDaysPerWeek: 7,
      scheduledWeekdays: [1, 2, 3, 4, 5, 6, 7],
      scheduledMonthDays: [],
      reminderTimes: ["08:00"],
      remindersEnabled: true,
      targetDurationMinutes: 10,
      notes: "",
      isPaused: false,
      isArchived: false,
      deleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("journals").doc("j_1").set({
      userId: ALICE_UID,
      dateIso: "2026-11-14",
      mood: "motivated",
      content: "Private reflection",
      habitId: "",
      deleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("habits").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("habits").doc("habit_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("journals").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("completions").doc("c_1").set({
      userId: ALICE_UID,
      habitId: "habit_1",
      dateIso: "2026-11-14",
      completedAtEpochMs: 1700000000000,
      isCompleted: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Invalid habit payload or spoofed userId is rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("bad_habit").set({
      userId: BOB_UID,
      name: "Spoof",
      categoryId: "cat_health",
      iconKey: "favorite",
      colorHex: "#6C63FF",
      startDate: "2026-11-14",
      frequencyType: "daily",
      targetDaysPerWeek: 7,
      remindersEnabled: false,
      isPaused: false,
      isArchived: false,
      deleted: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
