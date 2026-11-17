package kg.fastfood.crm.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Рабочая смена одного сотрудника в конкретный день.
 * Количество сотрудников в смене = число смен, пересекающихся по времени в этот день.
 */
@Entity
@Table(name = "shifts",
        uniqueConstraints = @UniqueConstraint(name = "uk_shift_employee_date_start",
                columnNames = {"employee_id", "shift_date", "start_time"}),
        indexes = @Index(name = "idx_shifts_demo_date", columnList = "is_demo, shift_date"))
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, foreignKey = @ForeignKey(name = "fk_shifts_employee"))
    private Employee employee;

    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /** Фактически отработанные часы (по умолчанию равны длительности смены). */
    @Column(name = "worked_hours", nullable = false, precision = 5, scale = 2)
    private BigDecimal workedHours;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    /** Плановая длительность смены в часах. */
    public BigDecimal getPlannedHours() {
        long minutes = Duration.between(startTime, endTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getShiftDate() { return shiftDate; }
    public void setShiftDate(LocalDate shiftDate) { this.shiftDate = shiftDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public BigDecimal getWorkedHours() { return workedHours; }
    public void setWorkedHours(BigDecimal workedHours) { this.workedHours = workedHours; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
