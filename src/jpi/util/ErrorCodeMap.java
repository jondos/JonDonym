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
package jpi.util;
import java.util.HashMap;

/**
 * Klasse die ein Mapping Http-Fehler-Code zu Fehlertext realisiert.
 *
 * @author Andreas Mueller
 */
public class ErrorCodeMap
{
    static private ErrorCodeMap errorCodeMap=null;
    static private HashMap m_map;
    private ErrorCodeMap()
    {
        m_map = new HashMap();
        m_map.put(new Integer(200),"OK");
        m_map.put(new Integer(411),"Length Required");
        m_map.put(new Integer(400),"Bad Request");
        m_map.put(new Integer(404),"Not Found");
        m_map.put(new Integer(409),"Conflict");
        m_map.put(new Integer(413),"Entity To Long");
        m_map.put(new Integer(500),"Internal Server Error");
        m_map.put(new Integer(505),"HTTP Version Not Supported");
    }
    public static ErrorCodeMap getInstance()
    {
        if(errorCodeMap==null)
            errorCodeMap= new ErrorCodeMap();
        return errorCodeMap;
    }
    /**
     * Liefert die Fehlerbeschriebung zum Fehlercode.
     *
     * @param Code Fehlercode
     * @return Fehlertext
     */
    public static String getDescription(int Code)
    {
        String description;
        if ((description=(String)m_map.get(new Integer(Code)))==null)
            description="Unkown";
        return description;

    }
}
