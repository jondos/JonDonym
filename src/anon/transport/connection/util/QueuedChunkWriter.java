package anon.transport.connection.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import anon.transport.connection.ConnectionException;
import anon.transport.connection.IChunkWriter;
import anon.util.ObjectQueue;


/**
 * Implementierung eines {@link IChunkWriter}, welcher die zu senden Chunks
 * nacheinander in eine Eingangs uebergebenen {@link BlockingQueue} einfuegt.
 */
public class QueuedChunkWriter implements IChunkWriter {

	/** Die BlockingQueue in welche Chunks eingefuegt werden. */
	private final ObjectQueue/*<byte[]>*/ m_writingQueue;

	/** Gibt an ob der Writer geschlossen ist. */
	private volatile boolean m_isClosed;

	/**
	 * Sammelt alle Threats, welche sich innerhalb der write() Methode befinden.
	 * <p>
	 * Dient dazu, beim Schliessen des Writers evtl. blockierte Threats mittelst
	 * {@link Thread#interrupt()} aufzuwecken.
	 */
	private final /*Collection<Thread>*/ Vector m_waitingThreads;

	/** Das Timeout fuer Schreiboperationen in Millisekunden */
	private int m_timeout;

	/**
	 * Erstellt einen neuen {@link QueuedChunkWriter} auf Grundlage der
	 * uebergebene Queue und dem entsprechenden Timeout.
	 * 
	 * @param a_writingQueue
	 *            Die Queue in welche die Chunks eingefuegt werden.
	 * @param a_timeout
	 *            Der initiale Wert fuer das Timeout der Schreiboperationen. Ein
	 *            Wert von 0 bestimmt ein unendliches Timeout.
	 */
	public QueuedChunkWriter(ObjectQueue/*<byte[]>*/ a_writingQueue, int a_timeout) {
		m_writingQueue = a_writingQueue;
		m_isClosed = false;
		m_waitingThreads = new Vector();//LinkedList<Thread>();
		m_timeout = a_timeout;
	}

	/**
	 * Erstellt einen neuen {@link QueuedChunkWriter} auf Grundlage der
	 * uebergebene Queue mit unendlichen Timeout.
	 * 
	 * @param a_writingQueue
	 *            Die Queue in welche die Chunks eingefuegt werden.
	 */
	public QueuedChunkWriter(ObjectQueue/*<byte[]>*/ a_readingQueue) {
		m_writingQueue = a_readingQueue;
		m_isClosed = false;
		m_waitingThreads = new Vector();//LinkedList<Thread>();
		m_timeout = 0;
	}

	public int getTimeout() {
		return m_timeout;
	}

	public void setTimeout(int a_value) {
		m_timeout = a_value;
	}

	public void writeChunk(byte[] a_chunk) throws ConnectionException {
		Thread caller = Thread.currentThread();
		try {
			// save caller for interrupting when closed
			m_waitingThreads.addElement(caller);
			if (m_isClosed)
				throw new ConnectionException("Reader allready closed");
			// after the previous step, we assume that the writer is open.
			// it could close until now, but we will
			// get an interruptedException when this happens
/*			if (m_timeout > 0)
				if (!m_writingQueue.offer(a_chunk, m_timeout,
						TimeUnit.MILLISECONDS))
					throw new ConnectionException("Timeout elapsed");
	*/
			m_writingQueue.push(a_chunk);
		} catch (Exception e) {
			throw new ConnectionException(
					"Innterupted while reading. Probaly closed Reader.");
		} finally {
			// the finally is our save block, as its guarantees the removing of
			// the
			// thread from the list.
			// every thread can only ones enter this method and should
			// always be there.
			boolean removed = m_waitingThreads.removeElement(caller);
//			assert removed : "Unable to remove caller Thread";
		}

	}

	public void close() throws IOException {
		synchronized(this)
		{
		if (m_isClosed)
			return; // nothing more to do
		m_isClosed=true;
		}
		// wake up all waiting threads
		Enumeration allthreads=m_waitingThreads.elements();
		while (allthreads.hasMoreElements())
			((Thread)(allthreads.nextElement())).interrupt();
	}

}
