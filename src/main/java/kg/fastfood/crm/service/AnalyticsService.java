package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.AnalyticsResults.*;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.dto.ProductSales;
import kg.fastfood.crm.entity.Forecast;
import kg.fastfood.crm.entity.Order;
import kg.fastfood.crm.entity.Shift;
import kg.fastfood.crm.math.*;
import kg.fastfood.crm.math.CostCalculator.SoldProduct;
import kg.fastfood.crm.repository.ForecastRepository;
import kg.fastfood.crm.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Модуль математической аналитики: подготавливает исходные данные из БД, вызывает классы
 * пакета {@code kg.fastfood.crm.math} и формирует результат с интерпретацией для руководителя.
 */
@Service
public class AnalyticsService {

    public static final String INTERVAL_HOUR = "HOUR";
    public static final String INTERVAL_HOUR_OF_DAY = "HOUR_OF_DAY";
    public static final String INTERVAL_DAY = "DAY";
    public static final int MAX_WINDOW = 60;

    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd.MM");
    private static final int[] COMPARE_WINDOWS = {3, 5, 7, 10, 14};

    private final FinanceService financeService;
    private final StaffService staffService;
    private final SettingsService settingsService;
    private final ForecastRepository forecastRepository;
    private final OrderRepository orderRepository;

    public AnalyticsService(FinanceService financeService,
                            StaffService staffService,
                            SettingsService settingsService,
                            ForecastRepository forecastRepository,
                            OrderRepository orderRepository) {
        this.financeService = financeService;
        this.staffService = staffService;
        this.settingsService = settingsService;
        this.forecastRepository = forecastRepository;
        this.orderRepository = orderRepository;
    }

    // ================================================================ Модель №1

    public AverageResult averages(SalesSnapshot data) {
        LinkedHashMap<LocalDate, Double> revenueByDay = financeService.dailyRevenue(data);
        LinkedHashMap<LocalDate, Double> ordersByDay = financeService.dailyOrders(data);
        List<String> labels = labels(revenueByDay.keySet());
        List<Double> dailyRevenue = new ArrayList<>(revenueByDay.values());
        List<Double> dailyOrders = new ArrayList<>(ordersByDay.values());
        if (data.orders().isEmpty()) {
            return new AverageResult(0, 0, 0, data.period().days(), 0, 0, 0, 0, labels, dailyOrders, dailyRevenue,
                    null, "Нет заказов за выбранный период — средние показатели не рассчитываются.");
        }
        double revenue = 0;
        List<Double> checks = new ArrayList<>();
        for (Order order : data.orders()) {
            revenue += order.getTotalAmount().doubleValue();
            checks.add(order.getTotalAmount().doubleValue());
        }
        long n = data.orders().size();
        double averageCheck = DescriptiveStatistics.averageCheck(revenue, n);
        double meanOrders = DescriptiveStatistics.mean(dailyOrders);
        double meanRevenue = DescriptiveStatistics.mean(dailyRevenue);
        double std = DescriptiveStatistics.standardDeviation(dailyRevenue);
        double median = DescriptiveStatistics.median(checks);
        double variation = meanRevenue == 0 ? 0 : std / meanRevenue * 100;

        String text = String.format(Locale.US,
                "За %d дн. принято %d заказов на сумму %s сом. В среднем за день — %.1f заказа и %s сом выручки. "
                        + "Средний чек A = R / N = %s сом, медианный чек — %s сом%s. "
                        + "Стандартное отклонение дневной выручки %s сом (коэффициент вариации %.0f%%): %s",
                data.period().days(), n, money(revenue), meanOrders, money(meanRevenue), money(averageCheck),
                money(median),
                median < averageCheck * 0.9 ? " (средний чек завышен несколькими крупными заказами)" : "",
                money(std), variation,
                variation < 20 ? "продажи стабильны, средние значения хорошо описывают типичный день."
                        : variation < 40 ? "продажи заметно колеблются по дням — учитывайте день недели при планировании."
                        : "продажи сильно колеблются, средние значения нужно использовать осторожно.");
        return new AverageResult(revenue, n, averageCheck, data.period().days(), meanOrders, meanRevenue, std, median,
                labels, dailyOrders, dailyRevenue, text, null);
    }

