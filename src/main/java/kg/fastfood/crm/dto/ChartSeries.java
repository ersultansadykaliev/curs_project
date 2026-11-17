package kg.fastfood.crm.dto;

import java.util.ArrayList;
import java.util.List;

/** Данные для графика Chart.js: подписи по оси X и одна или несколько серий значений. */
public class ChartSeries {

    private final List<String> labels = new ArrayList<>();
    private final List<List<Double>> values = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    public ChartSeries(List<String> labels) {
        this.labels.addAll(labels);
    }

    public ChartSeries add(String name, List<Double> series) {
        names.add(name);
        values.add(series);
        return this;
    }

    public List<String> getLabels() { return labels; }
    public List<List<Double>> getValues() { return values; }
    public List<String> getNames() { return names; }
}
