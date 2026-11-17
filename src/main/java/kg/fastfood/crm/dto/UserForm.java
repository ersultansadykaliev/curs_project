package kg.fastfood.crm.dto;

import jakarta.validation.constraints.*;

/** Создание нового пользователя системы. */
public class UserForm {

    @NotBlank(message = "Введите логин")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]{3,50}$", message = "Логин: 3–50 латинских букв, цифр, _ . -")
    private String username;

    @NotBlank(message = "Введите имя пользователя")
    @Size(max = 100, message = "Имя не длиннее 100 символов")
    private String fullName;

    @NotBlank(message = "Введите пароль")
    @Size(min = 6, max = 72, message = "Пароль должен быть от 6 до 72 символов")
    private String password;

    @NotBlank(message = "Выберите роль")
    private String role;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
