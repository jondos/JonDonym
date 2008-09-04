package anon.transport.connection;

/**
 * Konkretisierung von {@link IConnection}, bei welchem die Uebertragung von
 * Daten ueber bidirektional und stossweise in Form von Datenblocken beliebiger
 * Laenge (chunks) erfolgt.
 * <p>
 * Aufgabe der {@link IChunkConnection}, sowie der mit ihr verbunden Reader und
 * Writer, ist es dabei die Reihenfolge und Unversehrtheit der Datenbloecke zu
 * garantieren.
 * <p>
 * Die jeweiligen Reader und Writer treten dabei immer als Paar auf und sind
 * fest mit der Verbindung verknuepft. Entsprechend sollten
 * {@link #getChunkReader()} und {@link #getChunkWriter()} solange die selben
 * Objekte zurueckliefern, bis sich der Zustand der Verbindung aendert.
 */
public interface IChunkConnection extends IConnection {

	/**
	 * Liefert den {@link IChunkReader}, ueber welchen gesendete Datenbloecke des
	 * Kommunikationspartners gelesen werden koennen.
	 */
	IChunkReader getChunkReader();

	/**
	 * Liefert den {@link IChunkWriter}, ueber welchen Datenbloecke zum
	 * Kommunikationspartner gesendet werden koennen.
	 */
	IChunkWriter getChunkWriter();

}
