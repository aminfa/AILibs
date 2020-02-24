package ai.libs.hasco.simplified;

import ai.libs.hasco.serialization.ComponentLoader;

import java.io.File;
import java.io.IOException;

public class HASCOTester {

    private ComponentLoader loader = null;

    private ComponentRegistry registry = null;

    private ComponentEvaluator evaluator = null;

    public void loadComponents(File componentFile) throws IOException {
        if(loader == null) {
            loader = new ComponentLoader();
        }
        loader.loadComponents(componentFile);
    }

    public void assertComponentsLoaded() {
        if(loader == null) {
            throw new IllegalStateException();
        }
    }

    public ComponentRegistry getRegistry() {
        assertComponentsLoaded();
        if(registry == null)
            registry = ComponentRegistry.fromComponentLoader(loader);
        return registry;
    }

    public void setComponentEvaluator(ComponentEvaluator eval) {
        this.evaluator = eval;
    }


//    public static StdHASCO createStdHASCO(File componentFile, )

}
