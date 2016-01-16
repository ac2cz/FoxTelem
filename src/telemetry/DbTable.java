package telemetry;

public interface DbTable {

	public abstract String getTableCreateStmt();
	public abstract String getInsertStmt();
	
}
