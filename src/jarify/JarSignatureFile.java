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

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

final class JarSignatureFile extends JarManifest
{
	private Hashtable manifestdigests = new Hashtable();

	//~ Constructors

	/**
	 * Constructor
	 * @param name The fill path name to the signature file.
	 * @param size The size of the signature file.
	 * @param stream The input stream for this signature file.
	 */
	public JarSignatureFile(String name, long size, InputStream stream)
	{
		super(size, stream);
		this.name = name;

		parseManifestDigests();
	}

	private void parseManifestDigests()
	{
		// remove continuations until first entry (Name: xxx)
		int contPos;

		// get the manifest digest
		String header = contentStrRaw.substring(0,
												contentStrRaw.indexOf(newLine + JarConstants.MANIFEST_ENTRY));

		while ( (contPos = header.indexOf(newLine + " ")) != -1)
		{
			header = header.substring(0, contPos) +
				header.substring(contPos + newLine.length() + 1, header.length());
		}

		int startPos = 0;
		int endPos = 0;
		Vector tmpVector = new Vector();

		while ( (endPos = header.indexOf(newLine, startPos)) != -1)
		{
			tmpVector.addElement(header.substring(startPos, endPos));
			startPos = endPos + newLine.length();
		}

		// now process each line...magic!
		for (int i = 0; i < tmpVector.size(); i++)
		{
			String line = (String) tmpVector.elementAt(i);
			if ( (startPos = line.indexOf("-Manifest: ")) != -1)
			{
				String digName = line.substring(0, startPos);
				String value = line.substring(startPos + 11);
				manifestdigests.put(digName, value);
			}
		}
	}

	/**
	 * Returns the digest for the manifest file.
	 * NOTE: You have to specify the <b>full digest string</b> in the parameter digestID. This
	 * String is used to find the digest for the manifest file.
	 *
	 * @param  digestID Something like <b>SHA1-Digest</b> or <b>MD5-Digest</b>
	 * @return The digest for this file in a String format or null if file or digest were not found.
	 * @see JarConstants
	 */
	public String getManifestDigest(String digestID)
	{
		return (String) manifestdigests.get(digestID);
	}

	public Vector getManifestDigestList()
	{
		Enumeration digs = manifestdigests.keys();
		Vector digVector = new Vector();

		while (digs.hasMoreElements())
		{
			digVector.addElement(digs.nextElement());
		}

		return digVector;
	}

	/**
	 * Returns the alias for this signature file.
	 *
	 * @return The Alias for this signature file or null if there was an error;
	 */
	public String getAlias()
	{
		if (name.indexOf(".") == -1)
		{
			return null;
		}
		else
		{
			return name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."));
		}
	}
}
