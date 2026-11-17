package kg.fastfood.crm.service;

import kg.fastfood.crm.entity.*;
import kg.fastfood.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Генератор и менеджер демонстрационного набора данных (~400 заказов за 21 день).
 * Все демонстрационные записи помечаются флагом {@code is_demo = true} и не смешиваются
 * с реальными данными предприятия.
 */
@Service
public class DemoDataService {

    public record DemoStats(long orders, long products, long ingredients, long expenses,
                            long employees, long shifts,
                            long realOrders, long realProducts, long realIngredients,
                            long realExpenses, long realEmployees) {
        public boolean hasDemoData() {
            return orders > 0 || products > 0 || ingredients > 0 || expenses > 0 || employees > 0;
        }
    }

    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final ForecastRepository forecastRepository;
    private final SettingsService settingsService;

    public DemoDataService(IngredientRepository ingredientRepository,
                           ProductRepository productRepository,
                           ProductIngredientRepository productIngredientRepository,
                           OrderRepository orderRepository,
                           ExpenseRepository expenseRepository,
                           EmployeeRepository employeeRepository,
                           ShiftRepository shiftRepository,
                           ForecastRepository forecastRepository,
                           SettingsService settingsService) {
        this.ingredientRepository = ingredientRepository;
        this.productRepository = productRepository;
        this.productIngredientRepository = productIngredientRepository;
        this.orderRepository = orderRepository;
        this.expenseRepository = expenseRepository;
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.forecastRepository = forecastRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public DemoStats stats() {
        return new DemoStats(
                orderRepository.countByDemo(true),
                productRepository.countByDemo(true),
                ingredientRepository.countByDemo(true),
                expenseRepository.countByDemo(true),
                employeeRepository.countByDemo(true),
                shiftRepository.findForPeriod(true, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1)).size(),
                orderRepository.countByDemo(false),
                productRepository.countByDemo(false),
                ingredientRepository.countByDemo(false),
                expenseRepository.countByDemo(false),
                employeeRepository.countByDemo(false)
        );
    }

    /** Полностью удаляет все демонстрационные записи. Реальные данные не затрагиваются. */
    @Transactional
    public void clearDemoData() {
        forecastRepository.deleteDemo();
        shiftRepository.deleteDemo();
        employeeRepository.deleteDemo();
        expenseRepository.deleteDemo();
        orderRepository.deleteDemoItems();
        orderRepository.deleteDemo();
        productIngredientRepository.deleteDemo();
        productRepository.deleteDemo();
        ingredientRepository.deleteDemo();
        if (settingsService.isDemoMode()) {
            settingsService.setDemoMode(false);
        }
    }

    /** Загружает демонстрационный набор, если он ещё не загружен. */
    @Transactional
    public void loadIfEmpty() {
        if (orderRepository.countByDemo(true) > 0 || productRepository.countByDemo(true) > 0) {
            return;
        }
        seedDemoData();
        settingsService.setDemoMode(true);
    }

    /** Перезагружает демонстрационный набор с нуля. */
    @Transactional
    public void reloadDemoData() {
        clearDemoData();
        seedDemoData();
        settingsService.setDemoMode(true);
    }

