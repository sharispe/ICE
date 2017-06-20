/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
class SamplingInfo {

    Map<Integer, SamplingInfoLocal> samplingInfoLocal;

    public SamplingInfo() {
        samplingInfoLocal = new HashMap();
    }

    public void setSamplingInfoLocal(int i, SamplingInfoLocal s) {
        samplingInfoLocal.put(i, s);
    }

    public SamplingInfoLocal getSamplingInfoLocal(int i, boolean acceptLowerSampling) throws Exception {

        if (!samplingInfoLocal.containsKey(i)) {

            if (!acceptLowerSampling || i == 1) {
                throw new Exception("Error trying to access sampling of size " + i + ": No info for sampling of size " + i + " - existing samplings: " + samplingInfoLocal.keySet() + ". You should rebuild the sampling allowing for query of such size.");
            } else {
                System.out.println("No sampling of size " + i + " - performing recursive search");
                return getSamplingInfoLocal(i - 1, acceptLowerSampling);
            }
        } else {
            System.out.println("Using sampling: " + samplingInfoLocal.get(i));
            return samplingInfoLocal.get(i);
        }
    }

    void checkContainsInfo(Set<URI> indexURI) throws Exception {

        for (Map.Entry<Integer, SamplingInfoLocal> e : samplingInfoLocal.entrySet()) {
            for (URI u : indexURI) {
                if (!e.getValue().containsAllValuesFor(u)) {
                    throw new Exception("Error sampling size " + e.getKey() + " does not contain all information for URI: " + u + "\nSampling should probably be updated.");
                }
            }
        }

    }

}
