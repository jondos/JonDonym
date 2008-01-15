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
package anon.util;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This interface is only needed in the context of the ResourceLoader. It loads resources
 * and transforms them into concrete objects.
 * @author Rolf Wendolsky
 * @see anon.util.ResourceLoader
 */
public interface IResourceInstantiator
{
	/**
	 * Loads a file and transforms it into a concrete object.
	 * @param a_file a File
	 * @param a_topDirectory the top directory of this file; this is generally not needed to load
	 *                       the file
	 * @throws Exception if an error occurs
	 * @return an Object
	 */
	public Object getInstance(File a_file, File a_topDirectory) throws Exception;

	/**
	 * Loads a file and transforms it into a concrete object. Please not that JDK 1.1.8 does not
	 * correctly load zip entries! Please use the method ResourceLoader.loadResource(String)
	 * instead. By this way, only zip/jar files in the classpath may be loaded.
	 * @param a_entry a ZipEntry
	 * @param a_file the ZipFile of this zip entry
	 * @throws Exception if an error occurs
	 * @return an Object
	 */
	public Object getInstance(ZipEntry a_entry, ZipFile a_file) throws Exception;


	/**
	 * An Exception that is thrown by a ResourceInstantiator if too much instantiations have failed.
	 * The instantiator object will be invalid after throwing this exception.
	 * @author Rolf Wendolsky
	 */
	public class ResourceInstantiationException extends Exception
	{
	}
}
