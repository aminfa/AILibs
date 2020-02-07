package ai.libs.hasco.simplified.std

import spock.lang.Specification

class NumericSplitTest extends Specification {

    def reverse(split) {
        if (split instanceof List && split.size() == 2) {
            try {
                def min = reverse(split[0])
                def max = reverse(split[1])
                return [max, min]
            } catch (IllegalArgumentException ex) {
            }
        } else if (split instanceof BigDecimal) {
            return split * -1.0
        } else if (split instanceof Number) {
            return split * -1.0
        } else {
            throw new IllegalArgumentException("What is split: " + split)
        }

    }

    String convertToString(split, isFloat = true) {
        if (split instanceof List && split.size() == 2) {
            try {
                return String.format(Locale.US, "[%s, %s]", convertToString(split[0], true), convertToString(split[1], true))
            } catch (IllegalArgumentException ex) {
            }
        } else if (split instanceof BigDecimal) {
            if (isFloat)
                return convertToString(split.toDouble());
            else
                return convertToString(split.toLong(), false);
        } else if (split instanceof Number) {
            if (isFloat)
                return String.format(Locale.US, "%f", split.toDouble())
            else
                return String.valueOf(split.toLong())
        }
        throw new IllegalArgumentException("What is split: " + split)
    }

    def "test float splitting"() {
        when:
        def currentVal = "[${currentMin} , ${currentMax}]"
        def diff = currentMax - currentMin
        NumericSplit numSplit = new NumericSplit(currentVal,
                false, 0.0, 0.0)

        then:
        diff == numSplit.diff
        !numSplit.valueFixed


        when:
        numSplit.configureSplits(splitCount, minSplitSize)
        numSplit.createSplits()
        def strSplits = expectedSplit.collect { convertToString(it) }

        then:
        numSplit.splits == strSplits

        when:
        // test the same cases but *(-1)
        currentVal = "[${-currentMax} , ${-currentMin}]"
        numSplit = new NumericSplit(currentVal,
                false, 0.0, 0.0)

        then:
        diff == numSplit.diff
        !numSplit.valueFixed

        when:
        numSplit.configureSplits(splitCount, minSplitSize)
        numSplit.createSplits()
        strSplits = expectedSplit.collect {
            convertToString(reverse(it))
        }.reverse()

        then:
        numSplit.splits == strSplits


        where:
        currentMin | currentMax | splitCount | minSplitSize
        0.0        | 10.0       | 2          | 0.0
        0.0        | 10.0       | 2          | 1.0
        0.0        | 10.0       | 3          | 1.0
        0.0        | 10.0       | 5          | 1.0
        0.0        | 10.0       | 10         | 1.0
        0.0        | 10.0       | 4          | 0.5

        10.0       | 100.0      | 3          | 0.0
        10.0       | 100.0      | 3          | 10.0
        10.0       | 100.0      | 3          | 20.0
        10.0       | 100.0      | 3          | 30.0
        10.0       | 100.0      | 3          | 45.0

        -10.0      | 10.0       | 5          | 2.0
        -10.0      | 10.0       | 5          | 3.0
        -10.0      | 11.0       | 5          | 7.0

        expectedSplit << [
                [
                        [0.0, 5.0],
                        [5.0, 10.0]
                ],
                [
                        [0.0, 5.0],
                        [5.0, 10.0]
                ],
                [
                        [0.0, 10.0 / 3.0],
                        [10.0 / 3.0, 20.0 / 3.0],
                        [20.0 / 3.0, 10.0]
                ],
                (0..4).collect {
                    [it * 2.0, (it + 1) * 2.0]
                },
                (0.5..9.5),
                [[0.000000, 2.500000], [2.500000, 5.000000], [5.000000, 7.500000], [7.500000, 10.000000]],

                [
                        [10.0, 40.0],
                        [40.0, 70.0],
                        [70.0, 100.0]
                ],
                [
                        [10.0, 40.0],
                        [40.0, 70.0],
                        [70.0, 100.0]
                ],
                [25.0, 55.0, 85.0],
                [25.0, 55.0, 85.0],
                [32.5, 77.5],

                [
                        [-10, -6],
                        [-6, -2],
                        [-2, 2],
                        [2, 6],
                        [6, 10]
                ],
                [-8, -4, 0, 4, 8],
                [-6.5, 0.5, 7.5]
        ]
    }


