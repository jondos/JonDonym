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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * This class warps a jar file. <br>
 * It provides methods for reading the files containing in the jar file.
 */
final class JarFile
{
	//~ Instance variables

	/**
	 * the path of the jar file
	 * in case of remote files, it is the path for the local, temporary file
	 */
	private String m_FileName;

	/** the jar file */
	private ZipFile m_ZipFile;

	/** the manifest related to this jar file */
	private JarManifest m_Manifest;

	//~ Constructors

	/**
	 * Constructor
	 * If an exception occurs, the jar file could not be opened and read.
	 *
	 * @param name the path of the jar file
	 * @throws ZipException
	 * @throws IOException
	 * @throws SecurityException
	 */
	public JarFile(File file) throws ZipException, IOException, SecurityException
	{

		// open the jar file
		m_ZipFile = new ZipFile(file);

		// get the fully qualified file name
		m_FileName = m_ZipFile.getName();

		// initialize
		init();
	}

	/**
	 * Opens the jar file and read the manifest.
	 *
	 * @throws IOException if jar file could not be read
	 */
	private void init() throws IOException
	{
		ZipEntry mf = m_ZipFile.getEntry(JarConstants.MANIFEST_FILE);

		if (mf != null)
		{
			m_Manifest = new JarManifest(mf.getSize(), m_ZipFile.getInputStream(mf));
		}
	}

	/**
	 * Returns the mainfest.
	 *
	 * @return the mainfest
	 */
	public JarManifest getManifest()
	{
		return m_Manifest;
	}

	/**
	 * Test if a file entry exists in the jar archive.
	 *
	 * @param  name the name of the file entry
	 * @return true, if it exists
	 */
	public boolean fileExists(String name)
	{
		return (m_ZipFile.getEntry(name) != null);
	}

	/**
	 * Returns the signature file with the given alias.
	 * Note: alias is case sensitive
	 *
	 * @param  alias the alias of the signature file
	 * @return the signature file or null if no signature file with the given alias was found
	 */
	public JarSignatureFile getSignatureFile(String alias)
	{
		ZipEntry sigFile = m_ZipFile.getEntry(JarConstants.META_INF_DIR + "/" + alias + ".SF");

		try
		{
			if (sigFile != null)
			{
				return new JarSignatureFile(sigFile.getName(), sigFile.getSize(),
											m_ZipFile.getInputStream(sigFile));
			}
		}
		catch (IOException ex)
		{
		}

		return null;
	}

	/**
	 * Returns the signature block file that maps to the given name
	 *
	 * @param alias the alias of the signature block file
	 * @return the signature block file that maps to the given name
	 */
	public JarFileEntry getSignatureBlockFile(String alias)
	{
		Enumeration files = m_ZipFile.entries();
		ZipEntry fileEntry;

		alias = alias.toUpperCase();
		// filter the signature files with the given alias
		while (files.hasMoreElements())
		{
			fileEntry = (ZipEntry) files.nextElement();

			try
			{ // add a SignaturBlockFile that maps to the filter
				if (fileEntry.getName().startsWith(JarConstants.META_INF_DIR + "/" + alias)
					&& (fileEntry.getName().toUpperCase().endsWith(alias + ".DSA") ||
						fileEntry.getName().toUpperCase().endsWith(alias + ".RSA")))
				{
					return new JarFileEntry(fileEntry.getName(),
											fileEntry.getSize(),
											m_ZipFile.getInputStream(fileEntry));
				}
			}
			catch (IOException ex)
			{
			}
		}

		return null;
	}

	/**
	 * Returns the signature block files that maps to the given name
	 *
	 * @param alias the alias  of the signature block file
	 * @return the signature block files that maps to the given name
	 */
	public Vector getSignatureBlockFiles(String alias)
	{
		Vector sigBlockFiles = new Vector();
		Enumeration files = m_ZipFile.entries();
		ZipEntry fileEntry;

		// filter the signature files with the given alias
		while (files.hasMoreElements())
		{
			fileEntry = (ZipEntry) files.nextElement();

			try
			{ // add a SignaturBlockFile that maps to the filter
				if (fileEntry.getName().startsWith(JarConstants.META_INF_DIR + "/" + alias) &&
					!fileEntry.getName().toLowerCase().endsWith(".sf"))
				{
					sigBlockFiles.addElement(new JarFileEntry(fileEntry.getName(),
						fileEntry.getSize(),
						m_ZipFile.getInputStream(fileEntry)));
				}
			}
			catch (IOException ex)
			{
			}
		}

		return sigBlockFiles;
	}

	/**
	 * Returns the JarFileEntry for the file with the given name. <br>
	 * The name has to be realtiv to jar file and has to be a filename included in the jar file.
	 * The name can be a URL
	 *
	 * @param name the name of the file relative to jar file
	 * @return the JarFileEntry for the given file name or null if not found
	 */
	public JarFileEntry getFileByName(String name)
	{
		ZipEntry file = null;
		try
		{
			file = m_ZipFile.getEntry(name);
		}
		catch (Exception e)
		{}

		URL url = null;
		// could have been a URL
		if (file == null)
		{
			try
			{
				url = new URL(name);
			}
			catch (MalformedURLException e)
			{
				return null;
			}
			catch (Exception e)
			{
				return null;
			}
			try
			{
				return new JarFileEntry(url.getFile(), url.openConnection().getContentLength(),
										url.openStream());
			}
			catch (Exception e)
			{
				return null;
			}
		}

		try
		{
			if (file != null)
			{
				return new JarFileEntry(file.getName(), file.getSize(), m_ZipFile.getInputStream(file));
			}
		}
		catch (IOException ex)
		{
		}

		return null;
	}

	/**
	 * Returns the fully qualified name of the jar file.
	 *
	 * @return the fully qualified name of the jar file
	 */
	public String getName()
	{
		return m_FileName;
	}

	/**
	 *
	 * @return the alias' of all signatures this jar file was signed with
	 */
	public Vector getAliasList()
	{
		Vector alias = new Vector();

		Enumeration files = m_ZipFile.entries();

		while (files.hasMoreElements())
		{
			ZipEntry file = (ZipEntry) files.nextElement();

			// in case that there are backslashes in file name, transform it for better processing
			String name = file.getName().replace('\\', '/');

			int pos = file.getName().lastIndexOf("/");
			if (pos != -1)
			{
				// find all signature file entries in META-INF directory
				if (name.substring(0, pos).equals(JarConstants.META_INF_DIR) &&
					name.toLowerCase().endsWith(".sf"))
				{
					alias.addElement(name.substring(pos + 1, name.length() - 3));
				}
			}
		}

		return alias;
	}

	/**
	 * Close the jar file.
	 *
	 * @return true is close was succesfull, false else
	 */
	public boolean close()
	{
		try
		{
			m_ZipFile.close();
			return true;
		}
		catch (IOException ex)
		{
		}

		return false;
	}
}
