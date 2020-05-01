package ai.libs.hasco.simplified;

import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.simplified.std.SimpleHASCOAlgorithmView;
import ai.libs.hasco.simplified.std.SimpleHASCOStdBuilder;
import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGUIPlugin;
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import ai.libs.jaicore.ml.weka.WekaUtil;
import ai.libs.jaicore.ml.weka.dataset.IWekaInstances;
import ai.libs.jaicore.ml.weka.dataset.WekaInstances;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.SearchRolloutHistogramPlugin;
import javafx.embed.swing.JFXPanel;
import org.api4.java.ai.ml.core.dataset.splitter.SplitFailedException;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import weka.core.Instances;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SimpleHASCOWekaWithUI {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCOWekaWithUI.class);

    static Instances data;

    static List<IWekaInstances> split;

    static ComponentEvaluator pipelineEvaluator;

    static ComponentRegistry compsRegistry;

    public static void main(String[] args) throws IOException, InterruptedException, AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, AlgorithmException {
        loadComponents();
        loadData();
        createEvaluator();
        String requiredInterface = "AbstractClassifier";
        long seed = 10L;
        SimpleHASCOStdBuilder builder = new SimpleHASCOStdBuilder();
        builder.setSeed(seed);
        builder.setMinEvalQueueSize(16);
        builder.setThreadCount(8);
        builder.setRequiredInterface(requiredInterface);
        builder.setRegistry(compsRegistry);
        builder.setArtificialRoot(false);
        builder.setPublishNodeEvent(true);
        builder.setRefinementTime(2, TimeUnit.MINUTES);
        builder.setSampleTime(2, TimeUnit.MINUTES);
        builder.setEvaluator(pipelineEvaluator);
        SimpleHASCOAlgorithmView algorithm = builder.getAlgorithm();

        new JFXPanel();
        AlgorithmVisualizationWindow window = new AlgorithmVisualizationWindow(algorithm);
        window.withMainPlugin(new GraphViewPlugin());
        window.withPlugin(new NodeInfoGUIPlugin(new SimpleHASCOAlgorithmView.SimpleNodeInfoGenerator()), new SearchRolloutHistogramPlugin());
        builder.getAlgorithm().call();
    }

    static void loadData() {
        String arffSource = "softwareconfiguration/hasco-simplified/testrsc/datasets/heart.statlog.arff";
        long start = System.currentTimeMillis();
        File file = new File(arffSource);
        try {
            data = new Instances(new FileReader(file));
        } catch (IOException e) {
            throw new RuntimeException("Dataset could not be read: " + arffSource + ". Exception: " + e.getMessage(), e);
        }
        logger.info("Dataset read. Time to create dataset object was {} ms", System.currentTimeMillis() - start);
        data.setClassIndex(data.numAttributes() - 1);
        try {
            split = WekaUtil.getStratifiedSplit(new WekaInstances(data), 0, 0.7);
        } catch (SplitFailedException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static void createEvaluator() {
        pipelineEvaluator = new WekaClassifierComponentEvaluator(split);
    }

    static void loadComponents() {
        File compsFile = new File("softwareconfiguration/hasco-simplified/testrsc/wekaproblems/autoweka.json");
        ComponentLoader compsLoader = null;
        try {
            compsLoader = new ComponentLoader(compsFile);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't load component file: " + compsFile + ". Exception: " + e.getMessage(), e);
        }
        compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader);
    }
}
