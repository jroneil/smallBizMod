import puppeteer from "puppeteer-core";

const BASE = "http://localhost:3000";
const USER = "demo";
const PASS = "demo-local-development";

const results = [];
function ok(name, detail = "") {
  results.push({ name, pass: true, detail });
  console.log(`PASS  ${name}${detail ? ` — ${detail}` : ""}`);
}
function fail(name, detail) {
  results.push({ name, pass: false, detail });
  console.log(`FAIL  ${name} — ${detail}`);
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function textOf(page, selector) {
  return page.$eval(selector, (el) => (el.textContent || "").trim()).catch(() => "");
}

const browser = await puppeteer.launch({
  executablePath: "/usr/bin/google-chrome",
  headless: "new",
  args: ["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"],
  defaultViewport: { width: 1280, height: 900 },
});

try {
  const page = await browser.newPage();
  page.on("pageerror", (err) => console.log("  [pageerror]", err.message));

  // 1. Unauthenticated visit should be sent to /signin
  await page.goto(`${BASE}/`, { waitUntil: "networkidle2", timeout: 30000 });
  await page.waitForSelector("#username", { timeout: 15000 }).catch(() => {});
  const urlAfterGoto = new URL(page.url());
  if (urlAfterGoto.pathname === "/signin") {
    ok("unauthenticated root redirects to /signin", page.url());
  } else {
    fail("unauthenticated root redirects to /signin", page.url());
  }

  // 2. Sign in
  await page.type("#password", PASS);
  await Promise.all([
    page.waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 }).catch(() => {}),
    page.click("button[type=submit]"),
  ]);
  await delay(800);
  const afterSignin = new URL(page.url());
  if (afterSignin.pathname === "/customers") {
    ok("sign-in redirects to /customers", page.url());
  } else {
    fail("sign-in redirects to /customers", page.url());
  }

  // 3. Create a customer
  await page.waitForSelector('a[href="/customers/new"]', { timeout: 15000 });
  await Promise.all([
    page.waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 }).catch(() => {}),
    page.click('a[href="/customers/new"]'),
  ]);
  await page.waitForSelector("#name", { timeout: 15000 });
  const customerName = `E2E Customer ${Date.now()}`;
  await page.type("#name", customerName);
  await Promise.all([
    page.waitForNavigation({ waitUntil: "networkidle2", timeout: 30000 }).catch(() => {}),
    page.click('button[type=submit]'),
  ]);
  await delay(800);

  // We should be on a customer detail page now
  const detailHeading = await textOf(page, "h1");
  if (detailHeading === customerName) {
    ok("customer created and detail page shown", customerName);
  } else {
    fail("customer created and detail page shown", `h1="${detailHeading}"`);
  }

  // 4. Add a contact
  await page.waitForSelector("#contact-name", { timeout: 15000 });
  await page.type("#contact-name", "Casey Nolan");
  await page.type("#contact-role", "Property Manager");
  await page.type("#contact-email", "casey@example.com");
  await page.type("#contact-phone", "555-0100");
  await page.evaluate(() => {
    const button = [...document.querySelectorAll("button")]
      .find((b) => (b.textContent || "").trim().startsWith("Add contact"));
    if (button) button.click();
  });
  await delay(900);
  const bodyText = await page.evaluate(() => document.body.innerText);
  if (bodyText.includes("Casey Nolan")) {
    ok("contact added and listed");
  } else {
    fail("contact added and listed");
  }

  // 5. Save the Acme service profile
  await page.type("#serviceRegion", "Coastal District");
  await page.type("#accountNotes", "Gate code is on file");
  await page.evaluate(() => {
    const button = [...document.querySelectorAll("button")]
      .find((b) => (b.textContent || "").trim().includes("service profile"));
    if (button) button.click();
  });
  await delay(900);
  const bodyAfterSave = await page.evaluate(() => document.body.innerText);
  if (bodyAfterSave.includes("Saved")) {
    ok("acme service profile saved");
  } else {
    fail("acme service profile saved");
  }

  // 6. Reload and confirm persistence of contact + profile
  await page.reload({ waitUntil: "networkidle2", timeout: 30000 });
  await delay(1500);
  const persistedText = await page.evaluate(() => document.body.innerText);
  const regionValue = await page
    .$eval("#serviceRegion", (el) => el.value)
    .catch(() => "");
  const notesValue = await page
    .$eval("#accountNotes", (el) => el.value)
    .catch(() => "");
  const persisted = {
    contact: persistedText.includes("Casey Nolan"),
    region: regionValue === "Coastal District",
    notes: notesValue === "Gate code is on file",
  };
  if (persisted.contact && persisted.region && persisted.notes) {
    ok("contact and profile persist after reload");
  } else {
    fail("contact and profile persist after reload", JSON.stringify(persisted));
  }

  // 7. Sign out returns to /signin
  await page.evaluate(() => {
    const button = [...document.querySelectorAll("button")]
      .find((b) => (b.textContent || "").trim() === "Sign out");
    if (button) button.click();
  });
  await delay(800);
  const afterSignout = new URL(page.url());
  if (afterSignout.pathname === "/signin") {
    ok("sign-out returns to /signin");
  } else {
    fail("sign-out returns to /signin", page.url());
  }
} finally {
  await browser.close();
}

const failed = results.filter((r) => !r.pass);
console.log(`\n${results.length - failed.length}/${results.length} checks passed`);
process.exit(failed.length === 0 ? 0 : 1);
