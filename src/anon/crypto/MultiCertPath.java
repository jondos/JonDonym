package anon.crypto;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.util.encoders.Hex;

public class MultiCertPath
{
	private CertPath[] m_certPaths;
	private X509DistinguishedName m_subject;
	private X509DistinguishedName m_issuer;
	
	protected MultiCertPath(CertPath[] a_certPaths)
	{
		if(a_certPaths.length != 0 && a_certPaths[0] != null)
		{
			m_subject = a_certPaths[0].getFirstCertificate().getSubject();
			m_issuer = a_certPaths[0].getFirstCertificate().getIssuer();
						
			for(int i=1; i<a_certPaths.length; i++)
			{
				if(!m_subject.equals(a_certPaths[i].getFirstCertificate().getSubject()))
				{
					//TODO see below
				}
				if(!m_issuer.equals(a_certPaths[i].getFirstCertificate().getIssuer()))
				{
					//TODO print warning? throw IllegalArgumentsException
				}
			}
		}
		m_certPaths = a_certPaths;
	}
	
	public boolean isValid(Date a_date)
	{
		CertPath path = getFirstVerifiedPath();
		if(path != null)
		{
			return path.checkValidity(a_date);
		}
		return false;
	}
	
	public boolean isVerified()
	{
		return getFirstVerifiedPath() != null;
	}
	
	public CertPath getFirstVerifiedPath()
	{
		for(int i=0; i<m_certPaths.length; i++)
		{
			if(m_certPaths[i].verify())
			{
				return m_certPaths[i];
			}
		}
		return null;
	}
	
	/*public JAPCertificate getFirstCertificate()
	{
		CertPath verified = getFirstVerifiedPath();
		if(verified != null)
		{
			return verified.getFirstCertificate();
		}
		return null;
	}*/
	

	public String getXORofSKIs()
	{
		byte[] raw = new byte[20];
		JAPCertificate cert;
		
		for(int i=0; i<m_certPaths.length; i++)
		{
			cert = m_certPaths[i].getFirstCertificate();
			byte[] ski = cert.getRawSubjectKeyIdentifier();
			
			for(int j=0; j<raw.length; j++)
			{
				raw[j] = (byte) (raw[j] ^ ski[j]);
			}
		}
		return new String(Hex.encode(raw));
	}
	
	public Vector getEndEntityKeys()
	{
		Vector keys = new Vector();
		
		for(int i=0; i<m_certPaths.length; i++)
		{
			if(m_certPaths[i].verify())
			{
				keys.addElement(m_certPaths[i].getFirstCertificate().getPublicKey());
			}
		}
		if(keys.size() != 0)
		{
			return keys;
		}
		return null;
	}

	public X509DistinguishedName getSubject()
	{
		return m_subject;
	}
	
	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}
}