    // ================================================================ Модель №2

    /**
     * @param intervalType HOUR — каждый рабочий час, HOUR_OF_DAY — конкретный час суток, DAY — день
     * @param hour         час суток для HOUR_OF_DAY (null — самый загруженный час)
     * @param queryK       интересующее количество заказов k (null — округлённое λ)
     * @param productivity производительность E (заказов на человеко-час) для расчёта нужного персонала, может быть null
     */
    public PoissonResult poisson(SalesSnapshot data, String intervalType, Integer hour, Integer queryK, Double productivity) {
        String type = intervalType == null ? INTERVAL_HOUR : intervalType;
        int open = settingsService.getOpenHour();
        int close = settingsService.getCloseHour();
        long[] byHour = financeService.ordersByHour(data);

        Map<LocalDate, long[]> perDayHour = new HashMap<>();
        for (Order o : data.orders()) {
            perDayHour.computeIfAbsent(o.getOrderDate(), d -> new long[24])[o.getOrderTime().getHour()]++;
        }

        List<Long> counts = new ArrayList<>();
        String label;
        Integer usedHour = null;
        switch (type) {
            case INTERVAL_DAY -> {
                label = "1 день";
                for (LocalDate d = data.period().from(); !d.isAfter(data.period().to()); d = d.plusDays(1)) {
                    counts.add(sum(perDayHour.get(d), 0, 24));
                }
            }
            case INTERVAL_HOUR_OF_DAY -> {
                usedHour = hour != null && hour >= 0 && hour <= 23 ? hour : busiestHour(byHour);
                label = String.format("час %02d:00–%02d:00", usedHour, usedHour + 1);
                for (LocalDate d = data.period().from(); !d.isAfter(data.period().to()); d = d.plusDays(1)) {
                    long[] h = perDayHour.get(d);
                    counts.add(h == null ? 0 : h[usedHour]);
                }
            }
            default -> {
                type = INTERVAL_HOUR;
                label = String.format("1 час (рабочее время %02d:00–%02d:00)", open, close);
                for (LocalDate d = data.period().from(); !d.isAfter(data.period().to()); d = d.plusDays(1)) {
                    long[] h = perDayHour.get(d);
                    for (int i = open; i < close; i++) {
                        counts.add(h == null ? 0 : h[i]);
                    }
                }
            }
        }

        long n = 0;
        long maxObserved = 0;
        for (long c : counts) {
            n += c;
            maxObserved = Math.max(maxObserved, c);
        }
        long t = counts.size();
        if (n == 0 || t == 0) {
            return new PoissonResult(type, label, usedHour, n, t, 0, List.of(), 0, 0, 0, 0, 0, 0, 0,
                    productivity, null, null, "Нет заказов в выбранных интервалах — параметр λ не может быть оценён.");
        }

        double lambda = PoissonDistribution.lambda(n, t);
        int q95 = PoissonDistribution.quantile(0.95, lambda);
        int kMax = (int) Math.min(200, Math.max(maxObserved, PoissonDistribution.quantile(0.999, lambda)) + 1);

        long[] observed = new long[kMax + 1];
        for (long c : counts) {
            observed[(int) Math.min(c, kMax)]++;
        }
        List<PoissonRow> rows = new ArrayList<>();
        int mode = 0;
        double best = -1;
        for (int k = 0; k <= kMax; k++) {
            double p = PoissonDistribution.probability(k, lambda);
            if (p > best) {
                best = p;
                mode = k;
            }
            rows.add(new PoissonRow(k, p, PoissonDistribution.cumulative(k, lambda),
                    PoissonDistribution.atLeast(k, lambda), observed[k], (double) observed[k] / t));
        }

        List<Double> asDouble = new ArrayList<>();
        for (long c : counts) {
            asDouble.add((double) c);
        }
        double variance = DescriptiveStatistics.sampleVariance(asDouble);
        double dispersion = variance / lambda;

        int k = queryK != null && queryK >= 0 ? Math.min(queryK, 1000) : (int) Math.round(lambda);
        double pk = PoissonDistribution.probability(k, lambda);
        double pAtLeast = PoissonDistribution.atLeast(k, lambda);

        Integer staff = null;
        if (productivity != null && productivity > 0 && !INTERVAL_DAY.equals(type)) {
            staff = StaffProductivity.requiredStaff(q95, productivity);
        }

        StringBuilder text = new StringBuilder();
        text.append(String.format(Locale.US,
                "За период наблюдалось N = %d заказов в T = %d интервалах (%s), λ = N / T = %.3f. "
                        + "Наиболее вероятно %d заказ(ов) за интервал (P = %.1f%%). "
                        + "Вероятность ровно %d заказов — %.1f%%, не менее %d — %.1f%%. "
                        + "В 95%% интервалов заказов будет не больше %d — на эту нагрузку стоит рассчитывать кухню.",
                n, t, label, lambda, mode, best * 100, k, pk * 100, k, pAtLeast * 100, q95));
        if (staff != null) {
            text.append(String.format(Locale.US,
                    " При фактической производительности E = %.2f заказа/чел·ч для такой нагрузки нужно не менее %d сотрудник(ов) в смене.",
                    productivity, staff));
        }
        if (dispersion > 1.5) {
            text.append(String.format(Locale.US,
                    " Внимание: дисперсия наблюдений в %.1f раза больше λ — заказы распределены неравномерно (пиковые часы), "
                            + "поэтому для планирования точнее выбрать конкретный час суток.", dispersion));
        } else {
            text.append(String.format(Locale.US,
                    " Отношение дисперсии к среднему %.2f близко к 1 — модель Пуассона хорошо описывает данные.", dispersion));
        }
        return new PoissonResult(type, label, usedHour, n, t, lambda, rows, mode, q95, variance, dispersion,
                k, pk, pAtLeast, productivity, staff, text.toString(), null);
    }

