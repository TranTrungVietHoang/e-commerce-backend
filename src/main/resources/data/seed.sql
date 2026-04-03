-- Bước 1: Thêm user admin
INSERT INTO users (email, password_hash, full_name, phone, status, created_at, updated_at)
VALUES (
  'admin@demo.com',
  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKPGaHzZSMeEyMGahTTECBlkZe6a',  -- mật khẩu là: Admin@123
  'Admin System',
  '0999999991',
  'ACTIVE',
  GETDATE(),
  GETDATE()
);

-- Bước 2: Lấy ID vừa tạo và gán role ADMIN  
DECLARE @userId BIGINT = SCOPE_IDENTITY();
DECLARE @adminRoleId INT;
SELECT @adminRoleId = id FROM roles WHERE name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
VALUES (@userId, @adminRoleId);
