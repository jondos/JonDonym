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

/**
 * The Manifest of a JarFile.
 *
 * It contains of Manifest Entries.
 */
class JarManifest extends JarFileEntry
{
	/**
	 * This is an entry in the manifest.
	 */
	private class EntryData
	{
		/** the raw data of the entry (with continuations) */
		byte[] raw;

		/** a map of digests: digestID/digest*/
		Hashtable digests;

		public EntryData(byte[] rawdata)
		{
			raw = rawdata;
			digests = new Hashtable();
		}
	}

	private Hashtable entries = new Hashtable();
	private Vector fileNameList = new Vector();
	//private Hashtable tmp = new Hashtable();

	//~ Instance variables

	/** the content of the manifest as String */
	protected String contentStrRaw; //with continuations

	/** the character(s) of the newline used in this file */
	protected String newLine;

	//~ Constructors

	/**
	 * Constructor
	 *
	 * @param size The size of the Manifest file.
	 * @param stream An InputStream that is used for reading.
	 */
	public JarManifest(long size, InputStream stream)
	{
		super(JarConstants.MANIFEST_FILE, size, stream);
		init();
	}

	//~ Methods

	/**
	 * Reads the content of the file into a string variable.
	 * And finds out what is the new line character in this file.
	 */
	protected void init()
	{
		// the buffer for reading
		// must be initialized with the actual size of the manifest file
		byte[] buffer = getContent();

		if (buffer == null)
		{
			return;
		}

		contentStrRaw = new String(buffer);

		// find out what's the newline character(s)
		if (contentStrRaw.indexOf("\r\n") != -1)
		{
			newLine = "\r\n";
		}
		else if (contentStrRaw.indexOf("\r") != -1)
		{
			newLine = "\r";
		}
		else
		{
			newLine = "\n";

		}
		parse();
	}

	private void parse()
	{
		//fill the entries
		int startPos = 0, temp, newLinePos = 0;
		int endPos, digestStartPos;
		String rawEntry;
		String fileName;
		String digestName, digestValue;
		EntryData entryData;

		// now parse all entries
		while ( (startPos = contentStrRaw.indexOf(newLine + JarConstants.MANIFEST_ENTRY, startPos)) != -1)
		{
			// get the next entry
			startPos += newLine.length();
			endPos = contentStrRaw.indexOf(newLine + newLine, startPos);
			rawEntry = contentStrRaw.substring(startPos, endPos + newLine.length() * 2);
			entryData = new EntryData(rawEntry.getBytes());

			// remove continuations
			int contPos = 0;
			while ( (contPos = rawEntry.indexOf(newLine + " ")) != -1)
			{
				rawEntry = rawEntry.substring(0, contPos) +
					rawEntry.substring(contPos + newLine.length() + 1, rawEntry.length());
			}

			// get filename from entry
			fileName = rawEntry.substring(JarConstants.MANIFEST_ENTRY.length(), rawEntry.indexOf(newLine));
			fileNameList.addElement(fileName);
			entries.put(fileName, entryData);

			// now parse all digests within this entry
			startPos = rawEntry.indexOf(newLine);
			digestStartPos = startPos;
			while ( (digestStartPos = rawEntry.indexOf("-Digest: ", digestStartPos + 1)) != -1)
			{
				// find the newLine right before the digest
				temp = 0;
				while ( (temp = rawEntry.indexOf(newLine, temp + 1)) < digestStartPos)
				{
					newLinePos = temp;
				}
				digestName = rawEntry.substring(newLinePos + newLine.length(), digestStartPos + 7);
				digestValue = rawEntry.substring(digestStartPos + 9, rawEntry.indexOf(newLine, digestStartPos));
				entryData.digests.put(digestName, digestValue);
			}
			startPos = endPos - 3;
		}
	} // parse

	/**
	 * Returns all file names as String. The paths are relative to the root of the jar file, or URIs!
	 *
	 * @return a list containing all files that are listed in the manifest file as string objects or null if there was an error!
	 */
	public Vector getFileNames()
	{
		return fileNameList;
	}

	/**
	 * Returns the digest for a file.
	 * NOTE: You have to specify the <b>full digest string</b> in the parameter digestID. This
	 * String is used to find the digest in the manifest file.
	 *
	 * @param  file a JarFileEntry
	 * @param  digestID Something like <b>SHA1-Digest</b>: or <b>MD5-Digist:</b>
	 * @return The digest for this file in a String format or null if file or digest were not found.
	 * @see JarContants
	 */
	public String getDigest(JarFileEntry file, String digestID)
	{
		return getDigest(file.getName(), digestID);
	}

	/**
	 * Returns the digest for a file.
	 * NOTE: You have to specify the <b>full digest string</b> in the parameter digestID. This
	 * String is used to find the digest in the manifest file.
	 *
	 * @param  fileName the file name of the JarFileEntry
	 * @param  digestID Something like <b>SHA1-Digest</b>: or <b>MD5-Digist:</b>
	 * @return The digest for this file in a String format or null if file or digest were not found.
	 * @see JarContants
	 */
	public String getDigest(String fileName, String digestID)
	{
		EntryData entry = (EntryData) entries.get(fileName);

		if (entry == null)
		{
			return null;
		}

		return (String) entry.digests.get(digestID);
	}

	/**
	 * Get the Manifest Entry of a file.
	 * @param fileName
	 * @return file entry as byte array
	 */
	public byte[] getEntry(String fileName)
	{
		EntryData entry = (EntryData) entries.get(fileName);

		if (entry == null)
		{
			return null;
		}

		return (byte[]) entry.raw;
	}

	/**
	 * @returns A Vector with Strings of digests without colon!!!
	 */
	public Vector getDigestList(String fileName)
	{
		EntryData entry = (EntryData) entries.get(fileName);

		if (entry == null)
		{
			return null;
		}

		Enumeration digests = entry.digests.keys();
		Vector digVector = new Vector();

		while (digests.hasMoreElements())
		{
			digVector.addElement(digests.nextElement());
		}

		return digVector;
	}
}
