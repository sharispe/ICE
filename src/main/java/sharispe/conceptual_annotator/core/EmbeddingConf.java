package sharispe.conceptual_annotator.core;

/**
 *
 * @author sharispe
 */
class EmbeddingConf {

    String path;
    boolean containsOnlyLowerCaseLabels;
    EmbeddingConf(String model_voc_path, boolean containsOnlyLowerCaseLabels) {
        this.path = model_voc_path;
        this.containsOnlyLowerCaseLabels = containsOnlyLowerCaseLabels;
    }

    public String getEmbeddingsPath() {
        return path;
    }
    
    public boolean containsOnlyLowerCaseLabels(){
        return containsOnlyLowerCaseLabels;
    }
    
}
