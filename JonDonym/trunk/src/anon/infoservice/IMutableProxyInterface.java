/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.infoservice;


/**
 * A proxy interface with multiple proxies that may change over time.
 */
public interface IMutableProxyInterface
{
	/**
	 * Returns one of two possible proxy interfaces or null if the chosen interface
	 * is not allowed or available at present.
	 * @param a_bAnonInterface true, if the anonymous interface is chosen; false, if the
	 * direct interface is chosen
	 * @return IProxyInterfaceGetter the chosen proxy interface or null if the chosen
	 * interface is not allowed or available at present
	 */
	public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface);

	public interface IProxyInterfaceGetter
	{
		public ImmutableProxyInterface getProxyInterface();
	}

	public static class DummyMutableProxyInterface implements IMutableProxyInterface
	{
		private IProxyInterfaceGetter m_dummyGetter = new IProxyInterfaceGetter()
		{
			public ImmutableProxyInterface getProxyInterface()
			{
				return null;
			}
		} ;

		public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
		{
			if (a_bAnonInterface)
			{
				return null;
			}
			return m_dummyGetter;
		}
	}
}
