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
package infoservice.dynamic;

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;

/**
 * Superclass for implementations of
 * <code>IDynamicCascadeBuildingStrategy</code> Defines the convenient method
 * <code>buildCascade</code> which builds <code>MixCascade</code> objects
 * from a <code>Vector</code> of <code>MixInfo</code>
 * 
 * @author LERNGRUPPE
 */
public abstract class ADynamicCascadeBuildingStrategy implements IDynamicCascadeBuildingStrategy
{

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.dynamic.IDynamicCascadeBuildingStrategy#createCascades(java.util.Vector,
     *      java.util.Vector, java.util.Vector, long)
     */
    public abstract Vector createCascades(Vector firstMixes, Vector middleMixes, Vector lastMixes,
            long seed) throws Exception;

    /**
     * Builds a <code>MixCascade</code> object using the <code>MixInfo</code>
     * objects in the given <code>Vector</code>.
     * 
     * @param a_mixInfos
     *            The <code>MixInfo</code>s for the mixes assigned to the
     *            cascade
     * @return The <code>MixCascade</code> representing the new cascade
     * @throws Exception
     */
    protected MixCascade buildCascade(Vector a_mixInfos) throws Exception
    {
        MixInfo mixInfo = ((MixInfo) a_mixInfos.firstElement());
        Element mix = mixInfo.getXmlStructure();
        NodeList listenerInterfacesNodes = mix.getElementsByTagName("ListenerInterfaces");
        if (listenerInterfacesNodes.getLength() == 0)
        {
            throw (new Exception("First Mix has no ListenerInterfaces in its XML structure."));
        }
        Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
        NodeList listenerInterfaceNodes = listenerInterfacesNode
                .getElementsByTagName("ListenerInterface");
        if (listenerInterfaceNodes.getLength() == 0)
        {
            throw (new Exception("First Mix has no ListenerInterfaces in its XML structure."));
        }

        Vector listenerInterfaces = new Vector();
        for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
        {
            Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
            listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
        }
        Vector mixIds = new Vector();
        Enumeration enMixes = a_mixInfos.elements();
        while (enMixes.hasMoreElements())
        {
            MixInfo elem = (MixInfo) enMixes.nextElement();
            mixIds.add(elem.getId());
        }
        MixCascade result = new MixCascade(null, mixInfo.getId(), mixIds, listenerInterfaces);
        return result;
    }

}
