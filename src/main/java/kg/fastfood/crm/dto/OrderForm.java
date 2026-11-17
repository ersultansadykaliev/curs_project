package kg.fastfood.crm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Данные формы «Заказ» — шапка заказа и список позиций. */
public class OrderForm {

    private Long id;

    @Size(max = 40, message = "Номер чека не длиннее 40 символов")
    private String orderNumber;

    @NotNull(message = "Укажите дату заказа")
    @PastOrPresent(message = "Дата заказа не может быть в будущем")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate orderDate;

    @NotNull(message = "Укажите время заказа")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime orderTime;

    @Size(max = 255, message = "Комментарий не длиннее 255 символов")
    private String comment;

    /** Сумма вручную — используется, только если состав заказа не указан. */
    @DecimalMin(value = "0.0", message = "Сумма не может быть отрицательной")
    @Digits(integer = 10, fraction = 2, message = "Некорректная сумма")
    private BigDecimal manualTotal;

    @Valid
    private List<Item> items = new ArrayList<>();

    /** Позиция заказа в форме. */
    public static class Item {
        private Long productId;

        @Min(value = 1, message = "Количество должно быть не меньше 1")
        @Max(value = 1000, message = "Слишком большое количество")
        private Integer quantity;

        public Item() {
        }

        public Item(Long productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public LocalTime getOrderTime() { return orderTime; }
    public void setOrderTime(LocalTime orderTime) { this.orderTime = orderTime; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public BigDecimal getManualTotal() { return manualTotal; }
    public void setManualTotal(BigDecimal manualTotal) { this.manualTotal = manualTotal; }
    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }
}
