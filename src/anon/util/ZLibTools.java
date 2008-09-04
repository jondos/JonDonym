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
package anon.util;


import java.io.ByteArrayOutputStream;

import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class provides some utility methods for ZLib compression.
 */
final public class ZLibTools
{

	/**
	 * Compresses the specified data with ZLib in the best compression mode.
	 *
	 * @param a_data The data to compress.
	 *
	 * @return The compressed data or null, if there was an error while the compression.
	 */
	public static byte[] compress(byte[] a_data)
	{
		byte[] resultData = null;
		try
		{
			Deflater zipper = new Deflater(Deflater.BEST_COMPRESSION);
			ByteArrayOutputStream zippedData = new ByteArrayOutputStream();
			DeflaterOutputStream zipStream = new DeflaterOutputStream(zippedData, zipper);
			zipStream.write(a_data, 0, a_data.length);
			zipStream.finish();
			resultData = zippedData.toByteArray();
		}
		catch (Throwable e)
		{
			/* should not happen */
		}
		return resultData;
	}

	/**
	 * Decompresses the specified data.
	 *
	 * @param a_data The ZLib compressed data (whole block, not only parts).
	 *
	 * @return The uncompressed data or null, if the specified data are not ZLib compressed.
	 */
	public static byte[] decompress(byte[] a_data)
	{
		byte[] resultData = null;
		try
		{
			ByteArrayOutputStream unzippedData = new ByteArrayOutputStream();
			Inflater unzipper = new Inflater();
			unzipper.setInput(a_data);
			byte[] currentByte = new byte[10000];
			int len;
			while ((len=unzipper.inflate(currentByte)) >0)
			{
				unzippedData.write(currentByte,0,len);
			}
			unzippedData.flush();
			resultData = unzippedData.toByteArray();
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC,
						  "ZLIb decompress() decommpressed failed!");
			/* something was wrong with the compressed data */
		}
		return resultData;
	}
}
