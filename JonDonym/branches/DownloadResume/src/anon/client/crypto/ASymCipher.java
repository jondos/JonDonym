/*
 Copyright (c) 2000, The JAP-Team
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
package anon.client.crypto;

import java.math.BigInteger;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import anon.ErrorCodes;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;

public class ASymCipher {
  // /* My hack Crypt...
  private BigInteger m_Modulus;

  private BigInteger m_Exponent;

  private byte[] tmpP;

  public ASymCipher() {
    m_Modulus = null;
    m_Exponent = null;
    tmpP = new byte[128];
  }
  
  public int encrypt(byte[] from, int ifrom, byte[] to, int ito) {
    BigInteger P = null;
    if (from.length == 128) {
      P = new BigInteger(1, from);
    } else {
      System.arraycopy(from, ifrom, tmpP, 0, 128);
      P = new BigInteger(1, tmpP);
    }
    BigInteger C = P.modPow(m_Exponent, m_Modulus);
    byte[] r = C.toByteArray();
    if (r.length == 128) {
      System.arraycopy(r, 0, to, ito, 128);
    } else if (r.length == 129) {
      System.arraycopy(r, 1, to, ito, 128);
    } else {
      for (int k = 0; k < 128 - r.length; k++) {
        to[ito + k] = 0;
      }
      System.arraycopy(r, 0, to, ito + 128 - r.length, r.length);
    }
    return 128;
  }

  public int setPublicKey(BigInteger modulus, BigInteger exponent) {
    if (modulus == null || exponent == null) {
      return ErrorCodes.E_UNKNOWN;
    }
    m_Modulus = modulus;
    m_Exponent = exponent;
    return ErrorCodes.E_SUCCESS;
  }

  public int setPublicKey(Element xmlKey) {
    try {
      Element elemRSAKEyValue = (Element) xmlKey.getElementsByTagName("RSAKeyValue").item(0);
      BigInteger n = getBigIntegerFromXml(elemRSAKEyValue, "Modulus");
      BigInteger e = getBigIntegerFromXml(elemRSAKEyValue, "Exponent");
      return setPublicKey(n, e);
    } catch (Exception e) {
      return ErrorCodes.E_UNKNOWN;
    }
  }

  public MyRSAPublicKey getPublicKey() {
    return new MyRSAPublicKey(m_Modulus, m_Exponent);
  }

  private BigInteger getBigIntegerFromXml(Element root, String name) {
    try {
      Element child = (Element) root.getElementsByTagName(name).item(0);
      Text text = (Text) child.getFirstChild();
      String value = text.getNodeValue();
      byte[] b = Base64.decode(value);
      return new BigInteger(1, b);
    } catch (Exception e) {
      return null;
    }
  }

  // END My Hack CRYPT */

  /*
   * Cryptix... private Cipher cipherEnc=null; public JAPASymCipher() { try {
   * cipherEnc=Cipher.getInstance("RSA/ECB/NONE"); } catch(Exception e) {
   * JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"Init Asym-Cipher:
   * "+e.toString()); } }
   * 
   * public int encrypt(byte[] from,int ifrom,byte[] to,int ito) { try {
   * cipherEnc.update(from,ifrom,128,to,ito); return 128; } catch(Exception e) {
   * JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"encrypt ASym-Cipher:
   * "+e.toString()); return -1; } }
   * 
   * public int setPublicKey(BigInteger modulus,BigInteger exponent) { try {
   * cipherEnc.init(Cipher.ENCRYPT_MODE,(Key)new
   * RSAPrivateKeySpec(modulus,exponent)); return 0; } catch(Exception e) {
   * JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"setPublicKey ASym-Cipher:
   * "+e.toString()); return -1; } }
   */
}
