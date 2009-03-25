package anon.crypto;

import java.util.Vector;

public class CertPathInfo
{
	private JAPCertificate m_firstCert;
	private JAPCertificate m_secondCert;
	private JAPCertificate m_rootCert;
	private Vector m_subCACerts;
	private boolean m_verified;
	private boolean m_valid;
	private int m_errorCode;
	private int m_errorPosition;
	private int m_docType;
	
	public CertPathInfo(JAPCertificate a_firstCert, 
			            JAPCertificate a_secondCert, 
			            JAPCertificate a_rootCert,
			            Vector a_subCACerts,
			            int a_docType)
	{
		 m_firstCert = a_firstCert;
		 m_secondCert = a_secondCert;
		 m_rootCert = a_rootCert;
		 m_subCACerts = a_subCACerts;
	}
	
	public void setVerified(boolean verified)
	{
		m_verified = verified;
	}
	
	public boolean isVerified()
	{
		return m_verified;
	}
	
	public JAPCertificate getFirstCertificate()
	{
		return m_firstCert;
	}
	
	public JAPCertificate getSecondCertificate()
	{
		return m_secondCert;
	}
	
	public JAPCertificate getRootCertificate()
	{
		return m_rootCert;
	}
	
	public Vector getSubCACerts()
	{
		return m_subCACerts;
	}
	
	public int getDocType()
	{
		return m_docType;
	}
	
	public int getlength()
	{
		int len = 0;
		if(m_firstCert != null)
		{
			len++;
		}
		if(m_secondCert != null)
		{
			len++;
		}
		if(m_rootCert != null)
		{
			len++;
		}
		if(m_subCACerts != null)
		{
			len += m_subCACerts.size();
		}
		return len;
	}
	
	public String toString()
	{
		String info = new String();
		String tab = "\t";
		
		if(m_rootCert != null)
		{
			info += m_rootCert.getSubject().getCommonName()+"\n";
		}
		
		if(m_subCACerts != null)
		{
			for(int i=m_subCACerts.size()-1; i>=0; i--)
			{
				info += tab+((JAPCertificate)m_subCACerts.elementAt(i)).getSubject().getCommonName()+"\n";
				tab += tab;
			}
		}
		if(m_secondCert != null)
		{
			info += tab+m_secondCert.getSubject().getCommonName()+"\n";
			tab += tab;
		}
		if(m_firstCert != null)
		{
			info += tab+m_firstCert.getSubject().getCommonName()+"\n";
		}
		return info;
	}
}
