package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.ProductRow;
import kg.fastfood.crm.dto.RecipeLineForm;
import kg.fastfood.crm.entity.Product;
import kg.fastfood.crm.math.CostCalculator;
import kg.fastfood.crm.service.BusinessException;
import kg.fastfood.crm.service.IngredientService;
import kg.fastfood.crm.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Модуль расчёта себестоимости (C = Σ(qᵢ × pᵢ)) и редактирования технологических карт блюд.
 */
@Controller
@RequestMapping("/costing")
public class CostingController {

    private final ProductService productService;
    private final IngredientService ingredientService;

    public CostingController(ProductService productService, IngredientService ingredientService) {
        this.productService = productService;
        this.ingredientService = ingredientService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) Long productId, Model model) {
        List<ProductRow> rows = productService.listRows();
        Long selectedId = productId;
        if (selectedId == null && !rows.isEmpty()) {
            selectedId = rows.get(0).id();
        }
        Product selected = selectedId == null ? null : productService.getWithRecipe(selectedId);
        double selectedCost = selected == null ? 0.0 : ProductService.costOf(selected);
        double selectedPrice = selected == null ? 0.0 : selected.getPrice().doubleValue();

        model.addAttribute("products", rows);
        model.addAttribute("selected", selected);
        model.addAttribute("selectedCost", selectedCost);
        model.addAttribute("selectedMargin", CostCalculator.unitMargin(selectedPrice, selectedCost));
        model.addAttribute("selectedMarginPct", CostCalculator.marginPercent(selectedPrice, selectedCost));
        model.addAttribute("ingredients", ingredientService.list());
        model.addAttribute("recipeForm", new RecipeLineForm());
        return "costing/index";
    }

    @PostMapping("/{productId}/recipe")
    public String addRecipeLine(@PathVariable Long productId,
                                @Valid @ModelAttribute("recipeForm") RecipeLineForm form,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Выберите ингредиент и укажите положительное количество");
            return "redirect:/costing?productId=" + productId;
        }
        try {
            productService.addRecipeLine(productId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Состав блюда обновлён, себестоимость пересчитана");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/costing?productId=" + productId;
    }

    @PostMapping("/{productId}/recipe/{lineId}/delete")
    public String removeRecipeLine(@PathVariable Long productId,
                                   @PathVariable Long lineId,
                                   RedirectAttributes redirectAttributes) {
        productService.removeRecipeLine(productId, lineId);
        redirectAttributes.addFlashAttribute("successMessage", "Ингредиент убран из состава блюда");
        return "redirect:/costing?productId=" + productId;
    }
}
