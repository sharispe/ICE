/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

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
import sharispe.conceptual_annotator.utils.UtilsEval;
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
import slib.utils.impl.MapUtils;
import slib.utils.impl.UtilDebug;

/**
 *
 * @author sharispe
 */
public class MainVectorAggregationApproaches {

    public static void main(String[] args) {

        try {

            /**
             * **************************************************************
             * Configuration
             * **********************************************************
             */
            boolean perform_sampling = true;
            int max_size_sampling = 6;
            int nb_samplings = 100000;
            boolean acceptLowerSampling = true;

            String selected_model_name = Conf.model_name_glove_6B_50d;
            String selected_model_path = Conf.model_path_glove;
            boolean keepUpperCaseLabels = false; // for glove 

            String outputDirectory = Conf.outputDir;
            String model_voc_path = selected_model_path + "/" + selected_model_name + ".txt";

            AggregationMethod[] aggMethods = {AggregationMethod.SUM, AggregationMethod.MIN};

            /**
             * *****************************************************************
             * Load Index / Vectors / Queries
             * ****************************************************************
             */
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

            Set<Query> queries = Utils.loadQueries(queryFile, voc, indexURILabels.getURIs(), factory, uriPrefix_query, keepUpperCaseLabels);
            Utils.flushValidQueries(outputDirectory + "/queries_valid_vbased.tsv", queries);
            
            // We build a set containing all the URIs defined as expected results
            // of given queries. It will be used to performing the evaluation 
            // on a subset of URIs -- set to null if you don't want to apply a restriction
            Set<URI> resultURIs_restriction = indexURILabels.getURIs();
            if (Conf.ONLY_ACCEPT_QUERY_URIS_AS_RESUTLS) {
                resultURIs_restriction = new HashSet<>();
                for (Query q : queries) {
                    resultURIs_restriction.add(q.conceptURI);
                }
            }

            EvocationVectorFactory evocationVectorFactory = new EvocationVectorFactory(resultURIs_restriction);
            EvocationVectorComputer CEVComputer = new EvocationVectorComputer(evocationVectorFactory, indexURILabels, embeddings);

            /**
             * ***************************************************************
             * Perform Sampling if required
             * ***************************************************************
             */
            for (AggregationMethod aggregationMethod : aggMethods) {

                String samplingInfoFilePath = System.getProperty("user.dir") + "/resources/sampling_info_" + selected_model_name + "_agg=" + aggregationMethod.name() + ".txt";

                if (perform_sampling) {
                    CEVSampler.sample(nb_samplings, 1, max_size_sampling, CEVComputer, voc, aggregationMethod, samplingInfoFilePath);
                }

                /**
                 * ***************************************************************
                 * Load Sampling Data
                 * **************************************************************
                 */
                SamplingInfo samplingInfo = CEVSampler.loadSamplingInfo(samplingInfoFilePath, factory);
                samplingInfo.checkContainsInfo(evocationVectorFactory.indexURI.keySet());

                /**
                 * ****************************************
                 * Restrict possible results set availableURIs to null for no
                 * restriction ****************************************
                 */
                System.out.println("Applying restriction to possible results");

                Set<URI> availableURIs = new HashSet();
                for (Query q : queries) {
                    availableURIs.add(q.conceptURI);
                }

                /**
                 * ***************************************************************
                 * Full Evaluation
                 * **************************************************************
                 */
                System.out.println("Evaluation");

                Map< Query, RQueue<URI, Double>> query_results = new HashMap<>();

                for (Query query : queries) {
                    RQueue<URI, Double> results = performQuery(CEVComputer, samplingInfo, query, aggregationMethod, acceptLowerSampling, indexURILabels, availableURIs);
                    query_results.put(query, results);

                    String outputFile = outputDirectory + "/query_" + query.id + "_method=vector-based_agg=" + aggregationMethod.name() + "_embeddings=" + selected_model_name + ".tsv";
                    Utils.printResultsToFile(outputFile, query, results, indexURILabels);
                }

                /**
                 * ***************************************************************
                 * Precision / Recall / F-measure
                 * **************************************************************
                 */
                System.out.println("Analysing results");

                for (int k = 1; k <= 10; k++) {

                    double precision_sum = 0;
                    double recall_sum = 0;
                    double fmeasure_sum = 0;

                    for (Query q : query_results.keySet()) {

//                System.out.println("Processing query: " + q.getConceptURI() + "\t" + q.getWords());
                        Set<URI> expected_results = new HashSet<>();
                        expected_results.add(q.conceptURI);

                        Set<URI> given_results = new HashSet<>();

                        List<URI> search_results_uri = query_results.get(q).getLabels();
                        List<Double> search_results_scores = query_results.get(q).getValues();

                        for (int i = 0; i < search_results_uri.size() && i < k; i++) {
//                        System.out.println(search_results_uri.get(i) + "\t" + search_results_scores.get(i));
                            given_results.add(search_results_uri.get(i));
                        }

                        double precision = UtilsEval.precision(expected_results, given_results);
                        double recall = UtilsEval.recall(expected_results, given_results);
                        double fmeasure = UtilsEval.fmesure(expected_results, given_results);

//                System.out.println("Precision: "+precision);
//                System.out.println("Recall: "+recall);
//                System.out.println("Fmeasure: "+fmeasure);
//                System.out.println("-----------------");
                        precision_sum += precision;
                        recall_sum += recall;
                        fmeasure_sum += fmeasure;
                    }
                    System.out.println("--------------------------------------------------------------------");
                    System.out.println("k = " + k);
                    System.out.println("Average Precision : " + (precision_sum / (double) queries.size()));
                    System.out.println("Average Recall    : " + (recall_sum / (double) queries.size()));
                    System.out.println("Average Fmeasure  : " + (fmeasure_sum / (double) queries.size()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Exception detected: " + e.getMessage());
        }

    }

    private static RQueue<URI, Double> performQuery(EvocationVectorComputer CEVComputer, SamplingInfo samplingInfo, Query q, AggregationMethod aggMethod, boolean acceptLowerSampling, Index indexURILabels, Set<URI> acceptedURIs) throws Exception {
        Set<String> querySet = q.terms;
        System.out.println("----------------------------------------------------");
        System.out.println("- query: " + querySet);
        System.out.println("-      : expected:  " + q.conceptURI + " (" + indexURILabels.getPreferredLabel(q.conceptURI) + ")");
        System.out.println("----------------------------------------------------");
        EvocationVector conceptualEvocationVector = CEVComputer.computeCEV(q.terms, aggMethod);
        return computeScores(conceptualEvocationVector, samplingInfo, q, acceptLowerSampling, indexURILabels, acceptedURIs);
    }

    /**
     *
     * @param conceptualEvocationVector
     * @param samplingInfo
     * @param q
     * @param acceptLowerSampling
     * @param indexURILabels
     * @param acceptedURIs restriction on the set of URIs which can be provided.
     * All URIs associated to the conceptual Evocation Vector are considered if
     * it is set to null.
     * @return
     * @throws Exception
     */
    private static RQueue<URI, Double> computeScores(EvocationVector conceptualEvocationVector, SamplingInfo samplingInfo, Query q, boolean acceptLowerSampling, Index indexURILabels, Set<URI> acceptedURIs) throws Exception {

        Map<URI, Double> divergences = new HashMap<>();
        Map<URI, Double> vcr = new HashMap<>(); // variable centree reduite
        RQueue<URI, Double> results10best = new RQueue(acceptedURIs.size());

        int size_query = q.terms.size();
        SamplingInfoLocal info = samplingInfo.getSamplingInfoLocal(size_query, acceptLowerSampling);

        acceptedURIs = acceptedURIs != null ? acceptedURIs : new HashSet(conceptualEvocationVector.getURIOrdering());

        for (URI currentURI : acceptedURIs) {

            String preferredLabel = indexURILabels.getPreferredLabel(currentURI);
            // skip URIs for which a label defined as preferred label is in the query
            if (q.terms.contains(preferredLabel)) {
                System.out.println("skipping: " + currentURI + " preferred label contained in the query " + preferredLabel);
                continue;
            }

            double evocationValue = conceptualEvocationVector.get(currentURI);
            double standard_deviation = info.getStandardDeviation(currentURI);
            double average_value = info.getAverage(currentURI);

            double vcr_ = standard_deviation != 0 ? (evocationValue - average_value) / standard_deviation : 0.0;// variable centree reduite
            vcr.put(currentURI, vcr_);

            //double divergence = Math.abs((evocationValue - averageValues.get(currentURI)) / cutoff.get(currentURI));// averageValues.get(currentURI));
            double divergence = average_value != 0 ? (evocationValue - average_value) / average_value : 0.0;
            divergences.put(currentURI, divergence);

            results10best.add(currentURI, vcr_);
        }

        StringBuilder log = new StringBuilder();

        log.append("\tRANK\tURI\tURI_label\tVECTOR_VALUE\tMIN_BEST_VALUE\tAVERAGE\tDIVERGENCE\tVARIABLE_CENTRE_REDUITE\tIS_VALIDATED (true=*)\n");

        int count = 0;
        int expected_rank = 0;

        for (URI uri : MapUtils.sortByValueDecreasing(vcr).keySet()) {

            double evocationValue = conceptualEvocationVector.get(uri);
            double divergence = divergences.get(uri);
            double average_value = info.getAverage(uri);
            double vcr_ = vcr.get(uri);
            double min_best = info.getMinBest(uri);

            count++;

            String validated = evocationValue > min_best ? "*" : "-";

            String uriLabel = indexURILabels.getPreferredLabel(uri);
            String marker = "";
            if (q.conceptURI.equals(uri)) {
                marker = "***";
                expected_rank = count;
            }

            log.append(marker + "\t" + count + "\t" + uri + "\t" + uriLabel + "\t" + evocationValue + "\t" + min_best + "\t" + average_value + "\t" + divergence + "\t" + vcr_ + "\t" + validated + "\n");
        }
        System.out.println("Expected: rank " + expected_rank + "/" + vcr.size());
        System.out.println(log.toString());
        System.out.println("-----------------------");
        return results10best;
    }
}
