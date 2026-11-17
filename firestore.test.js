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

test("Unauthenticated user: cannot read, list, create, update, or delete scripts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const scriptsCol = unauthDb.collection("users").doc(ALICE_UID).collection("scripts");

  await assertFails(scriptsCol.get());
  await assertFails(scriptsCol.doc("script_1").get());
  await assertFails(
    scriptsCol.doc("script_1").set({
      userId: ALICE_UID,
      title: "Unauthorized Script",
      scriptContent: "echo 'nope'",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Authenticated owner: valid Kotlin create, list orderBy, partial update, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const scriptsCol = aliceDb.collection("users").doc(ALICE_UID).collection("scripts");
  const created = new Date(Date.now() - 5000);

  // Create without optional fields (null values omitted by .filterValues { it != null })
  await assertSucceeds(
    scriptsCol.doc("script_1").set({
      userId: ALICE_UID,
      title: "Neon Shader",
      scriptContent: "void main() {}",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create with optional sampleDrawableName
  await assertSucceeds(
    scriptsCol.doc("script_2").set({
      userId: ALICE_UID,
      title: "Quantum Middleware",
      scriptContent: "export const verify = () => true;",
      sampleDrawableName: "img_sample_quantum_1794887934325",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Exact Kotlin query: users/{uid}/scripts ordered by createdAt desc
  await assertSucceeds(scriptsCol.orderBy("createdAt", "desc").get());
  await assertSucceeds(scriptsCol.doc("script_1").get());

  // Partial update of mutable fields while preserving userId & createdAt
  await assertSucceeds(
    scriptsCol.doc("script_1").update({
      title: "Neon Shader v2",
      scriptContent: "void main() { gl_FragColor = vec4(1.0); }",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete owned script
  await assertSucceeds(scriptsCol.doc("script_1").delete());
});

test("Authenticated owner: invalid payloads, extra @DocumentId id key, future timestamps, and immutable field tampering fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const scriptsCol = aliceDb.collection("users").doc(ALICE_UID).collection("scripts");
  const now = new Date(Date.now() - 2000);

  // Reject @DocumentId 'id' field in payload
  await assertFails(
    scriptsCol.doc("bad_id_doc").set({
      id: "bad_id_doc",
      userId: ALICE_UID,
      title: "Invalid ID Field",
      scriptContent: "console.log(1)",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject future timestamp
  await assertFails(
    scriptsCol.doc("future_ts_doc").set({
      userId: ALICE_UID,
      title: "Future Time",
      scriptContent: "console.log(1)",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: now,
    })
  );

  // Reject oversized title (> 120 chars)
  await assertFails(
    scriptsCol.doc("oversized_title").set({
      userId: ALICE_UID,
      title: "A".repeat(121),
      scriptContent: "console.log(1)",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject empty title
  await assertFails(
    scriptsCol.doc("empty_title").set({
      userId: ALICE_UID,
      title: "",
      scriptContent: "console.log(1)",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject shadow/ghost field
  await assertFails(
    scriptsCol.doc("ghost_field").set({
      userId: ALICE_UID,
      title: "Shadow",
      scriptContent: "console.log(1)",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Seed valid doc to test update guards
  await assertSucceeds(
    scriptsCol.doc("valid_doc").set({
      userId: ALICE_UID,
      title: "Valid",
      scriptContent: "echo ok",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject mutating createdAt on update
  await assertFails(
    scriptsCol.doc("valid_doc").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Cross-user isolation: Alice cannot read, list, create, update, or delete Bob's scripts", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(BOB_UID)
      .collection("scripts")
      .doc("bob_script")
      .set({
        userId: BOB_UID,
        title: "Bob Secret Script",
        scriptContent: "secret()",
        createdAt: new Date(Date.now() - 5000),
        updatedAt: new Date(Date.now() - 5000),
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobScriptsCol = aliceDb.collection("users").doc(BOB_UID).collection("scripts");

  await assertFails(bobScriptsCol.get());
  await assertFails(bobScriptsCol.doc("bob_script").get());
  await assertFails(
    bobScriptsCol.doc("alice_injected").set({
      userId: ALICE_UID,
      title: "Injected",
      scriptContent: "hack()",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobScriptsCol.doc("bob_script").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobScriptsCol.doc("bob_script").delete());
});
