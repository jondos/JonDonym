/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * Manages the verification of all signatures.
 */
public class SignatureVerifier implements IXMLEncodable
{
		public static final int DOCUMENT_CLASS_NONE = 0;
	
        /**
         * This is the class for all documents coming from mixes (mixinfo, cascadeinfo, status).
         */
        public static final int DOCUMENT_CLASS_MIX = 1;

        /**
         * This is the class for all documents coming from infoservices (infoserviceinfo).
         */
        public static final int DOCUMENT_CLASS_INFOSERVICE = 2;

        /**
         * This is the class for all documents with JAP update specific stuff (WebStart files,
         * minimal JAP version).
         */
        public static final int DOCUMENT_CLASS_UPDATE = 3;

		public static final int DOCUMENT_CLASS_PAYMENT = 4;

        /**
         * Stores the name of the root node of the XML settings for this class.
         */
        public static final String XML_ELEMENT_NAME = "SignatureVerification";

		private static final String XML_ATTR_CHECK = "check";
		private static final String XML_DOCUMENT_CLASS = "DocumentClass";
		private static final String XML_ATTR_CLASS = "class";

        /**
         * Stores the instance of SignatureVerifier (Singleton).
         */
        private static SignatureVerifier ms_svInstance;

		/**
		 * Stores whether signature checks for a document type are done or not.
		 * It holde the document type as Integer key and a Boolean.
		 */
		private Hashtable m_hashSignatureChecks;

        /**
         * Stores all trusted certificates.
         */
        private CertificateStore m_trustedCertificates;

        /**
         * Stores whether signature checking is enabled or disabled. If this value is false, every
         * document is accept without checking the signature.
         */
        private boolean m_checkSignatures;

        /**
         * Creates a new instance of SignatureVerifier.
         */
        private SignatureVerifier()
        {
                m_trustedCertificates = new CertificateStore();
				m_hashSignatureChecks = new Hashtable();
                m_checkSignatures = true;
        }

        /**
         * Returns the instance of SignatureVerifier (Singleton). If there is no instance, there is a
         * new one created.
         *
         * @return The SignatureVerifier instance.
         */
        public static SignatureVerifier getInstance()
        {
			synchronized (SignatureVerifier.class)
			{
				if (ms_svInstance == null)
				{
					ms_svInstance = new SignatureVerifier();
				}
			}
			return ms_svInstance;
		}
        /**
         * Returns the name of the XML node used to store all settings of the SignatureVerifier
         * instance. This name can be used to find the XML node within a document when the settings
         * shall be loaded.
         *
         * @return The name of the XML node created when storing the settings.
         */
        public static String getXmlSettingsRootNodeName()
        {
                return XML_ELEMENT_NAME;
        }

        /**
         * Enables or disables the check of signatures. If signature checking is disabled, the verify
         * methods will return true in every case without checking anything. If this value is enabled,
         * every signature is verified against the internal certificate store.
         *
         * @param a_checkSignaturesEnabled True, if signature checking shall be enabled, false if it
         *                                 shall be disabled.
         */
        public void setCheckSignatures(boolean a_checkSignaturesEnabled)
        {
			m_checkSignatures = a_checkSignaturesEnabled;
        }

		public void setCheckSignatures(int a_documentClass, boolean a_bCheckignatures)
		{
			m_hashSignatureChecks.put(new Integer(a_documentClass), new Boolean(a_bCheckignatures));
		}

		public boolean isCheckSignatures(int a_documentClass)
		{
			Boolean bCheckSignatures = (Boolean)m_hashSignatureChecks.get(new Integer(a_documentClass));
			if (bCheckSignatures == null)
			{
				return true;
			}
			return bCheckSignatures.booleanValue();
		}

        /**
         * Returns whether signature verification is enabled or not. If signature checking is disabled,
         * the veriy methods will return true in every case without checking anything. If this value is
         * enabled, every signature is verified against the internal certificate store.
         *
         * @return True, if every signature is really verified against the internal certificate store or
         *         false if the verify methods are successful without performing any verification.
         */
        public boolean isCheckSignatures()
        {
			return m_checkSignatures;
        }

        /**
         * Returns the certificate store used for the verification of all signatures.
         *
         * @return The certificate store used for the signature verification.
         */
        public CertificateStore getVerificationCertificateStore()
        {
                return m_trustedCertificates;
        }

