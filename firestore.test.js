const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_shiraz";
const OWNER_EMAIL = "shiraz6862@gmail.com";
const CUSTOMER_UID = "customer_ali";
const OTHER_UID = "customer_bilal";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

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

function ownerDb() {
  return testEnv.authenticatedContext(OWNER_UID, {
    email: OWNER_EMAIL,
    email_verified: true,
  }).firestore();
}

function spoofedOwnerDb() {
  return testEnv.authenticatedContext("spoof_uid", {
    email: OWNER_EMAIL,
    email_verified: false,
  }).firestore();
}

function customerDb(uid = CUSTOMER_UID, phone = "+923001234567") {
  return testEnv.authenticatedContext(uid, {
    phone_number: phone,
  }).firestore();
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shop_info").doc("main").get());
  await assertFails(unauthDb.collection("products").where("shopId", "==", "mann_salwa").get());
  await assertFails(unauthDb.collection("categories").where("shopId", "==", "mann_salwa").get());
  await assertFails(unauthDb.collection("price_updates").where("shopId", "==", "mann_salwa").get());
  await assertFails(unauthDb.collection("orders").where("userId", "==", CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("users").doc(CUSTOMER_UID).get());
  await assertFails(unauthDb.collection("phone_registry").doc("pk_923001234567").get());
  await assertFails(unauthDb.collection("admins").doc(OWNER_UID).get());
  await assertFails(unauthDb.collection("backups").get());
});

