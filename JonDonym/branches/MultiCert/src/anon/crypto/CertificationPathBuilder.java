package anon.crypto;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;

public class CertificationPathBuilder
{
	private Vector m_endEntityCertificates;
	private Vector m_pathCertificates;
	private Vector m_trustAnchorCertificates;
	
	public CertificationPathBuilder(
			Vector a_endEntityCertificates, 
			Vector a_pathCertificates, 
			Vector a_trustAnchorCertificates)
	{
		m_endEntityCertificates = a_endEntityCertificates;
		m_pathCertificates = a_pathCertificates;
		m_trustAnchorCertificates = a_trustAnchorCertificates;
	}
	
	private void doExtendedNameChaining()
	{
		Enumeration endEntityCertificates = m_endEntityCertificates.elements();
		Enumeration pathCertificates = null;
		Enumeration trustAnchorCertificates = m_endEntityCertificates.elements();
		
		if(m_pathCertificates != null)
		{
			pathCertificates = m_pathCertificates.elements();
		}
		
		JAPCertificate endEntity, pathEntity, trustAnchor;
		X509DistinguishedName currentIssuer;
		X509AuthorityKeyIdentifier x509aki;
		String aki;
		
		while(endEntityCertificates.hasMoreElements())
		{
			endEntity = (JAPCertificate)endEntityCertificates.nextElement();
			currentIssuer = endEntity.getIssuer();
			x509aki = (X509AuthorityKeyIdentifier)endEntity.getExtensions().getExtension(X509AuthorityKeyIdentifier.IDENTIFIER);
			aki = x509aki.getValueWithoutColon();
			
			if(pathCertificates != null)
			{
				
			}
		}
	}
	
	private void findIssuer(
			Enumeration a_certificates, 
			X509DistinguishedName a_name, 
			String a_keyID)
	{
		
	}
	
	
}
