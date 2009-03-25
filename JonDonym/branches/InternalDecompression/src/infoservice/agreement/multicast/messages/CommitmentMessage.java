/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package infoservice.agreement.multicast.messages;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

import javax.naming.directory.InvalidAttributesException;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.Base64;

/**
 * @author LERNGRUPPE Represents a commitment message used by the implementation
 *         of the commitment scheme (Bruce Schneiers scheme).
 */
/**
 * @author agamemnon
 * 
 */
public class CommitmentMessage
{

    /**
     * A random number as String.
     */
    private String m_randomOne;

    /**
     * A second random number as String.
     */
    private String m_randomTwo;

    /**
     * This variable contains the message itself e.g. "Hello"
     */
    private String m_proposal;

    /**
     * This character will be used for string concatenation.
     */
    private char m_separatorCharacter;

    /**
     * Construct a <type>CommitmentMessage</type>. All necessary values will be
     * created automaticly.
     * 
     */
    public CommitmentMessage()
    {
        this.m_randomOne = Long.toString(new Random().nextLong());
        this.m_randomTwo = Long.toString(new Random().nextLong());
        this.m_proposal = Long.toString(new Random().nextLong());
        this.m_separatorCharacter = '#';
    }

    /**
     * Construct a <type>CommitmentMessage</type>. All values are given as a
     * concatenated string representation and will be parsed out.
     * 
     * @param a_stringconcat
     * @throws InvalidAttributesException
     */
    public CommitmentMessage(String a_stringconcat) throws InvalidAttributesException
    {
        this.m_separatorCharacter = '#';
        Vector stringVector = this.deconcat(a_stringconcat);
        if (stringVector.size() != 3)
        {
            throw new InvalidAttributesException("Size of Vector mustbe 3. But is "
                    + stringVector.size());
        }
        this.m_randomOne = (String) stringVector.get(0);
        this.m_randomTwo = (String) stringVector.get(1);
        this.m_proposal = (String) stringVector.get(2);
    }

    /**
     * Extracts the hash value from given random number and hash value
     * concatenation.
     * 
     * @param a_concat
     *            Random number and hash value concatenation
     * @return The hash value.
     * @throws InvalidAttributesException
     */
    public static synchronized String extractHashFromHashAndRandomOneConcatenation(String a_concat)
            throws InvalidAttributesException
    {
        Vector stringVector = deconcatenate(a_concat);
        if (stringVector.size() != 2)
        {
            throw new InvalidAttributesException("Size of Vector mustbe 2 But is "
                    + stringVector.size());
        }
        return (String) stringVector.get(0);
    }

    /**
     * Extracts the first random number from given random number and hash value
     * concatenation.
     * 
     * @param a_concat
     *            Random number and hash value concatenation
     * @return Random number one.
     * @throws InvalidAttributesException
     */
    public static synchronized String extractRandomOneFromHashAndRandomOneConcatenation(
            String a_concat) throws InvalidAttributesException
    {
        Vector stringVector = deconcatenate(a_concat);
        if (stringVector.size() != 2)
        {
            throw new InvalidAttributesException("Size of Vector mustbe 2 But is "
                    + stringVector.size());
        }
        return (String) stringVector.get(1);
    }

    /**
     * @todo This deconcat algorithm has leaks and should substitute by a better
     * one
     * 
     * Deconcatenate a given string in substrings and put them into a vector.
     * 
     * @param a_string
     *            The string to deconcatenate.
     * @return The result vector with substrings.
     */
    private static Vector deconcatenate(String a_string)
    {
        Vector result = new Vector();
        String str = new String();
        char m_trenner = '#';
        char c;
        for (int i = 0; i < a_string.length(); i++)
        {
            c = a_string.charAt(i);
            if (c == m_trenner && i < a_string.length() - 1 && a_string.charAt(i + 1) == m_trenner)
            {
                str += String.valueOf(c);
                i++;
                continue;
            }
            if (c == m_trenner)
            {
                result.add(str);
                str = new String();
                continue;
            }
            str += String.valueOf(c);
        }
        return result;
    }