    private void seedDemoData() {
        // 1. Ингредиенты (цены в сомах)
        Map<String, Ingredient> ing = new LinkedHashMap<>();
        ing.put("Булочка бриошь", saveIng("Булочка бриошь", "шт", "18.00"));
        ing.put("Говяжья котлета", saveIng("Говяжья котлета", "шт", "78.00"));
        ing.put("Куриное филе", saveIng("Куриное филе", "кг", "340.00"));
        ing.put("Говядина для шаурмы", saveIng("Говядина для шаурмы", "кг", "640.00"));
        ing.put("Лаваш", saveIng("Лаваш", "шт", "16.00"));
        ing.put("Сыр чеддер", saveIng("Сыр чеддер", "кг", "720.00"));
        ing.put("Сосиска говяжья", saveIng("Сосиска говяжья", "шт", "38.00"));
        ing.put("Булочка хот-дог", saveIng("Булочка хот-дог", "шт", "14.00"));
        ing.put("Картофель фри (полуфабрикат)", saveIng("Картофель фри (полуфабрикат)", "кг", "190.00"));
        ing.put("Наггетсы куриные", saveIng("Наггетсы куриные", "шт", "16.00"));
        ing.put("Овощи свежие (салат, томат, огурец)", saveIng("Овощи свежие (салат, томат, огурец)", "кг", "160.00"));
        ing.put("Соус фирменный", saveIng("Соус фирменный", "л", "320.00"));
        ing.put("Масло фритюрное", saveIng("Масло фритюрное", "л", "180.00"));
        ing.put("Кола 0.5 л (закупка)", saveIng("Кола 0.5 л (закупка)", "шт", "42.00"));
        ing.put("Айран 0.5 л (закупка)", saveIng("Айран 0.5 л (закупка)", "шт", "36.00"));
        ing.put("Сироп и лимон для лимонада", saveIng("Сироп и лимон для лимонада", "л", "95.00"));
        ing.put("Упаковка (коробка/стакан/пакет)", saveIng("Упаковка (коробка/стакан/пакет)", "шт", "8.00"));

        // 2. Товары и технологические карты
        Product burger = saveProduct("Бургер Классик", "Бургеры", "220.00", Map.of(
                ing.get("Булочка бриошь"), "1.0",
                ing.get("Говяжья котлета"), "1.0",
                ing.get("Сыр чеддер"), "0.025",
                ing.get("Овощи свежие (салат, томат, огурец)"), "0.06",
                ing.get("Соус фирменный"), "0.03",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product doubleBurger = saveProduct("Двойной чизбургер", "Бургеры", "310.00", Map.of(
                ing.get("Булочка бриошь"), "1.0",
                ing.get("Говяжья котлета"), "2.0",
                ing.get("Сыр чеддер"), "0.045",
                ing.get("Овощи свежие (салат, томат, огурец)"), "0.06",
                ing.get("Соус фирменный"), "0.04",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product shawarmaChicken = saveProduct("Шаурма куриная", "Шаурма", "210.00", Map.of(
                ing.get("Лаваш"), "1.0",
                ing.get("Куриное филе"), "0.14",
                ing.get("Овощи свежие (салат, томат, огурец)"), "0.10",
                ing.get("Соус фирменный"), "0.04",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product shawarmaBeef = saveProduct("Шаурма говяжья", "Шаурма", "260.00", Map.of(
                ing.get("Лаваш"), "1.0",
                ing.get("Говядина для шаурмы"), "0.13",
                ing.get("Овощи свежие (салат, томат, огурец)"), "0.10",
                ing.get("Соус фирменный"), "0.04",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product hotDog = saveProduct("Хот-дог фирменный", "Закуски", "150.00", Map.of(
                ing.get("Булочка хот-дог"), "1.0",
                ing.get("Сосиска говяжья"), "1.0",
                ing.get("Овощи свежие (салат, томат, огурец)"), "0.04",
                ing.get("Соус фирменный"), "0.025",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product fries = saveProduct("Картофель фри", "Закуски", "130.00", Map.of(
                ing.get("Картофель фри (полуфабрикат)"), "0.16",
                ing.get("Масло фритюрное"), "0.02",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product nuggets = saveProduct("Наггетсы 6 шт", "Закуски", "175.00", Map.of(
                ing.get("Наггетсы куриные"), "6.0",
                ing.get("Масло фритюрное"), "0.02",
                ing.get("Соус фирменный"), "0.03",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product cola = saveProduct("Кола 0.5 л", "Напитки", "85.00", Map.of(
                ing.get("Кола 0.5 л (закупка)"), "1.0"
        ));
        Product ayran = saveProduct("Айран 0.5 л", "Напитки", "70.00", Map.of(
                ing.get("Айран 0.5 л (закупка)"), "1.0"
        ));
        Product lemonade = saveProduct("Лимонад домашний", "Напитки", "95.00", Map.of(
                ing.get("Сироп и лимон для лимонада"), "0.25",
                ing.get("Упаковка (коробка/стакан/пакет)"), "1.0"
        ));
        Product combo = saveProduct("Комбо Бургер + Фри + Кола", "Комбо", "395.00", Map.of(
                ing.get("Булочка бриошь"), "1.0",
                ing.get("Говяжья котлета"), "1.0",
                ing.get("Сыр чеддер"), "0.025",
                ing.get("Картофель фри (полуфабрикат)"), "0.15",
                ing.get("Кола 0.5 л (закупка)"), "1.0",
                ing.get("Соус фирменный"), "0.03",
                ing.get("Упаковка (коробка/стакан/пакет)"), "2.0"
        ));

        // 3. Персонал и смены (за последние 21 день)
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(20);

        List<Employee> staff = List.of(
                saveEmp("Асанов Тимур", "Старший повар", "+996 555 102 340", "220.00", start.minusMonths(8)),
                saveEmp("Токтосунова Айгерим", "Кассир", "+996 700 412 890", "180.00", start.minusMonths(5)),
                saveEmp("Исаков Бекзат", "Повар", "+996 550 871 223", "195.00", start.minusMonths(4)),
                saveEmp("Маматова Сабина", "Кассир-сборщик", "+996 772 309 114", "175.00", start.minusMonths(3)),
                saveEmp("Кадыров Нурлан", "Повар-универсал", "+996 709 661 502", "190.00", start.minusMonths(2)),
                saveEmp("Жумабекова Алина", "Сборщик заказов", "+996 551 904 771", "170.00", start.minusMonths(1))
        );

        List<Shift> shiftsToSave = new ArrayList<>();
        for (int d = 0; d <= 20; d++) {
            LocalDate date = start.plusDays(d);
            boolean weekend = date.getDayOfWeek() == DayOfWeek.FRIDAY
                    || date.getDayOfWeek() == DayOfWeek.SATURDAY
                    || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            // Утренняя смена 09:00 - 16:00 (2 человека)
            shiftsToSave.add(buildShift(staff.get(d % 2 == 0 ? 0 : 2), date, LocalTime.of(9, 0), LocalTime.of(16, 0)));
            shiftsToSave.add(buildShift(staff.get(d % 2 == 0 ? 1 : 3), date, LocalTime.of(9, 0), LocalTime.of(16, 0)));
            // Вечерняя смена 16:00 - 23:00 (2 человека в будни, 3 в выходные)
            shiftsToSave.add(buildShift(staff.get(d % 2 == 0 ? 4 : 0), date, LocalTime.of(16, 0), LocalTime.of(23, 0)));
            shiftsToSave.add(buildShift(staff.get(d % 2 == 0 ? 3 : 1), date, LocalTime.of(16, 0), LocalTime.of(23, 0)));
            if (weekend) {
                shiftsToSave.add(buildShift(staff.get(5), date, LocalTime.of(12, 0), LocalTime.of(21, 0)));
            }
        }
        shiftRepository.saveAll(shiftsToSave);

        // 4. Заказы (~400 заказов за 21 день с пиками в обед 12-14 и вечером 18-21)
        Random rnd = new Random(42L);
        List<Product> mainDishes = List.of(burger, doubleBurger, shawarmaChicken, shawarmaBeef, hotDog, combo);
        int[] mainWeights = {24, 12, 28, 16, 10, 18};
        List<Product> sides = List.of(fries, nuggets);
        List<Product> drinks = List.of(cola, ayran, lemonade);
        // Вероятности по часам 9..22 (всего 14 рабочих часов)
        int[] hourWeights = {3, 5, 8, 14, 16, 11, 7, 6, 8, 12, 16, 15, 9, 4};

        List<Order> orders = new ArrayList<>();
        DateTimeFormatter numFmt = DateTimeFormatter.ofPattern("yyMMdd");
        for (int d = 0; d <= 20; d++) {
            LocalDate date = start.plusDays(d);
            DayOfWeek dow = date.getDayOfWeek();
            int baseOrders = switch (dow) {
                case FRIDAY -> 22;
                case SATURDAY -> 25;
                case SUNDAY -> 23;
                case MONDAY -> 15;
                default -> 18;
            };
            int dayOrders = baseOrders + rnd.nextInt(5) - 2; // итого ~400 заказов за 21 день

            List<LocalTime> times = new ArrayList<>();
            for (int i = 0; i < dayOrders; i++) {
                int hour = 9 + pickWeighted(rnd, hourWeights);
                int minute = rnd.nextInt(60);
                times.add(LocalTime.of(hour, minute));
            }
            times.sort(Comparator.naturalOrder());

            for (int i = 0; i < times.size(); i++) {
                Order order = new Order();
                order.setDemo(true);
                order.setOrderNumber("D-" + date.format(numFmt) + "-" + String.format("%03d", i + 1));
                order.setOrderDate(date);
                order.setOrderTime(times.get(i));
                if (i % 9 == 0) {
                    order.setComment("На вынос");
                } else if (i % 15 == 0) {
                    order.setComment("Доставка");
                }

                // Основное блюдо
                Product main = mainDishes.get(pickWeighted(rnd, mainWeights));
                int mainQty = rnd.nextInt(100) < 18 ? 2 : 1;
                addOrderLine(order, main, mainQty);

                // Закуска (если не комбо)
                if (main != combo && rnd.nextInt(100) < 42) {
                    Product side = sides.get(rnd.nextInt(sides.size()));
                    addOrderLine(order, side, 1);
                }

                // Напиток (если не комбо)
                if (main != combo && rnd.nextInt(100) < 62) {
                    Product drink = drinks.get(rnd.nextInt(drinks.size()));
                    addOrderLine(order, drink, mainQty > 1 && rnd.nextBoolean() ? 2 : 1);
                }

                order.recalculateTotal();
                orders.add(order);
            }
        }
        orderRepository.saveAll(orders);

        // 5. Расходы предприятия за 21 день
        List<Expense> expenses = new ArrayList<>();
        expenses.add(buildExpense("Аренда помещения (за 3 недели)", ExpenseCategory.RENT, "38000.00", start, ExpenseType.FIXED, "Постоянный арендный платёж"));
        expenses.add(buildExpense("Оклад и смены персонала (первая половина)", ExpenseCategory.SALARY, "28000.00", start.plusDays(6), ExpenseType.FIXED, "Выплата персоналу"));
        expenses.add(buildExpense("Оклад и смены персонала (вторая половина)", ExpenseCategory.SALARY, "29500.00", start.plusDays(19), ExpenseType.FIXED, "Выплата персоналу"));
        expenses.add(buildExpense("Электроэнергия и вода", ExpenseCategory.UTILITIES, "7400.00", start.plusDays(10), ExpenseType.VARIABLE, "Коммунальные начисления по счётчикам"));
        expenses.add(buildExpense("Интернет и облачная касса", ExpenseCategory.INTERNET, "1800.00", start.plusDays(2), ExpenseType.FIXED, "Тариф + ОФД"));
        expenses.add(buildExpense("Хозтовары и уборка", ExpenseCategory.OTHER, "2600.00", start.plusDays(12), ExpenseType.VARIABLE, "Моющие средства, салфетки"));
        // Закупки продуктов — отображаются отдельно, чтобы не было двойного списания с COGS
        expenses.add(buildExpense("Закупка мяса, лавашей и овощей (неделя 1)", ExpenseCategory.FOOD_PURCHASE, "24500.00", start.plusDays(1), ExpenseType.VARIABLE, "Накладная №101"));
        expenses.add(buildExpense("Закупка напитков и соусов (неделя 2)", ExpenseCategory.FOOD_PURCHASE, "21800.00", start.plusDays(8), ExpenseType.VARIABLE, "Накладная №108"));
        expenses.add(buildExpense("Закупка продуктов (неделя 3)", ExpenseCategory.FOOD_PURCHASE, "23100.00", start.plusDays(15), ExpenseType.VARIABLE, "Накладная №115"));
        expenseRepository.saveAll(expenses);

        // 6. Пара сохранённых прогнозов на прошлые дни, чтобы в таблице истории уже была проверка факт vs прогноз
        Forecast f1 = new Forecast();
        f1.setDemo(true);
        f1.setMethod("Скользящее среднее");
        f1.setWindowSize(7);
        f1.setTargetDate(end.minusDays(2));
        f1.setForecastValue(new BigDecimal("6950.00"));
        f1.setMae(new BigDecimal("740.00"));
        forecastRepository.save(f1);

        Forecast f2 = new Forecast();
        f2.setDemo(true);
        f2.setMethod("Скользящее среднее");
        f2.setWindowSize(7);
        f2.setTargetDate(end);
        f2.setForecastValue(new BigDecimal("7120.00"));
        f2.setMae(new BigDecimal("715.00"));
        forecastRepository.save(f2);
    }

    private Ingredient saveIng(String name, String unit, String cost) {
        Ingredient i = new Ingredient();
        i.setName(name);
        i.setUnit(unit);
        i.setUnitCost(new BigDecimal(cost));
        i.setDemo(true);
        return ingredientRepository.save(i);
    }

    private Product saveProduct(String name, String category, String price, Map<Ingredient, String> recipe) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setPrice(new BigDecimal(price));
        p.setActive(true);
        p.setDemo(true);
        for (Map.Entry<Ingredient, String> entry : recipe.entrySet()) {
            ProductIngredient pi = new ProductIngredient();
            pi.setProduct(p);
            pi.setIngredient(entry.getKey());
            pi.setQuantity(new BigDecimal(entry.getValue()));
            p.getIngredients().add(pi);
        }
        return productRepository.save(p);
    }

    private Employee saveEmp(String fullName, String position, String phone, String rate, LocalDate hireDate) {
        Employee e = new Employee();
        e.setFullName(fullName);
        e.setPosition(position);
        e.setPhone(phone);
        e.setHourlyRate(new BigDecimal(rate));
        e.setHireDate(hireDate);
        e.setActive(true);
        e.setDemo(true);
        return employeeRepository.save(e);
    }

    private Shift buildShift(Employee e, LocalDate date, LocalTime start, LocalTime end) {
        Shift s = new Shift();
        s.setEmployee(e);
        s.setShiftDate(date);
        s.setStartTime(start);
        s.setEndTime(end);
        s.setWorkedHours(s.getPlannedHours());
        s.setDemo(true);
        return s;
    }

    private void addOrderLine(Order order, Product product, int qty) {
        for (OrderItem existing : order.getItems()) {
            if (existing.getProduct().getId().equals(product.getId())) {
                existing.setQuantity(existing.getQuantity() + qty);
                return;
            }
        }
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(qty);
        item.setUnitPrice(product.getPrice());
        order.addItem(item);
    }

    private Expense buildExpense(String name, ExpenseCategory category, String amount,
                                 LocalDate date, ExpenseType type, String description) {
        Expense e = new Expense();
        e.setName(name);
        e.setCategory(category);
        e.setAmount(new BigDecimal(amount));
        e.setExpenseDate(date);
        e.setType(type);
        e.setDescription(description);
        e.setDemo(true);
        return e;
    }

    private int pickWeighted(Random rnd, int[] weights) {
        int sum = 0;
        for (int w : weights) {
            sum += w;
        }
        int r = rnd.nextInt(sum);
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (r < acc) {
                return i;
            }
        }
        return 0;
    }
}
