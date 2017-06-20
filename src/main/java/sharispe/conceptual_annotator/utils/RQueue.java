/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.utils;


import java.util.ArrayList;
import java.util.List;

/**
 * Result Queue
 *
 * @author Sébastien Harispe (sebastien.harispe@gmail.com)
 * @param <V> value type
 * @param <L> label type
 */
public class RQueue<L, V extends Number> {

    public final int capacity;
    private List<Double> values;
    private List<L> labels;
    int nbValues;
    double extremeValue;
    boolean maximize = true;

    public RQueue(int capacity) {
        this(capacity, true);
    }

    public RQueue(int capacity, boolean maximize) {
        this.maximize = maximize;
        this.capacity = capacity;
        this.values = new ArrayList(capacity + 1);
        this.labels = new ArrayList(capacity + 1);
        nbValues = 0;
    }

    /**
     *
     * @param label label 
     * @param value value
     * @return true if the value is added
     */
    public boolean add(L label, V value) {

        double v = value.doubleValue();
        
        if (Double.isNaN(v)) {
            return false;
        }
//        System.out.println(v+"/"+lowestValue);
        if (nbValues < capacity) {

            int id = getID(v);
            values.add(id, v);
            labels.add(id, label);
            nbValues++;

            extremeValue = values.get(nbValues - 1);

//            System.out.println("id: " + id);
            return true;
        } else if (maximize && v > extremeValue) {

//            System.out.println("pass");
            int id = getID(v);
            values.add(id, v);
            labels.add(id, label);
            values.remove(nbValues);
            labels.remove(nbValues);
            extremeValue = values.get(nbValues - 1);

//            System.out.println("id: " + id);
            return true;

        } else if (!maximize && v < extremeValue) { // minimize

//            System.out.println("pass");
            int id = getID(v);
            values.add(id, v);
            labels.add(id, label);
            values.remove(nbValues);
            labels.remove(nbValues);
            extremeValue = values.get(nbValues - 1);

//            System.out.println("id: " + id);
            return true;
        }

        return false;
    }

    private int getID(double value) {

        for (int i = 0; i < nbValues; i++) {

            if (maximize && value > values.get(i)) {
                return i;
            } else if (!maximize && value < values.get(i)) {
                return i;
            }

        }
        return nbValues;
    }

    @Override
    public String toString() {
        String out = "values: " + nbValues + "/" + capacity + "\n";

        for (int i = 0; i < values.size(); i++) {
            out += i + "\t" + values.get(i) + "\t" + labels.get(i) + "\n";
        }
        return out;
    }

    public static void main(String[] args) {

        RQueue<String, Double> kbestValues = new RQueue(3, true);

        System.out.println(kbestValues.toString());

        
        kbestValues.add("Nan", Double.NaN);
        kbestValues.add("King", 0.0);
        kbestValues.add("Camel2", 0.7);

        System.out.println(kbestValues.toString());

        kbestValues.add("Queen", 1.0);

        System.out.println(kbestValues.toString());

        kbestValues.add("Monkey", 0.2);

        System.out.println(kbestValues.toString());

        kbestValues.add("Camel", -0.7);

        System.out.println(kbestValues.toString());

        kbestValues.add("-0.5", -0.5);

        System.out.println(kbestValues.toString());

        kbestValues.add("0.5", 0.5);

        System.out.println(kbestValues.toString());

        kbestValues.add("-100", -100.0);

        System.out.println(kbestValues.toString());

    }

    public List<Double> getValues() {
        return values;
    }

    public List<L> getLabels() {
        return labels;
    }

}
