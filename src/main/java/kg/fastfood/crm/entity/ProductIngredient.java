package kg.fastfood.crm.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Строка технологической карты: сколько ингредиента (qᵢ) уходит на одну порцию товара. */
@Entity
@Table(name = "product_ingredients",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_ingredient", columnNames = {"product_id", "ingredient_id"}))
public class ProductIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pi_product"))
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pi_ingredient"))
    private Ingredient ingredient;

    /** Количество ингредиента (в единицах измерения ингредиента) на одну порцию. */
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal quantity;

    /** Стоимость этой строки: qᵢ × pᵢ. */
    public BigDecimal getLineCost() {
        return quantity.multiply(ingredient.getUnitCost());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Ingredient getIngredient() { return ingredient; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
}
