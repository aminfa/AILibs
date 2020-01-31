package ai.libs.hasco.simplified.std;

import java.util.function.Supplier;

public class RandomUtil {

    public static boolean binaryDecision(Supplier<Double> numberGenerator) {
        return numberGenerator.get() > 0.5d;
    }

    public static int decideBetween(Supplier<Double> numberGenerator, int choiceCount) {
        return (int)(choiceCount*numberGenerator.get());
    }

    public static String[][] getParamRefinementOrder() {



    }

}
