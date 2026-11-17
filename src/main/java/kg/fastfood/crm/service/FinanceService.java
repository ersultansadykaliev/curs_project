package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.FinanceRow;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.dto.ProductSales;
import kg.fastfood.crm.entity.Expense;
import kg.fastfood.crm.entity.ExpenseType;
import kg.fastfood.crm.entity.Order;
import kg.fastfood.crm.entity.OrderItem;
import kg.fastfood.crm.math.DescriptiveStatistics;
import kg.fastfood.crm.repository.ExpenseRepository;
import kg.fastfood.crm.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * Финансовая аналитика.
 * <pre>
 *   R    = Σ(Pᵢ × nᵢ)   выручка (сумма заказов)
 *   COGS = Σ(Cᵢ × nᵢ)   себестоимость проданных блюд (по технологическим картам)
 *   GP   = R − COGS     валовая прибыль
 *   OP   = GP − OE      операционная прибыль
 * </pre>
 * OE — операционные расходы <b>без категории «Закупка продуктов»</b>: закупленные продукты
 * попадают в затраты через COGS в момент продажи блюда, поэтому повторно они не вычитаются.
 */
@Service
public class FinanceService {

    /** Группировка финансового отчёта. */
    public enum Grouping {
        DAY("По дням"), WEEK("По неделям"), MONTH("По месяцам");

        private final String label;

        Grouping(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] MONTHS = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private final OrderRepository orderRepository;
    private final ExpenseRepository expenseRepository;
    private final ProductService productService;
    private final SettingsService settingsService;

    public FinanceService(OrderRepository orderRepository,
                          ExpenseRepository expenseRepository,
                          ProductService productService,
                          SettingsService settingsService) {
        this.orderRepository = orderRepository;
        this.expenseRepository = expenseRepository;
        this.productService = productService;
        this.settingsService = settingsService;
    }

    /** Загружает из БД всё необходимое для расчётов за период. */
    @Transactional(readOnly = true)
    public SalesSnapshot load(PeriodRange period) {
        boolean demo = settingsService.isDemoMode();
        List<Order> orders = new ArrayList<>(orderRepository.findForPeriod(demo, period.from(), period.to()));
        orders.sort(Comparator.comparing(Order::getOrderDate).thenComparing(Order::getOrderTime));
        List<Expense> expenses = expenseRepository.findByDemoAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
                demo, period.from(), period.to());
        return new SalesSnapshot(period, orders, productService.costMap(), expenses);
    }

    /** Сводные финансовые показатели за период снимка. */
    public FinancialSummary summary(SalesSnapshot data) {
        double revenue = 0;
        double cogs = 0;
        long units = 0;
        long withoutItems = 0;
        double revenueWithoutItems = 0;
        for (Order order : data.orders()) {
            revenue += order.getTotalAmount().doubleValue();
            if (order.getItems().isEmpty()) {
                withoutItems++;
                revenueWithoutItems += order.getTotalAmount().doubleValue();
            }
            for (OrderItem item : order.getItems()) {
                cogs += data.costOf(item.getProduct().getId()) * item.getQuantity();
                units += item.getQuantity();
            }
        }
        double fixedOe = 0;
        double variableOe = 0;
        double purchases = 0;
        for (Expense e : data.expenses()) {
            double amount = e.getAmount().doubleValue();
            if (!e.getCategory().isOperatingExpense()) {
                purchases += amount;
            } else if (e.getType() == ExpenseType.FIXED) {
                fixedOe += amount;
            } else {
                variableOe += amount;
            }
        }
        long ordersCount = data.orders().size();
        double averageCheck = ordersCount == 0 ? 0 : DescriptiveStatistics.averageCheck(revenue, ordersCount);
        double grossProfit = revenue - cogs;
        double operatingExpenses = fixedOe + variableOe;
        return new FinancialSummary(data.period(), ordersCount, units, revenue, averageCheck, cogs, grossProfit,
                operatingExpenses, fixedOe, variableOe, grossProfit - operatingExpenses, purchases,
                withoutItems, revenueWithoutItems);
    }

