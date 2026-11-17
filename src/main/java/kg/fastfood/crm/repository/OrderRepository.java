package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /** Заказы за период вместе с составом и товарами — для аналитики. */
    @Query("select distinct o from Order o left join fetch o.items i left join fetch i.product " +
            "where o.demo = :demo and o.orderDate between :from and :to")
    List<Order> findForPeriod(@Param("demo") boolean demo,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    @Query("select o from Order o left join fetch o.items i left join fetch i.product where o.id = :id")
    Optional<Order> findWithItemsById(@Param("id") Long id);

    /** Поиск заказов: по номеру чека или комментарию, с фильтром по датам. */
    @Query("select o from Order o where o.demo = :demo " +
            "and o.orderDate between :from and :to " +
            "and (:q = '' or lower(o.orderNumber) like lower(concat('%', :q, '%')) " +
            "     or lower(coalesce(o.comment, '')) like lower(concat('%', :q, '%')))")
    Page<Order> search(@Param("demo") boolean demo,
                       @Param("from") LocalDate from,
                       @Param("to") LocalDate to,
                       @Param("q") String query,
                       Pageable pageable);

    @Query("select coalesce(sum(o.totalAmount), 0) from Order o where o.demo = :demo " +
            "and o.orderDate between :from and :to " +
            "and (:q = '' or lower(o.orderNumber) like lower(concat('%', :q, '%')) " +
            "     or lower(coalesce(o.comment, '')) like lower(concat('%', :q, '%')))")
    BigDecimal sumForSearch(@Param("demo") boolean demo,
                            @Param("from") LocalDate from,
                            @Param("to") LocalDate to,
                            @Param("q") String query);

    boolean existsByOrderNumberAndDemo(String orderNumber, boolean demo);

    @Query("select min(o.orderDate) from Order o where o.demo = :demo")
    LocalDate findMinDate(@Param("demo") boolean demo);

    @Query("select max(o.orderDate) from Order o where o.demo = :demo")
    LocalDate findMaxDate(@Param("demo") boolean demo);

    long countByDemo(boolean demo);

    @Query("select count(i) from OrderItem i where i.product.id = :productId")
    long countItemsByProduct(@Param("productId") Long productId);

    /** Количество проданных единиц по каждому товару за всё время: [productId, sum(quantity)]. */
    @Query("select i.product.id, sum(i.quantity) from OrderItem i where i.order.demo = :demo group by i.product.id")
    List<Object[]> unitsSoldByProduct(@Param("demo") boolean demo);

    @Modifying
    @Query("delete from OrderItem i where i.order.id in (select o.id from Order o where o.demo = true)")
    int deleteDemoItems();

    @Modifying
    @Query("delete from Order o where o.demo = true")
    int deleteDemo();
}
