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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;

/**
 * This class is used to build new dynamic cascades. The selection which mix
 * will be in which cascade is done via the selected
 * <code>IDynamicCascadeBuildingStrategy</code>. Existing cascades for the
 * mixes which were assigned new cascades will be removed from the database
 * 
 * @author LERNGRUPPE
 */
public class DynamicCascadeConfigurator
{
    /**
     * This method gets called to build new cascades. The given seed is used as
     * the seed for the Pseudorandomnumbergenerator such that all InfoServices
     * can get the same resulting cascades (given they have the same seed which
     * is ensured at another place)
     * 
     * @param a_seed
     *            The seed for the PRNG
     */
    public void buildCascades(long a_seed)
    {
        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "Starting dynamic cascade configuration");
        Enumeration enumMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();

        Vector firstMixes = new Vector();
        Vector middleMixes = new Vector();
        Vector lastMixes = new Vector();

        while (enumMixes.hasMoreElements())
        {
            MixInfo mi = (MixInfo) enumMixes.nextElement();

            // Only use "usable" mixes
            if (!isUsable(mi))
                continue;

            switch (mi.getType())
            {
                case MixInfo.FIRST_MIX:
                    firstMixes.add(mi);
                    break;
                case MixInfo.MIDDLE_MIX:
                    middleMixes.add(mi);
                    break;
                case MixInfo.LAST_MIX:
                    lastMixes.add(mi);
                    break;
            }
        }

        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "FirstMixes: " + firstMixes.size()
                + ", MiddleMixes: " + middleMixes.size() + ", LastMixes: " + lastMixes.size());

        if ((firstMixes.size() == 0 && middleMixes.size() == 0) || lastMixes.size() == 0)
        {
            LogHolder.log(LogLevel.INFO, LogType.ALL,
                    "Not enough mixes to build new cascades, exiting");
            return;
        }

        Database.getInstance(VirtualCascade.class).removeAll();

        Vector newDynamicCascades = null;
        try
        {
            newDynamicCascades = DynamicConfiguration.getInstance().getCascadeBuildingStrategy()
                    .createCascades(firstMixes, middleMixes, lastMixes, a_seed);
        }
        catch (Exception e)
        {
            LogHolder.log(LogLevel.ERR, LogType.ALL, "Error while building new cascades: "
                    + e.toString());
            return;
        }

        Enumeration enumCascades = newDynamicCascades.elements();
        while (enumCascades.hasMoreElements())
        {
            MixCascade cascade = (MixCascade) enumCascades.nextElement();
            VirtualCascade tmp = new VirtualCascade(cascade);
            // Remove existing cascades containing one of the member-mixes
            // as they are terminated as soon as a mix calls reconfigure
            Enumeration enMixes = cascade.getMixIds().elements();
            boolean addTemporaryCascade = true;
            while (enMixes.hasMoreElements())
            {
                String mixId = enMixes.nextElement().toString();
                MixCascade oldCascade = getCurrentCascade(mixId);
                if (oldCascade != null)
                {
                    if (!cascade.getMixIDsAsString().equals(oldCascade.getMixIDsAsString()))
                    {
                        Database.getInstance(MixCascade.class).remove(oldCascade);
                    }
                    else
                    {
                        // We are lucky guys and picked the same cascade
                        // again...forget the temporary cascade
                        addTemporaryCascade = false;
                        break;
                    }
                }
            }
            if (addTemporaryCascade)
            {
                Database.getInstance(VirtualCascade.class).update(tmp);
            }
        }
        LogHolder.log(LogLevel.INFO, LogType.ALL, "Cascades  are ready!");
    }

    /**
     * Returns the current cascade for the mix with the given ID.
     * 
     * @param a_mixId
     *            The ID of the mix in question
     * @return The current <code>MixCascade</code> for the mix or
     *         <code>null</code>
     */
    private MixCascade getCurrentCascade(String a_mixId)
    {
        /* check whether the mix is already assigned to a mixcascade */
        Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList()
                .elements();
        MixCascade assignedCascade = null;
        while (knownMixCascades.hasMoreElements() && (assignedCascade == null))
        {
            MixCascade currentCascade = (MixCascade) (knownMixCascades.nextElement());
            if (currentCascade.getMixIds().contains(a_mixId))
            {
                /* the mix is assigned to that cascade */
                assignedCascade = currentCascade;
                break;
            }
        }
        return assignedCascade;
    }

    /**
     * Checks to see if the given <code>MixInfo</code> may be used in a
     * dynamic cascade A <code>MixInfo</code> is "usable" if a) it is a
     * dynamic mix b) it is not assigned to a semi-dynamic cascade
     * 
     * @param a_mixInfo
     *            The <code>MixInfo</code> to be tested
     * @return <code>true</code> if the mix may be used, <code>false</code>
     *         otherwise
     */
    private boolean isUsable(MixInfo a_mixInfo)
    {
        // If this mix is not dynamic, we can't use it
        if (!a_mixInfo.isDynamic())
            return false;

        // If this mix is in a cascade, we only can use it if the cascade is
        // completly dynamic and will be terminated anyway.
        MixCascade assignedCascade = getCurrentCascade(a_mixInfo.getId());
        if (assignedCascade != null)
        {
            Enumeration enumMixes = assignedCascade.getMixIds().elements();
            while (enumMixes.hasMoreElements())
            {
                String id = (String) enumMixes.nextElement();
                MixInfo mixInfo = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(id);
                if (mixInfo != null)
                {
                    if (!mixInfo.isDynamic())
                    {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
