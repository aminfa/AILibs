package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.NumericParameterDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NumericSplit {

    private final static Logger logger = LoggerFactory.getLogger(NumericSplit.class);

    private static final String REGEX_NUMERIC_RANGE =
            "\\[\\s*(?<num1>[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)" +
            "\\s*,\\s*" +
            "(?<num2>[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\s*]"; // E.g. matches: "[ -10 : 210.1 ]"

    private static final Pattern PATTERN_NUMERIC_RANGE = Pattern.compile(REGEX_NUMERIC_RANGE);

    private String componentName, paramName;

    private double min, max, fixedVal, diff;

    private boolean fixedValFlag = false;

    private boolean isIntegerFlag = false;

    private List<String> splits = new ArrayList<>();

    private double splitSize, minSplitSize;

    private int splitCount;

    public NumericSplit(String currentRange, NumericParameterDomain domain) {
        this(currentRange, Objects.requireNonNull(domain, "Domain is zero").isInteger(),
                domain.getMin(), domain.getMax());
    }

    NumericSplit(String currentRange, boolean isInteger, double minDomain, double maxDomain) {
        isIntegerFlag = isInteger;
        if(currentRange == null) {
            min = minDomain;
            max = maxDomain;
        } else {
            Matcher matcher = PATTERN_NUMERIC_RANGE.matcher(currentRange);
            if(matcher.matches()) {
                min = Double.parseDouble(matcher.group("num1"));
                max = Double.parseDouble(matcher.group("num2"));
            } else {
                try {
                    double v = Double.parseDouble(currentRange);
                    min = v;
                    max = v;
                    fixedVal = v;
                    fixedValFlag = true;
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("The current value has a syntax error: " + currentRange);
                }
            }
        }
        if(!isValueFixed()) {
            if(isIntegerFlag && max - min <= 1.0) {
                Optional<Long> meanFixedVal = meanInteger(min, max);
                if(meanFixedVal.isPresent()) {
                    fixedVal = meanFixedVal.get();
                } else {
                    throw new IllegalArgumentException("The given range, " + currentRange + ",  doesn't contain an integer element.");
                }
                fixedValFlag = true;
            } else if(min >= max) {
                logger.warn("Min value is greater than max: {}", currentRange);
                fixedVal = min;
                fixedValFlag = true;
            }
            diff = max - min;
        }
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }

    private String getDisplayText() {
        StringBuilder builder = new StringBuilder();
        builder.append("numeric ");
        builder.append(isIntegerFlag? "integer ": "float ");
        builder.append("param ");
        if(componentName != null && paramName != null) {
            builder.append("[").append(componentName).append("->").append(paramName).append("]");
        }
        return builder.toString();
    }


    public void configureSplits(int splitCount, double minSplitSize) {
        if(isValueFixed())
            return;
        if(minSplitSize < 0.) {
            minSplitSize = 0.;
        }
        if(splitCount < 2) {
            splitCount = 2;
        }
        if(diff <= minSplitSize) {
            fixedVal = (max + min)/2.;
            fixedValFlag = true;
            return;
        }
        splitSize = diff / splitCount;
        this.minSplitSize = minSplitSize;
        if(splitSize < minSplitSize) {
            splitSize = minSplitSize;
            this.splitCount = (int) Math.ceil(diff / splitSize);
        } else {
            this.splitCount = splitCount;
        }
    }


    public void createValues() {
        createSplits();
        List<String> oldSplits = new ArrayList<>(splits);
        splits.clear();
        for (String split : oldSplits) {
            Number value = null;
            Matcher matcher = PATTERN_NUMERIC_RANGE.matcher(split);
            if(matcher.matches()) {
                double min = Double.parseDouble(matcher.group("num1"));
                double max = Double.parseDouble(matcher.group("num2"));
                if(isIntegerFlag) {
                    Optional<Long> meanInt = meanInteger(min, max);
                    if(meanInt.isPresent())
                        value = meanInt.get();
                } else {
                    value = (min + max) / 2.0;
                }
            } else {
                try {
                    value = Double.parseDouble(split);
                } catch(NumberFormatException ex) {
                    logger.error("BUG: Couldn't parse outcome of createSplits: {} ", split, ex);
                }
            }
            if(value != null) {
                splits.add(numberToString(value));
            }
        }
    }

    private String numberToString(Number numb) {
        return valueToString(numb, isInteger());
    }

    public void createSplits() {
        splits.clear();
        if(isValueFixed()) {
            splits.add(numberToString(getFixedVal()));
            return;
        }
        if(splitSize <= 0) {
            logger.warn("{}: no splits created as split size is {}", getDisplayText(), splitSize);
            return;
        }
        for (int i = 0; i < splitCount; i++) {
            double splitMin = min + (i * splitSize);
            double splitMax = min + ((i+1) * splitSize);
            for (int j = i+1; j < splitCount; j++) {
                /*
                 * See if the following splits are still big enough,
                 * If they are smaller than mix split size expand the current split to contain the remaining range.
                 *
                 * Technically, no inner-loop is needed. As soon as the next split is smaller than the minSplitSize
                 * all remaining splits will be smaller.
                 * This is because splitSize and minSplitSize are fixed between iterations.
                 * If this changes however and these parameters do vary between iterations,
                 * this implementation would still work as expected.
                 */
                double nextSplitMax = min + (j+1) * splitSize;
                if(nextSplitMax > max) {
                    nextSplitMax = max;
                }
                double nextSplitSize = nextSplitMax - splitMax;
                if(nextSplitSize < minSplitSize) {
                    splitMax = nextSplitMax;
                    i++; // Count up i so the next iteration starts at the right spot.
                } else {
                    break;
                }
            }
            // check boundries:
            if(splitMin < min) {
                // shouldn't happen, but checking anyway:
                splitMin = min;
            }
            if(splitMax > max) {
                splitMax = max;
            }
            if(splitMin == min && splitMax == max) {
                continue;
            }
            Optional<String> split = createSplit(splitMin, splitMax);
            split.ifPresent(s -> splits.add(s));
        }
    }


    private Optional<String> createSplit(double min, double max) {
        double diff = max - min;
        if(diff < 0.) {
            diff = 0.;
            logger.warn("Split of {}: Max value is larger than min: [{}, {}).",getDisplayText(),  min, max);
            max = min;
        }
        if(diff <= minSplitSize/2. || diff == 0.) {
            /*
             * If half the difference is smaller than the current minSplitSize, then create a fixed value instead of the range.
             * We consider only half the diff,
             * because it only makes sense to create a range when it can be split again into large enough pieces next round.
             *
             * The fixed value is the mean of min and max.
             */
            return Optional.empty();
//            double mean = (max + min) / 2.;
//            if(isIntegerFlag) {
//                // Integer values are
//                Optional<Long> val = meanInteger(min, max);
//                if(val.isPresent()) {
//                    String fixedSplitVal = numberToString(val.get());
//                    return Optional.of(fixedSplitVal);
//                } else {
//                    // Else the integer value is not in [min, max), so ignore the split
//                    logger.warn("Split of {}: cannot split [{}, {}) " +
//                            " because no integer is part of its range", getDisplayText(), min, max);
//                    return Optional.empty();
//                }
//            } else {
//                String fixedSplitVal = numberToString(mean);
//                return Optional.of(fixedSplitVal);
//            }
        } else if(isIntegerFlag && diff < 1.0) {
            // if it is integer dont create a split smaller than 1.0
            Optional<Long> meanInteger = meanInteger(min, max);
            if(meanInteger.isPresent()) {
                String intVal = numberToString(meanInteger.get());
                return Optional.of(intVal);
            } else {
                logger.warn("Split of {}: Cannot split [{}, {}) because its difference {} is too small to contain further integer values",
                        getDisplayText(), min, max, diff);
                return Optional.empty();
            }
        } else {
            // Split is legal:
            return Optional.of(rangeToString(min, max));
        }
    }

    public static String rangeToString(double min, double max) {
        return String.format(Locale.US,
                "[%f, %f]",
                min, max);
    }

    public static String valueToString(Number number, boolean isInteger) {
        if(isInteger) {
            return String.valueOf(number.longValue());
        } else {
            return String.format(Locale.US, "%f", number.doubleValue());
        }
    }

    private Optional<Long> meanInteger(double min, double max) {
        if(Math.ceil(min) >= max) {
            return Optional.empty();
        }
        double mean = (max + min) / 2.;
        while(Math.ceil(mean) >= max) {
            mean--;
        }
        if(Math.ceil(mean) < min) {
            return Optional.empty();
        }
        return Optional.of((long) Math.ceil(mean));
    }

    public Number getFixedVal() {
        if(isIntegerFlag)
            return (int) fixedVal;
        else
            return fixedVal;
    }

    public List<String> getSplits() {
        return splits;
    }

    public boolean isValueFixed() {
        return fixedValFlag;
    }

    /*
     * The remaining getters are package private and should only be used in tests:
     */

    String getComponentName() {
        return componentName;
    }

    String getParamName() {
        return paramName;
    }

    double getMin() {
        return min;
    }

    double getMax() {
        return max;
    }

    double getDiff() {
        return diff;
    }

    boolean isInteger() {
        return isIntegerFlag;
    }

    double getSplitSize() {
        return splitSize;
    }

    double getMinSplitSize() {
        return minSplitSize;
    }

    int getSplitCount() {
        return splitCount;
    }

    public String getPreSplitRange() {
        return rangeToString(min, max);
    }

}
