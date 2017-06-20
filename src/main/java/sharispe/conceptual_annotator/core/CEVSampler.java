/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sharispe.conceptual_annotator.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.openrdf.model.URI;
import sharispe.conceptual_annotator.utils.RQueue;
import slib.graph.model.repo.URIFactory;

/**
 * Class used to compute the sampling for a given size
 *
 * Comparison https://radimrehurek.com/gensim/models/word2vec.html
 *
 *
 * @author sharispe
 */
public class CEVSampler {

    public static void sample(int nbSampling, int setSizeStart, int setSizeEnd, EvocationVectorComputer CEVComputer, Set<String> voc, AggregationMethod aggMethod, String outputFile) throws Exception {
        sample(nbSampling, setSizeStart, setSizeEnd, CEVComputer, voc, aggMethod, outputFile, null);
    }

    public static void sample(int nbSampling, int setSizeStart, int setSizeEnd, EvocationVectorComputer CEVComputer, Set<String> voc, AggregationMethod aggMethod, String outputFile, String logFile) throws Exception {

        System.out.println("Generating sampling for sets " + setSizeStart + " to " + setSizeEnd + "\tnumber of samplings: " + nbSampling + "\taggregation: " + aggMethod);
        System.out.println("output: " + outputFile);
        PrintWriter outputWriter = new PrintWriter(outputFile, "UTF-8");

        outputWriter.println("#INFO_SAMPLING: PATH_EMBEDDINGS=" + CEVComputer.embeddings.filePath + "CONCEPTUAL_SPACE="+CEVComputer.evf.size+" ; NB_SAMPLING_PER_SIZE=" + nbSampling + "; AGGREGATION=" + aggMethod + "; SIZE_SAMPLING_SET=" + setSizeStart + "-" + setSizeEnd + " DATE=" + new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(new Date()));

        for (int setSize = setSizeStart; setSize <= setSizeEnd; setSize++) {
            
            System.out.println("Performing sampling for random sets of size: "+setSize);

            Map<URI, Integer> numberNonNullValues = new HashMap();
            Map<URI, Double> sumNonNullValues = new HashMap();
            Map<URI, Double> sumNonNullValuesSquared = new HashMap();
            Map<URI, RQueue<String, Double>> bestResults = new HashMap();

            Random rn = new Random();
            List<URI> uriOrder = CEVComputer.evf.orderedUris;

            PrintWriter writer = null;
            if (logFile != null) {
                writer = new PrintWriter(logFile, "UTF-8");
                writer.write("LABEL_SAMPLE\t");

                for (int j = 0; j < uriOrder.size(); j++) {
                    URI u = uriOrder.get(j);

                    if (j != 0) {
                        writer.print("\t");
                    }
                    writer.print(u.getLocalName());
                }
                writer.println("");
            }

            List<String> vocList = new ArrayList(voc);

            for (int i = 0; i < nbSampling; i++) {

                if(i%100==0){
                    System.out.println("sampling (size="+setSize+"): " + (i + 1)+"/"+nbSampling);
                }
                
                Set<String> sample = new HashSet();

                for (int j = 0; j < setSize; j++) {

                    int id_j = rn.nextInt(vocList.size());
                    sample.add(vocList.get(id_j));
                }

//            writer.print(sample.toString()+"\t");
                EvocationVector evoc_agg = CEVComputer.computeCEV(sample, aggMethod);
                
//                System.out.println(sample);
//                System.out.println(evoc_agg);

                for (int j = 0; j < uriOrder.size(); j++) {

                    URI u = uriOrder.get(j);
                    double evoc_u = evoc_agg.get(u);

                    if (logFile != null) {
                        if (j != 0) {
                            writer.print("\t");
                        }
                        writer.print(evoc_agg.get(u));
                    }

                    if (!numberNonNullValues.containsKey(u)) {
                        numberNonNullValues.put(u, 0);
                        sumNonNullValues.put(u, 0.0);
                        sumNonNullValuesSquared.put(u, 0.0);
                        bestResults.put(u, new RQueue<String, Double>(nbSampling * 5 / 100));
                    }
                    if (evoc_agg.get(u) != EvocationVectorComputer.INIT_VEC_VALUE) { // this should be modified considering INIT_VEC_VALUE and aggregation
                        numberNonNullValues.put(u, numberNonNullValues.get(u) + 1);
                        sumNonNullValues.put(u, sumNonNullValues.get(u) + evoc_u);
                        sumNonNullValuesSquared.put(u, sumNonNullValuesSquared.get(u) + Math.pow(evoc_u, 2));
                        bestResults.get(u).add(sample.toString(), evoc_u);
                    }
                }
                if (logFile != null) {
                    writer.println("");
                }
            }
            if (logFile != null) {
                writer.close();
            }

            System.out.println("sampling finished");
            System.out.println("Global statistics for sampling of size: " + setSize);

            outputWriter.write(">");
            outputWriter.write("SAMPLING_SIZE=" + setSize + "\n");
            outputWriter.write("URI\tNUMBER_NON_NULL_VALUES\tPERCENT_NON_NULL_VALUES\tSUM\tAVERAGE\tSUM_SQUARED_VALUES\tAVERAGE_SQUARED\tMIN_BEST_95\tSTANDARD_DEVIATION\n");

            for (URI u : uriOrder) {

                double percent = numberNonNullValues.get(u) * 100.0 / (double) nbSampling;
                // E(x)
                double average = sumNonNullValues.get(u) / (double) nbSampling;
                // E(x^2)
                double averageSquared = sumNonNullValuesSquared.get(u) / (double) nbSampling;
                // \sqrt( E(x^2) - E(x)^2 )
                double standard_deviation_u = Math.sqrt(averageSquared - Math.pow(average, 2));

                // select min of bestValue
                Double min_best = EvocationVectorComputer.INIT_VEC_VALUE;
                for (Double l : bestResults.get(u).getValues()) {
                    if (l < min_best) {
                        min_best = l;
                    }
                }

                String info = u + "\t" + numberNonNullValues.get(u) + "\t" + percent + "\t"
                        + sumNonNullValues.get(u) + "\t" + average + "\t" + sumNonNullValuesSquared.get(u) + "\t"
                        + averageSquared + "\t" + min_best + "\t" + standard_deviation_u + "\n";

                outputWriter.write(info);
                System.out.println(u + ":" + numberNonNullValues.get(u) + "\t" + percent + "%\tsum: " + sumNonNullValues.get(u) + "\tavg: " + average + "\t95%:" + min_best + "\tsigma:" + standard_deviation_u);
            }

        }
        outputWriter.close();
    }

