/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import slib.graph.model.graph.G;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.utils.ex.SLIB_Ex_Critic;
import slib.utils.ex.SLIB_Exception;

/**
 * Adapted from SLIB WordNet Loader
 *
 * @author Sébastien Harispe (sebastien.harispe@gmail.com)
 */
public class WordNetIndexer {

    Map<URI, Set<String>> uriToLabels = new HashMap();
    Map<URI, String> uriPreferredLabel = new HashMap();

    Logger logger = LoggerFactory.getLogger(this.getClass());
    URIFactoryMemory dataRepo = URIFactoryMemory.getSingleton();

    public WordNetIndexer(G g, String filepath) throws SLIB_Exception {

        logger.info("-------------------------------------");
        logger.info("Loading WordNet Indexer");
        logger.info("-------------------------------------");

        logger.info("From " + filepath);
        logger.info("-----------------------------------------------------------");
        
        uriToLabels = new HashMap();
        uriPreferredLabel = new HashMap();

        boolean inHeader = true;

        String uriPrefix = g.getURI().getNamespace();

        try {

            if (filepath == null) {
                throw new SLIB_Ex_Critic("Error please precise a  file to load.");
            }

            FileInputStream fstream = new FileInputStream(filepath);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String line;
            String[] data;

            while ((line = br.readLine()) != null) {

                if (inHeader) {
                    if (line.startsWith("  ")) {
                        continue;
                    }
                    inHeader = false;
                }

                line = line.trim();
                data = line.split("\\s+");

                String synset_offset = data[0];
                String lex_filenum = data[1];
                String ss_type = data[2];

                URI synset = dataRepo.getURI(uriPrefix + synset_offset);

                int w_cnt = Integer.parseInt(data[3], 16);// hexa  

//                logger.info(synset_offset);
                Word[] words = extractWords(data, 4, w_cnt);
                System.out.println(synset+"\t"+Arrays.toString(words));
                
                Set<String> labels = new HashSet<>();
                
                for(Word w : words){
                    labels.add(w.word.replaceAll("_", " "));
                }
                uriToLabels.put(synset, labels);
                uriPreferredLabel.put(synset, words[0].word.replaceAll("_", " "));
                
                
            }
            in.close();
        } catch (IOException e) {
            throw new SLIB_Ex_Critic("Error loading the file: " + e.getMessage());
        }

        logger.info("Wordnet Loading ok.");
        logger.info("-------------------------------------");
    }
    
    public Set<String> getLabels(URI uri){
        return uriToLabels.get(uri);
    }
    
    public String getPreferredLabel(URI uri){
        return uriPreferredLabel.get(uri);
    }
    

    private Word[] extractWords(String[] data, int start_id, int w_cnt) {

        int c = 0;

        Word[] words = new Word[w_cnt];

        for (int i = start_id; c < w_cnt; i += 2) {
            words[c] = new Word(data[i], Integer.parseInt(data[i + 1], 16));
            c++;
        }

        return words;
    }

    private class Word {

        String word;
        int lex_id;

        Word(String word, int lex_id) {
            this.word = word;
            this.lex_id = lex_id;
        }

        @Override
        public String toString() {
            return this.word + "(" + lex_id + ")";
        }
    }

}
