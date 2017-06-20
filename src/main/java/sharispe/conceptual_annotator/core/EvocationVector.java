/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.util.List;
import org.openrdf.model.URI;

/**
 *
 * @author sharispe
 */
public class EvocationVector {

    EvocationVectorFactory evf;
    double[] vector;

    EvocationVector(EvocationVectorFactory evf) {
        this.evf = evf;
        vector = new double[evf.size];
    }

    public double get(URI u) {
        return vector[evf.indexURI.get(u)];
    }

    public void set(URI u, double val) {
        vector[evf.indexURI.get(u)] = val;
    }

    public void setVectorValues(double[] val) {
        for(int i = 0; i < val.length; i++) {
            vector[i] = val[i];
        }
    }

    public int getSize() {
        return vector.length;
    }
    
    public double[] getVec(){
        return vector;
    }
    
    public List<URI> getURIOrdering(){
        return evf.orderedUris;
    }
    
    
    @Override
    public String toString(){
        
        String s = "[size="+evf.orderedUris.size()+" : ";
        
        for (int i = 0; i < evf.orderedUris.size(); i++) {
            URI u = evf.orderedUris.get(i);
            s+= u.getLocalName()+":"+vector[i]+" ";
        }
        s+="]";
        return s;
    }

}
