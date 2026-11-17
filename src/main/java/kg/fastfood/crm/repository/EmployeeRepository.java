package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByDemoOrderByFullNameAsc(boolean demo);

    List<Employee> findByDemoAndActiveTrueOrderByFullNameAsc(boolean demo);

    Optional<Employee> findByFullNameIgnoreCaseAndDemo(String fullName, boolean demo);

    long countByDemo(boolean demo);

    @Modifying
    @Query("delete from Employee e where e.demo = true")
    int deleteDemo();
}
