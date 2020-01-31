package ai.libs.hasco.simplified

import ai.libs.hasco.model.Component
import ai.libs.hasco.serialization.ComponentLoader
import ai.libs.hasco.simplified.std.StdHASCO
import spock.lang.Specification

class SimpleHASCOTest extends Specification {

    String requiredInterface = "IFace";
    ComponentRegistry registry;

    void setup() {
        def componentFile = new File("testrsc/simpleproblemwithtwocomponents.json")
        def paramRefinementConfig = new ComponentLoader(componentFile)
        registry = new ComponentRegistry(new ArrayList<Component>(paramRefinementConfig.getComponents()));
    }

    void cleanup() {


    }

    def "test simple problem"() {
        SimpleHASCO hasco = StdHASCO.createStdImpl(registry, { Optional.of(1.0) }, requiredInterface, 1L)
        hasco.runSequentially()
        println("Done!")
        expect:
        true
    }
}
