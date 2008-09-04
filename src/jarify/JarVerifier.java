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
package jarify;

import java.io.File;
import java.io.IOException;
import java.security.SignatureException;
import java.util.Hashtable;
import java.util.Vector;
import java.util.zip.ZipException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.util.encoders.Base64;

import anon.crypto.JAPCertificate;
import anon.crypto.*;

/**
 * Verfies the authencity of a signed jar file.
 */
final public class JarVerifier
{
	/** The JarFile to authenticate to */
	private JarFile m_jarFile;

	/** The Manifest File of the JarFile */
	private JarManifest m_Manifest;

	/** The trusted certificate */
	private JAPCertificate m_certRoot;

	/** Contains all loaded Digest Class objects for caching purposes */
	private Hashtable digestCache = new Hashtable();

	/** Contains the signature block file (as PKCS#7) for each alias */
	private Hashtable aliasSBF = new Hashtable();

	//~ Constructors

	/**
	 * Constructor
	 *
	 * @param jarFilePath the JarFile to verify
	 */
	private JarVerifier(File jarFile) throws ZipException, IOException, SecurityException
	{
		m_certRoot = null;
		m_jarFile = new JarFile(jarFile);
		m_Manifest = m_jarFile.getManifest();

	}

	private void close()
	{
		m_jarFile.close();
	}

	//~ Methods

	/**
	 * Initializes the aliasSBF Hashtable and removes the aliases which cannot be<br>
	 * authenticated by the given root certificate.
	 *
	 * @param aliases All aliases from the Jarfile.
	 * @return Vector with the aliases that can be verified.
	 */
	private Vector InitAliases(Vector aliases)
	{
		Vector cerAliases = new Vector();

		for (int i = 0; i < aliases.size(); i++)
		{
			String alias = null;
			JAPCertificate[] certs = null;
			PKCS7SignedData block = null;

			alias = (String) aliases.elementAt(i);
			JarFileEntry sbf = m_jarFile.getSignatureBlockFile(alias);
			if (sbf == null)
			{
				continue;
			}

			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Checking certificate chain for alias: " + alias);

			try
			{
				block = new PKCS7SignedData(sbf.getContent());
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, ex.getMessage());
				continue;
			}
			if (block == null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Could not get PKCS#7 data object!");
				continue;
			}

			aliasSBF.put(alias, block);

			certs = block.getCertificates();
			if (certs == null)
			{
				continue;
			}
			try
			{
				certs[certs.length - 1].verify(m_certRoot.getPublicKey());
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, ex.getMessage());
				continue;
			}