    // ================================================================ Модели №3 и №6

    public ForecastResult forecast(SalesSnapshot data, int window) {
        LinkedHashMap<LocalDate, Double> revenueByDay = financeService.dailyRevenue(data);
        List<Double> series = new ArrayList<>(revenueByDay.values());
        List<String> labels = labels(revenueByDay.keySet());
        int n = series.size();
        LocalDate target = data.period().to().plusDays(1);

        if (window < 1 || window > MAX_WINDOW) {
            return emptyForecast(window, n, labels, series,
                    "Период скользящего среднего должен быть от 1 до " + MAX_WINDOW + " дней.");
        }
        if (data.orders().isEmpty()) {
            return emptyForecast(window, n, labels, series, "Нет заказов за выбранный период — прогноз не строится.");
        }
        if (!MovingAverageForecaster.hasEnoughData(n, window)) {
            return emptyForecast(window, n, labels, series, String.format(
                    "Недостаточно данных для прогноза: для окна w = %d нужно минимум %d дней наблюдений, "
                            + "а в выбранном периоде только %d. Фиктивные значения не подставляются — "
                            + "выберите более длинный период или уменьшите окно.", window, window, n));
        }

        double forecastValue = MovingAverageForecaster.forecastNext(series, window);
        List<Double> backtest = MovingAverageForecaster.backtest(series, window);

        // Ряд прогнозов, выровненный по датам: первые window дней прогноза нет; в конце — прогноз на завтра
        List<String> chartLabels = new ArrayList<>(labels);
        chartLabels.add(target.format(LABEL) + " (прогноз)");
        List<Double> actual = new ArrayList<>(series);
        actual.add(null);
        List<Double> predicted = new ArrayList<>();
        for (int i = 0; i < window; i++) {
            predicted.add(null);
        }
        predicted.addAll(backtest);
        predicted.add(forecastValue);

        Double mae = null;
        Double relative = null;
        String maeError = null;
        String maeText = null;
        List<Double> errors = new ArrayList<>();
        if (backtest.isEmpty()) {
            maeError = "Для оценки точности нужно хотя бы " + (window + 1)
                    + " дней: модель должна сделать прогноз хотя бы на один уже известный день.";
        } else {
            List<Double> actualPart = series.subList(window, n);
            mae = ForecastAccuracy.mae(actualPart, backtest);
            relative = ForecastAccuracy.relativeMaePercent(actualPart, backtest);
            if (relative.isNaN()) {
                relative = null;
            }
            for (int i = 0; i < backtest.size(); i++) {
                errors.add(Math.abs(actualPart.get(i) - backtest.get(i)));
            }
            maeText = String.format(Locale.US,
                    "Модель проверена на %d днях, для которых известна фактическая выручка: MAE = %s сом%s. "
                            + "То есть в среднем прогноз ошибается на эту сумму в день. %s",
                    backtest.size(), money(mae),
                    relative == null ? "" : String.format(Locale.US, " (%.1f%% от средней дневной выручки)", relative),
                    relative == null ? "" : relative < 10 ? "Точность высокая."
                            : relative < 20 ? "Точность приемлемая для планирования закупок и смен."
                            : "Точность низкая: выручка сильно колеблется, прогноз используйте как ориентир.");
        }

        List<WindowMae> comparison = new ArrayList<>();
        for (int w : COMPARE_WINDOWS) {
            if (n > w) {
                List<Double> bt = MovingAverageForecaster.backtest(series, w);
                comparison.add(new WindowMae(w, ForecastAccuracy.mae(series.subList(w, n), bt), bt.size()));
            }
        }
        if (!comparison.isEmpty() && maeText != null) {
            WindowMae best = Collections.min(comparison, Comparator.comparingDouble(WindowMae::mae));
            maeText += String.format(Locale.US, " Среди сравниваемых окон наименьшая ошибка у w = %d (MAE = %s сом).",
                    best.window(), money(best.mae()));
        }

        String forecastText = String.format(Locale.US,
                "Прогноз выручки на %s: F(t+1) = (сумма выручки за последние %d дн.) / %d = %s сом. "
                        + "Скользящее среднее сглаживает случайные колебания, но не учитывает рост, сезонность и праздники.",
                target.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), window, window, money(forecastValue));

