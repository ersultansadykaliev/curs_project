package kg.fastfood.crm.service;

/** Запрошенная запись не найдена (например, удалена другим пользователем). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
