package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Определяет период анализа: выбранный пользователем или (по умолчанию) весь период имеющихся заказов. */
@Service
public class PeriodService {

    private final OrderRepository orderRepository;
    private final SettingsService settingsService;

    public PeriodService(OrderRepository orderRepository, SettingsService settingsService) {
        this.orderRepository = orderRepository;
        this.settingsService = settingsService;
    }

    /** Полный период, за который есть заказы в текущем наборе данных. */
    public PeriodRange dataRange() {
        boolean demo = settingsService.isDemoMode();
        LocalDate min = orderRepository.findMinDate(demo);
        LocalDate max = orderRepository.findMaxDate(demo);
        if (min == null || max == null) {
            LocalDate today = LocalDate.now();
            return new PeriodRange(today.minusDays(29), today);
        }
        return new PeriodRange(min, max);
    }

    /** Период из параметров запроса; пустые даты заменяются границами имеющихся данных. */
    public PeriodRange resolve(LocalDate from, LocalDate to) {
        PeriodRange data = dataRange();
        LocalDate start = from != null ? from : data.from();
        LocalDate end = to != null ? to : data.to();
        if (start.isAfter(end)) {
            LocalDate tmp = start;
            start = end;
            end = tmp;
        }
        return new PeriodRange(start, end);
    }
}
