package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Данные формы «Ингредиент». */
public class IngredientForm {

    private Long id;

    @NotBlank(message = "Введите название ингредиента")
    @Size(max = 100, message = "Название не длиннее 100 символов")
    private String name;

    @NotBlank(message = "Укажите единицу измерения")
    @Size(max = 20, message = "Единица измерения не длиннее 20 символов")
    private String unit;

    @NotNull(message = "Укажите стоимость единицы")
    @DecimalMin(value = "0.0", message = "Стоимость не может быть отрицательной")
    @Digits(integer = 8, fraction = 4, message = "Некорректная стоимость")
    private BigDecimal unitCost;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
}
