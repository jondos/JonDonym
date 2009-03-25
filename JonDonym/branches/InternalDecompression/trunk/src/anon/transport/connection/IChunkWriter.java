package anon.transport.connection;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Durch einen {@link IChunkWriter} wird das Schreibende eines Datenkanals
 * beschrieben, ueber welchen Daten in Form von byte[] beliebiger Laenge
 * verschickt werden koennen.
 * <p>
 * Konzeptionel handelt es sich um die chunk-basierte Entsprechung eines
 * {@link OutputStream} und erlauben die gesicherte Uebertragung von mehreren
 * Bytes am Stueck (Chunk), wobei davon ausgegangen werden kann, dass der
 * gesendete Chunk inhaltlich genauso empfangen wird. Die Zuordnung und die
 * Reihenfolge der einzelnen Bytes innerhalb eines Chunks wird durch die
 * uebertragung nicht veraendert.
 * <p>
 * Die Einspeisung in den Kanal sollte durch die Schreibmethode immer sofort
 * erfolgen, weshalb keine notwendigkeit fuer eine {@link OutputStream#flush()}
 * aehnliche Methode besteht.
 */
public interface IChunkWriter {

	/**
	 * Versucht den uebergeben Chunk in den Kanal einzuspeisen und somit zum
	 * Empfaenger zu uebertragen.
	 * <p>
	 * Sofern der Kanal voll ist blockiert der Aufruf, bis es moeglich war den
	 * Chunk zu uebertragen oder eine entsprechende Ausnahme wird geworfen.
	 * 
	 * @param Der
	 *            zu uebertragene Chunk
	 */
	void writeChunk(byte[] a_chunk) throws ConnectionException;
	
	void close()  throws IOException;

}