			try
			{
				// Validate the certificate chain
				for (int j = 0; j < certs.length - 1; j++)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Checking certificate No. : " + j + 1);
					certs[j].verify(certs[j + 1].getPublicKey());
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Certificate No. " + j + 1 + " verified OK.");
				}
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, ex.getMessage());
				continue;
			}

			// All checks passed - insert into buffer
			cerAliases.addElement(alias);
		}
		return cerAliases;
	}

	public static boolean verify(File file, JAPCertificate cert)
	{
		try
		{
			JarVerifier jv = new JarVerifier(file);
			boolean b = jv.verify(cert);
			jv.close();
			return b;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	/**
	 * Verify the signature of the jarFile with the supplied certificate.
	 *
	 * Checks the signatures of the jarFile against the supplied certificate.
	 * If no Signer can be validated against this certificate, the verification
	 * fails.
	 *
	 * @param  cert Path to the certificate
	 * @return True if the JarFile is valid, false otherwise
	 */
	private boolean verify(JAPCertificate cert)
	{

		m_certRoot = cert;
		if (m_certRoot == null)
		{
			return false;
		}
		/*		try
		  {
		   //cer.checkValidity();
		  }
		  catch (CertificateException ex)
		  {
		   if (debug)
		   {
		 System.out.println(ex.getMessage());
		   }
		   return false;
		  }*/
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Searching for Signatures...");
		if (!isSignedJar())
		{
			return false;
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "This is a signed Jarfile.\n");

		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Verifying Manifest entries...");
		if (!verifyManifestDigests())
		{
			return false;
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Manifest entries verified OK.\n");

		Vector cerAliases = InitAliases(m_jarFile.getAliasList());
		if (cerAliases.size() < 1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "\nNo Aliases present that can be validated with the given root certificate!\n");
			return false;
		}

		String alias = null;

		for (int n = 0; n < cerAliases.size(); n++)
		{
			alias = (String) cerAliases.elementAt(n);
			if ( (alias == null) || (alias == ""))
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "\nAlias error");
				return false;
			}

			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "Verifying Signature File entries for alias \"" + alias + "\"...");
			if (!verifySFDigests(alias))
			{
				return false;
			}
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Entries verified OK.");

			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Verifying Signature for alias \"" + alias + "\"...");
			if (!verifySignature(alias))
			{
				return false;
			}
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Signature from \"" + alias + "\" is genuine.\n");
		}

		return true;
	}

	/**
	 * Checks the Signature File against the Signature Block File from the given alias.
	 *
	 * @param alias The entity whose signature(s) should be tested
	 * @return True if all signatures from this entity could be verified,<br>
	 * False otherwise
	 */
	private boolean verifySignature(String alias)
	{
		boolean sig_ok = false;
		JarSignatureFile sf = m_jarFile.getSignatureFile(alias);
		if (sf == null)
		{
			return false;
		}
		JarFileEntry sbf = m_jarFile.getSignatureBlockFile(alias);
		if (sbf == null)
		{
			return false;
		}
		byte[] sfBytes = sf.getContent();
		if (sfBytes == null)
		{
			return false;
		}

		String sbfName = sbf.getName();
		if (sbfName.endsWith(".DSA") || sbfName.endsWith(".RSA"))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "Found " + sbfName.substring(sbfName.lastIndexOf(".") + 1)
						  + " signature in : " + sbfName);

			try
			{
				/*
				 if( debug ) { System.out.println( "Validating certificate chain..." ); }
				 PKCS7SignedData block = new PKCS7SignedData( sbf.getContent() );
				 Certificate[] certs =  block.getCertificates();
				 if( debug ) { System.out.println( "Number of certificates: " + certs.length ); }

				 // Check last certificate against certificate of trusted authority
				 certs[certs.length-1].verify( cer.getPublicKey() );
				 */

				// Check the .SF against the signature in the signature block file
				PKCS7SignedData block = (PKCS7SignedData) aliasSBF.get(alias);

				sig_ok = block.verify(sfBytes);
				if (!sig_ok)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Wrong Signature in " + sbfName);

					return false;
				}
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Signature in " + sbfName + " verified OK.");

			}
			catch (SignatureException ex)
			{
				return false;
			}
			/*			catch( NoSuchAlgorithmException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			   catch( NoSuchProviderException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			   catch( CertificateException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			   catch( InvalidKeyException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			   catch( CRLException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			   catch( SecurityException ex )
			   {
			 System.out.println( ex );
			 return false;
			   }
			 */
		}
		return sig_ok;
	}

	/**
	 * Checks whether the jarFile is a signed one.<br>
	 * <p>
	 * Checks in particular:<br>
	 * - if Manifest file exists<br>
	 * - if at least one Signature file exists<br>
	 * - if at least one Signature Block file for each SF exists
	 * @return True if this jarFile is signed, False otherwise
	 */
	private boolean isSignedJar()
	{
		if (m_jarFile == null)
		{
			return false;
		}
		Vector aliases = m_jarFile.getAliasList();
		String[] algs = new String[2];
		String name;
		boolean found;

		algs[0] = ".DSA";
		algs[1] = ".RSA";

		if (aliases.size() < 1)
		{
			return false;
		}

		if (!m_jarFile.fileExists(JarConstants.MANIFEST_FILE))
		{
			return false;
		}

		for (int i = 0; i < aliases.size(); i++)
		{
			found = false;
			name = JarConstants.META_INF_DIR + "/" + aliases.elementAt(i);
			name = name.toUpperCase();
			for (int j = 0; j < algs.length; j++)
			{
				if (m_jarFile.fileExists(name + algs[j]))
				{
					found = true;
					break;
				}
			}
			if (!found)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Verifies the digests within the SF file against the digests<br>
	 *   computed of the entries in the manifest file.<br>
	 * Verifies the digest in the SF file against the digest<br>
	 *   computed from the entire manifest file.
	 *
	 * @param alias Specfies the signature file for the given alias
	 * @return True if validated
	 */
	private boolean verifySFDigests(String alias)
	{
		JarSignatureFile signatureFile = m_jarFile.getSignatureFile(alias);

		if (signatureFile == null)
		{
			return false;
		}

		// verify the digest of the whole manifest file
		Vector digList = signatureFile.getManifestDigestList();
		String digString;
		String manDigest;

		for (int j = 0; j < digList.size(); j++)
		{
			digString = (String) digList.elementAt(j);
			manDigest = signatureFile.getManifestDigest(digString);

			// compute the digest of the file
			Digest digest = getDigestClass(digString);
			byte[] hash = new byte[digest.getDigestSize()];
			try
			{
				byte[] manifestContent = m_Manifest.getContent();
				if (manifestContent == null)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Manifest file null.");
					return false;
				}
				digest.update(manifestContent, 0, manifestContent.length);
				digest.doFinal(hash, 0);

				// verify the digest
				if (!manDigest.equals(new String(Base64.encode(hash))))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Digest verify failed for manifest file.");
					return false;
				}
			}
			catch (Exception e)
			{
				return false;
			}
		}
		// verify the digests in the signature file against the digests of the entries in the manifest file
		Vector listSig = signatureFile.getFileNames();
		String fileName;
		String sigDigest;
		byte[] manEntry;

		// iterate all file entries in the manifest file
		for (int i = 0; i < listSig.size(); i++)
		{
			fileName = (String) listSig.elementAt(i);

			digList = m_Manifest.getDigestList(fileName);
			for (int j = 0; j < digList.size(); j++)
			{
				digString = (String) digList.elementAt(j);
				sigDigest = signatureFile.getDigest(fileName, digString);
				manEntry = m_Manifest.getEntry(fileName);

				Digest digest = getDigestClass(digString);
				byte[] sha1 = new byte[digest.getDigestSize()];

				try
				{
					digest.update(manEntry, 0, manEntry.length);
					digest.doFinal(sha1, 0);

					// compare the digests
					if (!sigDigest.equals(new String(Base64.encode(sha1))))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Digest verify failed for " + fileName);
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, digString);
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, sigDigest);
						return false;
					}
				}
				catch (Exception e)
				{
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Verifies the digests in the manifest file to the digests<br>
	 * calculated against the corresponding file.
	 *
	 * @return True is validated, false otherwise
	 */
	private boolean verifyManifestDigests()
	{
		Vector fileList = m_Manifest.getFileNames();
		String fileName;
		String manDigest;
		JarFileEntry fileEntry;
		Vector digList;
		String digString;

		// test every file entry
		for (int i = 0; i < fileList.size(); i++)
		{
			fileName = (String) fileList.elementAt(i);
			fileEntry = m_jarFile.getFileByName(fileName);

			// unexpected error
			if (fileEntry == null)
			{
				return false;
			}

			digList = m_Manifest.getDigestList(fileName);

			for (int j = 0; j < digList.size(); j++)
			{
				digString = (String) digList.elementAt(j);
				manDigest = m_Manifest.getDigest(fileEntry, digString);
				// compute the digest of the file
				Digest digest = getDigestClass(digString);
				byte[] sha1 = new byte[digest.getDigestSize()];

				try
				{
					byte[] fileContent = fileEntry.getContent();

					if (fileContent == null)
					{
						return false;
					}

					digest.update(fileContent, 0, fileContent.length);
					digest.doFinal(sha1, 0);

					// compare the digests
					if (!manDigest.equals(new String(Base64.encode(sha1))))
					{
						return false;
					}
				}
				catch (Exception e)
				{
					return false;
				}
			}
		}

		// everything seems to be okay
		return true;
	} // verifyManifestDigests

	/**
	 * This method retrieves the digest class for the given digest name and<br>
	 * tries to instanciate an object of this digest class.
	 *
	 * @param  digestID The name of the digest, e.g. 'SHA1-Digest'
	 * @return An object of the digest class for the given name or<br>
	 * null if no digest class was found
	 * @see    JarConstants
	 */
	private Digest getDigestClass(String digestID)
	{
		// remove first hyphen from digest name, because bouncycastle classes come without one
		int hyphen = digestID.indexOf("-");

		if (hyphen != -1)
		{
			digestID = digestID.substring(0, hyphen) + digestID.substring(hyphen + 1);
		}

		if (digestCache.contains(digestID))
		{
			Digest dig = (Digest) digestCache.get(digestID);
			dig.reset();
			return dig;
		}

		try
		{
			// try to load class
			Class digest = Class.forName("org.bouncycastle.crypto.digests." + digestID);

			// try to instanciate an object
			Digest digObj = (Digest) digest.newInstance();

			digestCache.put(digestID, digObj);

			return digObj;
		}
		catch (ClassNotFoundException e)
		{} // there is no such digest class supported by bouncycastle
		catch (IllegalAccessException e)
		{}
		catch (InstantiationException e)
		{}

		// in error case
		return null;
	}

}
