package sentiment;

public class SentiScore {
	private double positive;
	private double negative;
	private double objective;
	
	public SentiScore(double pos, double neg) {
		if(pos < 0 || neg < 0)
			throw new IllegalArgumentException("Sentiment scores cannot be below 0!");
		if(pos > 1 || neg > 1)
			throw new IllegalArgumentException("Sentiment scores cannot be above 1!");
		objective = 1 - (pos+neg);
		if(objective < 0)
			throw new IllegalArgumentException("Positive and negative scores cannot sum above 1!");
		positive = pos;
		negative = neg;
	}

	public double getPositive() {
		return positive;
	}

	public double getNegative() {
		return negative;
	}

	public double getObjective() {
		return objective;
	}
}
