-- ======================================================================
-- CRM Analytics for Fast Food — эталонная схема базы данных MySQL 8
-- ======================================================================

CREATE DATABASE IF NOT EXISTS fastfood_crm
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE fastfood_crm;

-- 1. Роли пользователей
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    description VARCHAR(100) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Пользователи системы
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    enabled BIT(1) NOT NULL DEFAULT b'1',
    role_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Настройки приложения (ключ - значение)
CREATE TABLE IF NOT EXISTS app_settings (
    setting_key VARCHAR(50) NOT NULL,
    setting_value VARCHAR(255) NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Ингредиенты
CREATE TABLE IF NOT EXISTS ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    unit_cost DECIMAL(12, 4) NOT NULL,
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ingredients_name_demo (name, is_demo),
    KEY idx_ingredients_demo (is_demo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Товары (меню)
CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NULL,
    price DECIMAL(12, 2) NOT NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_name_demo (name, is_demo),
    KEY idx_products_demo (is_demo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Состав товаров (технологические карты)
CREATE TABLE IF NOT EXISTS product_ingredients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    quantity DECIMAL(12, 4) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_ingredient (product_id, ingredient_id),
    CONSTRAINT fk_pi_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT fk_pi_ingredient FOREIGN KEY (ingredient_id) REFERENCES ingredients (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Заказы
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_number VARCHAR(40) NOT NULL,
    order_date DATE NOT NULL,
    order_time TIME(6) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    comment VARCHAR(255) NULL,
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number_demo (order_number, is_demo),
    KEY idx_orders_date (order_date),
    KEY idx_orders_demo_date (is_demo, order_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Состав заказов
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    KEY idx_order_items_product (product_id),
    CONSTRAINT fk_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_items_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Расходы предприятия
CREATE TABLE IF NOT EXISTS expenses (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    expense_date DATE NOT NULL,
    expense_type VARCHAR(10) NOT NULL,
    description VARCHAR(255) NULL,
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_expenses_date (expense_date),
    KEY idx_expenses_demo_date (is_demo, expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Сотрудники
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(100) NOT NULL,
    position VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NULL,
    hourly_rate DECIMAL(10, 2) NULL,
    hire_date DATE NULL,
    active BIT(1) NOT NULL DEFAULT b'1',
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_employees_demo (is_demo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. Рабочие смены
CREATE TABLE IF NOT EXISTS shifts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    shift_date DATE NOT NULL,
    start_time TIME(6) NOT NULL,
    end_time TIME(6) NOT NULL,
    worked_hours DECIMAL(5, 2) NOT NULL,
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shift_employee_date_start (employee_id, shift_date, start_time),
    KEY idx_shifts_demo_date (is_demo, shift_date),
    CONSTRAINT fk_shifts_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. Сохранённые прогнозы выручки
CREATE TABLE IF NOT EXISTS forecasts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    method VARCHAR(50) NOT NULL,
    window_size INT NOT NULL,
    target_date DATE NOT NULL,
    forecast_value DECIMAL(14, 2) NOT NULL,
    mae DECIMAL(14, 2) NULL,
    is_demo BIT(1) NOT NULL DEFAULT b'0',
    PRIMARY KEY (id),
    KEY idx_forecasts_demo_target (is_demo, target_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Базовые роли (администратор создаётся автоматически при запуске Spring Boot)
INSERT IGNORE INTO roles (name, description) VALUES
    ('ADMIN', 'Администратор системы'),
    ('MANAGER', 'Руководитель / менеджер');
