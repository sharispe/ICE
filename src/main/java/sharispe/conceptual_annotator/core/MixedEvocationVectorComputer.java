/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.utils.RQueue;
import sharispe.conceptual_annotator.utils.Utils;
import slib.sml.sm.core.engine.SM_Engine;
import slib.sml.sm.core.metrics.ic.utils.IC_Conf_Topo;
import slib.sml.sm.core.metrics.ic.utils.ICconf;
import slib.sml.sm.core.utils.SMConstants;
import slib.sml.sm.core.utils.SMconf;
import slib.utils.ex.SLIB_Ex_Critic;

/**
 *
 * @author seb
 */
public class MixedEvocationVectorComputer {

    private EmbeddingIndex embeddings;
    private SM_Engine conceptSim;
    public final Map<URI, Integer> conceptIds;
    public final Map<String, Integer> termIds;
    private Map<URI, Set<String>> indexUriToLabels;
    private Map<String, Set<URI>> indexLabelToUris;
    private double[] termEvocation;
    private double[] conceptEvocations;
    double thresholdSpreading = 0.01;
    int SPREAD_FACTOR = 1000; // number of closest neighboring nodes to consider
    double SYNONYM_DECAY_FACTOR = 0.01;
    boolean useConceptOrdering;

//    Map<URI, Set<URI>> parentCache;
//    Map<URI, Set<URI>> childrenCache;
//    Map<String, RQueue<String, Double>> bestTermProxCache;
    SMconf measureConf;

    public MixedEvocationVectorComputer(SM_Engine engine, EmbeddingIndex embeddings, Map<URI, Set<String>> indexUriToLabels, Map<String, Set<URI>> indexLabelToURIs, boolean useConceptOrdering) throws SLIB_Ex_Critic {

        this.conceptSim = engine;
        this.embeddings = embeddings;
        this.conceptIds = new HashMap();
        this.indexUriToLabels = indexUriToLabels;
        this.indexLabelToUris = indexLabelToURIs;

        this.useConceptOrdering = useConceptOrdering;

        // define an id for all terms and concepts
        // it will be used to strore associated value
        this.termIds = new HashMap();
        for (String s : embeddings.getVocabulary()) {
            termIds.put(s, termIds.size());
        }

        for (URI u : indexUriToLabels.keySet()) {
            conceptIds.put(u, conceptIds.size());
        }

        termEvocation = new double[termIds.size()];
        conceptEvocations = new double[conceptIds.size()];

        ICconf iconf = new IC_Conf_Topo(SMConstants.FLAG_ICI_SECO_2004);
        measureConf = new SMconf(SMConstants.FLAG_SIM_PAIRWISE_DAG_NODE_LIN_1998);
        measureConf.setICconf(iconf);

//        parentCache = new HashMap<>();
//        childrenCache = new HashMap<>();
//        bestTermProxCache = new HashMap<>();
    }

    public void spreadTermEvocation(String term) throws Exception {

        if (!embeddings.contains(term)) {
            throw new Exception("No vector representation for term: " + term + ". Concept labels or query terms that are not associated to embeddings have to be removed");
        }

        Set<String> processedTerms = new HashSet();
        Set<URI> processedConcepts = new HashSet();
        spreadTermEvocation_inner(term, 1, processedTerms, processedConcepts);
    }

    public double[] getTermEvocationVector() {
        return Arrays.copyOf(termEvocation, termEvocation.length);
    }

    public double[] getConceptualEvocationVector() {
        return Arrays.copyOf(conceptEvocations, conceptEvocations.length);
    }

    public void clearEvocationVectors() {
        termEvocation = new double[embeddings.getVocabulary().size()];
        conceptEvocations = new double[conceptIds.size()];
    }

    private void spreadConceptInner(URI uri, double quantityToSpread, Set<String> processedTerms, Set<URI> processedConcepts) throws SLIB_Ex_Critic {

        processedConcepts.add(uri);

//        System.out.println("spreading from "+uri);
//        System.out.println("processed concepts "+processedConcepts);
//        System.out.println("processed terms "+processedTerms);
        conceptEvocations[conceptIds.get(uri)] += quantityToSpread;

        if (useConceptOrdering) {

            if (quantityToSpread > thresholdSpreading) {

                // propagate the quantity / 2 to unprocessed synonyms of the current concept if any
                for (String synonym : indexUriToLabels.get(uri)) {

                    if (!processedTerms.contains(synonym)) {
//                        System.out.println("spreading to label synonym: " + synonym);
                        spreadTermEvocation_inner(synonym, quantityToSpread * SYNONYM_DECAY_FACTOR, processedTerms, processedConcepts);
                    }
                }
            }

            Set<URI> neighbors = new HashSet<>(conceptSim.getParents(uri));
            neighbors.addAll(conceptSim.getDescendantEngine().getNeighbors(uri));
            double sum = .0;
            for (URI n : neighbors) {
                sum += conceptSim.compare(measureConf, uri, n);
            }

            for (URI n : neighbors) {

                double sim = conceptSim.compare(measureConf, uri, n);
                double spreadValue = quantityToSpread * sim / sum * 0.1;
                conceptEvocations[conceptIds.get(n)] += spreadValue;

                if (spreadValue > thresholdSpreading && !processedConcepts.contains(n)) {
                    spreadConceptInner(n, spreadValue, processedTerms, processedConcepts);
                }
            }
        }
        processedConcepts.remove(uri);
    }

