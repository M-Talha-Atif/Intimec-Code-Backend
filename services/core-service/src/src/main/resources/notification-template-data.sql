-- Insert sample template for password reset notifications
INSERT INTO template_manager (
    id, template_name, template_subject, email_body, trigger_type, 
    is_enabled, tenant_id, created_at, updated_at, created_by_id, created_by_name
) VALUES (
    gen_random_uuid(),
    'Password Reset Notification',
    'Password Successfully Reset - aidentx',
    '<html><body><h2>Password Reset Successful</h2><p>Hello $username,</p><p>Your password has been successfully reset for your account: <strong>$email</strong></p><p>If you did not request this change, please contact our support team immediately.</p><p>Best regards,<br>aidentx Team</p></body></html>',
    'PASSWORD_RESET',
    true,
    'aidentx-tenant-1',
    NOW(),
    NOW(),
    'system',
    'system'
) ON CONFLICT DO NOTHING;

-- Insert sample email configuration if it doesn't exist
INSERT INTO email_configuration (
    id, email_address, username, password, smtp_hostname, smtp_port, 
    encryption_type, tenant_id, created_at, updated_at, created_by_id, created_by_name
) VALUES (
    gen_random_uuid(),
    'info.aidentx@gmail.com',
    'info.intimec@gmail.com',
    'oaef ycjg erye qtyd',
    'smtp.gmail.com',
    587,
    'STARTTLS',
    'aidentx-tenant-1',
    NOW(),
    NOW(),
    'system',
    'system'
) ON CONFLICT DO NOTHING;
