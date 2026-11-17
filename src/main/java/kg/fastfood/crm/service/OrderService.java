package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.OrderForm;
import kg.fastfood.crm.dto.OrderRow;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.entity.Order;
import kg.fastfood.crm.entity.OrderItem;
import kg.fastfood.crm.entity.Product;
import kg.fastfood.crm.repository.OrderRepository;
import kg.fastfood.crm.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Учёт заказов: список с поиском и фильтром по датам, добавление, редактирование, удаление. */
@Service
public class OrderService {

    public static final int PAGE_SIZE = 25;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final SettingsService settingsService;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        SettingsService settingsService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.settingsService = settingsService;
    }

    /** Поиск заказов (по номеру чека или комментарию) за период, новые сверху. */
    @Transactional(readOnly = true)
    public Page<OrderRow> search(PeriodRange period, String query, int page) {
        String q = query == null ? "" : query.trim();
        PageRequest pageRequest = PageRequest.of(Math.max(page, 0), PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "orderDate", "orderTime", "id"));
        return orderRepository.search(settingsService.isDemoMode(), period.from(), period.to(), q, pageRequest)
                .map(this::toRow);
    }

    /** Общая сумма найденных заказов. */
    @Transactional(readOnly = true)
    public double sumForSearch(PeriodRange period, String query) {
        String q = query == null ? "" : query.trim();
        return orderRepository.sumForSearch(settingsService.isDemoMode(), period.from(), period.to(), q).doubleValue();
    }

    /** Заказ с составом — для страницы подробностей. */
    @Transactional(readOnly = true)
    public Order getDetails(Long id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));
        if (order.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Заказ не найден в текущем наборе данных");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public OrderForm toForm(Long id) {
        Order order = getDetails(id);
        OrderForm form = new OrderForm();
        form.setId(order.getId());
        form.setOrderNumber(order.getOrderNumber());
        form.setOrderDate(order.getOrderDate());
        form.setOrderTime(order.getOrderTime());
        form.setComment(order.getComment());
        if (order.getItems().isEmpty()) {
            form.setManualTotal(order.getTotalAmount());
        }
        for (OrderItem item : order.getItems()) {
            form.getItems().add(new OrderForm.Item(item.getProduct().getId(), item.getQuantity()));
        }
        return form;
    }

    /** Создаёт или обновляет заказ. Возвращает id. */
    @Transactional
    public Long save(OrderForm form) {
        boolean demo = settingsService.isDemoMode();
        Map<Long, Integer> quantities = collectItems(form);
        if (quantities.isEmpty() && (form.getManualTotal() == null || form.getManualTotal().signum() <= 0)) {
            throw new BusinessException("Добавьте в заказ хотя бы один товар или укажите сумму заказа вручную");
        }

        Order order;
        if (form.getId() == null) {
            order = new Order();
            order.setDemo(demo);
        } else {
            order = getDetails(form.getId());
        }

        String number = ProductService.blankToNull(form.getOrderNumber());
        if (number == null) {
            number = order.getOrderNumber() != null ? order.getOrderNumber() : nextOrderNumber(demo, form.getOrderDate());
        }
        if (!number.equals(order.getOrderNumber()) && orderRepository.existsByOrderNumberAndDemo(number, demo)) {
            throw new BusinessException("Заказ с номером «" + number + "» уже существует");
        }
        order.setOrderNumber(number);
        order.setOrderDate(form.getOrderDate());
        order.setOrderTime(form.getOrderTime());
        order.setComment(ProductService.blankToNull(form.getComment()));

        // Сохраняем цену продажи старых позиций: цена в меню могла измениться после оформления заказа
        Map<Long, BigDecimal> oldPrices = new HashMap<>();
        for (OrderItem item : order.getItems()) {
            oldPrices.put(item.getProduct().getId(), item.getUnitPrice());
        }
        order.getItems().clear();

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> new BusinessException("Товар не найден (id " + entry.getKey() + ")"));
            if (product.isDemo() != demo) {
                throw new BusinessException("Товар «" + product.getName() + "» относится к другому набору данных");
            }
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(entry.getValue());
            item.setUnitPrice(oldPrices.getOrDefault(product.getId(), product.getPrice()));
            order.addItem(item);
        }

        if (order.getItems().isEmpty()) {
            order.setTotalAmount(form.getManualTotal());
        } else {
            order.recalculateTotal();
        }
        return orderRepository.save(order).getId();
    }

    @Transactional
    public void delete(Long id) {
        orderRepository.delete(getDetails(id));
    }

    /** Объединяет строки формы: пустые строки пропускаются, одинаковые товары складываются. */
    private Map<Long, Integer> collectItems(OrderForm form) {
        Map<Long, Integer> result = new LinkedHashMap<>();
        for (OrderForm.Item item : form.getItems()) {
            if (item == null || item.getProductId() == null) {
                continue;
            }
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new BusinessException("Укажите количество (не меньше 1) для каждого товара в заказе");
            }
            result.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return result;
    }

    /** Генерирует свободный номер чека вида M-251117-001. */
    private String nextOrderNumber(boolean demo, LocalDate date) {
        String prefix = "M-" + date.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-";
        for (int i = 1; i < 100_000; i++) {
            String candidate = prefix + String.format("%03d", i);
            if (!orderRepository.existsByOrderNumberAndDemo(candidate, demo)) {
                return candidate;
            }
        }
        throw new BusinessException("Не удалось сгенерировать номер заказа — укажите его вручную");
    }

    private OrderRow toRow(Order order) {
        StringJoiner summary = new StringJoiner(", ");
        for (OrderItem item : order.getItems()) {
            summary.add(item.getProduct().getName() + " × " + item.getQuantity());
        }
        return new OrderRow(order.getId(), order.getOrderNumber(), order.getOrderDate(), order.getOrderTime(),
                order.getTotalAmount().doubleValue(), order.getItemsCount(),
                order.getItems().isEmpty() ? "состав не указан" : summary.toString(), order.getComment());
    }
}
