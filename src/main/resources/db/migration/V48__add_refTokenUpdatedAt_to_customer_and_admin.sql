-- customer 테이블에 refreshToken 재발급 시간 컬럼 추가 (컬럼이 없을 경우에만)
SET @dbname = DATABASE();
SET @tablename = 'customer';
SET @columnname = 'refTokenUpdatedAt';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1', -- 컬럼이 이미 존재하면 아무것도 하지 않음
  CONCAT('ALTER TABLE ', @dbname, '.', @tablename, ' ADD COLUMN `', @columnname, '` DATETIME NULL AFTER `refToken`;')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- admin 테이블에 refreshToken 재발급 시간 컬럼 추가 (컬럼이 없을 경우에만)
SET @dbname = DATABASE();
SET @tablename = 'admin';
SET @columnname = 'refTokenUpdatedAt';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_name = @tablename)
      AND (table_schema = @dbname)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1', -- 컬럼이 이미 존재하면 아무것도 하지 않음
  CONCAT('ALTER TABLE ', @dbname, '.', @tablename, ' ADD COLUMN `', @columnname, '` DATETIME NULL AFTER `refToken`;')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
