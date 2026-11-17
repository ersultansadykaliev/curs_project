package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    List<Ingredient> findByDemoOrderByNameAsc(boolean demo);

    Optional<Ingredient> findByNameIgnoreCaseAndDemo(String name, boolean demo);

    long countByDemo(boolean demo);

    @Modifying
    @Query("delete from Ingredient i where i.demo = true")
    int deleteDemo();
}
