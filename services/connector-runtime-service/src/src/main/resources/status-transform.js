// This script runs in the GraalJS engine.
// The 'sourceAttributes' Java Map is automatically exposed as a JS object.

// Use modern JS syntax (const/let) and direct property access.
const accountStatus = sourceAttributes.account_status;

// The last evaluated expression in the script is its return value.
// This will return a boolean (true or false) back to the Java code.
accountStatus === 'active';