package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    @Query("select s from Shift s join fetch s.employee where s.demo = :demo " +
            "and s.shiftDate between :from and :to order by s.shiftDate desc, s.startTime, s.employee.fullName")
    List<Shift> findForPeriod(@Param("demo") boolean demo,
                              @Param("from") LocalDate from,
                              @Param("to") LocalDate to);

    boolean existsByEmployeeIdAndShiftDateAndStartTime(Long employeeId, LocalDate date, LocalTime startTime);

    long countByEmployeeId(Long employeeId);

    @Modifying
    @Query("delete from Shift s where s.employee.id = :employeeId")
    int deleteByEmployeeId(@Param("employeeId") Long employeeId);

    @Modifying
    @Query("delete from Shift s where s.demo = true")
    int deleteDemo();
}
