 package anon.crypto;

import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Holds a single <Signature>-Element which is held in an XMLSignature-object.
 * Only instances of XMLSignature should use the methods of this class.
 * @author Robert Hirschberger
 */
public class XMLSignatureElement implements IXMLEncodable
{
	private static final String XML_ELEMENT_NAME = "Signature";
	private static final String ELEM_CANONICALIZATION_METHOD = "CanonicalizationMethod";
	private static final String ELEM_SIGNATURE_METHOD = "SignatureMethod";
	private static final String ELEM_SIGNATURE_VALUE = "SignatureValue";
	private static final String ELEM_KEY_INFO = "KeyInfo";
	private static final String ELEM_SIGNED_INFO = "SignedInfo";
	private static final String ELEM_REFERENCE = "Reference";
	private static final String ELEM_DIGEST_VALUE = "DigestValue";
	private static final String ELEM_DIGEST_METHOD = "DigestMethod";
	private static final String ATTR_URI = "URI";
	private static final String ATTR_ALGORITHM = "Algorithm";
	
	private static final String DIGEST_METHOD_ALGORITHM = "http://www.w3.org/2000/09/xmldsig#sha1";
	
	private XMLSignature m_parent;
	private Element m_elemSignature;
	private String m_signatureMethod;
	private String m_signatureValue;
	private String m_referenceURI;
	private String m_digestMethod;
	private String m_digestValue;
	private byte[] m_signedInfoCanonical;

	/**
	 * Stores all appended certificates
	 * It is very important, that whenever this Vector is changed,
	 * we also have to change m_appendedCertXMLElements, because
	 * the values have to be at the same index of the Vectors
	 */
	private Vector m_appendedCerts;
	/** Stores the XML representation of the appended certificates */
	private Vector m_appendedCertXMLElements;
	/** Stores the certification Path of this Signature */
	private CertPath m_certPath;
	
	protected XMLSignatureElement(XMLSignature a_parent)
	{
		m_parent = a_parent;
		m_appendedCerts = new Vector();
		m_appendedCertXMLElements = new Vector();
	}
	
	protected XMLSignatureElement(XMLSignature a_parent,
							   Element a_element,
							   IMyPrivateKey a_signKey,
							   byte[] a_digestValue) throws Exception
	{
		this(a_parent);
		createSignatureElement(a_signKey, a_element, a_digestValue);
	}
	
	/**
	 * Creates a new signature from a signature element.
	 * @param a_element an XML Element
	 * @exception XMLParseException if the element is no valid signature element
	 */
	protected XMLSignatureElement(XMLSignature a_parent, Element a_element) throws XMLParseException
	{
		Node node, subnode;
		
		if (a_element == null || !a_element.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, "This is no signature element!");
		}
		
		m_parent = a_parent;
		m_elemSignature = a_element;
		findCertificates(m_elemSignature);

		node = XMLUtil.getFirstChildByName(m_elemSignature, ELEM_SIGNED_INFO);
		if (node == null)
		{

			m_signedInfoCanonical = XMLSignature.toCanonicalDeprecated(m_elemSignature);
			if (m_signedInfoCanonical == null)
			{
				throw new XMLParseException(ELEM_SIGNED_INFO);
			}
		}
		else
		{
			m_signedInfoCanonical = XMLSignature.toCanonical(node);
			/** @todo SIGNATURE_METHOD is optional due to compatibility reasons; make this mandatory */
			subnode = XMLUtil.getFirstChildByName(node, ELEM_SIGNATURE_METHOD);
			m_signatureMethod = XMLUtil.parseValue(subnode, "");

			node = XMLUtil.getFirstChildByName(node, ELEM_REFERENCE);
			if (node == null)
			{
				throw new XMLParseException(ELEM_REFERENCE);
			}
			m_referenceURI = XMLUtil.parseAttribute( (Element) node, ATTR_URI, "");

			/** @todo DIGEST_METHOD is optional due to compatibility reasons; make this mandatory */
			subnode = XMLUtil.getFirstChildByName(node, ELEM_DIGEST_METHOD);
			m_digestMethod = XMLUtil.parseValue(subnode, "");

			node = XMLUtil.getFirstChildByName(node, ELEM_DIGEST_VALUE);
			if (node == null)
			{
				throw new XMLParseException(ELEM_DIGEST_VALUE);
			}
			m_digestValue = XMLUtil.parseValue(node, "");
		}

