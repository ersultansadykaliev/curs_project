package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    boolean existsByIngredientId(Long ingredientId);

    long countByIngredientId(Long ingredientId);

    @Modifying
    @Query("delete from ProductIngredient pi where pi.product.id in (select p.id from Product p where p.demo = true)")
    int deleteDemo();
}
