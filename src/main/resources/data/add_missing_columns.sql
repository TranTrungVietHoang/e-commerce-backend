-- Script to add missing columns to existing tables
-- This runs before Hibernate initialization

-- 1. Add updated_at to cart_items if missing
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[cart_items]') AND name = 'updated_at')
    ALTER TABLE [cart_items] ADD [updated_at] datetime2(7) NOT NULL DEFAULT GETDATE();

-- 2. Ensure created_at also exists (just in case)
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID(N'[dbo].[cart_items]') AND name = 'created_at')
    ALTER TABLE [cart_items] ADD [created_at] datetime2(7) NOT NULL DEFAULT GETDATE();