		/**
		 * Verifies the signature of an XML document against the store of trusted certificates.
		 * This methode returns true, if the signature of the document is valid, the signing certificate
		 * can be derived from one of the trusted certificates (or is one of them) and if all of the
		 * needed certificates in the path have the permission to sign documents of this class. This
		 * method also returns always true if signature checking is disabled.
		 *
		 * @param a_rootNode The root node of the document. The Signature node must be one of the
		 *                   children of the root node.
		 * @param a_documentClass The class of the document. See the constants in this class.
		 *
		 * @return True, if the signature (and appended certificate) could be verified against the
		 *         trusted certificates or false if not.
		 *
		 * @todo The ID within the document should be compared to the ID stored in the certificate.
		 * @todo the return value should be the certificate that successfully verified the signature
		 */
		public boolean verifyXml(Document a_rootNode, int a_documentClass)
		{
			if (!m_checkSignatures || !isCheckSignatures(a_documentClass))
			{
				/* accept every document without testing the signature */
				return true;
			}

			if (a_rootNode == null)
			{
				return false;
			}
			return verifyXml(a_rootNode.getDocumentElement(), a_documentClass);
		}


        /**
         * Verifies the signature of an XML document against the store of trusted certificates.
         * This methode returns true, if the signature of the document is valid, the signing certificate
         * can be derived from one of the trusted certificates (or is one of them) and if all of the
         * needed certificates in the path have the permission to sign documents of this class. This
         * method also returns always true if signature checking is disabled.
         *
         * @param a_rootNode The root node of the document. The Signature node must be one of the
         *                   children of the root node.
         * @param a_documentClass The class of the document. See the constants in this class.
         *
         * @return True, if the signature (and appended certificate) could be verified against the
         *         trusted certificates or false if not.
         *
         * @todo The ID within the document should be compared to the ID stored in the certificate.
		 * @todo the return value should be the certificate that successfully verified the signature
         */
        public boolean verifyXml(Element a_rootNode, int a_documentClass)
        {
			if (!m_checkSignatures || !isCheckSignatures(a_documentClass))
			{
			/* accept every document without testing the signature */
				return true;
			}
			else
			{
				if (a_rootNode == null)
				{
					return false;
				}
				XMLSignature signature = this.getVerifiedXml(a_rootNode, a_documentClass);
				if(signature != null)
				{
					return signature.isVerified();
				}
				return false;
			}
        }

        /*
		private class SignatureHolder
		{
			XMLSignature m_signature;
			public void setSignature(XMLSignature a_signature)
			{
				m_signature = a_signature;
			}
			public XMLSignature getSignature()
			{
				return m_signature;
			}
		}*/

		public XMLSignature getVerifiedXml(final Element a_rootNode, final int a_documentClass)
		{
			/*
			final SignatureHolder sigholder = new SignatureHolder();
			Runnable run = new Runnable()
			{
				public void run()
				{
					sigholder.setSignature(getVerifiedXml_internal(a_rootNode, a_documentClass));
				}
			};
			Thread doit = new Thread(run);
			doit.setPriority(Thread.MIN_PRIORITY + 1);
			doit.start();
			try
			{
				doit.join();
			}
			catch (InterruptedException ex)
			{
			}

			return sigholder.getSignature(); */			
			return getVerifiedXml_internal(a_rootNode, a_documentClass);
		}

