/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class MainWikiData {
    
    Map<String, Set<URI>> index_labelToUris;
    URIFactory uriFactory;
    
    public MainWikiData(String labelIndexfile) throws Exception{
        uriFactory = URIFactoryMemory.getSingleton();
        labelIndex(labelIndexfile);
    }
    
    
    public void labelIndex(String path) throws Exception{
        
        index_labelToUris = new HashMap();
        
        int c = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                
                c++;
                
                if(c%10000 == 0){ System.out.println("Loading label: "+c); }

                line = line.trim();
                
                String data[] = line.split("\t",2);

                if (data.length < 2) {
                    System.out.println("skipping line : " + line);
                    continue;
                }
                
                Set<URI> uris = new HashSet();
                String[] data2 = data[1].split(",");
                
                for (int i = 0; i < data2.length; i++) {
                    String uri_s = data2[i].trim();
                    URI uri = uriFactory.getURI(uri_s);
                    uris.add(uri);
                }
                index_labelToUris.put(data[0].trim(), uris);
            }
        }
    }
    
    public static void main(String[] args) throws Exception{
        
        String labelIndex = "/data/corpus/wikidata/wikidata-terms_index.tsv";
        
        MainWikiData m = new MainWikiData(labelIndex);
    }
    
    
}
