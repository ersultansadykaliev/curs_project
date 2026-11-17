package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.ProductForm;
import kg.fastfood.crm.service.BusinessException;
import kg.fastfood.crm.service.ProductService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Модуль товаров: ассортимент, цены, продажи и себестоимость. */
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.listRows());
        return "products/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new ProductForm());
        model.addAttribute("isEdit", false);
        return "products/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", productService.toForm(id));
        model.addAttribute("isEdit", true);
        return "products/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") ProductForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", isEdit);
            return "products/form";
        }
        try {
            Long id = productService.save(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Товар обновлён" : "Товар добавлен. Теперь можно задать состав ингредиентов.");
            return isEdit ? "redirect:/products" : "redirect:/costing?productId=" + id;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("isEdit", isEdit);
            return "products/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Товар удалён");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/products";
    }
}
