package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.ImportResult;
import kg.fastfood.crm.dto.PasswordForm;
import kg.fastfood.crm.dto.SettingsForm;
import kg.fastfood.crm.dto.UserForm;
import kg.fastfood.crm.service.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Настройки системы: параметры предприятия, переключение Demo/Real, очистка демо-данных,
 * импорт CSV и управление пользователями.
 */
@Controller
@RequestMapping("/settings")
public class SettingsController {

    private final SettingsService settingsService;
    private final DemoDataService demoDataService;
    private final CsvImportService csvImportService;
    private final UserService userService;

    public SettingsController(SettingsService settingsService,
                              DemoDataService demoDataService,
                              CsvImportService csvImportService,
                              UserService userService) {
        this.settingsService = settingsService;
        this.demoDataService = demoDataService;
        this.csvImportService = csvImportService;
        this.userService = userService;
    }

    @GetMapping
    public String index(Authentication auth, Model model) {
        SettingsForm settingsForm = new SettingsForm();
        settingsForm.setBusinessName(settingsService.getBusinessName());
        settingsForm.setOpenHour(settingsService.getOpenHour());
        settingsForm.setCloseHour(settingsService.getCloseHour());

        model.addAttribute("settingsForm", settingsForm);
        model.addAttribute("passwordForm", new PasswordForm());
        model.addAttribute("userForm", new UserForm());
        model.addAttribute("demoStats", demoDataService.stats());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        model.addAttribute("isAdmin", isAdmin);
        if (isAdmin) {
            model.addAttribute("users", userService.listAll());
        }
        return "settings/index";
    }

    @PostMapping("/general")
    public String saveGeneral(@Valid @ModelAttribute("settingsForm") SettingsForm form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/settings";
        }
        try {
            settingsService.saveGeneral(form.getBusinessName(), form.getOpenHour(), form.getCloseHour());
            redirectAttributes.addFlashAttribute("successMessage", "Настройки предприятия сохранены");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings";
    }

    @PostMapping("/mode")
    public String switchMode(@RequestParam boolean demo, RedirectAttributes redirectAttributes) {
        settingsService.setDemoMode(demo);
        redirectAttributes.addFlashAttribute("successMessage",
                demo ? "Включён режим демонстрационных данных" : "Включён режим реальных данных предприятия");
        return "redirect:/settings";
    }

    @PostMapping("/demo/reload")
    public String reloadDemo(RedirectAttributes redirectAttributes) {
        demoDataService.reloadDemoData();
        redirectAttributes.addFlashAttribute("successMessage",
                "Демонстрационные данные загружены заново (~400 заказов за 21 день)");
        return "redirect:/settings";
    }

    @PostMapping("/demo/clear")
    public String clearDemo(RedirectAttributes redirectAttributes) {
        demoDataService.clearDemoData();
        redirectAttributes.addFlashAttribute("successMessage",
                "Все демонстрационные данные удалены. Система переключена на реальные данные.");
        return "redirect:/settings";
    }

    @PostMapping("/import")
    public String importCsv(@RequestParam("type") String type,
                            @RequestParam("file") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        ImportResult result = csvImportService.importFile(type, file);
        redirectAttributes.addFlashAttribute("importResult", result);
        return "redirect:/settings#csv-import";
    }

    @GetMapping("/import/template/{type}")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String type) {
        LocalDate today = LocalDate.now();
        String csv = switch (type) {
            case "ingredients" -> "name;unit;unit_cost\nБулочка кунжутная;шт;19.50\nСоус барбекю;л;340.00\n";
            case "products" -> "name;category;price;active\nЧикен бургер;Бургеры;215.00;true\nАйс-ти 0.5 л;Напитки;90.00;true\n";
            case "recipes" -> "product_name;ingredient_name;quantity\nЧикен бургер;Булочка кунжутная;1.0\nЧикен бургер;Соус барбекю;0.03\n";
            case "orders" -> "order_number;date;time;items;total_amount;comment\n"
                    + "CSV-001;" + today + ";12:30;\"Чикен бургер x2, Айс-ти 0.5 л x1\";;На вынос\n"
                    + "CSV-002;" + today + ";13:15;;480.00;Импорт без состава\n";
            case "expenses" -> "name;category;amount;date;type;description\n"
                    + "Аренда павильона;Аренда;35000.00;" + today + ";Постоянный;Ежемесячный платёж\n"
                    + "Закупка овощей;Закупка продуктов;4500.00;" + today + ";Переменный;Накладная\n";
            case "employees" -> "full_name;position;phone;hourly_rate;hire_date\n"
                    + "Алиев Данияр;Повар;+996 555 001 122;200.00;" + today.minusMonths(2) + "\n";
            case "shifts" -> "employee_name;shift_date;start_time;end_time;worked_hours\n"
                    + "Алиев Данияр;" + today + ";09:00;17:00;8.00\n";
            default -> throw new NotFoundException("Шаблон не найден: " + type);
        };
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] body = csv.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, out, 0, bom.length);
        System.arraycopy(body, 0, out, bom.length, body.length);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("template-" + type + ".csv").build());
        return ResponseEntity.ok().headers(headers).body(out);
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("passwordForm") PasswordForm form,
                                 BindingResult bindingResult,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/settings#security";
        }
        try {
            userService.changePassword(auth.getName(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Пароль успешно изменён");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings#security";
    }

    @PostMapping("/users")
    public String createUser(@Valid @ModelAttribute("userForm") UserForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/settings#users";
        }
        try {
            userService.createUser(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Пользователь «" + form.getUsername() + "» создан");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings#users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.toggleEnabled(id, auth.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Статус пользователя обновлён");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/settings#users";
    }
}
