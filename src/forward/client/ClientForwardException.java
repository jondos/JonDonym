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


/**
 * This is the implementation for exceptions with extended information about forwarding errors.
 */
public class ClientForwardException extends Exception {
  
  /**
   * This describes a connection error, like connection closed while reading or serious connection
   * errors.
   */
  public static final int ERROR_CONNECTION_ERROR = 1;
  
  /**
   * This describes a protocol error, like invalid signatures or invalid message length
   * information.
   */
  public static final int ERROR_PROTOCOL_ERROR = 2;
  
  /**
   * This describes a protocol version error.
   */
  public static final int ERROR_VERSION_ERROR = 3; 
  
  /**
   * This describes an unknown exception.
   */
  public static final int ERROR_UNKNOWN_ERROR = 255;
  
  
  /**
   * This stores the reason of this exception. Look at the constants in this class.
   */
  private int m_errorCode;

  
  /**
   * This creates a new ClientForwardException.
   *
   * @param a_errorCode A code describing the reason for this exception. Look at the constants
   *                    in this class.
   * @param a_message Additional information about this exception. Look at the constructor of the
   *                  Exception class for more information.
   */
  public ClientForwardException(int a_errorCode, String a_message) {
    super(a_message);
    m_errorCode = a_errorCode;
  }

  
  /**
   * Returns the error code with the reason for this exception. Look at the constants in this
   * class for more information about errors.
   *
   * @return The error code of this exception.
   */
  public int getErrorCode() {
    return m_errorCode;
  }
  
}