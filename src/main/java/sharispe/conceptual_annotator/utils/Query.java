/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.utils;

import java.util.Set;
import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
public class Query {

    public final Set<String> terms;
    public final String conceptLabel;
    public final URI conceptURI;
    public final String id; 

    public Query(String id, String conceptLabel, URI conceptURI, Set<String> terms) {
        this.terms = terms;
        this.conceptLabel = conceptLabel;
        this.conceptURI = conceptURI;
        this.id = id;
    }

    
    
    @Override
    public String toString(){
        return "query "+id+" expected URI: "+conceptURI+" ("+conceptLabel+")\tterms:"+terms;
    }

}
