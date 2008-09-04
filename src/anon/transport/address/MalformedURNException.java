package anon.transport.address;

import anon.transport.connection.ConnectionException;

/**
 * Gibt an, dass waehrend der Verarbeitung einer Endpoint-URN Abweichungen von
 * dem erwarteten Format auftraten.
 */
public class MalformedURNException extends ConnectionException {

	public MalformedURNException(String message) {
		super(message);
	}

}
