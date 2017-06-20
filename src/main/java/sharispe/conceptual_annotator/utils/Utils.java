/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.core.EvocationVectorFactory;
import sharispe.conceptual_annotator.core.Index;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class Utils {

    public static double[] summation(double[] vectorA, double[] vectorB) {

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("vectors must have the same lenght");
        }

        double[] sum = new double[vectorA.length];

        for (int i = 0; i < vectorA.length; i++) {
            sum[i] = vectorA[i] + vectorB[i];
        }
        return sum;
    }

    public static double[] minimize(double[] vectorA, double[] vectorB) {

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("vectors must have the same lenght");
        }

        double[] min = new double[vectorA.length];

        for (int i = 0; i < vectorA.length; i++) {

            // We get the mininmal non null value if one exists
//            if (vectorA[i] == 0 || vectorB[i] == 0) {
//                min[i] = vectorA[i] != 0 ? vectorA[i] : vectorB[i];
//            } else {
            min[i] = vectorA[i] < vectorB[i] ? vectorA[i] : vectorB[i];
//            }
        }
        return min;
    }

    // from http://stackoverflow.com/questions/11955728/how-to-calculate-the-median-of-an-array
    public static double median(double[] numArray) {
        Arrays.sort(numArray);
        double median;
        if (numArray.length % 2 == 0) {
            median = ((double) numArray[numArray.length / 2] + (double) numArray[numArray.length / 2 - 1]) / 2;
        } else {
            median = (double) numArray[numArray.length / 2];
        }
        return median;
    }

    public static double sum(double[] numArray) {
        double sum = .0;
        for (double val : numArray) {
            sum += val;
        }
        return sum;
    }

    public static double min(double[] numArray) {
        double min = numArray[0];
        for (int i = 1; i < numArray.length; i++) {
            if (min > numArray[i]) {
                min = numArray[i];
            }
        }
        return min;
    }

    public static double max(double[] numArray) {
        double max = numArray[0];
        for (int i = 1; i < numArray.length; i++) {
            if (max < numArray[i]) {
                max = numArray[i];
            }
        }
        return max;
    }

    public static void printRQUEUE(RQueue<URI, Double> resultSet) {
        List<URI> bestRelatedConcepts = resultSet.getLabels();
        List<Double> bestRelatedValues = resultSet.getValues();

        for (int i = 0;
                i < bestRelatedConcepts.size();
                i++) {
            String relTerm = bestRelatedConcepts.get(i).getLocalName();
            double sim = bestRelatedValues.get(i);
            System.out.println("\t" + relTerm + "\t" + sim);
        }
    }

    public static double sim(double[] vectorA, double[] vectorB) {

        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("vectors must have the same lenght");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Load the queries from the given file. Only the queries respecting the
     * following constraints will be loaded: (1) the concept URIs of the query
     * have to be contained in the provided URIs
     *
     * @param queryFile file defining the queries
     * @param voc the terms that can be used in a query (all terms defined in a
     * query that are not found in this set will be removed)
     * @param uris the URIs that are accepted as query expected answer
     * @param f the factory used to build concept URIs
     * @param uriPrefix the prefix to consider when building the URI
     * @param keepUpperCaseLabels does the terms provided in the query have to
     * be kept to Upper case if any upper case letter exists
     * @return the set of queries respecting the constrained defined above
     * @throws Exception
     */
    public static Set<Query> loadQueries(String queryFile, Set<String> voc, Set<URI> uris, URIFactory f, String uriPrefix, boolean keepUpperCaseLabels) throws Exception {

        System.out.println("----------------------------------------------------");
        System.out.println("Loading queries from: " + queryFile);
        System.out.println("----------------------------------------------------");
        Set<Query> queries = new HashSet();

        boolean containsHeader = true; // the query file is expected to contain a header

        try (BufferedReader br = new BufferedReader(new FileReader(queryFile))) {
            String line;

            while ((line = br.readLine()) != null) {

                if (containsHeader) {
                    containsHeader = false;
                    continue;
                }

                line = line.trim();

                String data[] = line.split(",");

                if (data.length <= 2) { // expect metadata + at least 2 terms
                    System.out.println("skipping line: " + line);
                    continue;
                }

                String id_query = data[0].trim();
                String expected_concept_label = data[1].trim();
                String URI_LocalName = data[2].trim();
                
                if(URI_LocalName.length()!=8) URI_LocalName = "0"+URI_LocalName;

                URI uri = f.getURI(uriPrefix + URI_LocalName);

                if (!uris.contains(uri)) {
                    System.out.println("Cannot find " + uri + " as a valid URI");
                    System.out.println("Excluding query: " + line);
                    continue;
                }

                Set<String> v = new HashSet();

                for (int i = 3; i < data.length; i++) {
                    String label = data[i].trim();
                    if (!label.isEmpty()) {
                        if (!keepUpperCaseLabels) {
                            label = label.toLowerCase();
                        }
                        if (voc.contains(label)) {
                            v.add(label);
                        }
                    }
                }

                if (v.size() < 2) {
                    System.out.println("Excluding query: " + line);
                    System.out.println("unsufficient number of terms with embedding: " + v);
                    continue;
                }
                queries.add(new Query(id_query, expected_concept_label, uri, v));
            }
        }

        System.out.println("queries loaded " + queries.size());
        return queries;
    }

    /**
     * Conceptual Evocation	WordNet Synset id	Term 1	Term 2	Term 3	Term…
     * [0]String associated to a label of conceptual evocation [1] Wordnet
     * Synset ID [2] Query Term 0 [3] Query Term 1 [4] ...
     *
     * @param f
     * @param uriPrefix
     * @param path
     * @param containsHeader
     * @param keepUpperCaseLabels
     * @return
     * @throws Exception
     */
    public static Map<URI, Set<String>> loadQueriesFromTSV(URIFactory f, String uriPrefix, String path, boolean containsHeader, boolean keepUpperCaseLabels) throws Exception {

        Map<URI, Set<String>> index = new HashMap<>();

        return index;
    }

    public static Map<URI, List<String>> loadIndexFromTSV(URIFactory f, String path, boolean containsHeader, boolean keepUpperCaseLabels) throws Exception {

        Map<URI, List<String>> index = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {

                if (containsHeader) {
                    containsHeader = false;
                    continue;
                }

                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    if (!line.isEmpty()) {
                        System.out.println("skipping line : " + line);
                    }
                    continue;
                }

                String data[] = line.split(",");

                if (data.length < 2) {
                    System.out.println("skipping line : " + line);
                    continue;
                }

                List<String> v = new ArrayList();

                for (int i = 1; i < data.length; i++) {
                    String label = data[i].trim();
                    if (!keepUpperCaseLabels) {
                        label = label.toLowerCase();
                    }
                    v.add(label);
                }
                index.put(f.getURI(data[0].trim()), v);

            }
        }
        return index;
    }

    public static String getLabel(URI uri, Index indexURILabels) {
        String label = "NOT_FOUND";
        if (indexURILabels.containsLabelsForURI(uri)) {
            label = indexURILabels.getLabels(uri).iterator().next();
        }
        return label;
    }

    public static String getLabels(URI uri, Index indexURILabels) {
        String labels = "NOT_FOUND";
        if (indexURILabels.containsLabelsForURI(uri)) {
            labels = indexURILabels.getLabels(uri).toString();
        }
        return labels;
    }

    public static Map<String, Set<URI>> buildLabelToURIsIndex(Map<URI, Set<String>> indexURItoLabels) {

        Map<String, Set<URI>> indexLabelToUris = new HashMap();
        for (URI u : indexURItoLabels.keySet()) {
            for (String s : indexURItoLabels.get(u)) {
                if (!indexLabelToUris.containsKey(s)) {
                    indexLabelToUris.put(s, new HashSet<URI>());
                }
                indexLabelToUris.get(s).add(u);
            }
        }
        return indexLabelToUris;
    }

    public static void printResultsToFile(String outputFile, Query query, RQueue<URI, Double> results, Index indexURILabels) throws FileNotFoundException {

        try (PrintWriter printWriter = new PrintWriter(outputFile)) {
            printWriter.println("#query id=" + query.id + " conceptLabel=" + query.conceptLabel + " conceptURI=" + query.conceptURI.getLocalName() + "  labels=" + query.terms);

            for (int i = 0; i < results.getLabels().size(); i++) {
                URI resultURI = results.getLabels().get(i);
                printWriter.println(i + "\t" + resultURI.getLocalName() + "\t" + Utils.getLabels(resultURI, indexURILabels));
            }
        }

    }

    public static void flushValidQueries(String outputFile, Set<Query> queries) throws FileNotFoundException {
        try (PrintWriter printWriter = new PrintWriter(outputFile)) {
            for (Query query : queries) {
                printWriter.println(query.id + "\t" + query.conceptURI.getLocalName() + "\t" + query.conceptLabel + "\t" + query.terms);
            }
        }
    }
}
