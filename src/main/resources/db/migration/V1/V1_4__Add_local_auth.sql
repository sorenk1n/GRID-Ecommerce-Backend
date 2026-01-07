CREATE TABLE IF NOT EXISTS `GridDB`.`user_credentials` (
    `users_id` VARCHAR(255) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`users_id`),
    CONSTRAINT `fk_user_credentials_users1`
        FOREIGN KEY (`users_id`)
        REFERENCES `GridDB`.`users` (`id`)
        ON DELETE CASCADE
) ENGINE = InnoDB;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = 'GridDB'
      AND table_name = 'users'
      AND index_name = 'username_UNIQUE'
);
SET @sql := IF(
    @idx_exists = 0,
    'ALTER TABLE `GridDB`.`users` ADD UNIQUE INDEX `username_UNIQUE` (`username` ASC) VISIBLE',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

