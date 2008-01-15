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
 * This defines the methods, every captcha generator has to implement.
 */
public interface ICaptchaGenerator
{

	/**
	 * Returns the string of valid captcha characters. The data which are included in the captcha
	 * can be words over this alphabet.
	 *
	 * @return An alphabet with characters this CaptchaGenerator supports.
	 */
	public String getValidCharacters();

	/**
	 * Returns the maximum number of characters of the captcha generators alphabet, which are
	 * supported as input when creating a captcha.
	 *
	 * @return The maximum captcha generator input word length.
	 */
	public int getMaximumStringLength();

	/**
	 * Creates a new captcha from the given input word. The input must be a word over the captcha
	 * generators alphabet. It haven't to be longer than the maximum string length supported from
	 * the captcha generator. This mehtod can throw an exception, if the input string is longer
	 * than the maximum supported length or if there are not allowed letters in. The return value
	 * is a Base64 encoded string with the captcha data.
	 *
	 * @param a_captchaString The input, which shall be included in the captcha.
	 *
	 * @return A Base64 encoded string with the captcha data.
	 */
	public String createCaptcha(String a_captchaString) throws Exception;

	/**
	 * Returns the format of the captcha data (e.g. JPEG). So the JAP client of the blockee can
	 * present the data corrctly.
	 *
	 * @return A string with the data format of the captcha.
	 */
	public String getCaptchaDataFormat();

}