		/**
		* Verifies the signature of an XML document against the store of trusted certificates.
		*
		* @param a_rootNode The root node of the document. The Signature node must be one of the
		*                   children of the root node.
		* @param a_documentClass The class of the document. See the constants in this class.
		*
		* @return the XMLSignature that should be verified. It is also returned if the verification
		*         was NOT successfull. Call isVerified() on the returned XMLSignature Object to get
		*         the result of the verification.
		*
		* @todo The ID within the document should be compared to the ID stored in the certificate.
		* @todo the return value should be the certificate that successfully verified the signature
		*/
	    public XMLSignature getVerifiedXml_internal(Element a_rootNode, int a_documentClass)
		{
			XMLSignature signature = null;
			synchronized (m_trustedCertificates)
			{
				/* get the direct useable certificates depending on the document type */
				Vector additionalCertificateInfoStructures = new Vector();
				switch (a_documentClass)
				{
					case DOCUMENT_CLASS_MIX:
					{
						additionalCertificateInfoStructures = m_trustedCertificates.
							getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_MIX);
						break;
					}
					case DOCUMENT_CLASS_INFOSERVICE:
					{
						additionalCertificateInfoStructures = m_trustedCertificates.
							getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE);
						break;
					}
					case DOCUMENT_CLASS_UPDATE:
					{
						additionalCertificateInfoStructures = m_trustedCertificates.
							getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_UPDATE);
						break;
					}
					case DOCUMENT_CLASS_PAYMENT:
					{
						additionalCertificateInfoStructures = m_trustedCertificates.
							getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_PAYMENT);
						break;
					}
				}
				Vector additionalCertPaths = new Vector();
				Enumeration additionalCertificatesEnumerator = additionalCertificateInfoStructures.elements();
				CertificateInfoStructure certStructure;
				while (additionalCertificatesEnumerator.hasMoreElements())
				{
					certStructure = (CertificateInfoStructure) additionalCertificatesEnumerator.nextElement();
					if (certStructure.isAvailable())
					{
						additionalCertPaths.addElement(certStructure.getCertPath());
					}
				}

				/* now we have everything -> verify the signature */
				try
				{
					signature = XMLSignature.getVerified(a_rootNode, a_documentClass, additionalCertPaths);
				}
				catch (Exception e)
				{
					/* this should only happen, if there is no signature child node */
				}
			}
			return signature;
		}


        /**
         * Returns all settings (including the verification certificate store) as an XML node.
         *
         * @param a_doc The parent document for the created XML node.
         *
         * @return The settings of this instance of SignatureVerifier as an XML node.
         */
        public Element toXmlElement(Document a_doc)
        {
			Element signatureVerificationNode = a_doc.createElement(XML_ELEMENT_NAME);
			synchronized (m_trustedCertificates)
			{
				Element checkSignaturesNode = a_doc.createElement("CheckSignatures");
				XMLUtil.setAttribute(checkSignaturesNode, XML_ATTR_CHECK, m_checkSignatures);
				synchronized (m_hashSignatureChecks)
				{
					Enumeration enumChecks = m_hashSignatureChecks.keys();
					Integer documentClass;
					boolean check;
					Element elemTemp;
					while (enumChecks.hasMoreElements())
					{
						documentClass = ((Integer)enumChecks.nextElement());
						check = ((Boolean)m_hashSignatureChecks.get(documentClass)).booleanValue();
						elemTemp = a_doc.createElement(XML_DOCUMENT_CLASS);
						XMLUtil.setAttribute(elemTemp, XML_ATTR_CLASS, documentClass.intValue());
						XMLUtil.setAttribute(elemTemp, XML_ATTR_CHECK, check);
						checkSignaturesNode.appendChild(elemTemp);
					}
				}
				Element trustedCertificatesNode = m_trustedCertificates.toXmlElement(a_doc);
				signatureVerificationNode.appendChild(checkSignaturesNode);
				signatureVerificationNode.appendChild(trustedCertificatesNode);
			}
			return signatureVerificationNode;
        }

		public void loadSettingsFromXml(Element a_signatureVerificationNode) throws Exception
		{
			loadSettingsFromXml(a_signatureVerificationNode, null);
		}

        /**
         * Restores the settings of this instance of SignatureVerifier with the settings stored in the
         * specified XML node.
         *
         * @param a_signatureVerificationNode The XML node for loading the settings from. The name of
         *                                    the needed XML node can be obtained by calling
         *                                    getXmlSettingsRootNodeName().
         */
        public void loadSettingsFromXml(Element a_signatureVerificationNode,
										Hashtable a_ignoredDocClasses) throws Exception
        {
			synchronized (m_trustedCertificates)
			{
				/* parse the whole SignatureVerification XML structure */
				Element checkSignaturesNode = (Element) (XMLUtil.getFirstChildByName(a_signatureVerificationNode,
					"CheckSignatures"));
				if (checkSignaturesNode == null)
				{
					throw (new Exception("No CheckSignatures node found."));
				}
				/* CheckSignatures node found -> get the value */
				m_checkSignatures = XMLUtil.parseAttribute(checkSignaturesNode, XML_ATTR_CHECK, true);
				NodeList listCheckSignatures = checkSignaturesNode.getElementsByTagName(XML_DOCUMENT_CLASS);
				int documentClass;
				for (int i = 0; i < listCheckSignatures.getLength(); i++)
				{
					documentClass = XMLUtil.parseAttribute(listCheckSignatures.item(i), XML_ATTR_CLASS, -1);
					if (documentClass >= 0 && (a_ignoredDocClasses == null ||
											   !a_ignoredDocClasses.containsKey(new Integer(documentClass))))
					{
						m_hashSignatureChecks.put(new Integer(documentClass),
												  new Boolean(XMLUtil.parseAttribute(
							listCheckSignatures.item(i), XML_ATTR_CHECK, true)));
					}
				}

				Element trustedCertificatesNode = (Element) (XMLUtil.getFirstChildByName(
					a_signatureVerificationNode, CertificateStore.getXmlSettingsRootNodeName()));
				if (trustedCertificatesNode == null)
				{
					throw (new Exception("No TrustedCertificates node found."));
				}
				/* TrustedCertificates node found -> load the certificates  */
				m_trustedCertificates.loadSettingsFromXml(trustedCertificatesNode);
			}
        }

}
