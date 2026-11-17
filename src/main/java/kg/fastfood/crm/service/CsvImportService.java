package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.ImportResult;
import kg.fastfood.crm.entity.*;
import kg.fastfood.crm.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Импорт операционных данных из CSV-файлов (UTF-8, разделитель «;» или «,»).
 * Выполняет проверку формата, обязательных полей и защиту от дублирования записей.
 */
@Service
public class CsvImportService {

    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("d.M.yyyy")
    };
    private static final DateTimeFormatter[] TIME_FORMATS = {
            DateTimeFormatter.ofPattern("H:mm"),
            DateTimeFormatter.ofPattern("HH:mm"),
            DateTimeFormatter.ofPattern("H:mm:ss"),
            DateTimeFormatter.ofPattern("HH:mm:ss")
    };

    private final IngredientRepository ingredientRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SettingsService settingsService;

    public CsvImportService(IngredientRepository ingredientRepository,
                            ProductRepository productRepository,
                            OrderRepository orderRepository,
                            ExpenseRepository expenseRepository,
                            EmployeeRepository employeeRepository,
                            ShiftRepository shiftRepository,
                            SettingsService settingsService) {
        this.ingredientRepository = ingredientRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.expenseRepository = expenseRepository;
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.settingsService = settingsService;
    }

    @Transactional
    public ImportResult importFile(String type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            ImportResult res = new ImportResult(type);
            res.addError("Выберите непустой CSV-файл для загрузки");
            return res;
        }
        String original = file.getOriginalFilename();
        if (original != null && !original.toLowerCase(Locale.ROOT).endsWith(".csv")
                && !original.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            ImportResult res = new ImportResult(type);
            res.addError("Поддерживаются только файлы формата CSV (.csv)");
            return res;
        }

        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!lines.isEmpty() || !line.isBlank()) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            ImportResult res = new ImportResult(type);
            res.addError("Не удалось прочитать файл: " + e.getMessage());
            return res;
        }

        if (lines.isEmpty()) {
            ImportResult res = new ImportResult(type);
            res.addError("Файл не содержит данных");
            return res;
        }

        // Убираем BOM UTF-8, если он присутствует (часто добавляется Excel)
        if (lines.get(0).startsWith("\uFEFF")) {
            lines.set(0, lines.get(0).substring(1));
        }

        char delimiter = detectDelimiter(lines.get(0));
        boolean demo = settingsService.isDemoMode();

        return switch (type) {
            case "ingredients" -> importIngredients(lines, delimiter, demo);
            case "products" -> importProducts(lines, delimiter, demo);
            case "recipes" -> importRecipes(lines, delimiter, demo);
            case "orders" -> importOrders(lines, delimiter, demo);
            case "expenses" -> importExpenses(lines, delimiter, demo);
            case "employees" -> importEmployees(lines, delimiter, demo);
            case "shifts" -> importShifts(lines, delimiter, demo);
            default -> {
                ImportResult res = new ImportResult(type);
                res.addError("Неизвестный тип импорта: " + type);
                yield res;
            }
        };
    }

    /** Формат: name;unit;unit_cost */
    private ImportResult importIngredients(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Ингредиенты");
        int start = isHeader(lines.get(0), "name", "название", "ингредиент") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 3) {
                result.addError(lineNum, "ожидаются 3 колонки: название; единица; стоимость");
                continue;
            }
            try {
                String name = requireText(cols.get(0), "название");
                String unit = requireText(cols.get(1), "единица измерения");
                BigDecimal cost = parsePositiveDecimal(cols.get(2), "стоимость единицы");

                if (ingredientRepository.findByNameIgnoreCaseAndDemo(name, demo).isPresent()) {
                    result.addDuplicate();
                    continue;
                }
                Ingredient ing = new Ingredient();
                ing.setName(name);
                ing.setUnit(unit);
                ing.setUnitCost(cost);
                ing.setDemo(demo);
                ingredientRepository.save(ing);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /** Формат: name;category;price;active */
    private ImportResult importProducts(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Товары");
        int start = isHeader(lines.get(0), "name", "название", "товар") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 3) {
                result.addError(lineNum, "ожидаются минимум 3 колонки: название; категория; цена [; активен]");
                continue;
            }
            try {
                String name = requireText(cols.get(0), "название товара");
                String category = cols.get(1).isBlank() ? null : cols.get(1).trim();
                BigDecimal price = parsePositiveDecimal(cols.get(2), "цена");
                boolean active = cols.size() < 4 || cols.get(3).isBlank() || parseBoolean(cols.get(3));

                if (productRepository.findByNameIgnoreCaseAndDemo(name, demo).isPresent()) {
                    result.addDuplicate();
                    continue;
                }
                Product p = new Product();
                p.setName(name);
                p.setCategory(category);
                p.setPrice(price);
                p.setActive(active);
                p.setDemo(demo);
                productRepository.save(p);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /** Формат: product_name;ingredient_name;quantity */
    private ImportResult importRecipes(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Состав блюд (технологические карты)");
        int start = isHeader(lines.get(0), "product", "товар", "блюдо") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 3) {
                result.addError(lineNum, "ожидаются 3 колонки: товар; ингредиент; количество");
                continue;
            }
            try {
                String productName = requireText(cols.get(0), "товар");
                String ingredientName = requireText(cols.get(1), "ингредиент");
                BigDecimal qty = parsePositiveDecimal(cols.get(2), "количество");

                Product product = productRepository.findByNameIgnoreCaseAndDemo(productName, demo)
                        .orElseThrow(() -> new IllegalArgumentException("товар «" + productName + "» не найден"));
                Ingredient ingredient = ingredientRepository.findByNameIgnoreCaseAndDemo(ingredientName, demo)
                        .orElseThrow(() -> new IllegalArgumentException("ингредиент «" + ingredientName + "» не найден"));

                Product loaded = productRepository.findWithIngredientsById(product.getId()).orElse(product);
                boolean exists = loaded.getIngredients().stream()
                        .anyMatch(pi -> pi.getIngredient().getId().equals(ingredient.getId()));
                if (exists) {
                    result.addDuplicate();
                    continue;
                }
                ProductIngredient pi = new ProductIngredient();
                pi.setProduct(loaded);
                pi.setIngredient(ingredient);
                pi.setQuantity(qty);
                loaded.getIngredients().add(pi);
                productRepository.save(loaded);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /**
     * Формат: order_number;date;time;items;total_amount;comment
     * Где items — например "Бургер Классик x2, Кола 0.5 л x1" (или пусто, если задан total_amount).
     */
    private ImportResult importOrders(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Заказы");
        int start = isHeader(lines.get(0), "order_number", "номер", "чек", "date", "дата") ? 1 : 0;

        Map<String, Product> productCache = new HashMap<>();
        for (Product p : productRepository.findAllWithIngredients(demo)) {
            productCache.put(p.getName().toLowerCase(Locale.ROOT), p);
        }

        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 4) {
                result.addError(lineNum, "ожидаются колонки: номер; дата; время; состав_или_пусто; сумма [; комментарий]");
                continue;
            }
            try {
                String number = requireText(cols.get(0), "номер заказа");
                if (orderRepository.existsByOrderNumberAndDemo(number, demo)) {
                    result.addDuplicate();
                    continue;
                }
                LocalDate date = parseDate(cols.get(1));
                LocalTime time = parseTime(cols.get(2));
                String itemsCol = cols.get(3).trim();
                String totalCol = cols.size() >= 5 ? cols.get(4).trim() : "";
                String comment = cols.size() >= 6 && !cols.get(5).isBlank() ? cols.get(5).trim() : null;

                Order order = new Order();
                order.setDemo(demo);
                order.setOrderNumber(number);
                order.setOrderDate(date);
                order.setOrderTime(time);
                order.setComment(comment);

                if (!itemsCol.isBlank()) {
                    parseOrderItems(order, itemsCol, productCache);
                    order.recalculateTotal();
                } else {
                    BigDecimal manualTotal = parsePositiveDecimal(totalCol, "сумма заказа");
                    order.setTotalAmount(manualTotal);
                }
                orderRepository.save(order);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /** Формат: name;category;amount;date;type;description */
    private ImportResult importExpenses(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Расходы");
        int start = isHeader(lines.get(0), "name", "название", "расход") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 5) {
                result.addError(lineNum, "ожидаются минимум 5 колонок: название; категория; сумма; дата; тип [; описание]");
                continue;
            }
            try {
                String name = requireText(cols.get(0), "название расхода");
                ExpenseCategory category = parseExpenseCategory(cols.get(1));
                BigDecimal amount = parsePositiveDecimal(cols.get(2), "сумма");
                LocalDate date = parseDate(cols.get(3));
                ExpenseType expenseType = parseExpenseType(cols.get(4));
                String desc = cols.size() >= 6 && !cols.get(5).isBlank() ? cols.get(5).trim() : null;

                if (expenseRepository.existsByDemoAndNameIgnoreCaseAndExpenseDateAndAmount(demo, name, date, amount)) {
                    result.addDuplicate();
                    continue;
                }
                Expense expense = new Expense();
                expense.setDemo(demo);
                expense.setName(name);
                expense.setCategory(category);
                expense.setAmount(amount);
                expense.setExpenseDate(date);
                expense.setType(expenseType);
                expense.setDescription(desc);
                expenseRepository.save(expense);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /** Формат: full_name;position;phone;hourly_rate;hire_date */
    private ImportResult importEmployees(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Сотрудники");
        int start = isHeader(lines.get(0), "full_name", "name", "фио", "сотрудник") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 2) {
                result.addError(lineNum, "ожидаются минимум 2 колонки: ФИО; должность [; телефон; ставка; дата_приёма]");
                continue;
            }
            try {
                String fullName = requireText(cols.get(0), "ФИО");
                String position = requireText(cols.get(1), "должность");
                String phone = cols.size() >= 3 && !cols.get(2).isBlank() ? cols.get(2).trim() : null;
                BigDecimal rate = cols.size() >= 4 && !cols.get(3).isBlank()
                        ? parseNonNegativeDecimal(cols.get(3), "ставка") : null;
                LocalDate hireDate = cols.size() >= 5 && !cols.get(4).isBlank()
                        ? parseDate(cols.get(4)) : null;

                if (employeeRepository.findByFullNameIgnoreCaseAndDemo(fullName, demo).isPresent()) {
                    result.addDuplicate();
                    continue;
                }
                Employee e = new Employee();
                e.setDemo(demo);
                e.setFullName(fullName);
                e.setPosition(position);
                e.setPhone(phone);
                e.setHourlyRate(rate);
                e.setHireDate(hireDate);
                e.setActive(true);
                employeeRepository.save(e);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    /** Формат: employee_name;shift_date;start_time;end_time;worked_hours */
    private ImportResult importShifts(List<String> lines, char delimiter, boolean demo) {
        ImportResult result = new ImportResult("Рабочие смены");
        int start = isHeader(lines.get(0), "employee", "фио", "сотрудник") ? 1 : 0;
        for (int i = start; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw.isBlank()) continue;
            int lineNum = i + 1;
            List<String> cols = parseCsvLine(raw, delimiter);
            if (cols.size() < 4) {
                result.addError(lineNum, "ожидаются колонки: ФИО_сотрудника; дата; начало; окончание [; отработано_часов]");
                continue;
            }
            try {
                String empName = requireText(cols.get(0), "ФИО сотрудника");
                Employee employee = employeeRepository.findByFullNameIgnoreCaseAndDemo(empName, demo)
                        .orElseThrow(() -> new IllegalArgumentException("сотрудник «" + empName + "» не найден"));
                LocalDate date = parseDate(cols.get(1));
                LocalTime startTime = parseTime(cols.get(2));
                LocalTime endTime = parseTime(cols.get(3));
                if (!startTime.isBefore(endTime)) {
                    throw new IllegalArgumentException("время окончания смены должно быть позже начала");
                }
                if (shiftRepository.existsByEmployeeIdAndShiftDateAndStartTime(employee.getId(), date, startTime)) {
                    result.addDuplicate();
                    continue;
                }
                Shift shift = new Shift();
                shift.setDemo(demo);
                shift.setEmployee(employee);
                shift.setShiftDate(date);
                shift.setStartTime(startTime);
                shift.setEndTime(endTime);
                BigDecimal worked = cols.size() >= 5 && !cols.get(4).isBlank()
                        ? parseNonNegativeDecimal(cols.get(4), "отработанные часы")
                        : shift.getPlannedHours();
                shift.setWorkedHours(worked);
                shiftRepository.save(shift);
                result.addImported();
            } catch (IllegalArgumentException ex) {
                result.addError(lineNum, ex.getMessage());
            }
        }
        return result;
    }

    private void parseOrderItems(Order order, String itemsCol, Map<String, Product> productCache) {
        String[] parts = itemsCol.split("[,|]");
        Map<Product, Integer> merged = new LinkedHashMap<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) continue;

            String name;
            int qty = 1;
            int idx = lastQuantitySeparator(trimmed);
            if (idx > 0) {
                name = trimmed.substring(0, idx).trim();
                String qtyStr = trimmed.substring(idx + 1).replaceAll("[^0-9]", "");
                if (qtyStr.isEmpty()) {
                    throw new IllegalArgumentException("некорректное количество в позиции «" + trimmed + "»");
                }
                qty = Integer.parseInt(qtyStr);
                if (qty < 1) {
                    throw new IllegalArgumentException("количество должно быть ≥ 1");
                }
            } else {
                name = trimmed;
            }
            Product product = productCache.get(name.toLowerCase(Locale.ROOT));
            if (product == null) {
                throw new IllegalArgumentException("товар «" + name + "» не найден в справочнике");
            }
            merged.merge(product, qty, Integer::sum);
        }
        if (merged.isEmpty()) {
            throw new IllegalArgumentException("не удалось распознать состав заказа");
        }
        for (Map.Entry<Product, Integer> entry : merged.entrySet()) {
            OrderItem item = new OrderItem();
            item.setProduct(entry.getKey());
            item.setQuantity(entry.getValue());
            item.setUnitPrice(entry.getKey().getPrice());
            order.addItem(item);
        }
    }

    private int lastQuantitySeparator(String s) {
        // Поддерживает форматы "Товар x2", "Товар х2", "Товар * 2", "Товар:2"
        String lower = s.toLowerCase(Locale.ROOT);
        for (int i = lower.length() - 1; i >= 1; i--) {
            char c = lower.charAt(i);
            if ((c == 'x' || c == 'х' || c == '*' || c == ':')
                    && i + 1 < lower.length()
                    && lower.substring(i + 1).trim().matches("\\d+")) {
                return i;
            }
        }
        return -1;
    }

    private ExpenseCategory parseExpenseCategory(String raw) {
        String val = requireText(raw, "категория").toLowerCase(Locale.ROOT);
        for (ExpenseCategory c : ExpenseCategory.values()) {
            if (c.name().equalsIgnoreCase(val) || c.getLabel().equalsIgnoreCase(val)) {
                return c;
            }
        }
        if (val.contains("аренд")) return ExpenseCategory.RENT;
        if (val.contains("зарплат") || val.contains("оклад")) return ExpenseCategory.SALARY;
        if (val.contains("коммун")) return ExpenseCategory.UTILITIES;
        if (val.contains("закуп") || val.contains("продукт")) return ExpenseCategory.FOOD_PURCHASE;
        if (val.contains("интернет") || val.contains("связ")) return ExpenseCategory.INTERNET;
        if (val.contains("проч") || val.contains("друг")) return ExpenseCategory.OTHER;
        throw new IllegalArgumentException("неизвестная категория расхода: «" + raw + "»");
    }

    private ExpenseType parseExpenseType(String raw) {
        String val = requireText(raw, "тип расхода").toLowerCase(Locale.ROOT);
        if (val.equals("fixed") || val.startsWith("постоян")) return ExpenseType.FIXED;
        if (val.equals("variable") || val.startsWith("перемен")) return ExpenseType.VARIABLE;
        throw new IllegalArgumentException("неизвестный тип расхода: «" + raw + "» (укажите Постоянный или Переменный)");
    }

    private LocalDate parseDate(String raw) {
        String val = requireText(raw, "дата");
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(val, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("неверный формат даты «" + val + "» (ожидается ГГГГ-ММ-ДД или ДД.ММ.ГГГГ)");
    }

    private LocalTime parseTime(String raw) {
        String val = requireText(raw, "время");
        for (DateTimeFormatter fmt : TIME_FORMATS) {
            try {
                return LocalTime.parse(val, fmt);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new IllegalArgumentException("неверный формат времени «" + val + "» (ожидается ЧЧ:ММ)");
    }

    private BigDecimal parsePositiveDecimal(String raw, String field) {
        BigDecimal bd = parseDecimal(raw, field);
        if (bd.signum() <= 0) {
            throw new IllegalArgumentException("поле «" + field + "» должно быть больше нуля");
        }
        return bd;
    }

    private BigDecimal parseNonNegativeDecimal(String raw, String field) {
        BigDecimal bd = parseDecimal(raw, field);
        if (bd.signum() < 0) {
            throw new IllegalArgumentException("поле «" + field + "» не может быть отрицательным");
        }
        return bd;
    }

    private BigDecimal parseDecimal(String raw, String field) {
        String cleaned = requireText(raw, field).replace(" ", "").replace('\u00A0', ' ').replace(',', '.');
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("некорректное число в поле «" + field + "»: «" + raw + "»");
        }
    }

    private boolean parseBoolean(String raw) {
        String val = raw.trim().toLowerCase(Locale.ROOT);
        return val.equals("1") || val.equals("true") || val.equals("да") || val.equals("yes") || val.equals("активен");
    }

    private String requireText(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("не заполнено обязательное поле «" + field + "»");
        }
        return raw.trim();
    }

    private boolean isHeader(String firstLine, String... markers) {
        String lower = firstLine.toLowerCase(Locale.ROOT);
        for (String marker : markers) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private char detectDelimiter(String headerLine) {
        int semicolons = countOutsideQuotes(headerLine, ';');
        int commas = countOutsideQuotes(headerLine, ',');
        return semicolons >= commas ? ';' : ',';
    }

    private int countOutsideQuotes(String line, char target) {
        boolean inQuotes = false;
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inQuotes = !inQuotes;
            else if (c == target && !inQuotes) count++;
        }
        return count;
    }

    /** Разбор строки CSV с поддержкой кавычек. */
    static List<String> parseCsvLine(String line, char delimiter) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
