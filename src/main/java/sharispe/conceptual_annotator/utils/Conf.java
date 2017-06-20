/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.utils;

/**
 *
 * @author sharispe
 */
public class Conf {

    public final static String model_path_glove = System.getProperty("user.dir") + "/resources/glove.6B/";
    public final static String model_path_word2vec = "/data/embeddings/word2vec/";

    public final static String model_name_glove_6B_50d = "glove.6B.50d";
    public final static String model_name_glove_840B_300d = "glove.840B.300d";
    public final static String model_name_word2vec_GoogleNews = "GoogleNews-vectors-negative300_noheader";
    
    public final static String wordnetData = System.getProperty("user.dir") + "/resources/WordNet-3.1/dict/";
    public static final String indexFile_wordnet = System.getProperty("user.dir") + "/resources/index_wordnet.tsv";
    public static final String uriPrefix_wordnet = "http://graph/wordnet/";
    
    public final static String queryFile = System.getProperty("user.dir") + "/resources/requests_complete.csv";
    public final static String outputDir = System.getProperty("user.dir") + "/resources/results/";
    
    // Only the URIs that are defined as a query expected result will be accepted as 
    // a results -- this restrict the number of possible results. 
    // It's used to perform a less contraining evaluation
    public final static boolean ONLY_ACCEPT_QUERY_URIS_AS_RESUTLS = true;
    
}