        return new ForecastResult(window, n, forecastValue, target, chartLabels, actual, predicted, mae, relative,
                backtest.size(), errors, comparison, forecastText, maeText, null, maeError);
    }

    /** Сохраняет прогноз в таблицу forecasts. */
    @Transactional
    public void saveForecast(ForecastResult result) {
        if (result.error() != null || result.forecastValue() == null) {
            throw new BusinessException(result.error() != null ? result.error() : "Прогноз не рассчитан");
        }
        Forecast f = new Forecast();
        f.setMethod("Скользящее среднее");
        f.setWindowSize(result.window());
        f.setTargetDate(result.targetDate());
        f.setForecastValue(BigDecimal.valueOf(result.forecastValue()).setScale(2, RoundingMode.HALF_UP));
        if (result.mae() != null) {
            f.setMae(BigDecimal.valueOf(result.mae()).setScale(2, RoundingMode.HALF_UP));
        }
        f.setDemo(settingsService.isDemoMode());
        forecastRepository.save(f);
    }

    /** Сохранённые прогнозы; для прошедших дат подставляется фактическая выручка. */
    @Transactional(readOnly = true)
    public List<SavedForecastRow> savedForecasts() {
        boolean demo = settingsService.isDemoMode();
        List<SavedForecastRow> rows = new ArrayList<>();
        for (Forecast f : forecastRepository.findTop20ByDemoOrderByCreatedAtDesc(demo)) {
            Double actual = null;
            List<Order> orders = orderRepository.findForPeriod(demo, f.getTargetDate(), f.getTargetDate());
            if (!orders.isEmpty()) {
                double sum = 0;
                for (Order o : orders) {
                    sum += o.getTotalAmount().doubleValue();
                }
                actual = sum;
            }
            rows.add(new SavedForecastRow(f.getId(), f.getCreatedAt(), f.getWindowSize(), f.getTargetDate(),
                    f.getForecastValue().doubleValue(), f.getMae() == null ? null : f.getMae().doubleValue(), actual));
        }
        return rows;
    }

    // ================================================================ Модель №4

    public MarginResult margins(SalesSnapshot data) {
        List<ProductSales> sales = financeService.productSales(data);
        if (sales.isEmpty()) {
            return new MarginResult(List.of(), 0, 0, 0, List.of(), null,
                    "Нет продаж с указанным составом заказа за выбранный период.");
        }
        List<SoldProduct> sold = new ArrayList<>();
        double revenue = 0;
        double cogs = 0;
        for (ProductSales s : sales) {
            double avgPrice = s.revenue() / s.units();
            sold.add(new SoldProduct(s.name(), s.units(), avgPrice, data.costOf(s.productId())));
            revenue += s.revenue();
            cogs += s.cogs();
        }
        double totalMargin = CostCalculator.totalMargin(sold);

        List<MarginRow> rows = new ArrayList<>();
        List<String> withoutRecipe = new ArrayList<>();
        for (SoldProduct p : sold) {
            double unitMargin = CostCalculator.unitMargin(p.price(), p.cost());
            double total = p.unitsSold() * unitMargin;
            boolean hasRecipe = p.cost() > 0;
            if (!hasRecipe) {
                withoutRecipe.add(p.name());
            }
            rows.add(new MarginRow(p.name(), p.unitsSold(), p.price(), p.cost(), unitMargin,
                    CostCalculator.marginPercent(p.price(), p.cost()), total,
                    totalMargin == 0 ? 0 : total / totalMargin * 100, hasRecipe));
        }
        rows.sort(Comparator.comparingDouble(MarginRow::totalMargin).reversed());

        MarginRow top = rows.get(0);
        MarginRow lowestPct = Collections.min(rows, Comparator.comparingDouble(MarginRow::marginPercent));
        double topShare = 0;
        int topCount = Math.min(3, rows.size());
        for (int i = 0; i < topCount; i++) {
            topShare += rows.get(i).shareOfMargin();
        }
        String text = String.format(Locale.US,
                "Общая маржинальная прибыль M_total = Σ nᵢ(Pᵢ − Cᵢ) = %s сом при выручке %s сом (%.1f%% выручки). "
                        + "Больше всего маржи приносит «%s» — %s сом (%.1f%%). "
                        + "Первые %d товара дают %.0f%% всей маржи. "
                        + "Самая низкая маржинальность у «%s» — %.1f%% от цены: стоит проверить цену или рецептуру.",
                money(totalMargin), money(revenue), revenue == 0 ? 0 : totalMargin / revenue * 100,
                top.name(), money(top.totalMargin()), top.shareOfMargin(), topCount, topShare,
                lowestPct.name(), lowestPct.marginPercent());
        if (!withoutRecipe.isEmpty()) {
            text += " У товаров без состава себестоимость считается равной 0 — заполните технологические карты: "
                    + String.join(", ", withoutRecipe) + ".";
        }
        return new MarginResult(rows, totalMargin, revenue, cogs, withoutRecipe, text, null);
    }

    // ================================================================ Модель №5

    public BreakEvenResult breakEven(SalesSnapshot data, FinancialSummary summary) {
        List<ProductSales> sales = financeService.productSales(data);
        long units = 0;
        double itemsRevenue = 0;
        double itemsCogs = 0;
        for (ProductSales s : sales) {
            units += s.units();
            itemsRevenue += s.revenue();
            itemsCogs += s.cogs();
        }
        double fixed = summary.fixedOperatingExpenses();
        double variableOverhead = summary.variableOperatingExpenses();
        long days = data.period().days();
        if (units == 0) {
            return breakEvenError(fixed, variableOverhead, days, "Нет продаж с указанным составом — структура ассортимента неизвестна.");
        }
        if (fixed <= 0) {
            return breakEvenError(fixed, variableOverhead, days,
                    "За выбранный период нет постоянных расходов (аренда, зарплата, связь и т. п.). "
                            + "Внесите их в разделе «Расходы» с типом «Постоянный».");
        }
        double overheadPerUnit = variableOverhead / units;

        List<SoldProduct> sold = new ArrayList<>();
        List<BreakEvenRow> productRows = new ArrayList<>();
        for (ProductSales s : sales) {
            double price = s.revenue() / s.units();
            double cv = data.costOf(s.productId()) + overheadPerUnit;
            sold.add(new SoldProduct(s.name(), s.units(), price, cv));
            Double beUnits = null;
            if (price - cv > 0) {
                beUnits = BreakEvenCalculator.unitsForProduct(fixed, price, cv);
            }
            productRows.add(new BreakEvenRow(s.name(), price, cv, price - cv, beUnits));
        }

        double weightedMargin = BreakEvenCalculator.weightedAverageMargin(sold);
        if (weightedMargin <= 0) {
            return breakEvenError(fixed, variableOverhead, days,
                    "Средневзвешенная маржа не положительна: цены не покрывают переменные затраты, безубыточность недостижима.");
        }
        double beUnits = BreakEvenCalculator.unitsForAssortment(fixed, weightedMargin);
        double marginRatio = BreakEvenCalculator.contributionMarginRatio(weightedMargin * units, itemsRevenue);
        double beRevenue = BreakEvenCalculator.revenueBreakEven(fixed, marginRatio);
        double safety = (itemsRevenue - beRevenue) / itemsRevenue * 100;
        double avgPrice = itemsRevenue / units;
        double avgCv = (itemsCogs + variableOverhead) / units;
        long ordersWithItems = summary.ordersCount() - summary.ordersWithoutItems();
        double unitsPerOrder = ordersWithItems == 0 ? 1 : (double) units / ordersWithItems;

        // График «объём — затраты — выручка»
        List<String> chartLabels = new ArrayList<>();
        List<Double> chartRevenue = new ArrayList<>();
        List<Double> chartCosts = new ArrayList<>();
        double maxX = Math.max(beUnits * 2, units * 1.2);
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double x = maxX * i / steps;
            chartLabels.add(String.valueOf(Math.round(x)));
            chartRevenue.add(avgPrice * x);
            chartCosts.add(fixed + avgCv * x);
        }

        String text = String.format(Locale.US,
                "Постоянные расходы за период F = %s сом. Средневзвешенная маржа на единицу товара M_avg = %s сом "
                        + "(средняя цена %s − средние переменные затраты %s). "
                        + "Точка безубыточности Q_BE = F / M_avg = %.0f ед. товара, или %s сом выручки "
                        + "(≈ %.1f ед. / %.1f заказа в день). Фактически продано %d ед. на %s сом. %s",
                money(fixed), money(weightedMargin), money(avgPrice), money(avgCv), beUnits, money(beRevenue),
                beUnits / days, beUnits / days / unitsPerOrder, units, money(itemsRevenue),
                safety >= 0
                        ? String.format(Locale.US, "Запас финансовой прочности %.1f%%: выручка может снизиться на эту долю, прежде чем появится убыток.", safety)
                        : String.format(Locale.US, "Выручка ниже точки безубыточности на %.1f%% — предприятие работает в убыток.", -safety));
        return new BreakEvenResult(fixed, variableOverhead, overheadPerUnit, units, itemsRevenue, avgPrice, avgCv,
                weightedMargin, beUnits, marginRatio, beRevenue, safety, days, beUnits / days,
                beUnits / days / unitsPerOrder, productRows, chartLabels, chartRevenue, chartCosts, text, null);
    }

    // ================================================================ Модель №7

    public ProductivityResult productivity(SalesSnapshot data) {
        PeriodRange period = data.period();
        List<Shift> shifts = staffService.shifts(period);
        if (shifts.isEmpty()) {
            return new ProductivityResult(0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), null,
                    "Нет смен за выбранный период — добавьте график работы в разделе «Персонал».");
        }
        Map<LocalDate, List<Order>> ordersByDate = new HashMap<>();
        for (Order o : data.orders()) {
            ordersByDate.computeIfAbsent(o.getOrderDate(), d -> new ArrayList<>()).add(o);
        }

        TreeMap<LocalDate, Double> hoursByDay = new TreeMap<>();
        Map<Long, Object[]> perEmployee = new LinkedHashMap<>(); // name, position, shifts, hours, orders
        double[] staffHoursByClockHour = new double[24];
        double totalHours = 0;
        for (Shift s : shifts) {
            double worked = s.getWorkedHours().doubleValue();
            totalHours += worked;
            hoursByDay.merge(s.getShiftDate(), worked, Double::sum);

            Object[] e = perEmployee.computeIfAbsent(s.getEmployee().getId(), id -> new Object[]{
                    s.getEmployee().getFullName(), s.getEmployee().getPosition(), 0L, 0.0, 0L});
            e[2] = (Long) e[2] + 1;
            e[3] = (Double) e[3] + worked;
            for (Order o : ordersByDate.getOrDefault(s.getShiftDate(), List.of())) {
                LocalTime time = o.getOrderTime();
                if (!time.isBefore(s.getStartTime()) && time.isBefore(s.getEndTime())) {
                    e[4] = (Long) e[4] + 1;
                }
            }
            addOverlap(staffHoursByClockHour, s.getStartTime(), s.getEndTime());
        }
        if (totalHours <= 0) {
            return new ProductivityResult(0, 0, 0, hoursByDay.size(), List.of(), List.of(), List.of(), List.of(), null,
                    "Отработанные часы равны нулю — укажите фактические часы в сменах.");
        }

        long orders = 0;
        long[] ordersByClockHour = new long[24];
        List<String> dayLabels = new ArrayList<>();
        List<Double> dayProductivity = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> day : hoursByDay.entrySet()) {
            List<Order> dayOrders = ordersByDate.getOrDefault(day.getKey(), List.of());
            orders += dayOrders.size();
            for (Order o : dayOrders) {
                ordersByClockHour[o.getOrderTime().getHour()]++;
            }
            dayLabels.add(day.getKey().format(LABEL));
            dayProductivity.add(day.getValue() > 0 ? StaffProductivity.productivity(dayOrders.size(), day.getValue()) : 0.0);
        }
        double e = StaffProductivity.productivity(orders, totalHours);
        int days = hoursByDay.size();

        List<EmployeeProductivity> employees = new ArrayList<>();
        for (Object[] row : perEmployee.values()) {
            double hours = (Double) row[3];
            long empOrders = (Long) row[4];
            employees.add(new EmployeeProductivity((String) row[0], (String) row[1], (Long) row[2], hours, empOrders,
                    hours > 0 ? StaffProductivity.productivity(empOrders, hours) : 0));
        }
        employees.sort(Comparator.comparing(EmployeeProductivity::name));

        List<HourProductivity> hours = new ArrayList<>();
        HourProductivity busiest = null;
        HourProductivity idlest = null;
        for (int h = 0; h < 24; h++) {
            if (ordersByClockHour[h] == 0 && staffHoursByClockHour[h] == 0) {
                continue;
            }
            Double eh = staffHoursByClockHour[h] > 0
                    ? StaffProductivity.productivity(ordersByClockHour[h], staffHoursByClockHour[h]) : null;
            HourProductivity hp = new HourProductivity(h, (double) ordersByClockHour[h] / days,
                    staffHoursByClockHour[h] / days, eh);
            hours.add(hp);
            if (eh != null) {
                if (busiest == null || eh > busiest.productivity()) busiest = hp;
                if (idlest == null || eh < idlest.productivity()) idlest = hp;
            }
        }

        String text = String.format(Locale.US,
                "За %d дн. со сменами обработано N = %d заказов за H = %.1f человеко-часов: E = N / H = %.2f заказа на человеко-час.",
                days, orders, totalHours, e);
        if (busiest != null && idlest != null && busiest != idlest) {
            text += String.format(Locale.US,
                    " Наибольшая нагрузка в %02d:00–%02d:00 (E = %.2f, в среднем %.1f заказа на %.1f сотрудника) — "
                            + "в это время персонала может не хватать. Наименьшая — в %02d:00–%02d:00 (E = %.2f): "
                            + "здесь возможен избыток сотрудников.",
                    busiest.hour(), busiest.hour() + 1, busiest.productivity(), busiest.averageOrders(), busiest.averageStaff(),
                    idlest.hour(), idlest.hour() + 1, idlest.productivity());
        }
        long ordersOutside = data.orders().size() - orders;
        if (ordersOutside > 0) {
            text += " Заказы в дни без внесённых смен (" + ordersOutside + " шт.) в расчёт не включены.";
        }
        return new ProductivityResult(orders, totalHours, e, days, employees, hours, dayLabels, dayProductivity, text, null);
    }

    // ================================================================ вспомогательные методы

    /** Добавляет к каждому часу суток долю часа, которую покрывает смена [start; end). */
    private void addOverlap(double[] byHour, LocalTime start, LocalTime end) {
        double from = start.getHour() + start.getMinute() / 60.0;
        double to = end.getHour() + end.getMinute() / 60.0;
        for (int h = 0; h < 24; h++) {
            double overlap = Math.min(to, h + 1) - Math.max(from, h);
            if (overlap > 0) {
                byHour[h] += overlap;
            }
        }
    }

    private int busiestHour(long[] byHour) {
        int best = 12;
        for (int h = 0; h < 24; h++) {
            if (byHour[h] > byHour[best]) {
                best = h;
            }
        }
        return best;
    }

    private long sum(long[] values, int from, int to) {
        if (values == null) {
            return 0;
        }
        long s = 0;
        for (int i = from; i < to; i++) {
            s += values[i];
        }
        return s;
    }

    private List<String> labels(Collection<LocalDate> dates) {
        List<String> labels = new ArrayList<>();
        for (LocalDate d : dates) {
            labels.add(d.format(LABEL));
        }
        return labels;
    }

    private ForecastResult emptyForecast(int window, int n, List<String> labels, List<Double> series, String error) {
        return new ForecastResult(window, n, null, null, labels, series, List.of(), null, null, 0, List.of(), List.of(),
                null, null, error, error);
    }

    private BreakEvenResult breakEvenError(double fixed, double variable, long days, String error) {
        return new BreakEvenResult(fixed, variable, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, days, 0, 0,
                List.of(), List.of(), List.of(), List.of(), null, error);
    }

    /** Формат денежной суммы: 12 345,67. */
    static String money(double value) {
        return String.format(new Locale("ru", "RU"), "%,.2f", value).replace(' ', ' ');
    }
}
