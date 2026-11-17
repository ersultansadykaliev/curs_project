package kg.fastfood.crm.dto;

import java.util.ArrayList;
import java.util.List;

/** Итог импорта CSV-файла: сколько строк добавлено, пропущено как дубликаты и какие были ошибки. */
public class ImportResult {

    private final String fileType;
    private int imported;
    private int duplicates;
    private final List<String> errors = new ArrayList<>();

    public ImportResult(String fileType) {
        this.fileType = fileType;
    }

    public void addImported() { imported++; }
    public void addDuplicate() { duplicates++; }
    public void addError(int line, String message) { errors.add("Строка " + line + ": " + message); }
    public void addError(String message) { errors.add(message); }

    public String getFileType() { return fileType; }
    public int getImported() { return imported; }
    public int getDuplicates() { return duplicates; }
    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
