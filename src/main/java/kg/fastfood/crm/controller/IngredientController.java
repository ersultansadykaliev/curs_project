package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.IngredientForm;
import kg.fastfood.crm.service.BusinessException;
import kg.fastfood.crm.service.IngredientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Модуль ингредиентов: справочник сырья, единиц измерения и стоимости единицы. */
@Controller
@RequestMapping("/ingredients")
public class IngredientController {

    private final IngredientService ingredientService;

    public IngredientController(IngredientService ingredientService) {
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("ingredients", ingredientService.list());
        model.addAttribute("usageCounts", ingredientService.usageCounts());
        return "ingredients/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new IngredientForm());
        model.addAttribute("isEdit", false);
        return "ingredients/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", ingredientService.toForm(id));
        model.addAttribute("isEdit", true);
        return "ingredients/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") IngredientForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", isEdit);
            return "ingredients/form";
        }
        try {
            ingredientService.save(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Ингредиент обновлён (себестоимость блюд пересчитана автоматически)"
                            : "Ингредиент добавлен");
            return "redirect:/ingredients";
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", isEdit);
            return "ingredients/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ingredientService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Ингредиент удалён");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/ingredients";
    }
}
