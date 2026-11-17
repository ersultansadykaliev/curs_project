package kg.fastfood.crm.config;

import jakarta.servlet.http.HttpServletRequest;
import kg.fastfood.crm.service.NotFoundException;
import kg.fastfood.crm.service.SettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Глобальные атрибуты для боковой панели и шапки, а также обработка типовых ошибок. */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SettingsService settingsService;

    public GlobalModelAdvice(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @ModelAttribute("businessName")
    public String businessName() {
        return settingsService.getBusinessName();
    }

    @ModelAttribute("demoMode")
    public boolean demoMode() {
        return settingsService.isDemoMode();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(NotFoundException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("errorTitle", "Запись не найдена");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }
}
