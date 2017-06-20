/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
public class EvocationVectorFactory {
    
    public final Set<URI> uris;
    public final List<URI> orderedUris;
    public final Map<URI, Integer> indexURI;
    public final int size;
    
    public EvocationVectorFactory(Set<URI> uris){
        this.uris = new HashSet<>(uris);
        this.orderedUris = new ArrayList(uris);
        
        indexURI = new HashMap();
        for (int i = 0; i < orderedUris.size(); i++) {
            indexURI.put(orderedUris.get(i), i);
        }
        size = uris.size();
    }
    
    public EvocationVector buildEvocationVector(){
        return new EvocationVector(this);
    }
    
}
