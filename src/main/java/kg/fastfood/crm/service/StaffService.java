package kg.fastfood.crm.service;

import kg.fastfood.crm.dto.EmployeeForm;
import kg.fastfood.crm.dto.PeriodRange;
import kg.fastfood.crm.dto.ScheduleForm;
import kg.fastfood.crm.dto.ShiftForm;
import kg.fastfood.crm.entity.Employee;
import kg.fastfood.crm.entity.Shift;
import kg.fastfood.crm.repository.EmployeeRepository;
import kg.fastfood.crm.repository.ShiftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/** Сотрудники и графики смен. */
@Service
public class StaffService {

    /** Ограничение на массовое создание смен, чтобы случайно не создать тысячи записей. */
    private static final int MAX_SCHEDULE_DAYS = 62;

    private final EmployeeRepository employeeRepository;
    private final ShiftRepository shiftRepository;
    private final SettingsService settingsService;

    public StaffService(EmployeeRepository employeeRepository,
                        ShiftRepository shiftRepository,
                        SettingsService settingsService) {
        this.employeeRepository = employeeRepository;
        this.shiftRepository = shiftRepository;
        this.settingsService = settingsService;
    }

    // ------------------------------------------------------------------ сотрудники

    @Transactional(readOnly = true)
    public List<Employee> employees() {
        return employeeRepository.findByDemoOrderByFullNameAsc(settingsService.isDemoMode());
    }

    @Transactional(readOnly = true)
    public List<Employee> activeEmployees() {
        return employeeRepository.findByDemoAndActiveTrueOrderByFullNameAsc(settingsService.isDemoMode());
    }

    @Transactional(readOnly = true)
    public EmployeeForm toForm(Long id) {
        Employee e = getEmployee(id);
        EmployeeForm form = new EmployeeForm();
        form.setId(e.getId());
        form.setFullName(e.getFullName());
        form.setPosition(e.getPosition());
        form.setPhone(e.getPhone());
        form.setHourlyRate(e.getHourlyRate());
        form.setHireDate(e.getHireDate());
        form.setActive(e.isActive());
        return form;
    }

    @Transactional
    public Long saveEmployee(EmployeeForm form) {
        boolean demo = settingsService.isDemoMode();
        String name = form.getFullName().trim();
        Optional<Employee> sameName = employeeRepository.findByFullNameIgnoreCaseAndDemo(name, demo);
        if (sameName.isPresent() && !sameName.get().getId().equals(form.getId())) {
            throw new BusinessException("Сотрудник «" + name + "» уже есть в списке");
        }
        Employee e;
        if (form.getId() == null) {
            e = new Employee();
            e.setDemo(demo);
        } else {
            e = getEmployee(form.getId());
        }
        e.setFullName(name);
        e.setPosition(form.getPosition().trim());
        e.setPhone(ProductService.blankToNull(form.getPhone()));
        e.setHourlyRate(form.getHourlyRate());
        e.setHireDate(form.getHireDate());
        e.setActive(form.isActive());
        return employeeRepository.save(e).getId();
    }

    /** Удаляет сотрудника. Если у него есть смены — сотрудник только деактивируется, чтобы сохранить историю часов. */
    @Transactional
    public String deleteEmployee(Long id) {
        Employee e = getEmployee(id);
        long shifts = shiftRepository.countByEmployeeId(id);
        if (shifts > 0) {
            e.setActive(false);
            return "У сотрудника " + shifts + " смен(ы) в истории, поэтому он не удалён, а помечен как неактивный.";
        }
        employeeRepository.delete(e);
        return "Сотрудник удалён";
    }

    // ------------------------------------------------------------------ смены

    @Transactional(readOnly = true)
    public List<Shift> shifts(PeriodRange period) {
        return shiftRepository.findForPeriod(settingsService.isDemoMode(), period.from(), period.to());
    }

