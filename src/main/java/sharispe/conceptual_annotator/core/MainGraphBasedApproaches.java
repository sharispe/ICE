/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.utils.Conf;
import sharispe.conceptual_annotator.utils.Query;
import sharispe.conceptual_annotator.utils.RQueue;
import sharispe.conceptual_annotator.utils.Utils;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.loader.wordnet.GraphLoader_Wordnet;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;
import slib.sml.sm.core.engine.SM_Engine;
import slib.utils.ex.SLIB_Ex_Critic;

/**
 *
 * Class used to perform the experiments for Graph-based approaches
 *
 * @author Sébastien Harispe
 */
public class MainGraphBasedApproaches {

    public static void main(String[] args) throws Exception {

        String selected_model_name = Conf.model_name_glove_6B_50d;
        String selected_model_path = Conf.model_path_glove;
        boolean keepUpperCaseLabels = false; // for glove 

        int best_k = 100; // for each query only the best k results (concepts) will be considered (in the final output)

        String outputDirectory = Conf.outputDir;
        String model_voc_path = selected_model_path + "/" + selected_model_name + ".txt";

        // Factory used to generate the URIs
        URIFactory factory = URIFactoryMemory.getSingleton();
        URI guri = factory.getURI(Conf.uriPrefix_wordnet);

        // Graph that will store the Wordnet synset partial ordering
        G wordnet = new GraphMemory(guri);

        // We load the data into the graph
        GraphLoader_Wordnet loader = new GraphLoader_Wordnet();
        GDataConf dataNoun = new GDataConf(GFormat.WORDNET_DATA, Conf.wordnetData + "data.noun");
        loader.populate(dataNoun, wordnet);

        GAction action = new GAction(GActionType.TRANSITIVE_REDUCTION);
        GraphActionExecutor.applyAction(action, wordnet);

        // Object used to compute concept similarities
        // by analysing the concept partial ordering
        SM_Engine engine = new SM_Engine(wordnet);

        // only those URIs matter, i.e. URI that are associated to node in the graph
        Set<URI> graphURIs = engine.getClasses();

        // defining the embedding configuration
        EmbeddingConf selectedEmbeddingConf = new EmbeddingConf(model_voc_path, keepUpperCaseLabels);

        String indexFile = Conf.indexFile_wordnet; // index defining the labels of concepts 
        String queryFile = Conf.queryFile;

        String uriPrefix_query = Conf.uriPrefix_wordnet;

        // Loading word embeddings considering the defined configuration
        EmbeddingIndex embeddings = new EmbeddingIndex(selectedEmbeddingConf);
        Set<String> voc = embeddings.getVocabulary();

        // Loading the index linked concepts and associated labels 
        // Labels that are not associated to any embedding are removed
        Index indexURILabels = new Index(indexFile, factory, graphURIs, keepUpperCaseLabels);
        indexURILabels.removeLabelNotFound(voc);

        Map<String, Set<URI>> indexLabelToURIs = Utils.buildLabelToURIsIndex(indexURILabels.indexURIToLabels);

        Set<Query> queries = Utils.loadQueries(queryFile, voc, graphURIs, factory, uriPrefix_query, keepUpperCaseLabels);
        Utils.flushValidQueries(outputDirectory + "/queries_valid_gbased.tsv", queries);

        // We build a set containing all the URIs defined as expected results
        // of given queries. It will be used to performing the evaluation 
        // on a subset of URIs -- set to null if you don't want to apply a restriction
        Set<URI> resultURIs_restriction = null;
        if (Conf.ONLY_ACCEPT_QUERY_URIS_AS_RESUTLS) {
            resultURIs_restriction = new HashSet<>();
            for (Query q : queries) {
                resultURIs_restriction.add(q.conceptURI);
            }
        }

        // We print some info
        System.out.println("Number of concepts partially ordered into the graph " + graphURIs.size());
        System.out.println("Number of concepts indexed (with a set of labels/potentially empty depending on the embeddings): " + indexURILabels.indexURIToLabels.size());
        System.out.println("Number of queries: " + queries.size());
        int restriction_size = resultURIs_restriction == null ? 0 : resultURIs_restriction.size();
        System.out.println("Number of concepts in the restriction: " + restriction_size);

        // We load all the queries by only considering the queries that are 
        // (1) for which the expected result has been identified by a wordnet concept
        // (2) and for which at least two terms among the provided terms have an embedding
        // Now we perform the computation of the results 
        // for all queries considering all the Conceptual Evocation Vector (CEV) aggregation approaches
        // The process is defined as follow : 
        // Considering a query :
        // we compute the CEVs for all terms provided in the query
        // those vector are then aggregated using an aggregation strategy (sum, average, min, max)
        // since the first step aiming at computing the CEVs is the same
        // considering the a given global configuration (i.e. propagating to the conceptual layer or not)
        // the aggregation strategy is performed after computing the CEVs
        // Define the two graph-based global configurations
        // defining if we propagate or not considering the conceptual layer (i.e. concept partial ordering provided by WordNet)
        boolean[] useConceptOrderingConfs = {true, false};
        GRAPH_BASED_AGGREGATION[] aggStrategies = GRAPH_BASED_AGGREGATION.values();

        for (boolean useConceptOrdering : useConceptOrderingConfs) {

            int countQuery = 0;

            // For all queries 
            for (Query query : queries) {

                countQuery++;

                System.out.println("performing query " + query + "\t" + countQuery + "/" + queries.size());
                Set<URI> excludedURIs = new HashSet<>(); // URIs that have a label provided in the query are excluded (game setting)

                // we compute the conceptual evocation vector for all terms
                MixedEvocationVectorComputer mevc = new MixedEvocationVectorComputer(engine, embeddings, indexURILabels.indexURIToLabels, indexLabelToURIs, useConceptOrdering);

                Map<String, double[]> queryTerm_conceptEvocation = new HashMap<>();
                for (String term : query.terms) {
                    if (indexLabelToURIs.containsKey(term)) {
                        excludedURIs.addAll(indexLabelToURIs.get(term));
                    }
                    System.out.println("computing conceptual evocation vector for term: " + term);
                    mevc.spreadTermEvocation(term);
                    queryTerm_conceptEvocation.put(term, mevc.getConceptualEvocationVector().clone());
                    mevc.clearEvocationVectors();
                }

                // we compute the results 
                // for all aggregation strategies
                for (GRAPH_BASED_AGGREGATION aggregationMethod : aggStrategies) {

                    System.out.println("Aggregation: " + aggregationMethod.name());
                    String outputFile = outputDirectory + "/query_" + query.id + "_method=graph-based_use_concept_ordering=" + useConceptOrdering + "_agg=" + aggregationMethod.name() + "_embeddings="+selected_model_name+".tsv";

                    RQueue<URI, Double> results = aggregate(queryTerm_conceptEvocation, mevc.conceptIds, best_k, aggregationMethod, resultURIs_restriction, excludedURIs);

                    Utils.printRQUEUE(results);
                    Utils.printResultsToFile(outputFile, query, results, indexURILabels);
                }
            }
        }
    }

