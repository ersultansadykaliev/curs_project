package kg.fastfood.crm.controller;

import jakarta.validation.Valid;
import kg.fastfood.crm.dto.OrderForm;
import kg.fastfood.crm.dto.OrderRow;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.Order;
import kg.fastfood.crm.entity.Product;
import kg.fastfood.crm.service.BusinessException;
import kg.fastfood.crm.service.OrderService;
import kg.fastfood.crm.service.PeriodService;
import kg.fastfood.crm.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Модуль заказов: просмотр, поиск, фильтрация по дате, добавление, редактирование, удаление. */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final ProductService productService;
    private final PeriodService periodService;

    public OrderController(OrderService orderService,
                           ProductService productService,
                           PeriodService periodService) {
        this.orderService = orderService;
        this.productService = productService;
        this.periodService = periodService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        PeriodRange period = periodService.resolve(from, to);
        Page<OrderRow> orders = orderService.search(period, q, page);
        double totalSum = orderService.sumForSearch(period, q);

        model.addAttribute("period", period);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("orders", orders);
        model.addAttribute("totalSum", totalSum);
        return "orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OrderForm form = new OrderForm();
        form.setOrderDate(LocalDate.now());
        form.setOrderTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
        form.getItems().add(new OrderForm.Item(null, 1));
        form.getItems().add(new OrderForm.Item(null, 1));
        populateFormModel(model, form, false);
        return "orders/form";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Order order = orderService.getDetails(id);
        model.addAttribute("order", order);
        return "orders/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        OrderForm form = orderService.toForm(id);
        if (form.getItems().isEmpty()) {
            form.getItems().add(new OrderForm.Item(null, 1));
        }
        populateFormModel(model, form, true);
        return "orders/form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") OrderForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        boolean isEdit = form.getId() != null;
        if (bindingResult.hasErrors()) {
            populateFormModel(model, form, isEdit);
            return "orders/form";
        }
        try {
            Long id = orderService.save(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    isEdit ? "Заказ успешно обновлён" : "Новый заказ успешно добавлен");
            return "redirect:/orders/" + id;
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            populateFormModel(model, form, isEdit);
            return "orders/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        orderService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "Заказ удалён");
        return "redirect:/orders";
    }

    private void populateFormModel(Model model, OrderForm form, boolean isEdit) {
        List<Product> products = productService.activeProducts();
        model.addAttribute("form", form);
        model.addAttribute("products", products);
        model.addAttribute("isEdit", isEdit);
    }
}
