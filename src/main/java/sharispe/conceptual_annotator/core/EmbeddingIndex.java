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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author sharispe
 */
public class EmbeddingIndex {

    int sizeVector;
    String filePath;
    Map<String, double[]> embeddings;
    
    
    

    public EmbeddingIndex(EmbeddingConf conf) throws Exception {
        this.filePath = conf.getEmbeddingsPath();
        loadWorEmbeddings(filePath);
    }
    
    public int size(){
        return embeddings.size();
    }
    
    public Set<String> getVocabulary(){
        return embeddings.keySet();
    }

    public double[] get(String label) {
        if (contains(label)) {
            return embeddings.get(label);
        }
        return null;
    }
    
    public int getSizeEmbeddings(){
        return sizeVector;
    }

    public boolean contains(String label) {
        return embeddings.containsKey(label);
    }
    
    
    private Map<String, double[]> loadWorEmbeddings(String path) throws Exception {

        System.out.println("Loading embeddings from: " + path+"\nThis process can be time consuming.");

        embeddings = new HashMap<>();
        Integer size = null;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {

                String data[] = line.trim().split("\\s+");

                List<Double> v = new ArrayList();

                for (int i = 1; i < data.length; i++) {
                    v.add(Double.parseDouble(data[i]));
                }

                double[] target = new double[v.size()];
                for (int i = 0; i < target.length; i++) {
                    target[i] = v.get(i);                // java 1.5+ style (outboxing)
                }
                
                if(size == null){
                    size = target.length;
                }
                else if(target.length != size){
                    throw new Exception("Error cannot load embeddings of different sizes");
                }

                embeddings.put(data[0], target);
            }
        }
        System.out.println(embeddings.size()+" embeddings loaded");
        return embeddings;
    }

}
