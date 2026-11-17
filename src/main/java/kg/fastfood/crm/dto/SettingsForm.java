package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;

/** Общие настройки предприятия. */
public class SettingsForm {

    @NotBlank(message = "Введите название предприятия")
    @Size(max = 100, message = "Название не длиннее 100 символов")
    private String businessName;

    @NotNull(message = "Укажите час открытия")
    @Min(value = 0, message = "Час открытия от 0 до 23")
    @Max(value = 23, message = "Час открытия от 0 до 23")
    private Integer openHour;

    @NotNull(message = "Укажите час закрытия")
    @Min(value = 1, message = "Час закрытия от 1 до 24")
    @Max(value = 24, message = "Час закрытия от 1 до 24")
    private Integer closeHour;

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }
    public Integer getOpenHour() { return openHour; }
    public void setOpenHour(Integer openHour) { this.openHour = openHour; }
    public Integer getCloseHour() { return closeHour; }
    public void setCloseHour(Integer closeHour) { this.closeHour = closeHour; }
}
