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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import anon.util.Base64;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class stores and creates signatures of XML nodes. The signing and verification processes
 * and the underlying XML signature structure are completely transparent to the using code.
 * Therefore, the XML_ELEMENT_NAME is not public. Just sign and verify what you want, you do not
 * need to know how it works! It is not allowed to change the structure of an element`s signature
 * node for other code than methods of this class. Otherwise, some methods could give false
 * results.
 * XMLSignature objects can only be created by signing or verifying XML nodes, or by getting an
 * unverified signature from an XML node.
 * @author Rolf Wendolsky, revised for MultiSign by Robert Hirschberger
 * @see http://www.w3.org/TR/xmldsig-core/
 */
public final class XMLSignature
{
	private static final String XML_ELEMENT_NAME = "Signature";
	
	/** The Vector of XMLSignatureElements kept by this object */
	private Vector m_signatureElements;
	/** The MultiCertPath assoicated with this signature */
	private MultiCertPath m_multiCertPath;
	/** The XORed SKIs of all Certs that verified a signature */
	private String m_xoredID;
	
	
	/**
	 * Creates a new and empty signature.
	 */
	private XMLSignature()
	{	
		m_signatureElements = new Vector();
	}
	
	/**
	 * Returns how many signatures the document has.
	 * @return the number of signatures
	 */
	public int countSignatures()
	{
		return m_signatureElements.size();
	}
	
	/**
	 * Return a Vector of the <Signature>-Elements contained in this object.
	 * To be called only by XMLSignatureElement when verifying
	 * their signature
	 * @return all Signature-Elements of this XMLSignature
	 */
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
	
	/**
	 * Create an array of the CertPaths from all the XMLSignatureElements.
	 * Note that only verified XMLSignatureElements have CertPaths. But this
	 * Method is only called if all Signatures have a verifier.
	 * @return an array of the associated CertPaths
	 * @see anon.crypto.XMLSignature.verify()
	 * @see anon.crypto.XMLSignatureElement.verify();
	 */
	private CertPath[] getCertPaths()
	{
		CertPath[] paths = new CertPath[m_signatureElements.size()];
		
		for(int i=0; i < m_signatureElements.size(); i++)
		{
			paths[i] = ((XMLSignatureElement) m_signatureElements.elementAt(i)).getCertPath();
		}	
		return paths;
	}
	
	/**
	 * This method is used by the checkId()-methods of the database classes,
	 * that compare the id of a given entry with the SubjectKeyIdentifier of
	 * the assoicated cert(s). If there is only one cert its ski is returned, 
	 * else the XOR of all included SKIs is returned.
	 * @see anon.infoservice.AbstractCertifiedDatabaseEntry.checkId()
	 * @see anon.infoservice.AbstractDistributableCertifiedDatabaseEntry.checkId()
	 * @return the xor of all end-entity-certs' SKIs
	 * @todo if a signatureElement is not verified no cert path is set and so the 
	 * checkId() -Method will fail and the message will be discarded because of one
	 * false signature.
	 */
	public String getXORofSKIs()
	{
		return m_xoredID;
	}
	
	/**
	 * Calculates the XOR of the SKIs once and stores it.
	 * @return
	 */
	private void calculateXORofSKIs()
	{
		Vector certificates = new Vector();
		Enumeration signatureElements = m_signatureElements.elements();
		while(signatureElements.hasMoreElements())
		{
			certificates.addElement(((XMLSignatureElement) signatureElements.nextElement()).getCertPath().getFirstCertificate());
		}
		m_xoredID = JAPCertificate.calculateXORofSKIs(certificates);
	}
	
	/**
	 * The Signature is verified if the MultiCertPath is verified.
	 * @return <code>true</code> if the MultiCertPath is verified.
	 */
	public boolean isVerified()
	{
		return m_multiCertPath.isVerified();
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
		return signInternal(a_node, Util.toVector(a_certificate));
	}
	
	
	public synchronized boolean addCertificate(JAPCertificate a_certificate)
	{
		Enumeration signatures;
		XMLSignatureElement current;
		
		if(a_certificate != null)
		{
			signatures = m_signatureElements.elements();
			while(signatures.hasMoreElements())
			{
				current = (XMLSignatureElement) signatures.nextElement();
				if(current.addCertificate(a_certificate))
				{
					return true;
				}	
			}
		}
		return false;
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
	
	/**
	 * Signs an XML node with multiple keys and creates a new XMLSignature from the signature. 
	 * The signature is added to the node, and any previous signature is removed. No certificate 
	 * is appended by default; if certificates need to be appended, they must be appended after signing. 
	 * If an error occurs while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKey a private key to sign the signature
	 * @return a new XMLSignature or null if no signature could be created
	 * @exception XMLParseException if the node could not be signed because it could not be
	 *            properly transformed into bytes
	 */
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
	 * Signs an XML node with all supplied private keys and creates a new XMLSignature from the signature. 
	 * The signatures are added to the node, and any previous signatures are removed. If an error 
	 * occurs while signing, the old signature (if present) is not removed from the node.
	 * @param a_node an XML node
	 * @param a_privateKeys the private keys or private certs to sign the signature
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
		IMyPrivateKey signKey;
		PKCS12 signCert = null;

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

		/* if there are any Signature nodes, remove them --> we create new ones */
		oldSignatureNodes = removeSignatureFromInternal(elementToSign);

		/* calculate a message digest for the node; this digest is signed later on */
		canonicalBuff = toCanonical(elementToSign);
		
		SHA1Digest digest =  new SHA1Digest();
		digest.update(canonicalBuff, 0, canonicalBuff.length);
		digestValue = new byte[digest.getDigestSize()];
		digest.doFinal(digestValue, 0);
		
		// create a signature with each provided private Key
		Enumeration keys = a_privateKeys.elements();
		try
		{
			while(keys.hasMoreElements())
			{
				Object obj = keys.nextElement();
				if(obj instanceof IMyPrivateKey)
				{
					signCert = null;
					signKey = (IMyPrivateKey) obj;
				}
				else
				{
					signCert = (PKCS12) obj;
					signKey = signCert.getPrivateKey();
				}
							 
				XMLSignatureElement sigElement = new XMLSignatureElement(xmlSignature, elementToSign, signKey, digestValue);
				if(signCert != null)
				{
					sigElement.addCertificate(signCert.getX509Certificate());
				}
				xmlSignature.m_signatureElements.addElement(sigElement);
			}
		}
		catch (Exception a_e) // if an error occured changes are undone
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, "Could not sign XML document!", a_e);
			
			if(xmlSignature.countSignatures() != 0)
			{
				removeSignatureFromInternal(elementToSign);
			}
			
			if(oldSignatureNodes != null)
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
	 * Creates a new XMLSignature from the node and creates a new MultiCertPath object.
	 * To get the verification-result call isVerified() on the retuned XMLSignature.
	 * @param a_node Node A signed XML node.
	 * @param a_documentType The document-Type of the node.
	 * @param a_directCertificates A Vector of CertPaths to verify the signature, if there are no
	 *                             appended certificates
	 * @return XMLSignature of the node, if there is one.
	 *         The signature is also returned if the verification was NOT successfull.
	 *         to get the result of the verification call isVerified() on the returned
	 *         XMLSignature object
	 * @throws XMLParseException if a signature element exists, but the element
	 *                           has an invalid structure
	 * @throws SignatureException if we found no verifier for one Signature, because the right cert was not
	 * 							  appended or cached or the Signature is wrong. In either way we do not know 
	 * 							  which cert to take for calculating the the XORed ID.
	 */
	public static XMLSignature getVerified(Node a_node, int a_documentType, Vector a_directCertificatePaths) 
		throws XMLParseException, SignatureException
	{
		XMLSignature signature;
		
		// find the signature (this call could throw an XMLParseException)
		signature = findXMLSignature(a_node);
		if (signature == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Could not find the <Signature> node!");
			return null;
		}
	
		Enumeration signatures = signature.m_signatureElements.elements();
		XMLSignatureElement currentSignature;
		
		//verify all signatures
		while (signatures.hasMoreElements())
		{
			currentSignature = (XMLSignatureElement)signatures.nextElement();
			
			if (!currentSignature.verify(a_node, a_documentType, a_directCertificatePaths))
			{
				throw new SignatureException("No verifier for a Signature found!");
			}
		}
		
		//build a multiCertPath from the verified Signatures.
		try
		{
			signature.m_multiCertPath = new MultiCertPath(signature.getCertPaths(), a_documentType);
		}
		catch (IllegalArgumentException iae) 
		{
			LogHolder.log(LogLevel.INFO, LogType.CRYPTO, iae);
			return null;
		}
		signature.calculateXORofSKIs();
		
		return signature;
	}
	
	/**
	 * Only verifies the signatures of an XML node with the given keys. 
	 * If one of the signatures was verified successfully <code>true</code> is returned.
	 * @param a_node an XML node
	 * @param a_publicKey a public key to verify the signature
	 * @return true if one of the signatures was ok, false otherwise
	 */
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
	 * Only verifies the signatures of an XML node with the given key. 
	 * If one of the signatures was verified successfully <code>true</code> is returned.
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
	 * Verifies the signature of an XML node and creates a new XMLSignature 
	 * from a valid signature.
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
	 * Gets the signature from a node if present. The signature is not verified and no 
	 * MultiCertPath is set, so getCertPath() will return null.
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
	 * Finds the signature elements of the given node if present. A signature element is only found
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
	
	public static byte[] toCanonical(Node a_inputNode, Vector a_excludedNodes) throws XMLParseException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (makeCanonical(a_inputNode, out, false, a_excludedNodes, false, "UTF-8") == -1)
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
		return makeCanonical(node, o, bSiblings, excludeNode, false);
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
	private static int makeCanonical
		(Node node, OutputStream o, boolean bSiblings, Node excludeNode, boolean a_bKeepSpaces)
	{
		return makeCanonical(node, o, bSiblings, Util.toVector(excludeNode), a_bKeepSpaces, "UTF-8");
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
	private static int makeCanonical
		(Node node, OutputStream o, boolean bSiblings, Vector excludedNodes, boolean a_bKeepSpaces, String charsetName)
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
			//for MultiSign threre can be more than one excluded node so we use a vector
			if (excludedNodes != null && excludedNodes.contains(node))
			{
				return 0;
			}
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				Element elem = (Element) node;
				o.write('<');
				if(charsetName != null)
				{
					o.write(elem.getNodeName().getBytes(charsetName));
				}
				else
				{
					o.write(elem.getNodeName().getBytes());
				}
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
						if(charsetName != null)
						{
							o.write(nodeNames[i].getBytes(charsetName));
						}
						else
						{
							o.write(nodeNames[i].getBytes());
						}
						o.write('=');
						o.write('\"');
						if(charsetName != null)
						{
							o.write(nodeValues[i].getBytes(charsetName));
						}
						else
						{
							o.write(nodeValues[i].getBytes());
						}
						o.write('\"');
					}
				}
				o.write('>');
				if (elem.hasChildNodes())
				{
					if (makeCanonical(elem.getFirstChild(), o, true, excludedNodes, a_bKeepSpaces, charsetName) == -1)
					{
						return -1;
					}
				}
				o.write('<');
				o.write('/');
				if(charsetName != null)
				{
					o.write(elem.getNodeName().getBytes(charsetName));
				}
				else
				{
					o.write(elem.getNodeName().getBytes());
				}
				o.write('>');
				if (bSiblings && 
					makeCanonical(elem.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces, charsetName) == -1)
				{
					return -1;
				}
			}
			else if (node.getNodeType() == Node.TEXT_NODE)
			{
				String textNode = node.getNodeValue();
				if (!a_bKeepSpaces)
				{
					textNode = textNode.trim();
				}
				if(charsetName != null)
				{
					o.write(textNode.getBytes(charsetName));
				}
				else
				{
					o.write(textNode.getBytes());
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces, charsetName) == -1)
				{
					return -1;
				}
				return 0;
			}
			else if (node.getNodeType() == Node.COMMENT_NODE)
			{
				if (a_bKeepSpaces)
				{
					if(charsetName != null)
					{
						o.write("<!--".getBytes(charsetName));
						o.write(node.getNodeValue().getBytes(charsetName));
						o.write("-->\n".getBytes(charsetName));
					}
					else
					{
						o.write("<!--".getBytes());
						o.write(node.getNodeValue().getBytes());
						o.write("-->\n".getBytes());
					}
				}
				if (makeCanonical(node.getNextSibling(), o, true, excludedNodes, a_bKeepSpaces, charsetName) == -1)
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
	
	/**
	 * Returns all <Signature>-Elements of this XMLSignature
	 * @param a_doc
	 * @return the <Signature>-Elements of this XMLSignature
	 */
	public Element[] getXMLElements(Document a_doc)
	{
		Element[] elements = new Element[m_signatureElements.size()];
		
		for(int i=0; i< m_signatureElements.size(); i++)
		{
			elements[i] = ((XMLSignatureElement) m_signatureElements.elementAt(i)).toXmlElement(a_doc);
		}
		
		return elements;
	}
}
