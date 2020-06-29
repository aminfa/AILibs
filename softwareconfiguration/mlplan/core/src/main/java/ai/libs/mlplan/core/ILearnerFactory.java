package ai.libs.mlplan.core;

import org.api4.java.ai.ml.core.learner.ISupervisedLearner;

import ai.libs.softwareconfiguration.optimizingfactory.BaseFactory;

public interface ILearnerFactory<L extends ISupervisedLearner<?, ?>> extends BaseFactory<L> {

}
