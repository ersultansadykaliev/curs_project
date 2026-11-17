package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** Все товары набора данных вместе с технологическими картами (одним запросом, без N+1). */
    @Query("select distinct p from Product p " +
            "left join fetch p.ingredients pi left join fetch pi.ingredient " +
            "where p.demo = :demo order by p.name")
    List<Product> findAllWithIngredients(@Param("demo") boolean demo);

    @Query("select p from Product p left join fetch p.ingredients pi left join fetch pi.ingredient where p.id = :id")
    Optional<Product> findWithIngredientsById(@Param("id") Long id);

    List<Product> findByDemoAndActiveTrueOrderByNameAsc(boolean demo);

    Optional<Product> findByNameIgnoreCaseAndDemo(String name, boolean demo);

    long countByDemo(boolean demo);

    @Modifying
    @Query("delete from Product p where p.demo = true")
    int deleteDemo();
}
