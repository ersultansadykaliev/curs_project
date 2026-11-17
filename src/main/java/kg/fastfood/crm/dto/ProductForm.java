package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Данные формы «Товар». */
public class ProductForm {

    private Long id;

    @NotBlank(message = "Введите название товара")
    @Size(max = 100, message = "Название не длиннее 100 символов")
    private String name;

    @Size(max = 50, message = "Категория не длиннее 50 символов")
    private String category;

    @NotNull(message = "Укажите цену продажи")
    @DecimalMin(value = "0.01", message = "Цена должна быть больше нуля")
    @Digits(integer = 10, fraction = 2, message = "Некорректная цена")
    private BigDecimal price;

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
