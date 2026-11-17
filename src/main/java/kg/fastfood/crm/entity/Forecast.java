package kg.fastfood.crm.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Сохранённый результат прогнозирования выручки (метод скользящего среднего). */
@Entity
@Table(name = "forecasts",
        indexes = @Index(name = "idx_forecasts_demo_target", columnList = "is_demo, target_date"))
public class Forecast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String method;

    /** Размер окна скользящего среднего (количество дней). */
    @Column(name = "window_size", nullable = false)
    private int windowSize;

    /** Дата, на которую сделан прогноз. */
    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "forecast_value", nullable = false, precision = 14, scale = 2)
    private BigDecimal forecastValue;

    /** Средняя абсолютная ошибка модели на истории на момент прогноза (может отсутствовать). */
    @Column(precision = 14, scale = 2)
    private BigDecimal mae;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public int getWindowSize() { return windowSize; }
    public void setWindowSize(int windowSize) { this.windowSize = windowSize; }
    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }
    public BigDecimal getForecastValue() { return forecastValue; }
    public void setForecastValue(BigDecimal forecastValue) { this.forecastValue = forecastValue; }
    public BigDecimal getMae() { return mae; }
    public void setMae(BigDecimal mae) { this.mae = mae; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
