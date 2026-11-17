package kg.fastfood.crm.service;

/**
 * Ошибка бизнес-логики с понятным пользователю сообщением
 * (например, «Нельзя удалить ингредиент: он используется в составе 3 товаров»).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
