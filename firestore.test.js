const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-f7f3be7b-496c-4d08-bf57-38a268f047a4";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const EVE_UID = "eve_789";

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

test("Unauthenticated user: cannot read private users or appUsers, but can read shayaris", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore(DATABASE_ID);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("appUsers").limit(10).get());
  await assertFails(unauthDb.collection("chatRooms").get());
  await assertSucceeds(unauthDb.collection("shayaris").limit(10).get());
  await assertSucceeds(unauthDb.collection("publicSites").limit(5).get());
});

test("Authenticated user: can read/write own private /users/{uid} and read /appUsers directory", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Shayar",
      email: "alice@example.com",
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());

  await assertSucceeds(
    aliceDb.collection("appUsers").doc(ALICE_UID).set({
      uid: ALICE_UID,
      userId: ALICE_UID,
      displayName: "Alice Shayar",
      handle: "@SL_1111",
      followersCount: 0,
      followingCount: 0,
      likesReceivedCount: 0,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertSucceeds(bobDb.collection("appUsers").limit(200).get());
  await assertFails(
    bobDb.collection("appUsers").doc(ALICE_UID).update({ displayName: "Hacked" })
  );
  await assertSucceeds(
    bobDb.collection("appUsers").doc(ALICE_UID).update({ followersCount: 1 })
  );
});

test("Authenticated user: follow/like socialEdges and notifications succeed", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  await assertSucceeds(
    bobDb.collection("socialEdges").doc(`${BOB_UID}_follow_${ALICE_UID}`).set({
      actorId: BOB_UID,
      targetId: ALICE_UID,
      type: "follow",
    })
  );
  await assertSucceeds(
    bobDb
      .collection("appUsers")
      .doc(ALICE_UID)
      .collection("notifications")
      .add({
        recipientId: ALICE_UID,
        senderId: BOB_UID,
        type: "follow",
        isRead: false,
      })
  );
});

test("Private 1-on-1 chat: participants can create room+message in batch & read; third party Eve gets PERMISSION_DENIED", async () => {
  const roomId = `room_${ALICE_UID}_${BOB_UID}`;
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);
  const eveDb = testEnv.authenticatedContext(EVE_UID).firestore(DATABASE_ID);

  // Reading non-existent room document works cleanly
  await assertSucceeds(aliceDb.collection("chatRooms").doc(roomId).get());

  // Atomic batch write creating room + first message + notification
  const batch = aliceDb.batch();
  const roomRef = aliceDb.collection("chatRooms").doc(roomId);
  const msgRef = roomRef.collection("messages").doc();
  batch.set(roomRef, {
    participantIds: [ALICE_UID, BOB_UID],
    lastMessage: "Wah Taj!",
    lastSenderId: ALICE_UID,
  });
  batch.set(msgRef, {
    roomId,
    participantIds: [ALICE_UID, BOB_UID],
    senderId: ALICE_UID,
    receiverId: BOB_UID,
    text: "Wah Taj!",
    isRead: false,
  });
  await assertSucceeds(batch.commit());

  await assertSucceeds(bobDb.collection("chatRooms").where("participantIds", "array-contains", BOB_UID).get());
  await assertSucceeds(bobDb.collection("chatRooms").doc(roomId).collection("messages").limit(50).get());

  // Eve (non-participant) cannot read room or messages or create spoofed message
  await assertFails(eveDb.collection("chatRooms").doc(roomId).get());
  await assertFails(eveDb.collection("chatRooms").doc(roomId).collection("messages").limit(50).get());
  await assertFails(
    eveDb.collection("chatRooms").doc(roomId).collection("messages").add({
      roomId,
      participantIds: [ALICE_UID, BOB_UID],
      senderId: EVE_UID,
      receiverId: ALICE_UID,
      text: "Intruder!",
    })
  );
});

test("Authenticated user: lowercase /appusers counters allow social increments while blocking profile hijacks, and appConfig/main is readable", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(DATABASE_ID);
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(DATABASE_ID);

  await assertSucceeds(
    aliceDb.collection("appusers").doc(ALICE_UID).set({
      uid: ALICE_UID,
      displayName: "Alice Shayar",
      handle: "@SL_1111",
      bio: "Dil ki duniya",
      city: "Jaipur",
      address: "MI Road",
      followersCount: 0,
      likesReceivedCount: 0,
    })
  );

  // Bob can read Alice's profile & increment social counters on /appusers/{uid}, but cannot overwrite her bio/name
  await assertSucceeds(bobDb.collection("appusers").limit(20).get());
  await assertSucceeds(
    bobDb.collection("appusers").doc(ALICE_UID).update({ likesReceivedCount: 1 })
  );
  await assertFails(
    bobDb.collection("appusers").doc(ALICE_UID).update({ bio: "Tampered bio" })
  );

  // Clients can read live admin settings in appConfig/main, cannot write them
  await assertSucceeds(bobDb.collection("appConfig").doc("main").get());
  await assertFails(
    bobDb.collection("appConfig").doc("main").set({ ads_enabled: false })
  );
});
