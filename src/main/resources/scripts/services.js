// Demo: interact with infrastructure services from a sandboxed JS script.
// Activate with: --spring.profiles.active=dev,grpc
// (or drop 'grpc' to use HTTP/WebClient instead)

// --- XDB / ABC (HTTP or gRPC, transparent) ---

// Read: list sheets from XDB
const sheet = services.abcSheet(
  "kit01 sheetid, kit02 name, kit03 title",
  "xykit",
  "sheet"
);
console.log("XDB sheet result:", JSON.stringify(sheet));

// Write: insert a row
const insertResult = services.abcExec(
  "insert", "xykit", "sheet", null,
  JSON.stringify({ sheetid: "demo-01", name: "Demo", title: "gRPC + JS" })
);
console.log("Insert result:", JSON.stringify(insertResult));

// --- Events ---
services.publish(
  "hex4w.script.events",
  "script-complete",
  JSON.stringify({ script: "services.js", ok: true })
);

// --- Lambda ---
const lambdaResult = services.invoke(
  "my-function",
  JSON.stringify({ action: "ping" })
);
console.log("Lambda result:", lambdaResult);

// Fire-and-forget Lambda (InvocationType.EVENT)
services.invokeAsync(
  "my-function",
  JSON.stringify({ action: "fire" })
);
console.log("Lambda async invoked (fire-and-forget)");

// --- Storage (S3) ---
const items = services.listItems("my-bucket");
console.log("S3 items:", items);

// --- Email ---
services.sendEmail(
  "admin@example.com",
  "Script alert",
  "Script services.js executed successfully."
);

// --- Cache (Redis) ---
services.cacheSet("demo-key", "demo-value");
const cached = services.cacheGet("demo-key");
console.log("Cache lookup:", cached);  // "demo-value"
services.cacheEvict("demo-key");

// Return combined result
JSON.stringify({
  sheetOk: sheet.ok,
  insertOk: insertResult.ok,
  cached: cached
});