    private void spreadTermEvocation_inner(String term, double quantityToSpread, Set<String> processedTerms, Set<URI> processedConcepts) throws SLIB_Ex_Critic {

        processedTerms.add(term);
//        System.out.println("spreading quantity " + quantityToSpread + " from term: " + term);
        //System.out.println("spreading quantity " + quantityToSpread + " to term: " + term + " (terms visited by current propagation: " + processedTerms.size() + ")");
        //System.out.println("processed terms: " + processedTerms);
//        System.out.println("processed concepts: " + processedConcepts);

        int idTerm = termIds.get(term);
        termEvocation[idTerm] += quantityToSpread;

        // propagate the given quantity to concepts with this label (if any)
        if (indexLabelToUris.containsKey(term)) {
            for (URI uri : indexLabelToUris.get(term)) {
//                System.out.println("spreading to desambiguation (concept): " + uri);
                if (!processedConcepts.contains(uri)) {
                    spreadConceptInner(uri, quantityToSpread, processedTerms, processedConcepts);
                }
            }
        }

        // if the quantity is above spreading threshold
        // propagate to k best related terms with similarities
        if (quantityToSpread > thresholdSpreading) {

            RQueue<String, Double> bestRelatedTermSim = getBestRelatedTermSim(term);
            List<String> bestRelatedTerms = bestRelatedTermSim.getLabels();
            List<Double> bestRelatedValues = bestRelatedTermSim.getValues();

            // compute normalizing factor
            double sum = 0;
            for (int i = 0; i < bestRelatedTerms.size(); i++) {
                sum += bestRelatedValues.get(i);
            }

            for (int i = 0; i < bestRelatedTerms.size(); i++) {
                String relTerm = bestRelatedTerms.get(i);
                double sim = quantityToSpread * bestRelatedValues.get(i) / sum;

                spreadTermEvocation_inner(relTerm, sim, processedTerms, processedConcepts);
            }
        }
        processedTerms.remove(term);
    }

    public void showBestTermEvocations() {
        RQueue<String, Double> q = new RQueue(30);
        for (String s : termIds.keySet()) {
            q.add(s, termEvocation[termIds.get(s)]);
        }
        List<String> bestRelatedTerms = q.getLabels();
        List<Double> bestRelatedValues = q.getValues();

        System.out.println("-----------");
        for (int i = 0; i < bestRelatedTerms.size(); i++) {
            String relTerm = bestRelatedTerms.get(i);
            double sim = bestRelatedValues.get(i);
            System.out.println("\t" + relTerm + "\t" + sim);
        }
        System.out.println("-----------");

    }

    public void showBestConceptEvocations() {
        RQueue<URI, Double> q = new RQueue(30);
        for (URI s : conceptIds.keySet()) {
            q.add(s, conceptEvocations[conceptIds.get(s)]);
        }
        List<URI> bestRelatedURI = q.getLabels();
        List<Double> bestRelatedValues = q.getValues();

        System.out.println("-----------");
        for (int i = 0; i < bestRelatedURI.size(); i++) {
            URI relConcept = bestRelatedURI.get(i);
            double sim = bestRelatedValues.get(i);
            System.out.println("\t" + indexUriToLabels.get(relConcept) + "\t" + sim);
        }
        System.out.println("-----------");

    }

    private RQueue<String, Double> getBestRelatedTermSim(String term) {
        double[] t_vec = embeddings.get(term);

        RQueue<String, Double> kBest = new RQueue(SPREAD_FACTOR);
        for (String s : embeddings.getVocabulary()) {
            if (!s.equals(term)) {
                double[] s_vec = embeddings.get(s);
                double sim = Utils.sim(t_vec, s_vec);
                kBest.add(s, sim);
            }
        }
        return kBest;
    }
}