    /**
     * Creates a string concatenation using all existing variables seperated by
     * <code>m_separatorCharacter</code>.
     * 
     * @return The string concatenation.
     */
    public String getConcatenation()
    {
        Vector stringVector = new Vector(3);
        stringVector.add(this.m_randomOne);
        stringVector.add(this.m_randomTwo);
        stringVector.add(this.m_proposal);
        return concat(stringVector);
    }

    /**
     * Creates a unique hash code.
     * 
     * @return The hash code.
     */
    public String getHashCode()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(this.m_randomOne);
        buf.append(this.m_randomTwo);
        buf.append(this.m_proposal);
        SHA1Digest digest = new SHA1Digest();
        byte[] proposalBytes = buf.toString().getBytes();
        digest.update(proposalBytes, 0, proposalBytes.length);
        byte[] tmp = new byte[digest.getDigestSize()];
        digest.doFinal(tmp, 0);
        return Base64.encode(tmp, false);
    }

    /**
     * Creates a string concatenation using the hash code an the first random
     * number seperated by <code>m_separatorCharacter</code>.
     * 
     * @return The string concatenation.
     */
    public String getHashValueAndRandomOne()
    {
        Vector stringVector = new Vector(2);
        stringVector.add(this.getHashCode());
        stringVector.add(this.m_randomOne);
        return concat(stringVector);
    }

    /**
     * Get the message itself e.g. "Hello".
     * 
     * @return The message.
     */
    public String getProposal()
    {
        return m_proposal;
    }

    /**
     * Gets the first random value according to Bruce Schneiers commitment
     * scheme.
     * 
     * @return The first random value.
     */
    public String getRandomOne()
    {
        return m_randomOne;
    }

    /**
     * Gets the second random value according to Bruce Schneiers commitment
     * scheme.
     * 
     * @return THe second random value.
     */
    public String getRandomTwo()
    {
        return m_randomTwo;
    }

    /**
     * Gets the character used as seperator for string concatenation.
     * 
     * @return The seperator.
     */
    public char getSeparator()
    {
        return m_separatorCharacter;
    }

    /**
     * Creates a string representation of this object.
     * 
     * @return The string representation.
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("R1: " + this.m_randomOne + "\n");
        buf.append("R2: " + this.m_randomTwo + "\n");
        buf.append("Proposal: " + this.m_proposal + "\n");
        buf.append("Hash: " + this.getHashCode() + "\n");
        return buf.toString();
    }

    /**
     * @todo This concat algorithm has leaks and should substitute by a better
     * one
     * 
     * Concatenates a set of strings given in a vector using
     * <code>m_separatorCharacter</code>.
     * 
     * @param a_strArray
     *            Vector with strings.
     * @return The concatenation.
     */
    private String concat(Vector a_strArray)
    {
        String result = "";
        String tmp = "";
        char c;
        // prepare it
        Enumeration en = a_strArray.elements();
        while (en.hasMoreElements())
        {
            tmp = (String) en.nextElement();
            for (int i = 0; i < tmp.length(); i++)
            {
                c = tmp.charAt(i);
                if (c == this.m_separatorCharacter)
                {
                    result += String.valueOf(c) + String.valueOf(c);
                }
                else
                {
                    result += String.valueOf(c);
                }
            }
            result += String.valueOf(this.m_separatorCharacter);
        }
        return result;
    }

    /**
     * @todo This deconcat algorithm has leaks and should substitute by a better
     * one
     * 
     * Deconcatenate a given string in substrings and put them into a vector.
     * 
     * @param a_string
     *            The string to deconcatenate.
     * @return The result vector with substrings.
     */
    private Vector deconcat(String a_string)
    {
        Vector result = new Vector();
        String str = new String();
        char c;
        for (int i = 0; i < a_string.length(); i++)
        {
            c = a_string.charAt(i);
            if (c == this.m_separatorCharacter && i < a_string.length() - 1
                    && a_string.charAt(i + 1) == this.m_separatorCharacter)
            {
                str += String.valueOf(c);
                i++;
                continue;
            }
            if (c == this.m_separatorCharacter)
            {
                result.add(str);
                str = new String();
                continue;
            }
            str += String.valueOf(c);
        }
        return result;
    }

}
