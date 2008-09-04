/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.security.SignatureException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.ErrorCodes;
import anon.client.crypto.ASymCipher;
import anon.client.crypto.KeyPool;
import anon.client.crypto.SymCipher;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLEncryption;
import anon.crypto.XMLSignature;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/**
 * @author Stefan Lieske
 */
public class KeyExchangeManager {

  /**
   * Stores the lock on the certificate used by the mixcascade to sign all
   * cascade related messages, like the MixCascade or MixCascadeStatus
   * structures. The certificate will be stored within the signature
   * verification certificate store until the lock is released (done when the
   * connection to the mixcascade is closed).
   */
  private int m_mixCascadeCertificateLock;

  private Object m_internalSynchronization;

  private boolean m_protocolWithTimestamp;

  private boolean m_protocolWithReplay;

  private boolean m_paymentRequired;

  private SymCipher m_firstMixSymmetricCipher;

  private boolean m_chainProtocolWithFlowControl;

  private FixedRatioChannelsDescription m_fixedRatioChannelsDescription;

  private MixParameters[] m_mixParameters;

  private SymCipher m_multiplexerInputStreamCipher;

  private SymCipher m_multiplexerOutputStreamCipher;

  private MixCascade m_cascade;


  /**
   * @todo allow to connect if one or more mixes (user specified) cannot be verified
   * @param a_inputStream InputStream
   * @param a_outputStream OutputStream
   * @param a_cascade the cascade to connect to; this is only used to update database entries
   * @throws XMLParseException
   * @throws SignatureException
   * @throws IOException
   * @throws UnknownProtocolVersionException
   * @todo remove MixInfo entries when changes in the certificate ID of a mix are discovered
   */
  public KeyExchangeManager(InputStream a_inputStream, OutputStream a_outputStream, MixCascade a_cascade,
							ITrustModel a_trustModel)
	  throws XMLParseException, SignatureException, IOException, UnknownProtocolVersionException,
	  TrustException
  {
	  try
	  {
		  m_mixCascadeCertificateLock = -1;
		  m_internalSynchronization = new Object();
		  DataInputStream dataStreamFromMix = new DataInputStream(a_inputStream);
		  /* read the length of the following XML structure */
		  int xmlDataLength = dataStreamFromMix.readUnsignedShort();
		  /* read the initial XML structure */
		  byte[] xmlData = new byte[xmlDataLength];
		  while (xmlDataLength > 0)
		  {
			  int bytesRead = a_inputStream.read(xmlData, xmlData.length - xmlDataLength, xmlDataLength);
			  if (bytesRead == -1)
			  {
				  throw new EOFException("EOF detected while reading initial XML structure.");
			  }
			  else
			  {
				  xmlDataLength = xmlDataLength - bytesRead;
			  }
		  }

		   /* process the received XML structure */
		   Element elem = XMLUtil.toXMLDocument(xmlData).getDocumentElement();
		   m_cascade = new MixCascade(elem, Long.MAX_VALUE, a_cascade.getId());

		  TrustException excepTrust = null;
		  SignatureException execSignature = null;
		  if (a_cascade.isUserDefined())
		  {
			  m_cascade.setUserDefined(true, a_cascade);
			  Database.getInstance(MixCascade.class).remove(m_cascade);
			  Database.getInstance(MixCascade.class).update(m_cascade);
			  Database.getInstance(MixInfo.class).update(
						   new MixInfo(MixInfo.DEFAULT_NAME, m_cascade.getCertPath()));
			  a_trustModel.checkTrust(m_cascade);
		  }
		  else
		  {
			  Database.getInstance(MixInfo.class).update(
						   new MixInfo(MixInfo.DEFAULT_NAME, m_cascade.getCertPath()));
			  MixCascade cascadeInDB =
				  (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_cascade.getId());
			  if (cascadeInDB != null)
			  {
				  // check if the cascade has changed its composition or trust since the last update
				  if (!m_cascade.compareMixIDs(cascadeInDB))
				  {
					  // remove this cascade from DB as its values have changed
					  Database.getInstance(MixCascade.class).remove(cascadeInDB);
				  }
				  boolean bCascadeTrust = false;
				  boolean bCascadeInDBTrust = false;

				  try
				  {
					  a_trustModel.checkTrust(m_cascade);
					  bCascadeTrust = true;
				  }
				  catch (TrustException a_e)
				  {
					  excepTrust = a_e;
				  }
				  catch (SignatureException a_e)
				  {
					  execSignature = a_e;
				  }
				  try
				  {
					  a_trustModel.checkTrust(cascadeInDB);
					  bCascadeInDBTrust = true;
				  }
				  catch (TrustException a_e)
				  {
				  }
				  catch (SignatureException a_e)
				  {
				  }
				  if (bCascadeTrust != bCascadeInDBTrust)
				  {
					  // remove this cascade from DB as its trust has changed
					  Database.getInstance(MixCascade.class).remove(cascadeInDB);
				  }
			  }
		  }

		  /** Very important: Check if this cascade is trusted. Otherwise, an exception is thrown. */
		  //a_trustModel.checkTrust(cascade);
		  if (excepTrust != null)
		  {
			  throw excepTrust;
		  }
		  if (execSignature != null)
		  {
			  throw execSignature;
		  }

		  /*
		   * get the appended certificate of the signature and store it in the
		   * certificate store (needed for verification of the MixCascadeStatus
		   * messages)
		   */
		  if (m_cascade.getCertificate() != null)
		  {
			  m_mixCascadeCertificateLock = SignatureVerifier.getInstance().
				  getVerificationCertificateStore().addCertificateWithoutVerification(
					  m_cascade.getCertificate(),
					  JAPCertificate.CERTIFICATE_TYPE_MIX, false, false);
			  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							"Added appended certificate from the MixCascade structure to the certificate store.");
		  }
		  else
		  {
			  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								"No appended certificates in the MixCascade structure.");
		  }

