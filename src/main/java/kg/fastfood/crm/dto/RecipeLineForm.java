package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Добавление ингредиента в состав товара. */
public class RecipeLineForm {

    @NotNull(message = "Выберите ингредиент")
    private Long ingredientId;

    @NotNull(message = "Укажите количество")
    @DecimalMin(value = "0.0001", message = "Количество должно быть больше нуля")
    @Digits(integer = 8, fraction = 4, message = "Некорректное количество")
    private BigDecimal quantity;

    public Long getIngredientId() { return ingredientId; }
    public void setIngredientId(Long ingredientId) { this.ingredientId = ingredientId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
