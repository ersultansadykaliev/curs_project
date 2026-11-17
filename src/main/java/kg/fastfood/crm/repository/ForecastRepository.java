package kg.fastfood.crm.repository;

import kg.fastfood.crm.entity.Forecast;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ForecastRepository extends JpaRepository<Forecast, Long> {

    List<Forecast> findTop20ByDemoOrderByCreatedAtDesc(boolean demo);

    @Modifying
    @Query("delete from Forecast f where f.demo = true")
    int deleteDemo();
}
