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
package forward.client;

import java.util.Enumeration;
import java.util.Vector;

import anon.infoservice.MixCascade;

/**
 * This implements the forwarding information structure with supported mixcascades, quality of
 * service parameters and the need for dummy traffic. This structure is just the information for
 * the client. Every value is controlled by the forwarder, so changes don't have an effect to
 * the forwarded connection.
 */
public class ForwardConnectionDescriptor {
  
  /**
   * This stores the mixcascades supported by the forwarder.
   */
  private Vector m_supportedMixCascades;
  
  /**
   * This stores maximum bandwidth (in bytes/second) the forwarder supports. The sum of upload
   * and download cannot be larger than that.
   */
  private int m_maximumBandwidth;
  
  /**
   * This stores the bandwidth (in bytes/second) guaranteed by the forwarder. We can send /
   * receive data with at least that rate (sum of upload and download).
   */
  private int m_guaranteedBandwidth;
  
  /**
   * This stores the minimal dummy traffic interval (in ms) for the forwarder. We have to create
   * dummy traffic with at least that rate. If this value is -1, the forwarder doesn't need dummy
   * traffic.
   */
  private int m_minDummyTrafficInterval;
  
  
  /**
   * This creates a new instance of ForwardConnectionDescriptor.
   */
  public ForwardConnectionDescriptor() {
    m_supportedMixCascades = new Vector();
    m_maximumBandwidth = 0;
    m_guaranteedBandwidth = 0;
    m_minDummyTrafficInterval = -1;
  }
  
  
  /**
   * Adds a MixCascade to the list of mixcascades supported by the forwarder.
   *
   * @param a_mixCascade The MixCascade to add.
   */
  public void addMixCascade(MixCascade a_mixCascade) {
    m_supportedMixCascades.addElement(a_mixCascade);
  }

  /**
   * Returns a snapshot of all supported mixcascades.
   *
   * @return A Vector with all mixcascades the forwarder supports.
   */  
  public Vector getMixCascadeList() {
    Vector entryList = new Vector();
    Enumeration mixCascades = m_supportedMixCascades.elements();
    while (mixCascades.hasMoreElements()) {
      entryList.addElement(mixCascades.nextElement());
    }
    return entryList;
  }

  /**
   * Sets the maximum bandwidth (sum of upload and download in bytes/second) the forwarder supports.
   *
   * @param a_maximumBandwidth The maximum bandwidth.
   */
  public void setMaximumBandwidth(int a_maximumBandwidth) {
    m_maximumBandwidth = a_maximumBandwidth;
  }
  
  /**
   * Returns the maximum bandwidth (sum of upload and download in bytes/second) the forwarder supports.
   *
   * @return The maximum bandwidth.
   */  
  public int getMaximumBandwidth() {
    return m_maximumBandwidth;
  }
  
  /**
   * Sets the guaranteed bandwidth (sum of upload and download in bytes/second) the forwarder
   * supplies.
   *
   * @param a_guaranteedBandwidth The guaranteed bandwidth.
   */
  public void setGuaranteedBandwidth(int a_guaranteedBandwidth) {
    m_guaranteedBandwidth = a_guaranteedBandwidth;
  }

  /**
   * Returns the guaranteed bandwidth (sum of upload and download in bytes/second) the forwarder
   * supplies.
   *
   * @return The guaranteed bandwidth.
   */
  public int getGuaranteedBandwidth() {
    return m_guaranteedBandwidth;
  }

  /**
   * Sets the dummy traffic interval (in ms) for the forwarder. We have to create dummy traffic
   * with at least that rate. If this value is -1, the forwarder doesn't need dummy traffic.
   *
   * @param a_minDummyTrafficInterval The minimum dummy traffic interval.
   */
  public void setMinDummyTrafficInterval(int a_minDummyTrafficInterval) {
    m_minDummyTrafficInterval = a_minDummyTrafficInterval;
  }

  /**
   * Returns the dummy traffic interval (in ms) for the forwarder. We have to create dummy traffic
   * with at least that rate. If this value is -1, the forwarder doesn't need dummy traffic.
   *
   * @return The minimum dummy traffic interval.
   */  
  public int getMinDummyTrafficInterval() {
    return m_minDummyTrafficInterval;
  }
  
}