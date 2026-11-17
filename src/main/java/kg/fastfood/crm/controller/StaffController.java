package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.AnalyticsResults.ProductivityResult;
import kg.fastfood.crm.dto.EmployeeForm;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.dto.ScheduleForm;
import kg.fastfood.crm.dto.ShiftForm;
import kg.fastfood.crm.entity.Shift;
import kg.fastfood.crm.service.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Модуль персонала: сотрудники, графики смен, отработанные часы и производительность E = N / H. */
@Controller
@RequestMapping("/staff")
public class StaffController {

    private final StaffService staffService;
    private final PeriodService periodService;
    private final FinanceService financeService;
    private final AnalyticsService analyticsService;

    public StaffController(StaffService staffService,
                           PeriodService periodService,
                           FinanceService financeService,
                           AnalyticsService analyticsService) {
        this.staffService = staffService;
        this.periodService = periodService;
        this.financeService = financeService;
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                        Model model) {
        PeriodRange period = periodService.resolve(from, to);
        SalesSnapshot snapshot = financeService.load(period);
        ProductivityResult productivity = analyticsService.productivity(snapshot);
        List<Shift> shifts = staffService.shifts(period);

        // Количество сотрудников в смене по датам
        Map<LocalDate, Set<Long>> staffPerDay = new LinkedHashMap<>();
        for (Shift s : shifts) {
            staffPerDay.computeIfAbsent(s.getShiftDate(), d -> new LinkedHashSet<>()).add(s.getEmployee().getId());
        }

        ScheduleForm scheduleForm = new ScheduleForm();
        scheduleForm.setDateFrom(LocalDate.now());
        scheduleForm.setDateTo(LocalDate.now().plusDays(6));
        scheduleForm.setStartTime(LocalTime.of(9, 0));
        scheduleForm.setEndTime(LocalTime.of(16, 0));

        model.addAttribute("period", period);
        model.addAttribute("employees", staffService.employees());
        model.addAttribute("activeEmployees", staffService.activeEmployees());
        model.addAttribute("shifts", shifts);
        model.addAttribute("staffPerDay", staffPerDay);
        model.addAttribute("productivity", productivity);
        model.addAttribute("scheduleForm", scheduleForm);
        return "staff/index";
    }

    @GetMapping("/employees/new")
    public String newEmployeeForm(Model model) {
        model.addAttribute("form", new EmployeeForm());
        model.addAttribute("isEdit", false);
        return "staff/employee-form";
    }

    @GetMapping("/employees/{id}/edit")
    public String editEmployeeForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", staffService.toForm(id));
        model.addAttribute("isEdit", true);
        return "staff/employee-form";
    }

    @PostMapping("/employees")
    public String saveEmployee(@Valid @ModelAttribute("form") EmployeeForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", isEdit);
            return "staff/employee-form";
        }
        try {
            staffService.saveEmployee(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Данные сотрудника обновлены" : "Сотрудник добавлен");
            return "redirect:/staff";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", isEdit);
            return "staff/employee-form";
        }
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        String message = staffService.deleteEmployee(id);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/staff";
    }

    @GetMapping("/shifts/new")
    public String newShiftForm(Model model) {
        ShiftForm form = new ShiftForm();
        form.setShiftDate(LocalDate.now());
        form.setStartTime(LocalTime.of(9, 0));
        form.setEndTime(LocalTime.of(16, 0));
        populateShiftModel(model, form, false);
        return "staff/shift-form";
    }

    @GetMapping("/shifts/{id}/edit")
    public String editShiftForm(@PathVariable Long id, Model model) {
        populateShiftModel(model, staffService.toShiftForm(id), true);
        return "staff/shift-form";
    }

    @PostMapping("/shifts")
    public String saveShift(@Valid @ModelAttribute("form") ShiftForm form,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            populateShiftModel(model, form, isEdit);
            return "staff/shift-form";
        }
        try {
            staffService.saveShift(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Смена обновлена" : "Смена добавлена");
            return "redirect:/staff";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateShiftModel(model, form, isEdit);
            return "staff/shift-form";
        }
    }

    @PostMapping("/shifts/{id}/delete")
    public String deleteShift(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        staffService.deleteShift(id);
        redirectAttributes.addFlashAttribute("successMessage", "Смена удалена");
        return "redirect:/staff";
    }

    @PostMapping("/schedule")
    public String createSchedule(@Valid @ModelAttribute("scheduleForm") ScheduleForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/staff";
        }
        try {
            int created = staffService.createSchedule(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "График создан: добавлено смен — " + created);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/staff";
    }

    private void populateShiftModel(Model model, ShiftForm form, boolean isEdit) {
        model.addAttribute("form", form);
        model.addAttribute("employees", staffService.activeEmployees());
        model.addAttribute("isEdit", isEdit);
    }
}
