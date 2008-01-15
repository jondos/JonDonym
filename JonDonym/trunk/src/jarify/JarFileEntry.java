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

/**
 * This class represents a single file within a JarFile.
 * So, the data exchange is done via this wrapper class.
 */
class JarFileEntry
{
	//~ Instance variables

	/** Name must be the fully qualified path name to this file (as given in the JAR MANIFEST.) */
	protected String name;

	/** an inputstream to read from this file */
	private InputStream stream;

	/** the uncompressed size of the file */
	private long size;

	/** the content of the file in a byte array */
	private byte[] content = null;

	//~ Constructors

	/**
	 * Create a JarFileEntry to an existing file in a JarFile.
	 *
	 * @param path The fully qulified path and name of the file or a URL name.
	 * @param size the uncompressed size of the file
	 * @param stream the initialized inputstream to read from this file
	 */
	public JarFileEntry(String path, long size, InputStream stream)
	{
		this.name = path;
		this.size = size;
		this.stream = stream;
	}

	//~ Methods

	/**
	 * Returns the fully qualified path of the file.
	 * @return The fully qualified path and name of the file.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the size of the file.
	 * @return The size of the file.
	 */
	public long getSize()
	{
		return size;
	}

	/**
	 * Returns the content of the file.
	 * @return The content of the file as byte[] or null on error.
	 */
	public byte[] getContent()
	{
		if (content == null)
		{
			// the buffer for reading
			// must be initialized with the actual size of the manifest file
			content = new byte[ (int) size];
			int lenRead = 0, t = 0;

			try
			{
				while (lenRead != size)
				{
					t = stream.read(content, lenRead, (int) size - lenRead);
					lenRead += t;
				}

			}
			catch (Exception e)
			{
				// if there is an exception, return null
				return null;
			}
			//if the size differs, return null
			if (lenRead != size)
			{
				return null;
			}
		}

		return content;
	}
}
