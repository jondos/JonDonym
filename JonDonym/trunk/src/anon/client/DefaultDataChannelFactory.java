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
package anon.client;

import anon.client.crypto.DefaultMixCipher;
import anon.client.crypto.FirstMixCipher;
import anon.client.crypto.IMixCipher;
import anon.client.crypto.KeyPool;
import anon.client.crypto.MixCipherChain;
import anon.client.crypto.SymCipher;


/** 
 * @author Stefan Lieske
 */
public class DefaultDataChannelFactory implements IDataChannelFactory {

  private static final int SYMMETRIC_CIPHER_KEY_LENGTH = 16;
  
  private static final int SYMMETRIC_CIPHER_BLOCK_LENGTH = 16;
  
  
  private KeyExchangeManager m_keyExchangeManager;
  
  private Multiplexer m_multiplexer;
  
  
  public DefaultDataChannelFactory(KeyExchangeManager a_keyExchangeManager, Multiplexer a_multiplexer) {
    m_keyExchangeManager = a_keyExchangeManager;
    m_multiplexer = a_multiplexer;
  }
  
  public AbstractDataChannel createDataChannel(int a_channelId, AbstractDataChain a_parentDataChain) {
    IMixCipher[] mixCiphers = new IMixCipher[m_keyExchangeManager.getMixParameters().length];
    for (int i = 0; i < mixCiphers.length; i++) {
      if ((i == 0) && (m_keyExchangeManager.getFirstMixSymmetricCipher() != null)) {
        /* first mix is using symmetric cipher */
        SymCipher channelCipher = new SymCipher();
        byte[] channelKey = new byte[SYMMETRIC_CIPHER_KEY_LENGTH];
        KeyPool.getKey(channelKey);
        channelCipher.setEncryptionKeyAES(channelKey);
        /* initialize the internal buffer of the cipher with a non-standard
         * value
         * Attention: The encryption key must not be modified after this
         *            point because in the default SymCipher-implementation
         *            a change of the encryption key would also reset the
         *            initialization of the internal buffer.
         */
        byte[] iv2Buffer = new byte[SYMMETRIC_CIPHER_BLOCK_LENGTH];
        for (int j = 0; j < iv2Buffer.length; j++) {
          iv2Buffer[j] = (byte)0xff;
        }
        channelCipher.setIV2(iv2Buffer);
        mixCiphers[i] = new FirstMixCipher(m_keyExchangeManager.getFirstMixSymmetricCipher(), channelCipher);
      }
      else {
        /* current mix is using asymmetric cipher */
        SymCipher channelCipher = new SymCipher();
        byte[] channelKey = new byte[SYMMETRIC_CIPHER_KEY_LENGTH];
        KeyPool.getKey(channelKey);
        channelCipher.setEncryptionKeyAES(channelKey);
        /* Attention: Maybe the key is modified again by the MixCipher
         * implementation because of necessary adaption to RSA (m < n -> key
         * shouldn't start with a 1) and the current timestamp (can only be
         * done when the first packet is sent). But this is no problem because
         * here no internal buffer needs initialization.
         */
        mixCiphers[i] = new DefaultMixCipher(m_keyExchangeManager.getMixParameters()[i], channelCipher);        
      }
    }
    FixedRatioChannelsDescription channelsDescription = m_keyExchangeManager.getFixedRatioChannelsDescription();
    if (channelsDescription == null) {
      /* old protocol -> create UnlimitedDataChannel instances */
      return (new UnlimitedDataChannel(a_channelId, m_multiplexer, a_parentDataChain, new MixCipherChain(mixCiphers)));  
    }
    return (new SimulatedLimitedDataChannel(a_channelId, m_multiplexer, a_parentDataChain, new MixCipherChain(mixCiphers), channelsDescription.getChannelDownstreamPackets(), channelsDescription.getChannelTimeout()));
  }
}
