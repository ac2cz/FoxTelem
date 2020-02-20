package telemetry;

public abstract class Conversion {
	protected String name; // must be unique in the namespace of the spacecraft
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }

}