		node = XMLUtil.getFirstChildByName(m_elemSignature, ELEM_SIGNATURE_VALUE);
		if (node == null)
		{
			throw new XMLParseException(ELEM_SIGNATURE_VALUE);
		}
		m_signatureValue = XMLUtil.parseValue(node, "");
	}
	
	private void createSignatureElement(IMyPrivateKey a_signKey, Element a_elementToSign, byte[] a_digestValue) throws Exception
	{
		byte[] signatureValue;
		
		m_referenceURI = ""; // no URI is set
		m_digestMethod = DIGEST_METHOD_ALGORITHM;
		m_digestValue = new String(Base64.encode(a_digestValue, false));

		/* now build the SignedInfo node tree */
		Document doc = a_elementToSign.getOwnerDocument();
		Element signedInfoNode = doc.createElement(ELEM_SIGNED_INFO);
		/** @todo the actual type of the canonicalization method is not known... */
		Element canonicalizationNode = doc.createElement(ELEM_CANONICALIZATION_METHOD);
		Element signatureMethodNode = doc.createElement(ELEM_SIGNATURE_METHOD);
		String signatureMethod =
			a_signKey.getSignatureAlgorithm().getXMLSignatureAlgorithmReference();
		if (signatureMethod != null)
		{
			m_signatureMethod = signatureMethod;
			XMLUtil.setAttribute(signatureMethodNode, ATTR_ALGORITHM, signatureMethod);
		}
		else
		{
			m_signatureMethod = "";
		}

		Element referenceNode = doc.createElement(ELEM_REFERENCE);
		if (m_referenceURI.length() > 0)
		{
			referenceNode.setAttribute(ATTR_URI, m_referenceURI);
		}
		Element digestMethodNode = doc.createElement(ELEM_DIGEST_METHOD);
		XMLUtil.setAttribute(digestMethodNode, ATTR_ALGORITHM, DIGEST_METHOD_ALGORITHM);
		Element digestValueNode = doc.createElement(ELEM_DIGEST_VALUE);
		XMLUtil.setValue(digestValueNode, m_digestValue);
		referenceNode.appendChild(digestMethodNode);
		referenceNode.appendChild(digestValueNode);
		signedInfoNode.appendChild(canonicalizationNode);
		signedInfoNode.appendChild(signatureMethodNode);
		signedInfoNode.appendChild(referenceNode);

		m_signedInfoCanonical = XMLSignature.toCanonical(signedInfoNode);

		/* now we sign the SignedInfo node tree */
		signatureValue = ByteSignature.sign(m_signedInfoCanonical, a_signKey);
		signatureValue =
			a_signKey.getSignatureAlgorithm().encodeForXMLSignature(signatureValue);
		if (signatureValue == null)
		{
			// An error occured while signing or encoding
			throw new Exception();
		}
		m_signatureValue = new String(Base64.encode(signatureValue, false));
		/* create the SignatureValue node and build the Signature tree */
		Element signatureValueNode = doc.createElement(ELEM_SIGNATURE_VALUE);
		signatureValueNode.appendChild(doc.createTextNode(m_signatureValue));
		Element signatureNode = doc.createElement(XML_ELEMENT_NAME);
		signatureNode.appendChild(signedInfoNode);
		signatureNode.appendChild(signatureValueNode);

		/* now add the Signature node as a child to our toSign node */
		a_elementToSign.appendChild(signatureNode);
		m_elemSignature = signatureNode;
	}
	
	private synchronized void findCertificates(Element a_xmlSignature)
	{
		m_appendedCerts = new Vector();
		m_appendedCertXMLElements = new Vector();
		JAPCertificate currentCertificate;
		Element elemContainer;
		Node nodeCertificate;

		elemContainer = (Element) XMLUtil.getFirstChildByName(a_xmlSignature, ELEM_KEY_INFO);
		if (elemContainer == null)
		{
			return;
		}

		elemContainer = (Element) XMLUtil.getFirstChildByName(elemContainer,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (elemContainer == null)
		{
			return;
		}

		nodeCertificate = XMLUtil.getFirstChildByName(elemContainer, JAPCertificate.XML_ELEMENT_NAME);
		while (nodeCertificate != null)
		{
			try
			{
				currentCertificate = JAPCertificate.getInstance(nodeCertificate);
				if (currentCertificate != null)
				{
					m_appendedCerts.addElement(currentCertificate);
					m_appendedCertXMLElements.addElement(nodeCertificate);
				}
			}
			catch (ClassCastException a_e)
			{
				// the node is non XML element; should not happen...
			}
			nodeCertificate = nodeCertificate.getNextSibling();
		}
	}
	
	public boolean verifyFast(Node a_node, IMyPublicKey a_publicKey) throws XMLParseException
	{
		return verify(a_node, a_publicKey);
	}
	
	/**
	 * Verifies this Signature Element with either the appended certs or
	 * with the directCertPaths, if there are no appended certs.
	 * @param a_node
	 * @param a_documentType
	 * @param a_directCertPaths
	 * @return
	 * @throws XMLParseException
	 */
	public boolean verify(Node a_node, int a_documentType, Vector a_directCertPaths) throws XMLParseException
	{
		Enumeration certificates;
		
		if(m_appendedCerts.size() > 0)
		{
			certificates = m_appendedCerts.elements();
			//try to verify the signature with its appended certificates
			while(certificates.hasMoreElements())
			{
				JAPCertificate currentCertificate = (JAPCertificate) certificates.nextElement();
				if(verify(a_node, currentCertificate.getPublicKey()))
				{
					Vector appendedCertificates = (Vector)this.getCertificates().clone();
					appendedCertificates.remove(currentCertificate);
					m_certPath = CertPath.getInstance(currentCertificate, a_documentType, appendedCertificates);
					return true;
				}
			}
		}
		else
		{
			//if there are no appended certs try verification with the stored certificates
			certificates = a_directCertPaths.elements();
			while(certificates.hasMoreElements())
			{
				CertPath currentPath = (CertPath) certificates.nextElement();
				
				if(verify(a_node, currentPath.getFirstCertificate().getPublicKey()))
				{
					m_certPath = currentPath;
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * This method is used to verify a node with a previously created XMLSignature.
	 * @param a_node an XML node
	 * @param a_signature an XMLSignature
	 * @param a_publicKey a public key
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 * @return true if the node could be verified with this signature; false otherwise
	 */
	private boolean verify(Node a_node, IMyPublicKey a_publicKey) throws
		XMLParseException
	{

		if (a_publicKey == null || a_node == null)
		{
			return false;
		}

		if (!checkMessageDigest(a_node))
		{
			return false;
		}

		if (!checkSignature(a_publicKey))
		{	
			return false;
		}

		return true;
	}

	/**
	 * Checks if the signature of the XMLSignatureElement's SIGNED_INFO is valid.
	 * @param a_publicKey a public key
	 * @return true if the signature of the XMLSignature`s SIGNED_INFO is valid; false otherwise
	 */
	private boolean checkSignature(IMyPublicKey a_publicKey)
	{
		byte[] buff;

		buff = Base64.decode(m_signatureValue);
		buff = a_publicKey.getSignatureAlgorithm().decodeForXMLSignature(buff);
		if (buff == null)
		{
			// an error occured while decoding the signature
			return false;
		}

		//testing Signature....
		return ByteSignature.verify(m_signedInfoCanonical, buff, a_publicKey);
	}
	
	/**
	 * 
	 * @param a_node
	 * @param a_signature
	 * @return
	 * @throws XMLParseException
	 */
	private boolean checkMessageDigest(Node a_node) throws XMLParseException
	{
		SHA1Digest sha1;
		byte[] digest;

		if (m_digestMethod == null)
		{
			// no digest was used; message is verified directly
			return true;
		}
		
		sha1 = new SHA1Digest();
		digest = new byte[sha1.getDigestSize()];
		byte[] buff = XMLSignature.toCanonical(a_node, m_parent.getSignatureElements());
		sha1.update(buff, 0, buff.length);
		sha1.doFinal(digest, 0);
		
		return Util.arraysEqual(Base64.decode(m_digestValue), digest);
	}
	
	protected Element getSignatureElement()
	{
		return m_elemSignature;
	}
	
	/**
	 * Returns the signature method that was used for creating this signature.
	 * @return the signature method that was used for creating this signature
	 */
	public String getSignatureMethod()
	{
		return m_signatureMethod;
	}

	/**
	 * Returns the digest method that was used for creating this signature.
	 * @return the digest method that was used for creating this signature
	 */
	public String getDigestMethod()
	{
		return m_digestMethod;
	}

	/**
	 * Returns the reference attribute URI.
	 * @return the reference attribute URI
	 */
	public String getReferenceURI()
	{
		return m_referenceURI.trim();
	}

	public CertPath getCertPath()
	{
		return m_certPath;
	}
	
	//TODO implement these methods in XMLSignature!
	
	/**
	 * Returns all X509 certificates that are embedded in this SignatureElement.
	 * @return all X509 certificates that are emmbeded in this SignatureElement;
	 */
	private synchronized Vector getCertificates()
	{
		Vector certificates = new Vector(m_appendedCerts.size());
		Enumeration enumCerts = m_appendedCerts.elements();
		while (enumCerts.hasMoreElements())
		{
			certificates.addElement(enumCerts.nextElement());
		}
		return certificates;
	}

	/**
	 * Returns if the specified certificate is already contained in this signature element.
	 * @param a_certificate an X509 certificate
	 * @return true if the specified certificate is already contained in this signature element;
	 *         false otherwise
	 */
	public synchronized boolean containsCertificate(JAPCertificate a_certificate)
	{
		return m_appendedCerts.contains(a_certificate);
	}

	/**
	 * Returns the number of certificates appended to this signature.
	 * @return the number of certificates appended to this signature
	 */
	public synchronized int countCertificates()
	{
		return m_appendedCerts.size();
	}

	/**
	 * Deletes all certificates from this signature.
	 */
	public synchronized void clearCertificates()
	{
		Enumeration certificates = m_appendedCertXMLElements.elements();
		Element currentElemCertificate;
		Node parentNode;

		while (certificates.hasMoreElements())
		{
			currentElemCertificate = (Element) certificates.nextElement();
			parentNode = currentElemCertificate.getParentNode();
			if (parentNode != null)
			{
				parentNode.removeChild(currentElemCertificate);
			}
		}
		m_appendedCertXMLElements.removeAllElements();
		m_appendedCerts.removeAllElements();
	}

	/**
	 * Removes a certificate from this signature.
	 * @param a_certificate an X509 certificate
	 * @return true if the certificate has been removed; false otherwise
	 */
	public synchronized boolean removeCertificate(JAPCertificate a_certificate)
	{
		int index = m_appendedCerts.indexOf(a_certificate);
		if (index >= 0)
		{
			m_appendedCerts.removeElementAt(index);
			if (! (index < m_appendedCertXMLElements.size()))
			{
				m_appendedCertXMLElements.removeElementAt(index);
				//the certificate was removed from both Vectors
				return true;
			}
		}
		//Item was not found
		return false;
	}

	/**
	 * Adds a certificate to the signature. The certificate is not added if the signature cannot
	 * be verified with it, or if the signature already contains the specified certificate.
	 * @param a_certificate JAPCertificate
	 * @return true if the certificate was added; false otherwise
	 */
	public synchronized boolean addCertificate(JAPCertificate a_certificate)
	{
		Element elemCertificate;
		Node nodeKeyInfo;
		Node nodeCertificateContainer;

		if (a_certificate == null)
		{
			return false;
		}

		// there are certificates to add; create the certificate structures if not available
		nodeKeyInfo = XMLUtil.getFirstChildByName(m_elemSignature, ELEM_KEY_INFO);
		if (nodeKeyInfo == null)
		{
			nodeKeyInfo = m_elemSignature.getOwnerDocument().createElement(ELEM_KEY_INFO);
			m_elemSignature.appendChild(nodeKeyInfo);
		}

		nodeCertificateContainer = XMLUtil.getFirstChildByName(nodeKeyInfo,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (nodeCertificateContainer == null)
		{
			nodeCertificateContainer =
				m_elemSignature.getOwnerDocument().createElement(
					JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
			nodeKeyInfo.appendChild(nodeCertificateContainer);
		}

		/* test if the signature already contains the certificate and
		 * if the certificate is suitable to verify the signature
		 */
		if (m_appendedCerts.contains(a_certificate) ||
			!checkSignature(a_certificate.getPublicKey()))
		{
			return false;
		}

		// create a new certificate element
		elemCertificate = a_certificate.toXmlElement(m_elemSignature.getOwnerDocument());

		// add the certificate to the two vectors
		m_appendedCerts.addElement(a_certificate);
		m_appendedCertXMLElements.addElement(elemCertificate);

		// add the certificate to the signature element
		nodeCertificateContainer.appendChild(elemCertificate);

		return true;
	}
		
	/**
	 * Creates a new XML element from this signature. The element is not connected with this
	 * XMLSignature object and should be used with care (or better: it should never be used,
	 * as it is not necessary...)
	 * @param a_doc an XML document
	 * @return the signature as XML element
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemSignature = toXmlElementInternal(a_doc);

		if (m_elemSignature == elemSignature)
		{
			// create a new signature element
			elemSignature = (Element) elemSignature.cloneNode(true);
		}

		return elemSignature;
	}

	/**
	 * Transforms this XMLSignature to an XML element. If the given XML document
	 * already is the owner document of the signature element kept by this XMLSignature,
	 * this signature element is returned. Otherwise, a new element is created.
	 * @param a_doc an XML document
	 * @return the signature as XML element
	 */
	private Element toXmlElementInternal(Document a_doc)
	{
		if (m_elemSignature.getOwnerDocument() == a_doc)
		{
			return m_elemSignature;
		}

		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_elemSignature, true);
		}
		catch (Exception a_e)
		{
			return null;
		}
	}
	
	/**
	 * Returns all certificates that are appended to the given signature element.
	 * @param a_xmlSignature an XML signature Element
	 * @return all certificates that are appended to the given signature node
	 * @todo deprecated method from XMLSignature moved here...
	 */
	/*private static Hashtable findCertificates(Element a_xmlSignature)
	{
		Hashtable certificates = new Hashtable();
		JAPCertificate currentCertificate;
		Element elemContainer;
		Node nodeCertificate;

		elemContainer = (Element) XMLUtil.getFirstChildByName(a_xmlSignature, ELEM_KEY_INFO);
		if (elemContainer == null)
		{
			return certificates;
		}

		elemContainer = (Element) XMLUtil.getFirstChildByName(elemContainer,
			JAPCertificate.XML_ELEMENT_CONTAINER_NAME);
		if (elemContainer == null)
		{
			return certificates;
		}

		nodeCertificate = XMLUtil.getFirstChildByName(elemContainer, JAPCertificate.XML_ELEMENT_NAME);
		while (nodeCertificate != null)
		{
			try
			{
				currentCertificate = JAPCertificate.getInstance( (Element) nodeCertificate);
				if (currentCertificate != null)
				{
					//certificates.put(currentCertificate, nodeCertificate);
					certificates.put(currentCertificate.getCertIdentifier(), currentCertificate);
				}
			}
			catch (ClassCastException a_e)
			{
				// the node not an XML element; should not happen...
			}

			nodeCertificate = nodeCertificate.getNextSibling();
		}

		return certificates;
	}*/
}
