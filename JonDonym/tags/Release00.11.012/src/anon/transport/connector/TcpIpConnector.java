package anon.transport.connector;

import java.io.IOException;
import java.net.Socket;

import anon.transport.address.IAddress;
import anon.transport.address.TcpIpAddress;
import anon.transport.connection.CommunicationException;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IConnection;
import anon.transport.connection.IStreamConnection;
import anon.transport.connection.SocketConnection;


public class TcpIpConnector implements IConnector {

	public IStreamConnection connect(TcpIpAddress a_address)
			throws ConnectionException {
		try {
			Socket transport = new Socket(a_address.getIPAddress(), a_address
					.getPort());
			return new SocketConnection(transport);
		} catch (IOException e) {
			throw new CommunicationException(e);
		}
	}

	public IConnection connect(IAddress a_address) throws ConnectionException {
		if (!(a_address instanceof TcpIpAddress))
			throw new IllegalArgumentException(
					"Connector can only handel Address of type TcpIpAddress");
		return null;
	}

}
