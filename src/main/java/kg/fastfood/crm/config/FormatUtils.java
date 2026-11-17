package kg.fastfood.crm.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Помощник форматирования чисел и валюты (кыргызский сом) в шаблонах Thymeleaf. */
@Component("fmt")
public class FormatUtils {

    private static final DecimalFormatSymbols RU_SYMBOLS;

    static {
        RU_SYMBOLS = new DecimalFormatSymbols(new Locale("ru", "RU"));
        RU_SYMBOLS.setGroupingSeparator(' ');
        RU_SYMBOLS.setDecimalSeparator(',');
    }

    public String money(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.00", RU_SYMBOLS);
        return df.format(value) + " сом";
    }

    public String money(BigDecimal value) {
        return value == null ? "0,00 сом" : money(value.doubleValue());
    }

    public String num(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.00", RU_SYMBOLS);
        return df.format(value);
    }

    public String num(double value, int decimals) {
        String pattern = decimals <= 0 ? "#,##0" : "#,##0." + "0".repeat(decimals);
        DecimalFormat df = new DecimalFormat(pattern, RU_SYMBOLS);
        return df.format(value);
    }

    public String pct(double value) {
        DecimalFormat df = new DecimalFormat("#,##0.0", RU_SYMBOLS);
        return df.format(value) + "%";
    }
}
