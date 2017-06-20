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
import sharispe.conceptual_annotator.utils.Utils;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class Index {

    Map<URI, Set<String>> indexURIToLabels;
    Map<URI, String> indexURIPreferredLabels;

    /**
     * 
     * @param tsvIndex
     * @param factory
     * @param uriToConsider set to null if no restriction has to be applied
     * @param keepUpperCaseLabels
     * @throws Exception 
     */
    public Index(String tsvIndex, URIFactory factory, Set<URI> uriToConsider, boolean keepUpperCaseLabels) throws Exception {
        boolean containsHeader = false; // no header expected in the index file
        Map<URI, List<String>> data = Utils.loadIndexFromTSV(factory, tsvIndex, containsHeader, keepUpperCaseLabels);

        indexURIToLabels = new HashMap<>();
        indexURIPreferredLabels = new HashMap<>();

        for (URI u : data.keySet()) {

            if (uriToConsider == null || uriToConsider.contains(u)) {

                indexURIPreferredLabels.put(u, data.get(u).get(0));
                indexURIToLabels.put(u, new HashSet(data.get(u)));
            }
        }

    }

    void removeLabelNotFound(Set<String> voc) {
        // cleaning concept labels
        int countConceptWithoutLabel = 0;
        int nbConcepts = indexURIToLabels.size();
        Map<URI, Set<String>> indexURIToLabelsClean = new HashMap<>();
        System.out.println("Removing labels of concepts not in given vocabulary");

        for (URI u : indexURIToLabels.keySet()) {

            indexURIToLabelsClean.put(u, new HashSet(indexURIToLabels.get(u)));

            for (String s : indexURIToLabels.get(u)) {
                if (!voc.contains(s)) {
                    indexURIToLabelsClean.get(u).remove(s);
                }
            }
            System.out.println("'" + u + "'" + "\t" + "\toriginal: " + indexURIToLabels.get(u) + "\tcleaned: " + indexURIToLabelsClean.get(u));
            if (indexURIToLabelsClean.get(u).isEmpty()) {
                System.out.println("[WARNING] No label associated to URI " + u + " - this URI cannot be queried");
                countConceptWithoutLabel++;
            }
        }
        indexURIToLabels = indexURIToLabelsClean;
        int p = countConceptWithoutLabel * 100 / nbConcepts;
        System.out.println("Number of concepts without labels: " + countConceptWithoutLabel + "/" + nbConcepts + "\t(" + p + "%)");
    }

    public boolean containsLabelsForURI(URI u) {
        return indexURIToLabels.containsKey(u) && indexURIToLabels.get(u) != null && !indexURIToLabels.get(u).isEmpty();
    }

    public Set<String> getLabels(URI u) {
        return indexURIToLabels.get(u);
    }

    public String getPreferredLabel(URI u) {
        return indexURIPreferredLabels.get(u);
    }

    public Set<URI> getURIs() {
        return indexURIToLabels.keySet();
    }

}