    /**
     *
     * @param queryTerm_conceptEvocation
     * @param conceptIds
     * @param best_k
     * @param aggregation_strategy
     * @param resultURIs_restriction set to null for no restriction
     * @param excludedURIs
     * @return
     * @throws SLIB_Ex_Critic
     */
    private static RQueue<URI, Double> aggregate(Map<String, double[]> queryTerm_conceptEvocation, Map<URI, Integer> conceptIds, int best_k, GRAPH_BASED_AGGREGATION aggregation_strategy, Set<URI> resultURIs_restriction, Set<URI> excludedURIs) throws SLIB_Ex_Critic {

        RQueue<URI, Double> resultSet = new RQueue(best_k);

        List<String> query = new ArrayList<>(queryTerm_conceptEvocation.keySet());

        int query_length = query.size();

        Set<URI> possibleURIresults = resultURIs_restriction != null ? resultURIs_restriction : conceptIds.keySet();

        for (URI conceptURI : possibleURIresults) {

            if (excludedURIs.contains(conceptURI)) {
                continue;
            }

            int concept_id = conceptIds.get(conceptURI);
            double[] val_term = new double[query_length];
            for (int i = 0; i < query_length; i++) {
                String t = query.get(i);
                val_term[i] = queryTerm_conceptEvocation.get(t)[concept_id];
            }
            double agg = 0;

            switch (aggregation_strategy) {
                case MEDIAN:
                    agg = Utils.median(val_term);
                    break;
                case SUM:
                    agg = Utils.sum(val_term);
                    break;
                case MIN:
                    agg = Utils.min(val_term);
                    break;
                case MAX:
                    agg = Utils.max(val_term);
                    break;
                default:
                    throw new SLIB_Ex_Critic("No aggregation defined for " + aggregation_strategy);
            }

            resultSet.add(conceptURI, agg);
        }
        return resultSet;
    }

    public enum GRAPH_BASED_AGGREGATION {

        MEDIAN, SUM, MIN, MAX;
    }
}
