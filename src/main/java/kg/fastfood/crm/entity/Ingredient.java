package kg.fastfood.crm.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Ингредиент: например «Булочка, шт, 15 сом» или «Говядина, кг, 650 сом». */
@Entity
@Table(name = "ingredients",
        uniqueConstraints = @UniqueConstraint(name = "uk_ingredients_name_demo", columnNames = {"name", "is_demo"}),
        indexes = @Index(name = "idx_ingredients_demo", columnList = "is_demo"))
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Единица измерения: кг, л, шт ... */
    @Column(nullable = false, length = 20)
    private String unit;

    /** Стоимость одной единицы измерения (pᵢ), сом. */
    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
}
