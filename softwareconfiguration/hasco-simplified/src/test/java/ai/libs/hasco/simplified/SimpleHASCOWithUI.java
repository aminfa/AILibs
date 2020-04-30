package ai.libs.hasco.simplified;

import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.simplified.std.CIRoot;
import ai.libs.hasco.simplified.std.SimpleHASCOAlgorithmView;
import ai.libs.hasco.simplified.std.SimpleHASCOStdBuilder;
import ai.libs.jaicore.graphvisualizer.plugin.graphview.GraphViewPlugin;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeDisplayInfoAlgorithmEventPropertyComputer;
import ai.libs.jaicore.graphvisualizer.window.AlgorithmVisualizationWindow;
import ai.libs.jaicore.search.gui.plugins.rollouthistograms.SearchRolloutHistogramPlugin;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class SimpleHASCOWithUI {

    public static void main(String[] args) throws IOException, InterruptedException, AlgorithmTimeoutedException, AlgorithmExecutionCanceledException, AlgorithmException {
        String requiredInterface = "IFace";
        File compsFile = new File("softwareconfiguration/hasco-simplified/testrsc/simpleproblemwithtwocomponents.json");
        ComponentLoader compsLoader = new ComponentLoader(compsFile);
        ComponentRegistry compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader);
        long seed = 1L;
        SimpleHASCOStdBuilder hasco = new SimpleHASCOStdBuilder();
        hasco.setSeed(seed);
        hasco.setMinEvalQueueSize(1);
        hasco.setThreadCount(2);
        hasco.setRequiredInterface(requiredInterface);
        hasco.setRegistry(compsRegistry);
        hasco.setArtificialRoot(false);
        hasco.setPublishNodeEvent(true);
        hasco.setEvaluator((s) -> Optional.of(1.0));
//        hasco.init();
        SimpleHASCOAlgorithmView algorithm = hasco.getAlgorithm();
        new JFXPanel();

        AlgorithmVisualizationWindow window = new AlgorithmVisualizationWindow(hasco.getAlgorithm(),
                Arrays.asList(new NodeDisplayInfoAlgorithmEventPropertyComputer<>(new SimpleHASCOAlgorithmView.SimpleNodeInfoGenerator())),
                new GraphViewPlugin(),
                new SearchRolloutHistogramPlugin());
//        Platform.runLater(window);
        window.show();
        hasco.getAlgorithm().call();
        synchronized (window) {
            window.wait();
        }
//        Thread.yield();
    }
}
