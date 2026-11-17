package kg.fastfood.crm.controller;

import kg.fastfood.crm.dto.AnalyticsResults.*;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

/**
 * Модуль математической аналитики: все 7 математических моделей
 * с формулами, исходными данными, вычислениями, графиками и интерпретацией.
 */
@Controller
@RequestMapping("/analytics")
public class AnalyticsController {

    private final PeriodService periodService;
    private final FinanceService financeService;
    private final AnalyticsService analyticsService;
    private final SettingsService settingsService;

    public AnalyticsController(PeriodService periodService,
                               FinanceService financeService,
                               AnalyticsService analyticsService,
                               SettingsService settingsService) {
        this.periodService = periodService;
        this.financeService = financeService;
        this.analyticsService = analyticsService;
        this.settingsService = settingsService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(defaultValue = AnalyticsService.INTERVAL_HOUR) String intervalType,
                        @RequestParam(required = false) Integer hour,
                        @RequestParam(required = false) Integer queryK,
                        @RequestParam(defaultValue = "7") int window,
                        Model model) {
        PeriodRange period = periodService.resolve(from, to);
        SalesSnapshot data = financeService.load(period);
        FinancialSummary summary = financeService.summary(data);

        AverageResult averages = analyticsService.averages(data);
        ProductivityResult productivity = analyticsService.productivity(data);
        Double eff = productivity.error() == null && productivity.productivity() > 0
                ? productivity.productivity() : null;
        PoissonResult poisson = analyticsService.poisson(data, intervalType, hour, queryK, eff);
        ForecastResult forecast = analyticsService.forecast(data, window);
        MarginResult margins = analyticsService.margins(data);
        BreakEvenResult breakEven = analyticsService.breakEven(data, summary);

        model.addAttribute("period", period);
        model.addAttribute("intervalType", intervalType);
        model.addAttribute("hour", poisson.hour());
        model.addAttribute("queryK", poisson.error() == null ? poisson.queryK() : queryK);
        model.addAttribute("window", window);
        model.addAttribute("openHour", settingsService.getOpenHour());
        model.addAttribute("closeHour", settingsService.getCloseHour());

        model.addAttribute("summary", summary);
        model.addAttribute("averages", averages);
        model.addAttribute("poisson", poisson);
        model.addAttribute("forecast", forecast);
        model.addAttribute("savedForecasts", analyticsService.savedForecasts());
        model.addAttribute("margins", margins);
        model.addAttribute("breakEven", breakEven);
        model.addAttribute("productivity", productivity);
        return "analytics/index";
    }

    @PostMapping("/forecast/save")
    public String saveForecast(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(defaultValue = "7") int window,
                               RedirectAttributes redirectAttributes) {
        PeriodRange period = periodService.resolve(from, to);
        SalesSnapshot data = financeService.load(period);
        try {
            ForecastResult result = analyticsService.forecast(data, window);
            analyticsService.saveForecast(result);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Прогноз выручки сохранён в историю прогнозов");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/analytics?from=" + period.from() + "&to=" + period.to() + "&window=" + window + "#model3";
    }
}
