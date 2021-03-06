package ai.libs.jaicore.ml.core.dataset.sampling.inmemory.casecontrol;

import java.util.ArrayList;
import java.util.Random;

import ai.libs.jaicore.basic.sets.Pair;
import ai.libs.jaicore.ml.core.dataset.IDataset;
import ai.libs.jaicore.ml.core.dataset.ILabeledAttributeArrayInstance;
import ai.libs.jaicore.ml.core.dataset.weka.WekaInstance;
import weka.classifiers.Classifier;
import weka.core.Instance;

public class LocalCaseControlSampling<I extends ILabeledAttributeArrayInstance<?>, D extends IDataset<I>> extends PilotEstimateSampling<I, D> {

	public LocalCaseControlSampling(final Random rand, final int preSampleSize, final D input) {
		super(input);
		this.rand = rand;
		this.preSampleSize = preSampleSize;
	}

	@Override
	protected ArrayList<Pair<I, Double>> calculateFinalInstanceBoundaries(final D instances, final Classifier pilotEstimator) {
		double boundaryOfCurrentInstance = 0.0;
		ArrayList<Pair<Instance, Double>> instanceProbabilityBoundaries = new ArrayList<>();
		double sumOfDistributionLosses = 0;
		double loss;
		for (I instance : instances) {
			Instance wekaInstance = ((WekaInstance<?>)instance).getElement();
			try {
				loss = 1 - pilotEstimator.distributionForInstance(wekaInstance)[(int) wekaInstance.classValue()];
			} catch (Exception e) {
				loss = 1;
			}
			sumOfDistributionLosses += loss;
		}
		for (I instance : instances) {
			Instance wekaInstance = ((WekaInstance<?>)instance).getElement();
			try {
				loss = 1 - pilotEstimator.distributionForInstance(wekaInstance)[(int) wekaInstance.classValue()];
			} catch (Exception e) {
				loss = 1;
			}
			boundaryOfCurrentInstance += loss / sumOfDistributionLosses;
			instanceProbabilityBoundaries.add(new Pair<Instance, Double>(wekaInstance, boundaryOfCurrentInstance));
		}
		ArrayList<Pair<I, Double>> probabilityBoundaries = new ArrayList<>();
		int iterator = 0;
		for (I instance : instances) {
			probabilityBoundaries.add(new Pair<I, Double>(instance, instanceProbabilityBoundaries.get(iterator).getY()));
			iterator++;
		}
		return probabilityBoundaries;
	}
}
