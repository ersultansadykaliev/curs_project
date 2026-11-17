package kg.fastfood.crm.service;

import kg.fastfood.crm.entity.AppSetting;
import kg.fastfood.crm.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Настройки приложения, хранящиеся в таблице app_settings.
 * <p>
 * Важная настройка — <b>режим данных</b>: демонстрационные и реальные данные хранятся в одних таблицах,
 * но помечены флагом is_demo и никогда не смешиваются. Все списки и вся аналитика работают
 * только с тем набором, который выбран в данный момент.
 */
@Service
public class SettingsService {

    public static final String KEY_DATA_MODE = "data_mode";
    public static final String KEY_BUSINESS_NAME = "business_name";
    public static final String KEY_OPEN_HOUR = "open_hour";
    public static final String KEY_CLOSE_HOUR = "close_hour";

    public static final String MODE_DEMO = "DEMO";
    public static final String MODE_REAL = "REAL";

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return repository.findById(key).map(AppSetting::getValue).orElse(defaultValue);
    }

    @Transactional
    public void set(String key, String value) {
        AppSetting setting = repository.findById(key).orElse(new AppSetting(key, value));
        setting.setValue(value);
        repository.save(setting);
    }

    /** true — сейчас выбран демонстрационный набор данных. */
    public boolean isDemoMode() {
        return MODE_DEMO.equals(get(KEY_DATA_MODE, MODE_REAL));
    }

    public void setDemoMode(boolean demo) {
        set(KEY_DATA_MODE, demo ? MODE_DEMO : MODE_REAL);
    }

    public String getBusinessName() {
        return get(KEY_BUSINESS_NAME, "Fast Food");
    }

    /** Час открытия (0–23). */
    public int getOpenHour() {
        return parseInt(get(KEY_OPEN_HOUR, "9"), 9);
    }

    /** Час закрытия (1–24), не включительно: 23 означает работу до 23:00. */
    public int getCloseHour() {
        return parseInt(get(KEY_CLOSE_HOUR, "23"), 23);
    }

    /** Количество рабочих часов в сутках. */
    public int getWorkingHoursPerDay() {
        return Math.max(1, getCloseHour() - getOpenHour());
    }

    @Transactional
    public void saveGeneral(String businessName, int openHour, int closeHour) {
        if (closeHour <= openHour) {
            throw new BusinessException("Час закрытия должен быть позже часа открытия");
        }
        set(KEY_BUSINESS_NAME, businessName.trim());
        set(KEY_OPEN_HOUR, String.valueOf(openHour));
        set(KEY_CLOSE_HOUR, String.valueOf(closeHour));
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
