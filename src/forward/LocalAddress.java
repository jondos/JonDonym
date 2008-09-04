package forward;

import anon.transport.address.AddressParameter;
import anon.transport.address.IAddress;

/**
 * Imlementierung einer Adress fuer lokalen Einsatz.
 */

public class LocalAddress implements IAddress{

	public static final String TRANSPORT_IDENTIFIER = "local";
	
	public AddressParameter[] getAllParameters() {
		return new AddressParameter[0];
	}

	public String getTransportIdentifier() {
		return TRANSPORT_IDENTIFIER;
	}

}
