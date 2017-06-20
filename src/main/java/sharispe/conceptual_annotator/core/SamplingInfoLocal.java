/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.HashMap;
import java.util.Map;
import org.openrdf.model.URI;

/**
 * Info for a sampling of a specific size
 *
 * @author sharispe
 */
public class SamplingInfoLocal {

    public final int size;
    Map<URI, Double> numberNonNullValues = null;
    Map<URI, Double> percents = null;
    Map<URI, Double> sumNonNullValues = null;
    Map<URI, Double> averages = null;
    Map<URI, Double> sumNonNullValuesSquared = null;
    Map<URI, Double> averageSquared = null;
    Map<URI, Double> min_best = null;
    Map<URI, Double> standard_deviation = null;

    public SamplingInfoLocal(int size) {
        this.size = size;
        numberNonNullValues = new HashMap<>();
        percents = new HashMap<>();
        sumNonNullValues = new HashMap<>();
        averages = new HashMap<>();
        sumNonNullValuesSquared = new HashMap<>();
        averageSquared = new HashMap<>();
        min_best = new HashMap<>();
        standard_deviation = new HashMap<>();
    }

    public void setStandardDeviation(URI u, Double value) {
        standard_deviation.put(u, value);
    }

    public Double getStandardDeviation(URI u) {
        return standard_deviation.get(u);
    }

    public void setMinBest(URI u, Double value) {
        min_best.put(u, value);
    }

    public Double getMinBest(URI u) {
        return min_best.get(u);
    }

    public void setAverageSquared(URI u, Double value) {
        averageSquared.put(u, value);
    }

    public Double getAverageSquared(URI u) {
        return averageSquared.get(u);
    }

    public void setSumNonNullValuesSquared(URI u, Double value) {
        sumNonNullValuesSquared.put(u, value);
    }

    public Double getSumNonNullValuesSquared(URI u) {
        return sumNonNullValuesSquared.get(u);
    }

    public void setAverage(URI u, Double value) {
        averages.put(u, value);
    }

    public Double getAverage(URI u) {
        return averages.get(u);
    }

    public void setSumNonNullValues(URI u, Double value) {
        sumNonNullValues.put(u, value);
    }

    public Double getSumNonNullValues(URI u) {
        return sumNonNullValues.get(u);
    }

    public void setPercent(URI u, Double value) {
        percents.put(u, value);
    }

    public Double getPercent(URI u) {
        return percents.get(u);
    }

    public void setNumberNonNullValues(URI u, Double value) {
        numberNonNullValues.put(u, value);
    }

    public Double getNumberNonNullValues(URI u) {
        return numberNonNullValues.get(u);
    }

    boolean containsAllValuesFor(URI u) {

        return numberNonNullValues.containsKey(u)
                && percents.containsKey(u)
                && sumNonNullValues.containsKey(u)
                && averages.containsKey(u)
                && sumNonNullValuesSquared.containsKey(u)
                && averageSquared.containsKey(u)
                && min_best.containsKey(u)
                && standard_deviation.containsKey(u);
    }

}
