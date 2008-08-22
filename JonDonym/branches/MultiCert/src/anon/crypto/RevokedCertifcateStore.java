package anon.crypto;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class RevokedCertifcateStore
{
	private static RevokedCertifcateStore m_instance;
	private static final String CRL_PATH = "crls/";
	
	private Hashtable m_revokedCerts;
	
	private RevokedCertifcateStore()
	{
		CertificateRevocationList crl = null;
		m_revokedCerts = new Hashtable();
		Enumeration crls = CertificateRevocationList.getInstance(CRL_PATH, true, null).elements();
		
		while(crls.hasMoreElements())
		{
			crl = (CertificateRevocationList) crls.nextElement();
			//TODO if(crl.verify())
			{
				addRevocations(crl);
			}
		}
		// no crls found
		if(crl == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.CRYPTO, "Could not load default CRLs!");
		}
	}
	
	public void addRevocations(CertificateRevocationList a_crl)
	{
		RevokedCertificate revCert;
		X509DistinguishedName issuer;
		Enumeration revocations = a_crl.getRevokedCertificates().elements();
		boolean indirectCRL = a_crl.isIndirectCRL();
		
		synchronized(m_revokedCerts)
		{
			while(revocations.hasMoreElements())
			{
				revCert = (RevokedCertificate) revocations.nextElement();
				issuer = null;
				if(indirectCRL)
				{
					issuer = revCert.getCertificateIssuer();
				}
				if(issuer == null)
				{
					issuer = a_crl.getIssuer();
				}
				m_revokedCerts.put(issuer.toString()+revCert.getSerialNumber().toString(), revCert);
			}
		}
	}
	
	private static String keyValue(JAPCertificate a_cert)
	{
		return (a_cert.getIssuer().toString()+a_cert.getSerialNumber().toString());
	}
	
	public boolean isCertificateRevoked(JAPCertificate a_cert)
	{
		synchronized (m_revokedCerts)
		{
			return m_revokedCerts.containsKey(keyValue(a_cert));
		}
	}
	
	public Date getRevocationDate(JAPCertificate a_cert)
	{
		RevokedCertificate revCert;
		synchronized (m_revokedCerts)
		{
			if(isCertificateRevoked(a_cert))
			{
				revCert = (RevokedCertificate) m_revokedCerts.get(keyValue(a_cert));
				return revCert.getRevocationDate();
			}
			return null;
		}
	}

	public static RevokedCertifcateStore getInstance()
	{
		synchronized (RevokedCertifcateStore.class)
		{
			if(m_instance == null)
			{
				m_instance = new RevokedCertifcateStore();
			}
			return m_instance;
		}
	}
}
