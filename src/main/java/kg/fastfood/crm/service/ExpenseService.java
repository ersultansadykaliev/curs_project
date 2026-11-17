package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.ExpenseForm;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.Expense;
import kg.fastfood.crm.entity.ExpenseCategory;
import kg.fastfood.crm.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Учёт расходов предприятия. */
@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SettingsService settingsService;

    public ExpenseService(ExpenseRepository expenseRepository, SettingsService settingsService) {
        this.expenseRepository = expenseRepository;
        this.settingsService = settingsService;
    }

    /** Расходы за период; category = null — все категории. */
    @Transactional(readOnly = true)
    public List<Expense> list(PeriodRange period, ExpenseCategory category) {
        List<Expense> all = expenseRepository.findByDemoAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(
                settingsService.isDemoMode(), period.from(), period.to());
        if (category == null) {
            return all;
        }
        List<Expense> filtered = new ArrayList<>();
        for (Expense e : all) {
            if (e.getCategory() == category) {
                filtered.add(e);
            }
        }
        return filtered;
    }

    /** Суммы расходов по категориям. */
    public Map<ExpenseCategory, Double> totalsByCategory(List<Expense> expenses) {
        Map<ExpenseCategory, Double> totals = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory c : ExpenseCategory.values()) {
            totals.put(c, 0.0);
        }
        for (Expense e : expenses) {
            totals.merge(e.getCategory(), e.getAmount().doubleValue(), Double::sum);
        }
        return totals;
    }

    @Transactional(readOnly = true)
    public ExpenseForm toForm(Long id) {
        Expense e = get(id);
        ExpenseForm form = new ExpenseForm();
        form.setId(e.getId());
        form.setName(e.getName());
        form.setCategory(e.getCategory());
        form.setAmount(e.getAmount());
        form.setExpenseDate(e.getExpenseDate());
        form.setType(e.getType());
        form.setDescription(e.getDescription());
        return form;
    }

    @Transactional
    public Long save(ExpenseForm form) {
        Expense expense;
        if (form.getId() == null) {
            expense = new Expense();
            expense.setDemo(settingsService.isDemoMode());
        } else {
            expense = get(form.getId());
        }
        expense.setName(form.getName().trim());
        expense.setCategory(form.getCategory());
        expense.setAmount(form.getAmount());
        expense.setExpenseDate(form.getExpenseDate());
        expense.setType(form.getType());
        expense.setDescription(ProductService.blankToNull(form.getDescription()));
        return expenseRepository.save(expense).getId();
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(get(id));
    }

    private Expense get(Long id) {
        Expense e = expenseRepository.findById(id).orElseThrow(() -> new NotFoundException("Расход не найден"));
        if (e.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Расход не найден в текущем наборе данных");
        }
        return e;
    }
}
