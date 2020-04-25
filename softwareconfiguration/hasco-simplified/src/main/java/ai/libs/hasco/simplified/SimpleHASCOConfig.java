package ai.libs.hasco.simplified;

import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;
import ai.libs.jaicore.basic.IOwnerBasedConfig;
import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({ "file:~/conf/simple-hasco.conf",
        "classpath:conf/simple-hasco.conf" })
public interface SimpleHASCOConfig extends IOwnerBasedAlgorithmConfig {

    String HASCO_SIMPLIFIED_MIN_EVAL_QUEUE_SIZE = "hasco.simplified.minEvalQueueSize";

    String HASCO_SIMPLIFIED_REFINEMENT_TIME = "hasco.simplified.refinementTime";

    String HASCO_SIMPLIFIED_SAMPLE_TIME = "hasco.simplified.sampleTime";

    String HASCO_SIMPLIFIED_SEED = "hasco.simplified.seed";

    String HASCO_SIMPLIFIED_PUBLISH_NODES = "hasco.simplified.publishNodes";

    String HASCO_SIMPLIFIED_STD_RANDOM_NUMERIC_SPLIT = "hasco.simplified.std.randomNumericSplit";
    String HASCO_SIMPLIFIED_STD_DRAW_NUMERIC_SPLIT_MEANS = "hasco.simplified.std.drawNumericSplitMeans";

    String HASCO_SIMPLIFIED_THREAD_COUNT = "hasco.simplified.threadCount";

    String HASCO_SIMPLIFIED_SAMPLES_PER_REFINEMENT = "hasco.simplified..samplesPerRefinement";

    String SIMPLIFIED_STD_NUM_SPLITS_FALLBACK = "simplified.std.numSplitsFallback";
    String SIMPLIFIED_STD_MIN_RANGE_SIZE_FALLBACK = "simplified.std.minRangeSizeFallback";

    @Key(HASCO_SIMPLIFIED_MIN_EVAL_QUEUE_SIZE)
    @DefaultValue("8")
    Integer getMinEvalQueueSize();

    @Key(HASCO_SIMPLIFIED_REFINEMENT_TIME)
    @DefaultValue("8000")
    Long getRefinementTime();

    @Key(HASCO_SIMPLIFIED_SAMPLE_TIME)
    @DefaultValue("2000")
    Long getSampleTime();

    @Key(HASCO_SIMPLIFIED_SEED)
    Long getSeed();

    @Key(HASCO_SIMPLIFIED_PUBLISH_NODES)
    @DefaultValue("false")
    Boolean areNodesPublished();

    @Key(HASCO_SIMPLIFIED_STD_RANDOM_NUMERIC_SPLIT)
    @DefaultValue("false")
    Boolean randomNumericSplit();

    @Key(HASCO_SIMPLIFIED_STD_DRAW_NUMERIC_SPLIT_MEANS)
    @DefaultValue("true")
    Boolean meanNumericSplit();

    @Key(HASCO_SIMPLIFIED_THREAD_COUNT)
    @DefaultValue("8")
    Integer getWorkerThreadCount();

    @Key(HASCO_SIMPLIFIED_SAMPLES_PER_REFINEMENT)
    @DefaultValue("2")
    Integer getNumSamplesPerRefinement();

    @Key(SIMPLIFIED_STD_MIN_RANGE_SIZE_FALLBACK)
    @DefaultValue("2")
    Integer getMinRangeSizeFallback();

    @Key(SIMPLIFIED_STD_NUM_SPLITS_FALLBACK)
    @DefaultValue("2")
    Integer getNumSplitsFallback();

}