    /** Финансовый отчёт с разбивкой по дням, неделям или месяцам. */
    public List<FinanceRow> breakdown(SalesSnapshot data, Grouping grouping) {
        // Ключ группы — дата начала дня / недели / месяца
        Map<LocalDate, double[]> groups = new TreeMap<>(); // [orders, revenue, cogs, oe]
        PeriodRange period = data.period();
        for (LocalDate d = period.from(); !d.isAfter(period.to()); d = d.plusDays(1)) {
            groups.putIfAbsent(groupStart(d, grouping), new double[4]);
        }
        for (Order order : data.orders()) {
            double[] g = groups.get(groupStart(order.getOrderDate(), grouping));
            g[0] += 1;
            g[1] += order.getTotalAmount().doubleValue();
            for (OrderItem item : order.getItems()) {
                g[2] += data.costOf(item.getProduct().getId()) * item.getQuantity();
            }
        }
        for (Expense e : data.expenses()) {
            if (e.getCategory().isOperatingExpense()) {
                groups.get(groupStart(e.getExpenseDate(), grouping))[3] += e.getAmount().doubleValue();
            }
        }
        List<FinanceRow> rows = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> entry : groups.entrySet()) {
            LocalDate start = entry.getKey();
            LocalDate end = groupEnd(start, grouping);
            LocalDate shownStart = start.isBefore(period.from()) ? period.from() : start;
            LocalDate shownEnd = end.isAfter(period.to()) ? period.to() : end;
            double[] g = entry.getValue();
            double gp = g[1] - g[2];
            rows.add(new FinanceRow(label(shownStart, shownEnd, grouping), shownStart, shownEnd,
                    (long) g[0], g[1], g[2], gp, g[3], gp - g[3]));
        }
        return rows;
    }

    /** Выручка по каждому дню периода (дни без заказов = 0). */
    public LinkedHashMap<LocalDate, Double> dailyRevenue(SalesSnapshot data) {
        LinkedHashMap<LocalDate, Double> result = emptyDays(data.period());
        for (Order order : data.orders()) {
            result.merge(order.getOrderDate(), order.getTotalAmount().doubleValue(), Double::sum);
        }
        return result;
    }

    /** Количество заказов по каждому дню периода. */
    public LinkedHashMap<LocalDate, Double> dailyOrders(SalesSnapshot data) {
        LinkedHashMap<LocalDate, Double> result = emptyDays(data.period());
        for (Order order : data.orders()) {
            result.merge(order.getOrderDate(), 1.0, Double::sum);
        }
        return result;
    }

    /** Количество заказов по часам суток (индекс 0..23). */
    public long[] ordersByHour(SalesSnapshot data) {
        long[] counts = new long[24];
        for (Order order : data.orders()) {
            counts[order.getOrderTime().getHour()]++;
        }
        return counts;
    }

    /** Продажи по товарам за период, отсортированные по количеству (по убыванию). */
    public List<ProductSales> productSales(SalesSnapshot data) {
        Map<Long, String> names = new HashMap<>();
        Map<Long, long[]> units = new HashMap<>();
        Map<Long, double[]> money = new HashMap<>(); // [выручка, себестоимость]
        for (Order order : data.orders()) {
            for (OrderItem item : order.getItems()) {
                Long id = item.getProduct().getId();
                names.put(id, item.getProduct().getName());
                units.computeIfAbsent(id, k -> new long[1])[0] += item.getQuantity();
                double[] m = money.computeIfAbsent(id, k -> new double[2]);
                m[0] += item.getLineTotal().doubleValue();
                m[1] += data.costOf(id) * item.getQuantity();
            }
        }
        List<ProductSales> result = new ArrayList<>();
        for (Long id : names.keySet()) {
            result.add(new ProductSales(id, names.get(id), units.get(id)[0], money.get(id)[0], money.get(id)[1]));
        }
        result.sort(Comparator.comparingLong(ProductSales::units).reversed().thenComparing(ProductSales::name));
        return result;
    }

    private LinkedHashMap<LocalDate, Double> emptyDays(PeriodRange period) {
        LinkedHashMap<LocalDate, Double> result = new LinkedHashMap<>();
        for (LocalDate d = period.from(); !d.isAfter(period.to()); d = d.plusDays(1)) {
            result.put(d, 0.0);
        }
        return result;
    }

    private LocalDate groupStart(LocalDate date, Grouping grouping) {
        return switch (grouping) {
            case DAY -> date;
            case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> date.withDayOfMonth(1);
        };
    }

    private LocalDate groupEnd(LocalDate start, Grouping grouping) {
        return switch (grouping) {
            case DAY -> start;
            case WEEK -> start.plusDays(6);
            case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
        };
    }

    private String label(LocalDate start, LocalDate end, Grouping grouping) {
        return switch (grouping) {
            case DAY -> start.format(DAY_FORMAT);
            case WEEK -> start.format(DateTimeFormatter.ofPattern("dd.MM")) + " – " + end.format(DAY_FORMAT);
            case MONTH -> MONTHS[start.getMonthValue() - 1] + " " + start.getYear();
        };
    }
}
