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
package infoservice.tor;

import anon.tor.ordescription.ORListFetcher;

/**
 * This is a dummy implementation of the ORListFetcher interface. It is used by the
 * TorDirectoryAgent of the infoservice, when a already downloaded TOR nodes list structure shall
 * be parsed.
 */
public class DummyORListFetcher implements ORListFetcher {

  /**
   * Stores the raw TOR nodes list, which shall be parsed.
   */
  private byte[] m_torNodesListStructure;


  /**
   * Creates a new DummyORListFetcher. This simply stores the specified raw TOR nodes list for
   * later parsing.
   *
   * @param a_torNodesListStructure The raw TOR nodes list, which shall be parsed later.
   */
  public DummyORListFetcher(byte[] a_torNodesListStructure) {
    m_torNodesListStructure = a_torNodesListStructure;
  }


  /**
   * Returns the stored raw TOR nodes list.
   *
   * @return The raw TOR nodes list specified in the constructor.
   */
  public byte[] getORList() {
    return m_torNodesListStructure;
  }

  public byte[] getRouterStatus()
  {
	  return null;
  }

  public byte[] getDescriptor(String digest)
  {
	  return null;
  }

  public byte[] getDescriptorByFingerprint(String fingerprint)
  {
	  return null;
  }

  public byte[] getAllDescriptors()
  {
	  return m_torNodesListStructure;
 }

  public byte[] getStatus(String fingerprint)
  {
	  return null;
  }
}
