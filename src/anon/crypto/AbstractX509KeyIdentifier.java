package anon.crypto;

import java.util.StringTokenizer;
import java.util.Vector;

import org.bouncycastle.asn1.DERSequence;

import anon.util.Util;

/**
 * The key identifier is calculated using a SHA1 hash over the BIT STRING from
 * SubjectPublicKeyInfo as defined in RFC3280.
 * For DSA-PublicKeys the AlgorithmIdentifier of the SubjectPublicKeyInfo MUST 
 * contain the DSA-Parameters as specified in RFC 3279
 * @author Rolf Wendolsky, Robert Hirschberger
 */
public abstract class AbstractX509KeyIdentifier extends AbstractX509Extension 
{
	protected String m_value;
	
	public AbstractX509KeyIdentifier(String a_identifier, byte[] a_value)
	{
		super(a_identifier, false, a_value);
	}
	
    public AbstractX509KeyIdentifier(DERSequence a_extension)
    {
    	super(a_extension);
    }
    
	/**
	 * Returns the key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 * @return the key identifier as human-readable hex string of the form
	 * A4:54:21:52:F1:...
	 */
	public String getValue()
	{
		return m_value;
	}

	/**
	 * Returns the key identifier as human-readable hex string without ":"
	 * separators.
	 * @return the key identifier as human-readable hex string without ":"
	 * separators
	 */
	public String getValueWithoutColon()
	{
		StringTokenizer tokenizer = new StringTokenizer(m_value, ":");
		String value = "";

		while (tokenizer.hasMoreTokens())
		{
			value += tokenizer.nextToken();
		}
		return value;
	}

	/**
	 * Returns the key identifier as human-readable hex string.
	 * @return a Vector containing the key identifier as human-readable hex string
	 */
	public Vector getValues()
	{
		return Util.toVector(m_value);
	}
}