		  /* get the used channel protocol version */
		  if (m_cascade.getMixProtocolVersion() == null)
		  {
			  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
				  "MixProtocolVersion (channel) node expected in received XML structure."));
		  }

		  m_protocolWithTimestamp = false;
		  m_protocolWithReplay = false;
		  m_paymentRequired = m_cascade.isPayment();
		  if (!m_cascade.isPaymentProtocolSupported())
		  {
			  throw (new UnknownProtocolVersionException(
				  "Unsupported payment protocol version ('" + m_cascade.getPaymentProtocolVersion() + "')."));
		  }
		  m_firstMixSymmetricCipher = null;
		  /*
		   * lower protocol versions not listed here are obsolete and not supported
		   * any more
		   */
		  LogHolder.log(LogLevel.DEBUG, LogType.NET,
						"Cascade is using channel-protocol version '" + m_cascade.getMixProtocolVersion() +
						"'.");
		  if (m_cascade.getMixProtocolVersion().equals("0.2"))
		  {
			  /* no modifications of the default-settings required */
		  }
		  else if (m_cascade.getMixProtocolVersion().equals("0.4"))
		  {
			  m_firstMixSymmetricCipher = new SymCipher();
		  }
		  else if (m_cascade.getMixProtocolVersion().equals("0.81"))
		  {
			  m_protocolWithTimestamp = false;
			  m_protocolWithReplay = true;
			  m_firstMixSymmetricCipher = new SymCipher();
		  }
		  else if (m_cascade.getMixProtocolVersion().equalsIgnoreCase("0.9"))
		  {
			  m_firstMixSymmetricCipher = new SymCipher();
		  }
		  else
		  {
			  throw (new UnknownProtocolVersionException(
				  "Unknown channel protocol version used ('" + m_cascade.getMixProtocolVersion() + "')."));
		  }

		 m_mixParameters = new MixParameters[m_cascade.getNumberOfMixes()];
		  for (int i = 0; i < m_cascade.getNumberOfMixes(); i++)
		  {
			  MixInfo mixinfo = m_cascade.getMixInfo(i);

			  if (mixinfo == null)
			  {
				  // should not happen
				  throw new XMLParseException("Could not get MixInfo object for Mix " + i + "!");

			  }

			  if (i > 0 && !mixinfo.isVerified())
			  {
				  throw (new SignatureException(
					  "Received XML structure has an invalid signature for Mix " +
					  Integer.toString(i) + "."));
			  }




			  MixInfo oldMixinfo = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(mixinfo.getId());
			  if (mixinfo.getCertificate() != null &&
				  (oldMixinfo == null || oldMixinfo.getCertificate() == null ||
				   !oldMixinfo.getCertificate().equals(mixinfo.getCertificate())))
			  {
				  // update the database so the the (new) certificate gets available
				  Database.getInstance(MixInfo.class).update(mixinfo);
			  }
			  Element currentMixNode = mixinfo.getXmlStructure();
			  m_mixParameters[i] = new MixParameters(mixinfo.getId(), new ASymCipher());
			  if (m_mixParameters[i].getMixCipher().setPublicKey(currentMixNode) != ErrorCodes.E_SUCCESS)
			  {
				  throw (new XMLParseException(
					  "Received XML structure contains an invalid public key for Mix " + Integer.toString(i) +
					  "."));
			  }
			  if (i == (m_cascade.getNumberOfMixes() - 1))
			  {
				  /* get the chain protocol version from the last mix */
				  NodeList chainMixProtocolVersionNodes = currentMixNode.getElementsByTagName(
					  "MixProtocolVersion");
				  if (chainMixProtocolVersionNodes.getLength() == 0)
				  {
					  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
						  "MixProtocolVersion (chain) node expected in received XML structure."));
				  }
				  /* there should be only one chain mix protocol version node */
				  Element chainMixProtocolVersionNode = (Element) (chainMixProtocolVersionNodes.item(0));
				  String chainMixProtocolVersionValue = XMLUtil.parseValue(chainMixProtocolVersionNode, (String)null);
				  if (chainMixProtocolVersionValue == null)
				  {
					  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
						  "MixProtocolVersion (chain) node has no value."));
				  }
				  chainMixProtocolVersionValue = chainMixProtocolVersionValue.trim();
				  m_chainProtocolWithFlowControl = false;
				  m_fixedRatioChannelsDescription = null;
				  /*
				   * lower protocol versions not listed here are obsolete and not
				   * supported any more
				   */
				  LogHolder.log(LogLevel.DEBUG, LogType.NET,
								"Cascade is using chain-protocol version '" + chainMixProtocolVersionValue +
								"'.");
				  if (chainMixProtocolVersionValue.equals("0.3"))
				  {
					  /* no modification of the default settings required */
				  }
				  else if (chainMixProtocolVersionValue.equals("0.4"))
				  {
					  m_chainProtocolWithFlowControl = true;
				  }
				  else if (chainMixProtocolVersionValue.equals("0.5"))
				  {
					  /* simulated 1:n channels */
					  NodeList downstreamPacketsNodes = currentMixNode.getElementsByTagName(
										 "DownstreamPackets");
					  if (downstreamPacketsNodes.getLength() == 0)
					  {
						  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
							  "DownstreamPackets node expected in received XML structure."));
					  }
					  /* there should be only one downstream packets node */
					  Element downstreamPacketsNode = (Element) (downstreamPacketsNodes.item(0));
					  int downstreamPackets = XMLUtil.parseValue(downstreamPacketsNode, -1);
					  if (downstreamPackets < 1)
					  {
						  throw (new XMLParseException("DownstreamPackets", "Node has an invalid value."));
					  }
					  NodeList channelTimeoutNodes = currentMixNode.getElementsByTagName("ChannelTimeout");
					  if (channelTimeoutNodes.getLength() == 0)
					  {
						  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
							  "ChannelTimeout node expected in received XML structure."));
					  }
					  /* there should be only one channel timeout node */
					  Element channelTimeoutNode = (Element) (channelTimeoutNodes.item(0));
					  long channelTimeout = XMLUtil.parseValue(channelTimeoutNode, -1);
					  if (channelTimeout < 1)
					  {
						  throw (new XMLParseException("ChannelTimeout node has an invalid value."));
					  }
					  channelTimeout = 1000L * channelTimeout;
					  NodeList chainTimeoutNodes = currentMixNode.getElementsByTagName("ChainTimeout");
					  if (chainTimeoutNodes.getLength() == 0)
					  {
						  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG,
							  "ChainTimeout node expected in received XML structure."));
					  }
					  /* there should be only one chain timeout node */
					  Element chainTimeoutNode = (Element) (chainTimeoutNodes.item(0));
					  long chainTimeout = XMLUtil.parseValue(chainTimeoutNode, -1);
					  if (chainTimeout < 1)
					  {
						  throw (new XMLParseException("ChainTimeout", "Node has an invalid value."));
					  }
					  chainTimeout = 1000L * chainTimeout;
					  m_fixedRatioChannelsDescription = new FixedRatioChannelsDescription(downstreamPackets,
						  channelTimeout, chainTimeout);
				  }
				  else
				  {
					  throw (new UnknownProtocolVersionException(
						  "Unknown chain protocol version used ('" + chainMixProtocolVersionValue + "')."));
				  }
			  }
		  }
		  /* sending symmetric keys for multiplexer stream encryption */
		  m_multiplexerInputStreamCipher = new SymCipher();
		  m_multiplexerOutputStreamCipher = new SymCipher();
		  /* ensure that keypool is started */
		  KeyPool.start();
		  LogHolder.log(LogLevel.DEBUG, LogType.NET, "Starting key exchange...");
		  if (m_firstMixSymmetricCipher == null)
		  {
			  /*
			   * create a new MixPacket with the keys (channel id and flags don't matter)
			   */
			  MixPacket keyPacket = new MixPacket(0);
			  byte[] keyPacketIdentifier = "KEYPACKET".getBytes();
			  System.arraycopy(keyPacketIdentifier, 0, keyPacket.getPayloadData(), 0,
							   keyPacketIdentifier.length);
			  byte[] keyBuffer = new byte[32];
			  KeyPool.getKey(keyBuffer, 0);
			  KeyPool.getKey(keyBuffer, 16);
			  System.arraycopy(keyBuffer, 0, keyPacket.getPayloadData(), keyPacketIdentifier.length,
							   keyBuffer.length);
			  m_mixParameters[0].getMixCipher().encrypt(keyPacket.getPayloadData(), 0,
				  keyPacket.getPayloadData(), 0);
			  a_outputStream.write(keyPacket.getRawPacket());
			  m_multiplexerInputStreamCipher.setEncryptionKeyAES(keyBuffer, 0, 16);
			  m_multiplexerOutputStreamCipher.setEncryptionKeyAES(keyBuffer, 16, 16);
		  }
		  else
		  {
			  /*
			   * the first mix uses a symmetric cipher for mixing -> send also keys
			   * for that cipher
			   */
			  Document keyDoc = XMLUtil.createDocument();
			  if (keyDoc == null)
			  {
				  throw (new XMLParseException("Cannot create XML document for key exchange."));
			  }
			  Element japKeyExchangeNode = keyDoc.createElement("JAPKeyExchange");
			  japKeyExchangeNode.setAttribute("version", "0.1");
			  Element linkEncryptionNode = keyDoc.createElement("LinkEncryption");
			  byte[] multiplexerKeys = new byte[64];
			  KeyPool.getKey(multiplexerKeys, 0);
			  KeyPool.getKey(multiplexerKeys, 16);
			  KeyPool.getKey(multiplexerKeys, 32);
			  KeyPool.getKey(multiplexerKeys, 48);
			  m_multiplexerOutputStreamCipher.setEncryptionKeyAES(multiplexerKeys, 0, 32);
			  m_multiplexerInputStreamCipher.setEncryptionKeyAES(multiplexerKeys, 32, 32);
			  XMLUtil.setValue(linkEncryptionNode, Base64.encode(multiplexerKeys, true));
			  japKeyExchangeNode.appendChild(linkEncryptionNode);
			  Element mixEncryptionNode = keyDoc.createElement("MixEncryption");
			  byte[] mixKeys = new byte[32];
			  KeyPool.getKey(mixKeys, 0);
			  KeyPool.getKey(mixKeys, 16);
			  m_firstMixSymmetricCipher.setEncryptionKeyAES(mixKeys, 0, 32);
			  XMLUtil.setValue(mixEncryptionNode, Base64.encode(mixKeys, true));
			  japKeyExchangeNode.appendChild(mixEncryptionNode);
			  keyDoc.appendChild(japKeyExchangeNode);
			  //jap.JAPExtension.sendDialog(keyDoc, m_cascade);
			  Element mixReplayNode = keyDoc.createElement("ReplayDetection");
			  if (m_protocolWithReplay)
			  {
			  	XMLUtil.setValue(mixReplayNode, "true");
			  } 
			  else 
			  {
				XMLUtil.setValue(mixReplayNode, "false");
			  }
			  japKeyExchangeNode.appendChild(mixReplayNode);
			  XMLEncryption.encryptElement(japKeyExchangeNode,
										   m_mixParameters[0].getMixCipher().getPublicKey());
			  ByteArrayOutputStream keyExchangeBuffer = new ByteArrayOutputStream();
			  byte[] keyExchangeXmlData = XMLUtil.toByteArray(keyDoc);
			  DataOutputStream keyExchangeDataStream = new DataOutputStream(keyExchangeBuffer);
			  keyExchangeDataStream.writeShort(keyExchangeXmlData.length);
			  keyExchangeDataStream.flush();
			  keyExchangeBuffer.write(keyExchangeXmlData);
			  keyExchangeBuffer.flush();
			  byte[] keyExchangeData = keyExchangeBuffer.toByteArray();
			  a_outputStream.write(keyExchangeData);
			  a_outputStream.flush();
			  /*
			   * now receive and check the signature responded from the mix -> this
			   * doesn't much sense because if the mix uses other keys, it cannot
			   * decrypt our messages and we cannot decrypt messages from the mix ->
			   * this is only a denial-of-service attack and an attacker who is able
			   * to modify the keys (he cannot read them because they are crypted with
			   * the public key of the mix which was signed by the mix with a
			   * signature verified against an internal certificate) is also able to
			   * modify every single packet
			   */

			  /*
			   * TODO: It's very nasty to use the old raw signature implementation. It
			   * should be rewritten in the next version of the key exchange protocol
			   * (if a signature is still used there).
			   */
			  int keySignatureXmlDataLength = dataStreamFromMix.readUnsignedShort();
			  //jap.JAPExtension.successfulSend(m_cascade);

			  byte[] keySignatureXmlData = new byte[keySignatureXmlDataLength];
			  while (keySignatureXmlDataLength > 0)
			  {
				  int bytesRead = a_inputStream.read(keySignatureXmlData,
					  keySignatureXmlData.length - keySignatureXmlDataLength, keySignatureXmlDataLength);
				  if (bytesRead == -1)
				  {
					  throw new EOFException(
						  "EOF detected while reading symmetric key signature XML structure.");
				  }
				  else
				  {
					  keySignatureXmlDataLength = keySignatureXmlDataLength - bytesRead;
				  }
			  }
			  Document keySignatureDoc = XMLUtil.toXMLDocument(keySignatureXmlData);
			  Element keySignatureNode=null;
// if version=0.81
			  if (m_protocolWithReplay)
			  {
				  Element mixExchange = keySignatureDoc.getDocumentElement();
				  Element mixReplay = (Element)mixExchange.getFirstChild();
				  Element mixe = (Element)mixReplay.getFirstChild();
				  for (int i = 0; i < m_cascade.getNumberOfMixes(); i++)
				  {
					  for (int foo = 0; foo < m_cascade.getNumberOfMixes(); foo++)
					  {
						if ((mixe.getAttribute("id")).equals(m_mixParameters[foo].getMixId()))
						{
							m_mixParameters[foo].setReplayOffset(Integer.parseInt(mixe.getFirstChild().getFirstChild().getNodeValue()));
						}					
					  }
					  mixe=(Element)mixe.getNextSibling();
				  }
				  MixParameters.m_referenceTime=System.currentTimeMillis()/1000;
				  keySignatureNode = (Element)mixExchange.getLastChild();
			  }
			  else
			  {
				  keySignatureNode = keySignatureDoc.getDocumentElement();
			  }
			  if (keySignatureNode == null)
			  {
				 throw (new XMLParseException(XMLParseException.ROOT_TAG,
					 "No document element in received symmetric key signature XML structure."));
			  }

			  keyDoc.getDocumentElement().appendChild(XMLUtil.importNode(keyDoc, keySignatureNode, true));

			  if (XMLSignature.verify(keyDoc, m_cascade.getCertificate()) == null)
			  {
				  throw (new SignatureException("Invalid symmetric keys signature received."));
			  }
		  }
	  }
	  catch (SignatureException e)
	  {
		  /* clean up */
		  removeCertificateLock();
		  throw e;
	  }
  }

  public boolean isProtocolWithTimestamp() {
    return m_protocolWithTimestamp;
  }

  public boolean isPaymentRequired() {
    return m_paymentRequired;
  }

  public boolean isChainProtocolWithFlowControl() {
    return m_chainProtocolWithFlowControl;
  }

  public FixedRatioChannelsDescription getFixedRatioChannelsDescription() {
    return m_fixedRatioChannelsDescription;
  }

  public SymCipher getFirstMixSymmetricCipher() {
    return m_firstMixSymmetricCipher;
  }

  public SymCipher getMultiplexerInputStreamCipher() {
    return m_multiplexerInputStreamCipher;
  }

  public SymCipher getMultiplexerOutputStreamCipher() {
    return m_multiplexerOutputStreamCipher;
  }

  public MixParameters[] getMixParameters() {
    return m_mixParameters;
  }

  public MixCascade getConnectedCascade()
  {
	  return m_cascade;
  }

  public void removeCertificateLock() {
    synchronized (m_internalSynchronization) {
      if (m_mixCascadeCertificateLock != -1) {
        SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(m_mixCascadeCertificateLock);
        m_mixCascadeCertificateLock = -1;
      }
    }
  }

}
