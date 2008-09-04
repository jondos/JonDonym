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

package captcha;




/**
 * This class is a factory for creating objects, which implements the CaptchaGenerator interface.
 * This class is a singleton;
 */
public class CaptchaGeneratorFactory
{

	/**
	 * Stores the instance of CaptchaGeneratorFactory (Singleton).
	 */
	private static CaptchaGeneratorFactory ms_cgfInstance;

	/**
	 * Returns the instance of CaptchaGeneratorFactory (Singleton). If there is no instance, a new
	 * one is created.
	 *
	 * @return The CaptchaGeneratorFactory instance.
	 */
	public static CaptchaGeneratorFactory getInstance()
	{
		if (ms_cgfInstance == null)
		{
			ms_cgfInstance = new CaptchaGeneratorFactory();
		}
		return ms_cgfInstance;
	}

	/**
	 * This creates a new CaptchaGeneratorFactory.
	 */
	private CaptchaGeneratorFactory()
	{
	}

	/**
	 * Returns an object, which implements the CaptchaGenerator interface.
	 *
	 * @return A new instance of a CaptchaGenerator.
	 */
	public ICaptchaGenerator getCaptchaGenerator()
	{
		/* at the moment, we create always 300x100 zipped binary image */
		return new ZipBinaryImageCaptchaGenerator(300, 100);
	}

}