    def "test integer splitting" () {
        when:
        def currentVal = "[${currentMin} , ${currentMax}]"
        def diff = currentMax - currentMin
        NumericSplit numSplit = new NumericSplit(currentVal,
                true, 0.0, 0.0)

        then:
        diff == numSplit.diff
        !numSplit.valueFixed


        when:
        numSplit.configureSplits(splitCount, minSplitSize)
        numSplit.createSplits()
        def strSplits = expectedSplit.collect { convertToString(it, false) }

        then:
        numSplit.splits == strSplits


        where:
        currentMin | currentMax | splitCount | minSplitSize
        0.0        | 10.0       | 2          | 0.0
        0.0        | 10.0       | 2          | 1.0
        0.0        | 10.0       | 3          | 1.0
        0.0        | 10.0       | 5          | 1.0
        0.0        | 10.0       | 4          | 0.5
        0.0        | 10.0       | 10         | 0.0

        10.0       | 100.0      | 3          | 0.0
        10.0       | 100.0      | 3          | 10.0
        10.0       | 100.0      | 3          | 20.0
        10.0       | 100.0      | 3          | 30.0
        10.0       | 100.0      | 3          | 45.0

        -10.0      | 10.0       | 5          | 2.0
        -10.0      | 10.0       | 5          | 3.0
        -10.0      | 11.0       | 5          | 7.0

        expectedSplit << [
                [
                        [0.0, 5.0],
                        [5.0, 10.0]
                ],
                [
                        [0.0, 5.0],
                        [5.0, 10.0]
                ],
                [
                        [0.0, 10.0 / 3.0],
                        [10.0 / 3.0, 20.0 / 3.0],
                        [20.0 / 3.0, 10.0]
                ],
                (0..4).collect {
                    [it * 2.0, (it + 1) * 2.0]
                },
                [[0.000000, 2.500000], [2.500000, 5.000000], [5.000000, 7.500000], [7.500000, 10.000000]],
                (0..9),

                [
                        [10.0, 40.0],
                        [40.0, 70.0],
                        [70.0, 100.0]
                ],
                [
                        [10.0, 40.0],
                        [40.0, 70.0],
                        [70.0, 100.0]
                ],
                [25.0, 55.0, 85.0],
                [25.0, 55.0, 85.0],
                [33, 78],

                [
                        [-10, -6],
                        [-6, -2],
                        [-2, 2],
                        [2, 6],
                        [6, 10]
                ],
                [-8, -4, 0, 4, 8],
                [-6, 1, 8]
        ]
    }

    def "test uneven splits"() {
        when:
        def numSplit = new NumericSplit("[0.0, 15.0]",
                false, 0.0, 0.0)
        numSplit.configureSplits(5, 4.0)
        numSplit.createSplits()

        then:
        numSplit.splits == [2, 6, 11.5].collect { convertToString it }

        when:
        numSplit.configureSplits(10, 2.0)
        numSplit.createSplits()
        then:
        numSplit.splits == [1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.5].collect {
            convertToString it
        }

        when:
        numSplit = new NumericSplit("[0, 21]", true, 0.0,0.0)
        numSplit.configureSplits(3, 8)
        numSplit.createSplits()

        then:
        numSplit.splits == [4, 15].collect {convertToString(it, false)}


        when:
        numSplit = new NumericSplit("[0, 10]", true, 0.0,0.0)
        numSplit.configureSplits(10, 0.0)
        numSplit.createSplits()

        then:
        numSplit.splits == (0..9).collect {convertToString(it, false)}


    }

    def "test error init"() {
        when:
        new NumericSplit(null, null)
        then:
        thrown(NullPointerException)

        when:
        new NumericSplit("[1,2]", null)
        then:
        thrown(NullPointerException)

        when:
        new NumericSplit("This is not a legal value", false, 0.0, 0.0)
        then:
        thrown(IllegalArgumentException)

        when:
        new NumericSplit("[0.1, 0.9]", true, 0.0, 0.0)
        then:
        thrown(IllegalArgumentException)
    }

    def "test fixed init"() {
        def numSplit;
        when:
        numSplit = new NumericSplit("[1,2]", false, 0.0, 0.0)

        then:
        !numSplit.valueFixed

        when:
        numSplit = new NumericSplit("[1,1]", false, 0.0, 0.0)
        then:
        numSplit.valueFixed
        numSplit.fixedVal == 1.0

        when:
        numSplit = new NumericSplit("[0, -1]", false, 0.0, 0.0)
        then:
        numSplit.valueFixed
        numSplit.fixedVal == 0.0

        when:
        numSplit = new NumericSplit("[0.5, 1.5]", true, 0.0, 0.0)
        then:
        numSplit.valueFixed
        numSplit.fixedVal == 1.0

        when:
        numSplit = new NumericSplit(null, false, 0.0, 0.0)
        then:
        numSplit.valueFixed
        numSplit.fixedVal == 0.0
    }

}
