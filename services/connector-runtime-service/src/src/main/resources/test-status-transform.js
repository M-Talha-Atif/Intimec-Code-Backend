// 'sourceAttributes' is a Java Map, which requires using the .get() method.

(() => {
    // Correctly read values from the Java Map
    const givenName = sourceAttributes.get('GIVEN_NAME');
    const surname = sourceAttributes.get('SURNAME');
    const status = sourceAttributes.get('account_status');

    // Now perform the logic
    const fullName = `${givenName} ${surname}`;
    const isActive = status === 'active';

    // Return a new object. This will be the value of the 'derivedAttributes' field.
    return {
        fullName: fullName,
        active: isActive,
        sourceSystem: 'csv-test'
    };
})();