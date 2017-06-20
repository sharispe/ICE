package sharispe.conceptual_annotator.core;

import java.io.PrintWriter;
import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.utils.Conf;
import slib.graph.algo.utils.GAction;
import slib.graph.algo.utils.GActionType;
import slib.graph.algo.utils.GraphActionExecutor;
import slib.graph.algo.validator.dag.ValidatorDAG;
import slib.graph.io.conf.GDataConf;
import slib.graph.io.loader.wordnet.GraphLoader_Wordnet;
import slib.graph.io.util.GFormat;
import slib.graph.model.graph.G;
import slib.graph.model.impl.graph.memory.GraphMemory;
import slib.graph.model.impl.repo.URIFactoryMemory;
import slib.graph.model.repo.URIFactory;

/**
 *
 * @author sharispe
 */
public class GenerateWordNetIndex {

    public static void main(String[] args) throws Exception {

        // Location of WordNet Data
        String dataloc = Conf.wordnetData;

        // We create the graph
        URIFactory factory = URIFactoryMemory.getSingleton();
        URI guri = factory.getURI(Conf.uriPrefix_wordnet);
        G wordnet = new GraphMemory(guri);

        // We load the data into the graph
        GraphLoader_Wordnet loader = new GraphLoader_Wordnet();

        GDataConf dataNoun = new GDataConf(GFormat.WORDNET_DATA, dataloc + "data.noun");

        loader.populate(dataNoun, wordnet);

        // We root the graph which has been loaded (this is optional but may be required to compare synset which do not share common ancestors).
        GAction addRoot = new GAction(GActionType.REROOTING);
        GraphActionExecutor.applyAction(addRoot, wordnet);

        // This is optional. It just shows which are the synsets which are not subsumed
        ValidatorDAG validatorDAG = new ValidatorDAG();
        Set<URI> roots = validatorDAG.getTaxonomicRoots(wordnet);
        System.out.println("Roots: " + roots);

        // We create an index to map the nouns to the vertices of the graph
        // We only build an index for the nouns in this example

        WordNetIndexer indexWordnetNoun = new WordNetIndexer(wordnet, dataloc + "data.noun");
        
        
        

        // Flush the index
        PrintWriter writer = new PrintWriter(Conf.indexFile_wordnet, "UTF-8");

        for (URI u : wordnet.getV()) {

            String labels = null;

            Set<String> labelList = indexWordnetNoun.getLabels(u);
            
            if(labelList == null){
                continue;
            }
            
            
            String preferredLabel = indexWordnetNoun.getPreferredLabel(u);

            for (String s : labelList) {

                if (labels == null) {
                    labels = "";
                } else {
                    labels += ", ";
                }
                labels += s;
            }
            writer.println(u + ", " + preferredLabel+", "+labels);
        }
        writer.close();
        System.out.println("index generated at: "+Conf.indexFile_wordnet);
    }

}
