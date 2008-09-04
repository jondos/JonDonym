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
package forward.server;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.infoservice.MixCascade;

/**
 * This class stores all MixCascades, where messages from blockees can be forwarded to.
 */
public class ForwardCascadeDatabase {
 
  /**
   * Stores all allowed MixCascades, where messages can be forwarded to.
   */
  private Hashtable m_allowedCascades;
  

  /**
   * This creates a new instance of a ForwardCascadeDatabase.
   */
  public ForwardCascadeDatabase() {
    m_allowedCascades = new Hashtable();
  }
   
    
  /**
   * Returns the MixCascade with the specified id, if it is in the database. If the database
   * doesn't contain a MixCascade with that id, null is returned.
   *
   * @param a_id The id of the demanded MixCascade.
   *
   * @return The MixCascade with the given id or null, if there is no MixCascade with that id
   *         in the database.
   */
  public MixCascade getMixCascadeById(String a_id) {
    return (MixCascade)(m_allowedCascades.get(a_id));
  }

  /**
   * Returns an XML representation of this database (AllowedCascades node). This node contains
   * a list of MixCascade nodes.
   *
   * @param doc The XML document, which is the environment for the created XML node.
   *
   * @return The AllowedCascades XML node.
   */
  public Element toXmlNode(Document doc) {
    Element allowedCascadesNode = doc.createElement("AllowedCascades");
    synchronized (m_allowedCascades) {
      /* get always consisten values */
      Enumeration cascades = m_allowedCascades.elements();
      while (cascades.hasMoreElements()) {
        allowedCascadesNode.appendChild(((MixCascade)(cascades.nextElement())).toXmlElement(doc));
      }
    }
    return allowedCascadesNode;
  }
  
  /**
   * Returns a snapshot of all values in the database of allowed cascades.
   *
   * @return A Vector with all values which are stored in the allowed cascades database.
   */
  public Vector getEntryList()
  {
    Vector entryList = new Vector();
    synchronized (m_allowedCascades)
    {
      /* get the actual values */
      Enumeration allowedCascadesElements = m_allowedCascades.elements();
      while (allowedCascadesElements.hasMoreElements())
      {
        entryList.addElement(allowedCascadesElements.nextElement());
      }
    }
    return entryList;
  }
  
  /**
   * Adds a mixcascade to the list of allowed mixcascades for forwarding. If there is already a
   * mixcascade with the same ID in the database, the old entry is replaced by this new one.
   *
   * @param a_cascade The mixcascade to add.
   */
  public void addCascade(MixCascade a_cascade) {
    synchronized (m_allowedCascades) {
      if (m_allowedCascades.containsKey(a_cascade.getId()) == false) {
        /* only log, if there was a new entry added and not an old one updated */
        LogHolder.log(LogLevel.INFO, LogType.MISC, "ForwardCascadeDatabase: addCascade: The mixcascade " + a_cascade.getName() + " was added to the list of useable cascades for the clients.");
      }
      m_allowedCascades.put(a_cascade.getId(), a_cascade);
    }
  }
  
  /**
   * Removes the mixcascade with the given id from the list of allowed cascades.
   *
   * @param a_id The id of the mixcascade to remove.
   */
  public void removeCascade(String a_id) {
    synchronized (m_allowedCascades) {
      MixCascade cascadeToRemove = (MixCascade)(m_allowedCascades.get(a_id));
      if (cascadeToRemove != null) {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "ForwardCascadeDatabase: removeCascade: The mixcascade " + cascadeToRemove.getName() + " was removed from the list of useable cascades for the clients.");
      }
      m_allowedCascades.remove(a_id);
    }
  }
  
  /**
   * Removes all mixcascades from the database of allowed cascades. So access to the cascades is
   * forbidden for all new client connections.
   */
  public void removeAllCascades() {
    synchronized (m_allowedCascades) {
      LogHolder.log(LogLevel.INFO, LogType.MISC, "ForwardCascadeDatabase: removeAllCascades: All mixcascades were removed from the list of useable cascades for the clients.");      
      m_allowedCascades.clear();
    }
  }
  
} 