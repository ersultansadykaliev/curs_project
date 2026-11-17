package kg.fastfood.crm.controller;

import kg.fastfood.crm.dto.AnalyticsResults.ForecastResult;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.dto.ProductSales;
import kg.fastfood.crm.service.AnalyticsService;
import kg.fastfood.crm.service.FinanceService;
import kg.fastfood.crm.service.PeriodService;
import kg.fastfood.crm.service.SalesSnapshot;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** Главная панель (Dashboard) с основными показателями и графиками. */
@Controller
public class DashboardController {

    private static final DateTimeFormatter LABEL_FMT = DateTimeFormatter.ofPattern("dd.MM");

    private final PeriodService periodService;
    private final FinanceService financeService;
    private final AnalyticsService analyticsService;

    public DashboardController(PeriodService periodService,
                               FinanceService financeService,
                               AnalyticsService analyticsService) {
        this.periodService = periodService;
        this.financeService = financeService;
        this.analyticsService = analyticsService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model) {
        PeriodRange period = periodService.resolve(from, to);
        SalesSnapshot snapshot = financeService.load(period);
        FinancialSummary summary = financeService.summary(snapshot);
        ForecastResult forecast = analyticsService.forecast(snapshot, 7);

        LinkedHashMap<LocalDate, Double> dailyRev = financeService.dailyRevenue(snapshot);
        List<String> dayLabels = new ArrayList<>();
        for (LocalDate d : dailyRev.keySet()) {
            dayLabels.add(d.format(LABEL_FMT));
        }
        List<Double> dayValues = new ArrayList<>(dailyRev.values());

        long[] byHour = financeService.ordersByHour(snapshot);
        List<String> hourLabels = new ArrayList<>();
        List<Long> hourCounts = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            if (byHour[h] > 0 || (h >= 9 && h <= 22)) {
                hourLabels.add(String.format("%02d:00", h));
                hourCounts.add(byHour[h]);
            }
        }

        List<ProductSales> allSales = financeService.productSales(snapshot);
        List<ProductSales> topProducts = allSales.size() > 7 ? allSales.subList(0, 7) : allSales;
        List<String> topNames = new ArrayList<>();
        List<Long> topUnits = new ArrayList<>();
        for (ProductSales ps : topProducts) {
            topNames.add(ps.name());
            topUnits.add(ps.units());
        }

        model.addAttribute("period", period);
        model.addAttribute("summary", summary);
        model.addAttribute("forecast", forecast);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("dayLabels", dayLabels);
        model.addAttribute("dayValues", dayValues);
        model.addAttribute("hourLabels", hourLabels);
        model.addAttribute("hourCounts", hourCounts);
        model.addAttribute("topNames", topNames);
        model.addAttribute("topUnits", topUnits);
        return "dashboard";
    }
}
