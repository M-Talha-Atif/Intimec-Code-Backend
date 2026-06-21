-- Set passwords for newly created users
-- Password: SecurePass123! (BCrypt hash)
-- This hash is for "SecurePass123!" with BCrypt

-- BCrypt hash for "SecurePass123!"
-- You can generate this with: https://bcrypt-generator.com/ or use Spring's BCryptPasswordEncoder

UPDATE users 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqcLaKU5K8k8F7YKdpgBZQBIaZFpy'
WHERE email IN (
    'ceo@intimec.com',
    'cto@intimec.com',
    'eng.manager@intimec.com',
    'qa.manager@intimec.com',
    'developer1@intimec.com',
    'developer2@intimec.com',
    'qa.engineer1@intimec.com',
    'qa.engineer2@intimec.com'
);

-- Verify the update
SELECT email, username, 
       CASE WHEN password_hash IS NOT NULL THEN 'SET' ELSE 'NULL' END as password_status
FROM users 
WHERE email IN (
    'ceo@intimec.com',
    'cto@intimec.com',
    'eng.manager@intimec.com',
    'qa.manager@intimec.com',
    'developer1@intimec.com',
    'developer2@intimec.com',
    'qa.engineer1@intimec.com',
    'qa.engineer2@intimec.com'
);
