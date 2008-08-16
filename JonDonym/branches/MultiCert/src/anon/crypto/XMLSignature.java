/*
 Copyright (c) 2000 - 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.util.encoders.Hex;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.Util;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * This class stores and creates signatures of XML nodes. The signing and verification processes
 * and the underlying XML signature structure are completely transparent to the using code.
 * Therefore, the XML_ELEMENT_NAME is not public. Just sign and verify what you want, you do not
 * need to know how it works! It is not allowed to change the structure of an element`s signature
 * node for other code than methods of this class. Otherwise, some methods could give false
 * results.
 * XMLSignature objects can only be created by signing or verifying XML nodes, or by getting an
 * unverified signature from an XML node.
 * @author Rolf Wendolsky
 * @see http://www.w3.org/TR/xmldsig-core/
 */
public final class XMLSignature
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

	private Vector m_signatureElements;
	private int m_verifiedSignatureCount;
	private MultiCertPath m_multiCertPath;
	
	/**
	 * Creates a new and empty signature.
	 */
	private XMLSignature()
	{	
		m_signatureElements = new Vector();
		m_verifiedSignatureCount = 0;
	}
	
	public int getNumberOfSignatures()
	{
		return m_signatureElements.size();
	}
	
	protected Vector getSignatureElements()
	{
		Vector elements = new Vector();
		Enumeration signatures = m_signatureElements.elements();
		XMLSignatureElement currentSignature;
		
		while(signatures.hasMoreElements())
		{
			currentSignature = (XMLSignatureElement) signatures.nextElement();
			elements.addElement(currentSignature.getSignatureElement());
		}
		return elements;
	}
	
	public MultiCertPath getMultiCertPath()
	{
		return m_multiCertPath;
	}
	
	private CertPath[] getCertPaths()
	{
		CertPath[] paths = new CertPath[m_signatureElements.size()];
		
		for(int i=0; i < m_signatureElements.size(); i++)
		{
			paths[i] = ((XMLSignatureElement) m_signatureElements.get(i)).getCertPath();
		}
		
		return paths;
	}
	
	public boolean isVerified()
	{
		//TODO change me?
		if(m_verifiedSignatureCount > 0)
		{
			return true;
		}
		return false;
	}

	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. Also, the public X509 certificate
	 * from the PKCS12 certificate is added to the signature (and the node, respective).
	 * If an error occurs while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_certificate a certificate to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	public static XMLSignature sign(Node a_node, PKCS12 a_certificate) throws XMLParseException
	{
		XMLSignature signature = signInternal(a_node, Util.toVector(a_certificate.getPrivateKey()));

		/*if (signature != null)
		{
			signature.addCertificate(a_certificate.getX509Certificate());
			signature.m_certPath = new CertPath(a_certificate.getX509Certificate());
		}*/

		return signature;
	}

	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. No certificate is appended by default;
	 * if certificates need to be appended, they must be appended after signing. If an error occurs
	 * while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKey a private key to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	public static XMLSignature sign(Node a_node, IMyPrivateKey a_privateKey) throws XMLParseException
	{
		return signInternal(a_node, Util.toVector(a_privateKey));
	}
	
	public static XMLSignature multiSign(Node a_node, Vector a_privateKeys) throws XMLParseException
	{
		return signInternal(a_node, a_privateKeys);
	}

	/**
	 * getHashValueOfElement: takes an XML node and returns its hash value
	 *
	 * @param nodeToHash Node
	 * @return String the SHA1 hash value of the node (might be null if an exception occured)
	 */
	public static String getHashValueOfElement(Node nodeToHash)
	{
		byte[] digestValue = null;
		try
		{
			digestValue = MessageDigest.getInstance("SHA-1").digest(toCanonical(nodeToHash));
		}
		catch (Exception ex)
		{
			  LogHolder.log(LogLevel.WARNING, LogType.PAY, "could not create hash value of node");
			  return null;
		}
		return Base64.encode(digestValue, false);
	}
	
	/**
	 * Same method as getHashValueOfElement,
	 * except the String returned is already Base64-encoded
	 *
	 * necessary to avoid discrepancies between the results of getHashValueOfElement
	 * between the BI(Java) and PIG (Ruby/Java-bridge)
	 *
	 * @param nodeToHash Node
	 * @return String
	 */
	public static String getEncodedHashValue(Element nodeToHash)
	{
		return getHashValueOfElement(nodeToHash);
	}
	
	/**
	 * Signs an XML node and creates a new XMLSignature from the signature. The signature is added
	 * to the node, and any previous signature is removed. If an error occurs while signing, the
	 * old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKey a private key to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
	private static XMLSignature signInternal(Node a_node, Vector a_privateKeys) throws
		XMLParseException
	{
		byte[] digestValue;
		byte[] canonicalBuff;
		Element elementToSign;
		XMLSignature xmlSignature;
		Vector oldSignatureNodes;

		if (a_node == null || a_privateKeys == null || a_privateKeys.size() == 0)
		{
			return null;
		}
		else if (a_node instanceof Document)
		{
			elementToSign = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			elementToSign = (Element) a_node;
		}
		else
		{
			return null;
		}

		// create an empty XMLSignature; it will be 'filled' while signing the node
		xmlSignature = new XMLSignature();

		/* if there are any Signature nodes, remove them --> we create a new one */
		oldSignatureNodes = removeSignatureFromInternal(elementToSign);

		/* calculate a message digest for the node; this digest is signed later on */
		canonicalBuff = toCanonical(elementToSign);
		
		SHA1Digest digest =  new SHA1Digest();
		digest.update(canonicalBuff, 0, canonicalBuff.length);
		digestValue = new byte[digest.getDigestSize()];
		digest.doFinal(digestValue, 0);
		
		Enumeration keys = a_privateKeys.elements();
		
		while(keys.hasMoreElements())
		{
			try
			{
				IMyPrivateKey signKey = (IMyPrivateKey) keys.nextElement();
				XMLSignatureElement sigElement = new XMLSignatureElement(xmlSignature, elementToSign, signKey, digestValue);
				xmlSignature.m_signatureElements.addElement(sigElement);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, "Could not sign XML document!", a_e);
			}
		}
		if(xmlSignature.getNumberOfSignatures() == 0)
		{
			LogHolder.log(LogLevel.ERR, LogType.CRYPTO, "Could not create a Signature for XML document!");
			if (oldSignatureNodes != null)
			{
				Enumeration oldSigs = oldSignatureNodes.elements();
				while(oldSigs.hasMoreElements())
				{
					elementToSign.appendChild((Element)oldSigs.nextElement());
				}
			}
			return null;
		}

		return xmlSignature;
	}

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature.
	 * @param a_node an XML node
	 * @param a_certificate a certificate to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	/*public static XMLSignature verify(Node a_node, JAPCertificate a_certificate) throws XMLParseException
	{
		return verify(a_node, Util.toVector(a_certificate));
	}*/

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature.
	 * @param a_node an XML node
	 * @param a_certificateList certificates to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	/*public static XMLSignature verify(Node a_node, Vector a_certificateList) throws XMLParseException
	{
		// the certificates can be used as root certificates or directly on the signature
		//return verify(a_node, a_certificateList, a_certificateList);
		Vector vecCertPaths = new Vector(a_certificateList.size());
		for (int i = 0; i < a_certificateList.size(); i++)
		{
			vecCertPaths.addElement(new CertPath( (JAPCertificate) a_certificateList.elementAt(i)));
		}

		XMLSignature signature = getVerified(a_node, a_certificateList, vecCertPaths, false);
		if (signature != null && signature.isVerified())
		{
			return signature;
		}
		else
		{
			return null;
		}
	}*/

	/**
	 * New Implementation of the verify()-method. This one can also verify a
	 * chain of certificats to verify the Signature of an XML node.
	 * While trying to verify the Signature the certification Path is builded.
	 * The certification Path is null if the signature could not be verified.
	 *
	 * @param a_node Node A signed XML node.
	 * @param a_rootCertificates Vector A Vector of trusted root certificates which is used to verify
	 *                                  the last(or only) certificate appended at the signature
	 * @param a_directCertificates A Vector of CertPaths to verify the signature, if there are no
	 *                             appended certificates
	 * @param a_bCheckValidity If this is true, the validity of the certs is checked and expired
	 *                      certs are treated as invalid.
	 * @return XMLSignature of the node, if there is one.
	 *         The signature is also returned if the verification was NOT successfull.
	 *         to get the result of the verification call isVerified() on the returned
	 *         XMLSignature object
	 * @throws XMLParseException if a signature element exists, but the element
	 *                           has an invalid structure
	 * @todo if the signature could not be verified, the certpath should contain the appended certificate
	 *       so that the user can check (in a panel f.e.) wich appended certs are invalid
	 * @todo remove the check of the first certificate when the certPath could not be verified
	 *       this is only implemented because of compatibility reasons
	 */
	public static XMLSignature getVerified(Node a_node, int a_documentType,
										   Vector a_directCertificatePaths, boolean a_bCheckValidity) throws
		XMLParseException
	{
		XMLSignature signature;
		/*Enumeration certificates;
		boolean oneCertAppended = false;
		JAPCertificate currentCertificate;
		JAPCertificate nextCertificate = null;
		*/
		// find the signature (this call could throw an XMLParseException)
		signature = findXMLSignature(a_node);
		if (signature == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Could not find the <Signature> node!");
			return null;
		}

		//start verification
		
		// NEW from here
		Enumeration signatures = signature.m_signatureElements.elements();
		XMLSignatureElement currentSignature;
		
		while(signatures.hasMoreElements())
		{
			currentSignature = (XMLSignatureElement)signatures.nextElement();
			if(currentSignature.verify(a_node, a_documentType, a_directCertificatePaths))
			{
				if(currentSignature.getCertPath().verify());
				{
					signature.m_verifiedSignatureCount++;
				}
				
				//get incomplete CertPath form SigElement and give it with appendedCerts from sig
				//root Certs to CertPathBuilder which does name-chaining to build a path or looks
				//if there is a valid path for the verifying cert
				//if there is not yet a verified path, but certpathbuilder found one, give CertPath
				//to CertPathVerifier... maybe use some inner classes in CertPath?
			}
		}
		
		signature.m_multiCertPath = new MultiCertPath(signature.getCertPaths());
		
		return signature;
			
			/*
		try
		{
			//get the included certificates
			//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Looking for appended certificates...");
			certificates = signature.getCertificates().elements();
			if ( (certificates != null) && (certificates.hasMoreElements()))
			{
				//we found at least one certificate
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Found " + signature.countCertificates() + " appended certificates!");
				currentCertificate = (JAPCertificate) certificates.nextElement();
				signature.m_certPath = new CertPath(currentCertificate);

				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Trying to build certification path...");
				while (certificates.hasMoreElements())
				{
					//take the next certificate an try to verify the previous
					nextCertificate = (JAPCertificate) certificates.nextElement();
					//check validity if checking is set
					if (!currentCertificate.verify(nextCertificate.getPublicKey())
						|| (a_bCheckValidity && ! (nextCertificate.getValidity().isValid(new Date()))))
					{
						//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
						//			  "Trying to build certification path -stopped!");
						break; //the building of the certPath stops here
					}
					//the cert that was used for verification is now verified
					signature.m_certPath.add(nextCertificate);
					currentCertificate = nextCertificate;
					if (!certificates.hasMoreElements()) //we reached the last cert in the path
					{
						LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
									  "Trying to build certification path -success!");
					}
				}
				//the certspath was traversed as far as possible or there was only one certificate
				if (nextCertificate == null)
				{
					oneCertAppended = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to build certification path -only one certificate appended!");
				}

				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify signature against first certifcate...");
				currentCertificate = signature.getCertificationPath().getFirstCertificate();
				if (currentCertificate != null)
				{
					// check validity if neccessary
					if (!verify(a_node, signature, currentCertificate.getPublicKey())
						|| (a_bCheckValidity && ! (currentCertificate.getValidity().isValid(new Date()))))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
									  "Trying to verify signature against first certifcate -failed");
						//the verification failed, the found CertPath is set
						return signature;
					}
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify signature against first certifcate -success!");
				}

				//the first (=Mix) cert could verify the signature
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify last certificate against root certificates...");
				currentCertificate = signature.m_certPath.getLatestAddedCertificate();
				if (currentCertificate != null && currentCertificate.verify(a_rootCertificates))
				{
					signature.m_bVerified = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify last certificate against root certificates -success");
				}
				else
				{
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify last certificate against root certificates -failed");
				}
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "No appended certificates found!");
			}

			if (!signature.isVerified())
			{
				CertPath certPathNew;

				/**
				 * Either no appended certificates were found, or the appended certificate path could not
				 * be verified against the root certificates.
				 * Try to verify the signature using the direct certificates
				 */
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Trying to verify signature against direct certificates...");
/*
				certPathNew = getVerifier(a_node, signature, a_directCertificatePaths, a_bCheckValidity);
				if (certPathNew != null)
				{
					signature.m_certPath = certPathNew;
					signature.m_bVerified = true;
					//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
					//			  "Trying to verify signature against direct certificates -success");
				}
				else
				{
					if (signature.m_certPath == null)
					{
						signature.m_certPath = new CertPath( (JAPCertificate)null);
					}
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify signature against direct certificates...-failed");
				}
			}

			if (!signature.isVerified() && !oneCertAppended) //the last certificate in the Path could not be verified
			{ //now try to verify the first cert against the root certs if there are more certs in the signature
				//this is for maintaining compatibility to older systems. it can be removed when the systems are adapted
				LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
							  "Trying to verify first certificate against root certificates...");
				currentCertificate = signature.m_certPath.getFirstCertificate();
				if (currentCertificate != null && currentCertificate.verify(a_rootCertificates))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify first certificate against root certificates -success");
					//signature.m_certPath.add(verifyingCertificate);
					signature.m_bVerified = true;
				}
				else //the first certificate could not be verified against the rootCerts
				{ //verification ends here, the signature is returned but could not be verified
					LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
								  "Trying to verify first certificate against root certificates -failed");
				}
			}
		} //end of verification
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, e);
		}
		if (signature.isVerified())
		{
			a_directCertificatePaths.addElement(signature.getCertificationPath());
		}*/

		//return signature;
	}
	
	public static boolean verifyFast(Node a_node, Vector a_publicKeys)
	{	
		Enumeration keys = a_publicKeys.elements();
		IMyPublicKey currentKey;
		
		while(keys.hasMoreElements())
		{
			currentKey = (IMyPublicKey) keys.nextElement();
			if(verifyFast(a_node, currentKey))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Only verifies the signatures of an XML node. If one of the
	 * signatures was verified successfully <code>true</code> is returned.
	 * @param a_node an XML node
	 * @param a_publicKey a public key to verify the signature
	 * @return true if one of the signatures was ok, false otherwise
	 */
	public static boolean verifyFast(Node a_node, IMyPublicKey a_publicKey)
	{
		try
		{
			return verify(a_node, a_publicKey)!= null;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	/**
	 * Verifies the signature of an XML node and creates a new XMLSignature from a valid
	 * signature. This method is not as fast as verify(Node, X509Certificate) as a temporary
	 * certificate has to be created from the public key. Therefore, it is not recommended.
	 * @param a_node an XML node
	 * @param a_publicKey a public key to verify the signature
	 * @return the XMLSignature of the node; null if the node could not be verified
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 */
	public static XMLSignature verify(Node a_node, IMyPublicKey a_publicKey) throws XMLParseException
	{
		XMLSignature xmlSignature = findXMLSignature(a_node);
		XMLSignatureElement signature;
		Enumeration signatures = xmlSignature.m_signatureElements.elements();
		
		while(signatures.hasMoreElements())
		{
			try
			{
				signature = (XMLSignatureElement) signatures.nextElement();
				if(signature.verifyFast(a_node, a_publicKey))
				{
					return xmlSignature;
				}
			}
			catch (Throwable t)
			{
				//just check next Signature
			}	
		}
		return null;
	}
	
	

	/**
	 * Gets the signature from a node if present. The signature is not verified.
	 * @param a_node an XML node
	 * @throws XMLParseException if the signature is present but has an invalid XML structure
	 * @return the node`s XMLSignature or null if no signature was found
	 */
	public static XMLSignature getUnverified(Node a_node) throws XMLParseException
	{
		XMLSignature signature;

		if (a_node == null)
		{
			return null;
		}

		signature = findXMLSignature(a_node);
		
		return signature;
	}

	/**
	 * Removes the signature from an XML node if a signature exists.
	 * @param a_node an XML Node
	 * @return true if the signature has been removed; false if the node did not have any signature
	 */
	public static boolean removeSignatureFrom(Node a_node)
	{
		if (removeSignatureFromInternal(a_node) == null)
		{
			return false;
		}

		return true;
	}


	/**
	 * Appends this XMLSignature to an XML node. If the node already has a signature, it is removed
	 * first. The signature is only appended to the node if the node`s message digest is equal to
	 * the signature`s stored message digest. If the new signature could not be appended, the old
	 * signature is not removed (if present).
	 * @param a_node an XML node
	 * @return true if the signature has been appended; false otherwise
	 */
	/*public boolean appendSignatureTo(Node a_node)
	{
		Document doc;
		Element element;
		Node elemOldSignature;
		Element elemNewSignature;

		if (a_node instanceof Document)
		{
			doc = (Document) a_node;
			element = doc.getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			element = (Element) a_node;
			doc = element.getOwnerDocument();
		}
		else
		{
			return false;
		}

		// check if this is a valid signature for this element!
		/*try
		{
			if (!XMLSignature.checkMessageDigest(element))
			{
				return false;
			}
		}
		catch (XMLParseException a_e)
		{
			return false;
		}*/

		// create the signature element
		/*elemNewSignature = toXmlElementInternal(doc);

		// remove any existing signatures
		while ( (elemOldSignature = XMLUtil.getFirstChildByName(element, XML_ELEMENT_NAME)) != null)
		{
			element.removeChild(elemOldSignature);
		}

		// append this signature element
		element.appendChild(elemNewSignature);

		return true;
	}*/

	

	/**
	 * Removes the signature from an XML node if a signature exists.
	 * @param a_node an XML Node
	 * @return the removed signature node or null if the node did not have any signature
	 */
	private static Vector removeSignatureFromInternal(Node a_node)
	{
		Vector nodes = new Vector();
		Element signatureNode = null;
		Node nextRemovedNode;
		Element element;

		if (a_node instanceof Document)
		{
			element = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			element = (Element) a_node;
		}
		else
		{
			return null;
		}

		// remove any existing signatures
		while ( (nextRemovedNode = XMLUtil.getFirstChildByName(element, XML_ELEMENT_NAME)) != null)
		{
			try
			{
				signatureNode = (Element) element.removeChild(nextRemovedNode);
				nodes.addElement(signatureNode);
			}
			catch (ClassCastException a_e)
			{
				// should not happen
			}
		}
		if(nodes.size() == 0)
		{
			return null;
		}
		return nodes;
	}

	

	/**
	 * Finds the signature element of the given node if present. This signature element is only found
	 * if it is a direct child of a_node. The signature is not verified.
	 * @param a_node an XML Node
	 * @return the node`s XMLSignature or null if no signature node was found
	 * @exception XMLParseException if the node has an invalid valid XML signature element structure
	 */
	private static XMLSignature findXMLSignature(Node a_node) throws XMLParseException
	{
		XMLSignature xmlSignature;
		Element elementVerified;
		Node signatureNode;

		if (a_node == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}

		if (a_node instanceof Document)
		{
			elementVerified = ( (Document) a_node).getDocumentElement();
		}
		else if (a_node instanceof Element)
		{
			elementVerified = (Element) a_node;
		}
		else
		{
			return null;

		}

		signatureNode = XMLUtil.getFirstChildByName(elementVerified, XML_ELEMENT_NAME);
		xmlSignature = new XMLSignature();
		
		while(signatureNode != null)
		{
			try
			{
				// this call could throw an XMLParseException if the structure is invalid
				XMLSignatureElement sigElement = new XMLSignatureElement(xmlSignature, (Element) signatureNode);
				xmlSignature.m_signatureElements.addElement(sigElement);
			}
			catch (ClassCastException a_e)
			{
				// should not happen
			}
			signatureNode = XMLUtil.getNextSiblingByName(signatureNode, XML_ELEMENT_NAME);
		}
		if(xmlSignature.m_signatureElements.size() == 0)
		{
			return null;
		}
		return xmlSignature;
	}
	
	public void clearCertificates()
	{
		Enumeration signatures = m_signatureElements.elements();
		XMLSignatureElement currentSignature;
		
		while(signatures.hasMoreElements())
		{
			currentSignature = (XMLSignatureElement) signatures.nextElement();
			currentSignature.clearCertificates();
		}
	}

	/**
	 * Returns all certificates that are appended to the given signature element.
	 * @param a_xmlSignature an XML signature Element
	 * @return all certificates that are appended to the given signature node
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
	
	public static byte[] toCanonical(Node a_inputNode, Vector a_excludedNodes) throws XMLParseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (makeCanonical(a_inputNode, out, false, a_excludedNodes, false) == -1)
		{
			throw new XMLParseException(a_inputNode.getNodeName(),
										"Could not make the node canonical!");
		}

		try
		{
			out.flush();
		}
		catch (IOException a_e)
		{
		}

		return out.toByteArray();

	}

	/**
	 * Is only used if no digest value is found. Then, the entire document is verified against the signature.
	 * @todo mark this as deprecated and remove
	 * @param a_inputNode Node
	 * @return byte[]
	 */
	public static byte[] toCanonicalDeprecated(Node a_inputNode)
	{
		if (a_inputNode == null || a_inputNode.getPreviousSibling() == null)
		{
			return null;
		}

		byte[] dataToVerify;
		Node parent;

		parent = a_inputNode.getParentNode();
		parent.removeChild(a_inputNode);
		dataToVerify = XMLUtil.toByteArray(parent.getOwnerDocument());
		parent.appendChild(a_inputNode);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DataOutputStream dataOut = new DataOutputStream(out);
		try
		{
			dataOut.writeShort(dataToVerify.length);
			dataOut.flush();
			out.write(dataToVerify);
			out.flush();
			return out.toByteArray();
		}
		catch (IOException a_e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Could not make xml data canonical!", a_e);
			return null;
		}
	}

	public static byte[] toCanonical(Node inputNode) throws XMLParseException
	{
		return toCanonical(inputNode, false);
	}

	/**
	 * Creates a byte array from an XML node tree.
	 * @param inputNode The node (incl. the whole tree) which is flattened to a byte array.
	 *
	 * @return the node as a byte array (incl. the whole tree).
	 * @exception XMLParseException if the node could not be properly transformed into bytes
	 */
	public static byte[] toCanonical(Node inputNode, boolean a_bKeepSpaces) throws XMLParseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (makeCanonical(inputNode, out, false, null, a_bKeepSpaces) == -1)
		{
			throw new XMLParseException(inputNode.getNodeName(), "Could not make the node canonical!");
		}

		try
		{
			out.flush();
		}
		catch (IOException a_e)
		{
		}
		return out.toByteArray();

	}
	/**
	 * same as toCanonical(Node):byte[], except returning a String
	 * only necessary for use in Ruby (since handling a Java byte array in Ruby wouldnt work)
	 *
	 * @param inputNode Node
	 * @return String
	 * @throws XMLParseException
	 */
	public static String toCanonicalString(Element input)
	{
		try
		{
			byte[] canonicalBytes = toCanonical(input);
			return new String(canonicalBytes);
		} catch (Exception e)
		{
			//nothing to be done from ruby
			return ("canonicalization error");
		}
	}

	private static int makeCanonical(Node node, OutputStream o, boolean bSiblings, Node excludeNode)
	{
		return makeCanonical(node, o, bSiblings, Util.toVector(excludeNode), false);
	}


	/**
	 * @todo find a better way to get the data of the node as a bytestream, for
	 *       compatibility reasons we use this now; it cannot be verifed that this canonicalization
	 *       method is compatible to one of the methods defined by w3c
	 * @param node Node
	 * @param o OutputStream
	 * @param bSiblings boolean
	 * @param excludeNode Node
	 * @return int
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-CanonicalizationMethod
	 * @see http://www.w3.org/TR/xml-c14n
	 */
	private static int makeCanonical(Node node, OutputStream o, boolean bSiblings, Vector excludedNodes, boolean a_bKeepSpaces)
	{
		try
		{
			if (node == null)
			{
				return 0;
			}
			if (node instanceof Document)
			{
				if (a_bKeepSpaces)
				{
					o.write(XMLUtil.createDocumentStructure());
					o.write('\n');
				}
				node = ( (Document) node).getDocumentElement();
			}

			if (excludedNodes != null && excludedNodes.contains(node))
			{
				return 0;
			}
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element elem = (Element) node;
				o.write('<');
				o.write(elem.getNodeName().getBytes());
				NamedNodeMap attr = elem.getAttributes();
				if (attr.getLength() > 0)
				{
					// sort attributes by name
					String[] nodeNames = new String[attr.getLength()];
					String[] nodeValues = new String[attr.getLength()];
					for (int i = 0; i < attr.getLength(); i++)
					{
						nodeNames[i] = attr.item(i).getNodeName();
						nodeValues[i] = attr.item(i).getNodeValue();
					}
					Util.sort(nodeNames, nodeValues);

					for (int i = 0; i < attr.getLength(); i++)
					{
						o.write(' ');
						o.write(nodeNames[i].getBytes());
						o.write('=');
						o.write('\"');
						o.write(nodeValues[i].getBytes());
						o.write('\"');
					}
				}
				o.write('>');
				if (elem.hasChildNodes())
				{
					if (makeCanonical(elem.getFirstChild(), o, true, excludedNodes, a_bKeepSpaces) == -1)
					{
						return -1;
					}
				}
				o.write('<');
				o.write('/');
				o.write(elem.getNodeName().getBytes());
				o.write('>');
				if (bSiblings && makeCanonical(elem.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces) == -1)
				{
					return -1;
				}
			}
			else if (node.getNodeType() == Node.TEXT_NODE)
			{
				if (a_bKeepSpaces)
				{
					o.write(node.getNodeValue().getBytes());
				}
				else
				{
					o.write(node.getNodeValue().trim().getBytes());
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces) == -1)
				{
					return -1;
				}
				return 0;
			}
			else if (node.getNodeType() == Node.COMMENT_NODE)
			{
				if (a_bKeepSpaces)
				{
					o.write("<!--".getBytes());
					o.write(node.getNodeValue().getBytes());
					o.write("-->\n".getBytes());
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces) == -1)
				{
					return -1;
				}
				return 0;
			}
			else
			{
				return -1;
			}
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}
	
	public Element[] getXMLElements(Document a_doc)
	{
		Element[] elements = new Element[m_signatureElements.size()];
		
		for(int i=0; i< m_signatureElements.size(); i++)
		{
			elements[i] = ((XMLSignatureElement) m_signatureElements.get(i)).toXmlElement(a_doc);
		}
		
		return elements;
	}

	/**
	 * This method is used to verify a node with a previously created XMLSignature.
	 * @param a_node an XML node
	 * @param a_signature an XMLSignature
	 * @param a_verifyingCertificatePaths a Vector of CertPaths to verify the signature
	 * @exception XMLParseException if a signature element exists, but the element
	 *                              has an invalid structure
	 * @return the certificate that verified this signature; null if it could not be verified
	 *
	 */
	/*private static CertPath getVerifier(Node a_node, XMLSignature a_signature,
										Vector a_verifyingCertificatePaths,
										boolean a_bCheckValidity) throws XMLParseException
	{
		Enumeration certificates = a_verifyingCertificatePaths.elements();
		CertPath currentCertificate = null;
		boolean bVerified = false;
		while (!bVerified && certificates.hasMoreElements())
		{
			currentCertificate = (CertPath) certificates.nextElement();
			if (currentCertificate.getEndEntity() != null)
			{
				bVerified = verify(a_node, a_signature,
								   currentCertificate.getEndEntity())
					&&
					(!a_bCheckValidity || (currentCertificate.getEndEntity().getValidity().isValid(new Date())));
			}
		}
		//if the verification was successfull return the cert that verified, else return null
		return bVerified ? currentCertificate : null;
	}
	
	
	public Vector getXMLSignatureElements()
	{
		return m_signatureElements;
	}*/
}