test("Verified owner can manage shop_info, categories, products, and price_updates; customers and spoofed emails cannot", async () => {
  const owner = ownerDb();
  const customer = customerDb();
  const spoofed = spoofedOwnerDb();
  const ts = pastDate(10000);

  await assertSucceeds(
    owner.collection("shop_info").doc("main").set({
      shopId: "mann_salwa",
      businessName: "من سلوٰی ٹریڈر",
      address: "چک 225 احمد نگر، مین ٹینکی والا روڈ، فیصل آباد",
      contact1Name: "محمد شہباز",
      contact1Phone: "03237644924",
      contact2Name: "شیراز احمد",
      contact2Phone: "03098897096",
      whatsappNumber: "03098897096",
      announcement: "آج کے تازہ تھوک ریٹس جاری کر دیے گئے ہیں",
      lastRateUpdateLabel: "تازہ ترین ریٹ لسٹ",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    customer.collection("shop_info").doc("main").update({
      whatsappNumber: "03000000000",
      updatedAt: pastDate(),
    })
  );
  await assertFails(
    spoofed.collection("shop_info").doc("main").update({
      whatsappNumber: "03000000000",
      updatedAt: pastDate(),
    })
  );

  await assertSucceeds(
    owner.collection("categories").doc("cat_atta").set({
      shopId: "mann_salwa",
      name: "آٹا",
      sortOrder: 1,
      iconKey: "flour",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    owner.collection("products").doc("prod_atta_20").set({
      shopId: "mann_salwa",
      name: "فائن آٹا نمبر 1",
      category: "آٹا",
      packingDetails: "20 کلو تھیلا",
      wholesalePrice: 2650,
      previousWholesalePrice: 2700,
      retailPrice: 2800,
      available: true,
      specialOffer: true,
      offerText: "50 روپے فی تھیلا رعایت",
      updatedByUid: OWNER_UID,
      priceUpdatedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(
    customer.collection("products").doc("prod_atta_20").update({
      wholesalePrice: 100,
      updatedByUid: CUSTOMER_UID,
      updatedAt: pastDate(),
    })
  );
  await assertFails(spoofed.collection("products").doc("prod_atta_20").delete());

  const secondaryOwner = testEnv.authenticatedContext("owner_shirazahmad", {
    email: "shirazahmad6862@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(secondaryOwner.collection("users").get());

  const randomGoogleUser = testEnv.authenticatedContext("random_google_uid", {
    email: "random@gmail.com",
    email_verified: true,
  }).firestore();
  await assertFails(randomGoogleUser.collection("users").get());
  await assertFails(randomGoogleUser.collection("products").doc("prod_atta_20").delete());

  // Authorized Admin Phone Numbers (03098897096 & 03237644924)
  const phoneAdmin1 = testEnv.authenticatedContext("admin_phone_1", {
    phone_number: "+923098897096",
  }).firestore();
  const phoneAdmin2 = testEnv.authenticatedContext("admin_phone_2", {
    phone_number: "+923237644924",
  }).firestore();

  await assertSucceeds(
    phoneAdmin1.collection("products").doc("prod_trending_1").set({
      shopId: "mann_salwa",
      name: "سپر کرنل چاول",
      category: "چاول",
      packingDetails: "25 کلو بوری",
      wholesalePrice: 7400,
      previousWholesalePrice: 7200,
      retailPrice: 7800,
      available: true,
      specialOffer: true,
      trending: true,
      stockCount: 45,
      offerText: "خصوصی آفر",
      updatedByUid: "admin_phone_1",
      priceUpdatedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(phoneAdmin2.collection("orders").get());
  await assertSucceeds(phoneAdmin2.collection("users").get());
});

test("User signup requires pending status, enforces one account per phone, and admin controls approve/block/unblock", async () => {
  const customer = customerDb(CUSTOMER_UID, "+923001234567");
  const other = customerDb(OTHER_UID, "+923219876543");
  const owner = ownerDb();
  const ts = pastDate(5000);

  // Customer cannot self-approve on signup
  await assertFails(
    customer.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      phoneNumber: "+923001234567",
      displayName: "علی رضا",
      shopName: "علی کریانہ",
      city: "فیصل آباد",
      shopOrAddress: "علی کریانہ، فیصل آباد",
      accountStatus: "approved",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid signup starts as 'pending' and claims phone_registry
  await assertSucceeds(
    customer.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      phoneNumber: "+923001234567",
      displayName: "علی رضا",
      shopName: "علی کریانہ",
      city: "فیصل آباد",
      shopOrAddress: "علی کریانہ، فیصل آباد",
      accountStatus: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    customer.collection("phone_registry").doc("pk_923001234567").set({
      userId: CUSTOMER_UID,
      phoneNumber: "+923001234567",
      createdAt: ts,
    })
  );

  // Duplicate claim on same phone number fails
  await assertFails(
    other.collection("phone_registry").doc("pk_923001234567").set({
      userId: OTHER_UID,
      phoneNumber: "+923001234567",
      createdAt: pastDate(),
    })
  );

  // Customer cannot approve self or list all users
  await assertFails(
    customer.collection("users").doc(CUSTOMER_UID).update({
      accountStatus: "approved",
      updatedAt: pastDate(),
    })
  );
  await assertFails(customer.collection("users").get());
  await assertFails(other.collection("users").doc(CUSTOMER_UID).get());

  // Pending customer CANNOT create an order
  await assertFails(
    customer.collection("orders").doc("ord_pending_user").set({
      userId: CUSTOMER_UID,
      customerName: "علی رضا",
      customerPhone: "+923001234567",
      customerAddress: "فیصل آباد",
      items: [{ productId: "p1", name: "چینی", packing: "50 کلو", qty: 1, unitPrice: 6800 }],
      itemCount: 1,
      totalAmount: 6800,
      status: "pending",
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );

  // Owner lists all users and approves CUSTOMER_UID
  await assertSucceeds(owner.collection("users").get());
  await assertSucceeds(
    owner.collection("users").doc(CUSTOMER_UID).update({
      accountStatus: "approved",
      updatedAt: pastDate(2000),
    })
  );

  // Approved customer CAN create order
  await assertSucceeds(
    customer.collection("orders").doc("ord_1").set({
      userId: CUSTOMER_UID,
      customerName: "علی رضا",
      customerPhone: "+923001234567",
      customerAddress: "فیصل آباد",
      items: [{ productId: "p1", name: "چینی", packing: "50 کلو", qty: 2, unitPrice: 6800 }],
      itemCount: 2,
      totalAmount: 13600,
      status: "pending",
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );

  // Owner blocks customer -> blocked customer cannot place new orders
  await assertSucceeds(
    owner.collection("users").doc(CUSTOMER_UID).update({
      accountStatus: "blocked",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    customer.collection("orders").doc("ord_2").set({
      userId: CUSTOMER_UID,
      customerName: "علی رضا",
      customerPhone: "+923001234567",
      customerAddress: "فیصل آباد",
      items: [{ productId: "p1", name: "چینی", packing: "50 کلو", qty: 1, unitPrice: 6800 }],
      itemCount: 1,
      totalAmount: 6800,
      status: "pending",
      createdAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );

  // Owner unblocks customer -> approved again
  await assertSucceeds(
    owner.collection("users").doc(CUSTOMER_UID).update({
      accountStatus: "approved",
      updatedAt: pastDate(100),
    })
  );
});

test("Client queries on products, categories, and price_updates require shopId filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const ts = pastDate(5000);
    await db.collection("products").doc("p1").set({
      shopId: "mann_salwa",
      name: "سپر کرنل باسمتی",
      category: "چاول",
      packingDetails: "25 کلو بوری",
      wholesalePrice: 7200,
      previousWholesalePrice: 7000,
      retailPrice: 7600,
      available: true,
      specialOffer: false,
      updatedByUid: OWNER_UID,
      priceUpdatedAt: ts,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const customer = customerDb();
  await assertSucceeds(customer.collection("products").where("shopId", "==", "mann_salwa").get());
  await assertFails(customer.collection("products").get());
  await assertFails(customer.collection("categories").get());
  await assertFails(customer.collection("price_updates").get());
});
