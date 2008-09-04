/*
Copyright (c) 2004, The JAP-Team
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

package anon.mixminion.message;

import anon.util.ByteArrayUtil;

/**
 * @author Stefan Roenisch
 *
 */
public class SingleBlockMessage extends MessageImplementation {
	static final int SINGLETON_HEADER_LEN = 22;
	private byte[] m_payload;

	/**
	 * Constructor
	 * @param compressed_payload
	 */
	public SingleBlockMessage(byte[] compressed_payload) {
		m_payload = compressed_payload;
	}

	/**
	 *
	 */
	public byte[][] buildPayload() {

		// Let PADDING_LEN = 28KB - LEN(M_C) - SINGLETON_HEADER_LEN - OVERHEAD
		// Let PADDING = Rand(PADDING_LEN)
		// return Flag 0 | Int(15,LEN(M_C)) | Hash(M_C | PADDING) | M_C | PADDING
		int paddin_len = 28 * 1024 - m_payload.length - SINGLETON_HEADER_LEN;
		byte[] payload_padding = MixMinionCryptoUtil.randomArray(paddin_len);
		byte[] first = ByteArrayUtil.inttobyte(m_payload.length, 2);
		byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(m_payload, payload_padding));
		byte[][] all = new byte[1][28 * 1024];
		all[0] = ByteArrayUtil.conc(first, hash, m_payload, payload_padding);
		return all;
	}

}
