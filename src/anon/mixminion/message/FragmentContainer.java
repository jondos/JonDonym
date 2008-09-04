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
import anon.mixminion.fec.FECCode;
import anon.mixminion.fec.FECCodeFactory;
import anon.util.ByteArrayUtil;

/**
 *
 * @author Stefan Roenisch
 *
 */
public class FragmentContainer {

	private byte[] m_id = null;
	private int FRAGSIZE = 28 * 1024 - 47;
	private byte[][] m_fragments;
	private boolean m_readytoreassemble = false;
	private int m_counter;
	private int[] m_indizes;
	private int m_numberoffrags;
	private boolean[] m_add;



	/**
	 * Constructor
	 * Build a new Fragment Container with id of the message to reassemble
	 * and numberoffrags needed packets to reassemble
	 * @param id
	 * @param numberoffrags
	 */
	public FragmentContainer(byte[] id, int numberoffrags)
	{
		m_id = id;
		m_numberoffrags = numberoffrags;
		m_fragments = new byte[numberoffrags][FRAGSIZE];
		m_counter = numberoffrags-1;
		m_indizes = new int[numberoffrags];

		double exf = 4.0 / 3.0;
		// Let K = Min(16, 2**CEIL(Log2(M_SIZE)))
		double tmp = Math.log(m_numberoffrags) / Math.log(2);
		tmp = Math.ceil(tmp);
		tmp = Math.pow(2, tmp);
		int k = (int) Math.min(16, tmp);
		// Let N = Ceil(EXF*K)
		int n = (int) Math.ceil(exf * k);
		m_add = new boolean[n];
	}

	/**
	 * Adds a Fragment with specified Index
	 * returns true if enough packets are in the container to reassemble
	 * @param frag
	 * @param index
	 * @return
	 */
	public boolean addFragment(byte[] frag, int index)
	{
		if (m_readytoreassemble)
		{
			return true;
		}

		if (!m_add[index])
		{
			m_add[index] = true;
			m_indizes[m_counter] = index;
			m_fragments[m_counter] = frag;
			m_counter--;
		}


		if (m_counter == -1)
			{
			m_readytoreassemble = true;
			return true;
			}
		else return false;
	}

	/**
	 * return the id of the container/message
	 * @return
	 */
	public byte[] getID() {
		return m_id;
	}

	/**
	 * reassembles the message if possible,
	 * return null if impossible, otherwise a bytearray containing
	 * the whitened, compressed, padded message
	 * @return
	 */
	public byte[] reassembleMessage()
	{
		byte[] message = null;
		if (m_readytoreassemble)
		{
			message = new byte[0];
			double exf = 4.0 / 3.0;
			// Let K = Min(16, 2**CEIL(Log2(M_SIZE)))
			double tmp = Math.log(m_numberoffrags) / Math.log(2);
			tmp = Math.ceil(tmp);
			tmp = Math.pow(2, tmp);
			int k = (int) Math.min(16, tmp);
			// Let N = Ceil(EXF*K)
			int n = (int) Math.ceil(exf * k);

			FECCode fec = FECCodeFactory.getDefault().createFECCode(k,n);
			int[] offsets = new int[m_numberoffrags];

			fec.decode(m_fragments,offsets,m_indizes,28 * 1024 - 47,false);

			for (int i = 0; i < k; i++) {
				message = ByteArrayUtil.conc(message,m_fragments[i]);
			}

			return message;
		}
		else
			return message;
	}
}
