package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Данные формы «Сотрудник». */
public class EmployeeForm {

    private Long id;

    @NotBlank(message = "Введите ФИО сотрудника")
    @Size(max = 100, message = "ФИО не длиннее 100 символов")
    private String fullName;

    @NotBlank(message = "Укажите должность")
    @Size(max = 50, message = "Должность не длиннее 50 символов")
    private String position;

    @Size(max = 30, message = "Телефон не длиннее 30 символов")
    @Pattern(regexp = "^$|^[+0-9()\\s-]{5,30}$", message = "Телефон может содержать только цифры, пробелы, +, -, ( )")
    private String phone;

    @DecimalMin(value = "0.0", message = "Ставка не может быть отрицательной")
    @Digits(integer = 8, fraction = 2, message = "Некорректная ставка")
    private BigDecimal hourlyRate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @PastOrPresent(message = "Дата приёма не может быть в будущем")
    private LocalDate hireDate;

    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
