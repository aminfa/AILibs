package ai.libs.hasco.simplified.std;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.Spliterator;
import java.util.function.Supplier;
import java.util.stream.Stream;

class ImplUtil {

    static boolean binaryDecision(Supplier<Double> numberGenerator) {
        return numberGenerator.get() > 0.5d;
    }

    static int decideBetween(Supplier<Double> numberGenerator, int choiceCount) {
        return (int)(choiceCount*numberGenerator.get());
    }
    static <E> E decideBetween(Supplier<Double> numberGenerator, List<E> list) {
        int i = decideBetween(numberGenerator, list.size());
        return list.get(i);
    }

}
