package decoder;

public interface Listener<T>
{
	public void receive( T t );
}