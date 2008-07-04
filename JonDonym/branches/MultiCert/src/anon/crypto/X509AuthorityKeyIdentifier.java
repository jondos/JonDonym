package anon.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;

/**
 * The Authority Public Key Identifier as specified in RFC 5280 is calcluated
 * the same way as the SubjectKeyIdentifier. Note that the Optional fields for
 * the GeneralName of the Issuer and his certificate's Serial Number are omitted!
 * @author Robert Hirschberger
 *
 */
public class X509AuthorityKeyIdentifier extends AbstractX509KeyIdentifier {

	public static final String IDENTIFIER = X509Extensions.AuthorityKeyIdentifier.getId();
		
	private BigInteger m_serial;
	private GeneralNames m_names;

	/**
	 * Creates a new X509AuthorityKeyIdentifier from a public key.
	 * @param a_publicKey a public key
	 */
	public X509AuthorityKeyIdentifier(IMyPublicKey a_publicKey)
	{
		super(IDENTIFIER, createDEROctets(a_publicKey, null, null));
		createValue();
	}
	
	public X509AuthorityKeyIdentifier(IMyPublicKey a_publicKey, GeneralNames a_name, BigInteger a_serial)
	{
		super(IDENTIFIER, createDEROctets(a_publicKey, a_name, a_serial));
		createValue();
	}
	
	/**
	 * Creates an extension from a BouncyCastle DER sequence. For internal use only.
	 * @param a_extension a DERSequence
	 */
	public X509AuthorityKeyIdentifier(DERSequence a_extension)
	{
		super(a_extension);
		createValue();
	}

	/**
	 * Returns "AuthorityKeyIdentifier".
	 * @return "AuthorityKeyIdentifier"
	 */
	public String getName()
	{
		return "AuthorityKeyIdentifier";
	}
	
	private static byte[] createDEROctets(IMyPublicKey a_publicKey, GeneralNames a_name, BigInteger a_serial)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		AuthorityKeyIdentifier aki;
		
		if(a_name != null && a_serial != null)
		{
			aki = new AuthorityKeyIdentifier(a_publicKey.getAsSubjectPublicKeyInfo(), a_name, a_serial);
		}
		else
		{
			aki = new AuthorityKeyIdentifier(a_publicKey.getAsSubjectPublicKeyInfo());
		}
		
		try
		{
			new DEROutputStream(out).writeObject((DERSequence)aki.getDERObject());
		}
		catch (Exception a_e)
		{
			// should never happen
			throw new RuntimeException("Could not write DER object to bytes!");
		}

	   return out.toByteArray();
	}
	
	private void createValue()
	{
		byte[] identifier;

		try
		{
			AuthorityKeyIdentifier aki = new AuthorityKeyIdentifier((DERSequence)new ASN1InputStream(
					 new ByteArrayInputStream(getDEROctets())).readObject());
			identifier = aki.getKeyIdentifier();
			m_value = ByteSignature.toHexString(identifier);
			m_serial = aki.getAuthorityCertSerialNumber();
			m_names = aki.getAuthorityCertIssuer();
		}
		catch (Exception a_e)
		{
			// this should never happen
			throw new RuntimeException("Could not read authority key identifier from byte array!");
		}
	}
	
	public Vector getValues()
	{
		Vector<String> v = new Vector();
		v.add(m_value);
		if(m_names != null)
		{
			GeneralName[] names = m_names.getNames();
			for(int i=0; i<names.length; i++)
			{
				String name = new String(names[i].getName().getDERObject().getDEREncoded());
				//name = name+" ("+AbstractX509AlternativeName.getTagAsString(names[i].getTagNo())+")";
				v.add(name);	
			}
		}
		if(m_serial != null)
		{
			v.add("authorityCertSerialNumber: "+m_serial);
		}
		return v;
	}
}
