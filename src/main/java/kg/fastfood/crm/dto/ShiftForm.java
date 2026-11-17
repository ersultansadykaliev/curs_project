package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Одна смена сотрудника (добавление / редактирование отработанных часов). */
public class ShiftForm {

    private Long id;

    @NotNull(message = "Выберите сотрудника")
    private Long employeeId;

    @NotNull(message = "Укажите дату смены")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate shiftDate;

    @NotNull(message = "Укажите начало смены")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "Укажите окончание смены")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    /** Фактически отработанные часы. Если пусто — берётся длительность смены. */
    @DecimalMin(value = "0.0", message = "Часы не могут быть отрицательными")
    @DecimalMax(value = "24.0", message = "Не больше 24 часов за смену")
    private BigDecimal workedHours;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }
}
