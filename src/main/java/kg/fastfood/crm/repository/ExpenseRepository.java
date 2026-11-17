package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByDemoAndExpenseDateBetweenOrderByExpenseDateDescIdDesc(boolean demo, LocalDate from, LocalDate to);

    boolean existsByDemoAndNameIgnoreCaseAndExpenseDateAndAmount(boolean demo, String name, LocalDate date, BigDecimal amount);

    long countByDemo(boolean demo);

    @Modifying
    @Query("delete from Expense e where e.demo = true")
    int deleteDemo();
}
