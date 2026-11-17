package kg.fastfood.crm.controller;

import kg.fastfood.crm.dto.FinanceRow;
import kg.fastfood.crm.dto.FinancialSummary;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.ExpenseCategory;
import kg.fastfood.crm.service.ExpenseService;
import kg.fastfood.crm.service.FinanceService;
import kg.fastfood.crm.service.FinanceService.Grouping;
import kg.fastfood.crm.service.PeriodService;
import kg.fastfood.crm.service.SalesSnapshot;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Финансовые отчёты: R, COGS, GP, OE, OP с группировкой по дням, неделям и месяцам. */
@Controller
@RequestMapping("/finance")
public class FinanceController {

    private final PeriodService periodService;
    private final FinanceService financeService;
    private final ExpenseService expenseService;

    public FinanceController(PeriodService periodService,
                             FinanceService financeService,
                             ExpenseService expenseService) {
        this.periodService = periodService;
        this.financeService = financeService;
        this.expenseService = expenseService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        @RequestParam(defaultValue = "DAY") Grouping grouping,
                        Model model) {
        PeriodRange period = periodService.resolve(from, to);
        SalesSnapshot data = financeService.load(period);
        FinancialSummary summary = financeService.summary(data);
        List<FinanceRow> rows = financeService.breakdown(data, grouping);
        Map<ExpenseCategory, Double> expenseByCategory = expenseService.totalsByCategory(data.expenses());

        List<String> chartLabels = new ArrayList<>();
        List<Double> chartRevenue = new ArrayList<>();
        List<Double> chartCosts = new ArrayList<>();
        List<Double> chartProfit = new ArrayList<>();
        for (FinanceRow r : rows) {
            chartLabels.add(r.label());
            chartRevenue.add(r.revenue());
            chartCosts.add(r.cogs() + r.operatingExpenses());
            chartProfit.add(r.operatingProfit());
        }

        model.addAttribute("period", period);
        model.addAttribute("grouping", grouping);
        model.addAttribute("groupings", Grouping.values());
        model.addAttribute("summary", summary);
        model.addAttribute("rows", rows);
        model.addAttribute("expenseByCategory", expenseByCategory);
        model.addAttribute("chartLabels", chartLabels);
        model.addAttribute("chartRevenue", chartRevenue);
        model.addAttribute("chartCosts", chartCosts);
        model.addAttribute("chartProfit", chartProfit);
        return "finance/index";
    }
}
