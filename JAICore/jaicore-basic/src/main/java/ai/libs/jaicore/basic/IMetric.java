package ai.libs.jaicore.basic;

public interface IMetric<T> {
	public double getDistance(T a, T b);
}
