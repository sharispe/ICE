/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.utils.Utils;

/**
 *
 * @author sharispe
 */
public class EvocationVectorComputer {
    
    public EvocationVectorFactory evf;
    Index indexURIToLabels;
    EmbeddingIndex embeddings;
    public static final Double INIT_VEC_VALUE = 0.0;
    
    
    public EvocationVectorComputer(EvocationVectorFactory evf, Index indexURIToLabels, EmbeddingIndex embeddings){
        this.evf = evf;
        this.embeddings = embeddings;
        this.indexURIToLabels = indexURIToLabels;
    }
    
    
    /**
     * Compute CEV for the set of terms considering the given embeddings
     * CEV Conceptual Evocation Vector
     * @param set
     * @param aggMethod
     * @return 
     */
    public EvocationVector computeCEV(Set<String> set, AggregationMethod aggMethod) {

        //System.out.println("Computing conceptual evocation for "+set);
        EvocationVector evoc_agg = evf.buildEvocationVector();

        boolean first = true;

        for (String w : set) {

            if (!embeddings.contains(w)) {
                System.out.println("No embedding for '" + w + "'");
                continue;
            }

            EvocationVector evoc_w = computeCEV(w);

            if (evoc_w != null) {

                if (first) {
                    evoc_agg = evoc_w;
                    first = false;
                } else {
                    
                    double[] modif = null;
                    
                    if (aggMethod == AggregationMethod.SUM) {
                        modif = Utils.summation(evoc_agg.getVec(), evoc_w.getVec());
                    } else if (aggMethod == AggregationMethod.MIN) {
                        modif = Utils.minimize(evoc_agg.getVec(), evoc_w.getVec());
                    }
                    evoc_agg.setVectorValues(modif);
                }
            }
        }

        return evoc_agg;

    }

    public EvocationVector computeCEV(String s) {

//        System.out.println("computing conceptual evocation for "+s);
        EvocationVector vec = evf.buildEvocationVector();

        if (!embeddings.contains(s)) {
//            System.out.println("embeddings does not contains vector for '" + s + "'");
            return null;
        }

        double[] vec_s = embeddings.get(s);

        for(URI u : evf.orderedUris) {


            String lmax = null;
//            System.out.println("\t\t" + u);
            double prox_u = INIT_VEC_VALUE;
            // max from all the compared labels

            Set<String> labels = indexURIToLabels.getLabels(u);
            if (!labels.isEmpty()) {

                boolean isFirstLabel = true;

                for (String l : labels) {

//                    System.out.print("\t\t\ttesting label "+l);
                    if (embeddings.contains(l)) {
                        double tmpprox = Utils.sim(vec_s, embeddings.get(l));
//                        System.out.println(" --- sim: "+tmpprox);
                        if (isFirstLabel || tmpprox >= prox_u) {
                            prox_u = tmpprox;
                            lmax = l;
                            isFirstLabel = false;
                        }
                    } else {
//                        System.out.println("embeddings does not contains vector for '" + l + "'");
                    }

                }
            } else {
//                System.out.println("No label for "+u);
            }
            vec.set(u,prox_u);
//            System.out.println("\t\t\t" + lmax + "\t" + prox_u);
        }
        return vec;
    }

    
}
