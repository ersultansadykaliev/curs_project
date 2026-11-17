package kg.fastfood.crm.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Товар (блюдо или напиток) из меню.
 * Себестоимость в таблице не хранится — она всегда рассчитывается по составу: C = Σ(qᵢ × pᵢ),
 * поэтому автоматически меняется при изменении цен ингредиентов.
 */
@Entity
@Table(name = "products",
        uniqueConstraints = @UniqueConstraint(name = "uk_products_name_demo", columnNames = {"name", "is_demo"}),
        indexes = @Index(name = "idx_products_demo", columnList = "is_demo"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String category;

    /** Цена продажи, сом. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean active = true;

    /** true — запись из демонстрационного набора данных. */
    @Column(name = "is_demo", nullable = false)
    private boolean demo;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductIngredient> ingredients = new ArrayList<>();

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
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public List<ProductIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<ProductIngredient> ingredients) { this.ingredients = ingredients; }
}
