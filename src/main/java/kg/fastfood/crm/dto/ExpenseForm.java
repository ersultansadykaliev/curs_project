package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;
import kg.fastfood.crm.entity.ExpenseCategory;
import kg.fastfood.crm.entity.ExpenseType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Данные формы «Расход». */
public class ExpenseForm {

    private Long id;

    @NotBlank(message = "Введите название расхода")
    @Size(max = 100, message = "Название не длиннее 100 символов")
    private String name;

    @NotNull(message = "Выберите категорию")
    private ExpenseCategory category;

    @NotNull(message = "Укажите сумму")
    @DecimalMin(value = "0.01", message = "Сумма должна быть больше нуля")
    @Digits(integer = 10, fraction = 2, message = "Некорректная сумма")
    private BigDecimal amount;

    @NotNull(message = "Укажите дату")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expenseDate;

    @NotNull(message = "Выберите тип расхода")
    private ExpenseType type;

    @Size(max = 255, message = "Описание не длиннее 255 символов")
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ExpenseCategory getCategory() { return category; }
    public void setCategory(ExpenseCategory category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public ExpenseType getType() { return type; }
    public void setType(ExpenseType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
