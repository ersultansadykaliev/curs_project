package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.ExpenseForm;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.Expense;
import kg.fastfood.crm.entity.ExpenseCategory;
import kg.fastfood.crm.entity.ExpenseType;
import kg.fastfood.crm.service.BusinessException;
import kg.fastfood.crm.service.ExpenseService;
import kg.fastfood.crm.service.PeriodService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Модуль расходов предприятия. */
@Controller
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;
    private final PeriodService periodService;

    public ExpenseController(ExpenseService expenseService, PeriodService periodService) {
        this.expenseService = expenseService;
        this.periodService = periodService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) ExpenseCategory category,
                       Model model) {
        PeriodRange period = periodService.resolve(from, to);
        List<Expense> expenses = expenseService.list(period, category);
        List<Expense> allInPeriod = category == null ? expenses : expenseService.list(period, null);
        Map<ExpenseCategory, Double> totals = expenseService.totalsByCategory(allInPeriod);

        double operatingTotal = 0;
        double purchasesTotal = 0;
        for (Expense e : expenses) {
            if (e.getCategory().isOperatingExpense()) {
                operatingTotal += e.getAmount().doubleValue();
            } else {
                purchasesTotal += e.getAmount().doubleValue();
            }
        }

        model.addAttribute("period", period);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("expenses", expenses);
        model.addAttribute("totalsByCategory", totals);
        model.addAttribute("operatingTotal", operatingTotal);
        model.addAttribute("purchasesTotal", purchasesTotal);
        return "expenses/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        ExpenseForm form = new ExpenseForm();
        form.setExpenseDate(LocalDate.now());
        form.setType(ExpenseType.FIXED);
        form.setCategory(ExpenseCategory.RENT);
        populateFormModel(model, form, false);
        return "expenses/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        populateFormModel(model, expenseService.toForm(id), true);
        return "expenses/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") ExpenseForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, isEdit);
            return "expenses/form";
        }
        try {
            expenseService.save(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Расход обновлён" : "Расход зарегистрирован");
            return "redirect:/expenses";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, form, isEdit);
            return "expenses/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        expenseService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Расход удалён");
        return "redirect:/expenses";
    }

    private void populateFormModel(Model model, ExpenseForm form, boolean isEdit) {
        model.addAttribute("form", form);
        model.addAttribute("isEdit", isEdit);
        model.addAttribute("categories", ExpenseCategory.values());
        model.addAttribute("types", ExpenseType.values());
    }
}
