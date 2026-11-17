# Архитектура системы CRM Analytics for Fast Food

## 1. Общая архитектурная модель

Приложение построено по классической многослойной архитектуре Spring Boot (MVC):

```text
Browser (Bootstrap + Chart.js)
        │  HTTP / HTML (Thymeleaf)
        ▼
  controller/   — обработка HTTP-запросов, валидация форм, передача моделей в UI
        │
        ▼
   service/     — бизнес-логика, транзакции, подготовка снимков данных (SalesSnapshot)
     │     │
     │     └──▶ math/ — чистые математические и вероятностные расчёты (stateless Java-классы)
     ▼
  repository/   — Spring Data JPA интерфейсы и оптимизированные JPQL-запросы
        │
        ▼
   entity/      — JPA-сущности, связи, индексы и ограничения целостности
        │
        ▼
   MySQL 8 / H2 — реляционное хранилище данных
```

Принципы проектирования:
- В контроллерах (`controller`) нет сложных вычислений и прямых обращений к БД.
- Вся математика вынесена в отдельный пакет `kg.fastfood.crm.math` — чистые функции без привязки к Spring и БД, что делает их простыми для понимания и модульного тестирования.
- Сервисы (`service`) формируют единый снимок периода `SalesSnapshot` и передают исходные массивы в математические классы.

---

## 2. Структура пакетов (`src/main/java/kg/fastfood/crm`)

| Пакет | Назначение |
|---|---|
| `config` | `SecurityConfig` (BCrypt + защита маршрутов), `DataInitializer` (первичная инициализация ролей, администратора и демо-данных), `GlobalModelAdvice`, `FormatUtils` (`@fmt`) |
| `controller` | 11 контроллеров: `AuthController`, `DashboardController`, `OrderController`, `ProductController`, `IngredientController`, `CostingController`, `ExpenseController`, `StaffController`, `AnalyticsController`, `FinanceController`, `SettingsController` |
| `dto` | Неизменяемые `record`-результаты (`AnalyticsResults`, `FinancialSummary`, `FinanceRow`, `OrderRow`, `ProductRow`, `ProductSales`, `PeriodRange`) и формы с Bean Validation (`OrderForm`, `ProductForm`, `IngredientForm`, `ExpenseForm`, `EmployeeForm`, `ShiftForm`, `ScheduleForm`, `SettingsForm`, `UserForm`, `PasswordForm`) |
| `entity` | 12 JPA-сущностей и 2 перечисления (`User`, `Role`, `AppSetting`, `Order`, `OrderItem`, `Product`, `Ingredient`, `ProductIngredient`, `Expense`, `ExpenseCategory`, `ExpenseType`, `Employee`, `Shift`, `Forecast`) |
| `math` | 7 математических классов (`DescriptiveStatistics`, `PoissonDistribution`, `MovingAverageForecaster`, `CostCalculator`, `BreakEvenCalculator`, `ForecastAccuracy`, `StaffProductivity`) |
| `repository` | Spring Data JPA репозитории для всех таблиц, включая `left join fetch` для предотвращения проблемы N+1 |
| `service` | Бизнес-логика: `OrderService`, `ProductService`, `IngredientService`, `ExpenseService`, `StaffService`, `FinanceService`, `AnalyticsService`, `PeriodService`, `SettingsService`, `UserService`, `DemoDataService`, `CsvImportService` |

---

## 3. Структура базы данных

Эталонный DDL-скрипт находится в [`sql/schema.sql`](../sql/schema.sql). Основные таблицы:

1. `roles` — роли пользователей (`ADMIN`, `MANAGER`).
2. `users` — учётные записи пользователей (пароли хранятся в виде BCrypt-хеша).
3. `app_settings` — настройки предприятия (`business_name`, `open_hour`, `close_hour`, `data_mode`).
4. `ingredients` — ингредиенты (единица измерения, стоимость единицы).
5. `products` — товары меню (цена продажи, категория, флаг активности).
6. `product_ingredients` — технологические карты (связь `products` ↔ `ingredients` с нормой расхода `quantity`).
7. `orders` — заказы (уникальный номер чека в рамках набора данных, дата, время, итоговая сумма).
8. `order_items` — позиции заказов (товар, количество, цена на момент продажи).
9. `expenses` — расходы предприятия (категория, тип `FIXED`/`VARIABLE`, сумма, дата).
10. `employees` — сотрудники предприятия.
11. `shifts` — рабочие смены сотрудников (дата, время начала/окончания, отработанные часы).
12. `forecasts` — сохранённые прогнозы выручки (метод, окно, целевая дата, прогноз, MAE).

---

## 4. Изоляция демонстрационных и реальных данных

Для соблюдения требования «не смешивать демонстрационные и реальные данные» во всех операционных таблицах используется булево поле `is_demo`:
- В режиме **DEMO** (`SettingsService.isDemoMode() == true`) запросы фильтруют только записи с `is_demo = true`.
- В режиме **REAL** отображаются и анализируются только реальные данные (`is_demo = false`).
- Кнопка «Очистить демо-данные» в разделе «Настройки» удаляет в правильном порядке внешних ключей только записи с `is_demo = true`, не затрагивая реальные данные предприятия.

---

## 5. Безопасность и надёжность

- Аутентификация через Spring Security 6 с хешированием паролей `BCryptPasswordEncoder`.
- Страницы управления пользователями (`/settings/users/**`) доступны только пользователям с ролью `ADMIN`.
- Валидация всех пользовательских форм (`@NotBlank`, `@DecimalMin`, `@Min`, `@PastOrPresent`), защита от деления на ноль и некорректных значений в сервисах и математических классах.
- При входе в `Orders` или `Expenses` применяется защита от двойного учёта: категория расходов `FOOD_PURCHASE` («Закупка продуктов») исключается из операционных расходов `OE`, поскольку себестоимость сырья учитывается в `COGS`.