    static SamplingInfo loadSamplingInfo(String samplingInfoFilePath, URIFactory factory) throws Exception {

        SamplingInfo samplingInfo = new SamplingInfo();

        System.out.println("Loading sampling_info from : " + samplingInfoFilePath);

        try (BufferedReader br = new BufferedReader(new FileReader(samplingInfoFilePath))) {
            String line;

            int current_size_sampling;
            boolean isHeader = false;

            SamplingInfoLocal samplingInfoLocal = null;

            while ((line = br.readLine()) != null) {

                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    System.out.println("skipping line : " + line);
                    continue;
                } else if (line.charAt(0) == '>') {

                    System.out.println(line);
                    // we expected >SAMPLING_SIZE=2
                    String data[] = line.split("=");
                    current_size_sampling = Integer.parseInt(data[1]);
                    isHeader = true;
                    samplingInfoLocal = new SamplingInfoLocal(current_size_sampling);
                    samplingInfo.setSamplingInfoLocal(current_size_sampling, samplingInfoLocal);

                } else if (isHeader) {
                    isHeader = false;
                } else {
                    String data[] = line.split("\t");
                    URI u = factory.getURI(data[0]);
                    double numberNonNullValues = Double.parseDouble(data[1]);
                    samplingInfoLocal.setNumberNonNullValues(u, numberNonNullValues);

                    double percent = Double.parseDouble(data[2]);
                    samplingInfoLocal.setPercent(u, percent);

                    double sumNonNullValue = Double.parseDouble(data[3]);
                    samplingInfoLocal.setSumNonNullValues(u, sumNonNullValue);

                    double average = Double.parseDouble(data[4]);
                    samplingInfoLocal.setAverage(u, average);

                    double sumNonNullValuesSquared = Double.parseDouble(data[5]);
                    samplingInfoLocal.setSumNonNullValuesSquared(u, sumNonNullValuesSquared);

                    double averageSquared = Double.parseDouble(data[6]);
                    samplingInfoLocal.setAverageSquared(u, averageSquared);

                    double min_best = Double.parseDouble(data[7]);
                    samplingInfoLocal.setMinBest(u, min_best);

                    double standard_deviation = Double.parseDouble(data[8]);
                    samplingInfoLocal.setStandardDeviation(u, standard_deviation);
                }
            }
            return samplingInfo;

        }
    }

}
