package kg.fastfood.crm.dto;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Период анализа «с — по» (обе даты включительно). */
public record PeriodRange(LocalDate from, LocalDate to) {

    public PeriodRange {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Не указаны даты периода");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Дата начала периода позже даты окончания");
        }
    }

    /** Количество календарных дней в периоде. */
    public long days() {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
