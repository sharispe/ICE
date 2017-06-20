/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.utils;

import java.util.Set;
import org.openrdf.model.URI;
import slib.utils.impl.SetUtils;

/**
 *
 * @author sharispe
 */
public class UtilsEval {
    
    public static double precision(Set<URI> relevant, Set<URI> retrieved) {
        return (double) SetUtils.intersection(relevant, retrieved).size() / (double) retrieved.size();
    }

    public static double recall(Set<URI> relevant, Set<URI> retrieved) {
        return (double) SetUtils.intersection(relevant, retrieved).size() / (double) relevant.size();
    }

    public static double fmesure(Set<URI> relevant, Set<URI> retrieved) {
        double precision = precision(relevant, retrieved);
        double recall = recall(relevant, retrieved);
        
        if(precision + recall == 0) return 0;

        double fmesure = 2.0 * (precision * recall) / (precision + recall);
        return fmesure;
    }
    
}