    @Transactional(readOnly = true)
    public ShiftForm toShiftForm(Long id) {
        Shift s = getShift(id);
        ShiftForm form = new ShiftForm();
        form.setId(s.getId());
        form.setEmployeeId(s.getEmployee().getId());
        form.setShiftDate(s.getShiftDate());
        form.setStartTime(s.getStartTime());
        form.setEndTime(s.getEndTime());
        form.setWorkedHours(s.getWorkedHours());
        return form;
    }

    @Transactional
    public void saveShift(ShiftForm form) {
        validateTimes(form.getStartTime().isBefore(form.getEndTime()));
        Employee employee = getEmployee(form.getEmployeeId());
        Shift shift;
        if (form.getId() == null) {
            if (shiftRepository.existsByEmployeeIdAndShiftDateAndStartTime(employee.getId(), form.getShiftDate(), form.getStartTime())) {
                throw new BusinessException("У сотрудника уже есть смена в это время");
            }
            shift = new Shift();
            shift.setDemo(employee.isDemo());
        } else {
            shift = getShift(form.getId());
        }
        shift.setEmployee(employee);
        shift.setShiftDate(form.getShiftDate());
        shift.setStartTime(form.getStartTime());
        shift.setEndTime(form.getEndTime());
        BigDecimal hours = form.getWorkedHours() != null ? form.getWorkedHours() : shift.getPlannedHours();
        shift.setWorkedHours(hours);
        shiftRepository.save(shift);
    }

    /** Создаёт смены для выбранных сотрудников на каждый день диапазона. Возвращает количество созданных смен. */
    @Transactional
    public int createSchedule(ScheduleForm form) {
        validateTimes(form.getStartTime().isBefore(form.getEndTime()));
        if (form.getDateFrom().isAfter(form.getDateTo())) {
            throw new BusinessException("Дата начала графика позже даты окончания");
        }
        long days = ChronoUnit.DAYS.between(form.getDateFrom(), form.getDateTo()) + 1;
        if (days > MAX_SCHEDULE_DAYS) {
            throw new BusinessException("График можно создать максимум на " + MAX_SCHEDULE_DAYS + " дня за один раз");
        }
        int created = 0;
        for (Long employeeId : form.getEmployeeIds()) {
            Employee employee = getEmployee(employeeId);
            for (LocalDate d = form.getDateFrom(); !d.isAfter(form.getDateTo()); d = d.plusDays(1)) {
                if (shiftRepository.existsByEmployeeIdAndShiftDateAndStartTime(employeeId, d, form.getStartTime())) {
                    continue; // такая смена уже есть — пропускаем, чтобы не было дублей
                }
                Shift shift = new Shift();
                shift.setEmployee(employee);
                shift.setShiftDate(d);
                shift.setStartTime(form.getStartTime());
                shift.setEndTime(form.getEndTime());
                shift.setWorkedHours(shift.getPlannedHours());
                shift.setDemo(employee.isDemo());
                shiftRepository.save(shift);
                created++;
            }
        }
        return created;
    }

    @Transactional
    public void deleteShift(Long id) {
        shiftRepository.delete(getShift(id));
    }

    private void validateTimes(boolean startBeforeEnd) {
        if (!startBeforeEnd) {
            throw new BusinessException("Время окончания смены должно быть позже времени начала (смены через полночь не поддерживаются — разделите их на две)");
        }
    }

    private Employee getEmployee(Long id) {
        if (id == null) {
            throw new BusinessException("Выберите сотрудника");
        }
        Employee e = employeeRepository.findById(id).orElseThrow(() -> new NotFoundException("Сотрудник не найден"));
        if (e.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Сотрудник не найден в текущем наборе данных");
        }
        return e;
    }

    private Shift getShift(Long id) {
        Shift s = shiftRepository.findById(id).orElseThrow(() -> new NotFoundException("Смена не найдена"));
        if (s.isDemo() != settingsService.isDemoMode()) {
            throw new NotFoundException("Смена не найдена в текущем наборе данных");
        }
        return s;
    }
}
