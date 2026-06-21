// This script's only job is to create the composed principal name.
// It receives all source attributes in the 'sourceAttributes' binding.
(() => {
    // Read the required values from the Java Map using the .get() method.
    // We use the original attribute names from the source system.
    const uid = sourceAttributes.get('__UID__');
    const email = sourceAttributes.get('EMAIL');

    // The script's final return value becomes the value of the target attribute.
    return `${uid}@${email}`;
})();