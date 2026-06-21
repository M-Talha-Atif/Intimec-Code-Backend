// Groovy Script
// This script generates a department code based on the user's given name.

def givenName = sourceAttributes.get('GIVEN_NAME')

def resultString = "DEPT_${givenName.toUpperCase()}"

// The last expression is the return value in Groovy
return resultString.toString()