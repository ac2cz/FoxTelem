package telemetry;

public abstract class Conversion {
	protected String name; // must be unique in the namespace of the spacecraft
	
	Conversion(String name) {
		if (name == null) throw new IllegalArgumentException("Conversion name null");
		this.name = name;
	}
	
	public String getName() { return name; }
	abstract public double calculate(double x);

}
