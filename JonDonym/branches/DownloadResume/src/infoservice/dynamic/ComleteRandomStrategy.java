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

import java.util.Random;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is an implementation of a <code>IDynamicCascadeBuildingStrategy</code>.
 * It creates new cascades purly at random using the seed previously agreed upon
 * by all other InfoServices.
 * 
 * @author ss1
 * 
 */
public class ComleteRandomStrategy extends ADynamicCascadeBuildingStrategy
{

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.dynamic.IDynamicCascadeBuildingStrategy#createCascades(java.util.Vector,
     *      java.util.Vector, java.util.Vector, long)
     */
    public Vector createCascades(Vector firstMixes, Vector middleMixes, Vector lastMixes, long seed)
            throws Exception
    {
        // We do not distinguish first and middle mixes
        Vector firstAndMiddleMixes = new Vector();
        firstAndMiddleMixes.addAll(firstMixes);
        firstAndMiddleMixes.addAll(middleMixes);
        return circuit(firstAndMiddleMixes, lastMixes, seed);
    }

    /**
     * Creates the mix cascades.
     * 
     * @param firstAndMiddleMixes
     *            Set of available first and middle mixes.
     * @param lastMixes
     *            Set of available last-mixes.
     * @param seedForRandomGenerator
     * @return Vector with three ones in it. Each one contains a quantity of
     *         cascades with equal length.
     * @throws Exception
     */
    public Vector circuit(Vector firstAndMiddleMixes, Vector lastMixes, long seedForRandomGenerator)
            throws Exception
    {
        if (firstAndMiddleMixes == null || firstAndMiddleMixes.size() == 0 || lastMixes == null
                || lastMixes.size() == 0)
            throw new Exception("Invalid parameters");

        // Are there enough Mixes available? If not -> return null.
        if (firstAndMiddleMixes.size() < DynamicConfiguration.getInstance().getMaxCascadeLength() - 1
                || lastMixes.size() == 0)
            throw new Exception("Not enough mixes to build cascades");

        /*
         * (1) Step one. In a first step we have to randomize the mix-lists
         */
        Random r = new Random(seedForRandomGenerator);
        firstAndMiddleMixes = randomizeVector(firstAndMiddleMixes, r);
        lastMixes = randomizeVector(lastMixes, r);

        /*
         * (2) Step two. Now we have to calculate the exact amount of each
         * quantity, but we can't wire the mixes at this step. For an optimal
         * security we have to curcuit all the longest cascades in one rush,
         * then the reduced by one cascades in one rush and so on.
         */
        final int ratioFirstQuantity = 3;
        final int ratioSecondQuantity = 1;
        final int ratioThirdQuantity = 1;
        int numberOfFirstAndMiddleMixes = firstAndMiddleMixes.size();
        int numberOfLastMixes = lastMixes.size();
        int amountOfQuantityOne = 0;
        int amountOfQuantityTwo = 0;
        int amountOfQuantityThree = 0;
        boolean thereAreEnoughMixesAvailable = true;

        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "Count of last mixes:  " + numberOfLastMixes);
        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "Count of middle and lfirst mixes: "
                + numberOfFirstAndMiddleMixes);
        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "Upper bound for length: "
                + DynamicConfiguration.getInstance().getMaxCascadeLength());
        LogHolder.log(LogLevel.DEBUG, LogType.ALL, "Lower bound for length: "
                + DynamicConfiguration.getInstance().getMinCascadeLength());

        while (thereAreEnoughMixesAvailable)
        {
            // At first the longest cascades
            for (int i = 0; i < ratioFirstQuantity; i++)
            {
                if (numberOfLastMixes - 1 >= 0
                        && numberOfFirstAndMiddleMixes
                                - DynamicConfiguration.getInstance().getMaxCascadeLength() + 1 >= 0)
                {
                    numberOfLastMixes--;
                    numberOfFirstAndMiddleMixes -= DynamicConfiguration.getInstance()
                            .getMaxCascadeLength() - 1;
                    amountOfQuantityOne++;
                }
                else
                {
                    thereAreEnoughMixesAvailable = false;
                    break;
                }
            }
            // //At second the Cascades which are reduced by one mix
            if (DynamicConfiguration.getInstance().getMaxCascadeLength() - 1 >= DynamicConfiguration
                    .getInstance().getMinCascadeLength())
                for (int i = 0; i < ratioSecondQuantity; i++)
                {
                    if (thereAreEnoughMixesAvailable
                            && numberOfLastMixes - 1 >= 0
                            && numberOfFirstAndMiddleMixes
                                    - DynamicConfiguration.getInstance().getMaxCascadeLength() + 2 >= 0)
                    {
                        numberOfLastMixes--;
                        numberOfFirstAndMiddleMixes -= DynamicConfiguration.getInstance()
                                .getMaxCascadeLength() - 2;
                        amountOfQuantityTwo++;
                    }
                    else
                    {
                        thereAreEnoughMixesAvailable = false;
                        break;
                    }
                }

            // At third the Cascades which are reduced by two mixes
            if (DynamicConfiguration.getInstance().getMaxCascadeLength() - 2 >= DynamicConfiguration
                    .getInstance().getMinCascadeLength())
                for (int i = 0; i < ratioThirdQuantity; i++)
                {
                    if (thereAreEnoughMixesAvailable
                            && numberOfLastMixes - 1 >= 0
                            && numberOfFirstAndMiddleMixes
                                    - DynamicConfiguration.getInstance().getMaxCascadeLength() + 3 >= 0)
                    {
                        numberOfLastMixes--;
                        numberOfFirstAndMiddleMixes -= DynamicConfiguration.getInstance()
                                .getMaxCascadeLength() - 3;
                        amountOfQuantityThree++;
                    }
                    else
                    {
                        thereAreEnoughMixesAvailable = false;
                        break;
                    }
                }
        }

        /*
         * (3) Step three. Now we we can build cascades
         */
        // Constants and variables
        Vector result = new Vector();
        int firstAndMiddleMixesIndex = 0;
        int lastMixesIndex = 0;

        for (int i = 0; i < amountOfQuantityOne; i++)
        {
            Vector cascArr = new Vector(DynamicConfiguration.getInstance().getMaxCascadeLength());
            for (int j = 1; j < DynamicConfiguration.getInstance().getMaxCascadeLength(); j++)
            {
                cascArr.addElement(firstAndMiddleMixes.get(firstAndMiddleMixesIndex));
                firstAndMiddleMixesIndex++;
            }
            cascArr.addElement(lastMixes.get(lastMixesIndex));
            lastMixesIndex++;
            result.addElement(buildCascade(cascArr));
        }

        for (int i = 0; i < amountOfQuantityTwo; i++)
        {
            Vector cascArr = new Vector(
                    DynamicConfiguration.getInstance().getMaxCascadeLength() - 1);
            for (int j = 1; j < DynamicConfiguration.getInstance().getMaxCascadeLength() - 1; j++)
            {
                cascArr.addElement(firstAndMiddleMixes.get(firstAndMiddleMixesIndex));
                firstAndMiddleMixesIndex++;
            }
            cascArr.addElement(lastMixes.get(lastMixesIndex));
            lastMixesIndex++;
            result.addElement(buildCascade(cascArr));
        }

        for (int i = 0; i < amountOfQuantityThree; i++)
        {
            // wire it
            Vector cascArr = new Vector(
                    DynamicConfiguration.getInstance().getMaxCascadeLength() - 2);
            for (int j = 1; j < DynamicConfiguration.getInstance().getMaxCascadeLength() - 2; j++)
            {
                cascArr.addElement(firstAndMiddleMixes.get(firstAndMiddleMixesIndex));
                firstAndMiddleMixesIndex++;
            }
            cascArr.addElement(lastMixes.get(lastMixesIndex));
            lastMixesIndex++;
            result.addElement(buildCascade(cascArr));
        }

        LogHolder.log(LogLevel.INFO, LogType.ALL, "Cascades of length "
                + DynamicConfiguration.getInstance().getMaxCascadeLength() + " : "
                + amountOfQuantityOne);
        LogHolder.log(LogLevel.INFO, LogType.ALL, "Cascades of length "
                + (DynamicConfiguration.getInstance().getMaxCascadeLength() - 1) + " : "
                + amountOfQuantityTwo);
        LogHolder.log(LogLevel.INFO, LogType.ALL, "Cascades of length "
                + (DynamicConfiguration.getInstance().getMaxCascadeLength() - 2) + " : "
                + amountOfQuantityThree);
        LogHolder.log(LogLevel.INFO, LogType.ALL, "Remaining last mixes:             "
                + numberOfLastMixes);
        LogHolder.log(LogLevel.INFO, LogType.ALL, "Remaining middle and first: "
                + numberOfFirstAndMiddleMixes);

        /**
         * @todo Build cascades of length one for the remaining mixes The mix
         *       will known that this means there is no new cascade for it but
         *       its old one has been terminated. This could be done more
         *       explicitly
         */
        for (; firstAndMiddleMixesIndex < firstAndMiddleMixes.size(); firstAndMiddleMixesIndex++)
        {
            Vector cascArr = new Vector();
            cascArr.add(firstAndMiddleMixes.get(firstAndMiddleMixesIndex));
            result.add(buildCascade(cascArr));
        }

        for (; lastMixesIndex < lastMixes.size(); lastMixesIndex++)
        {
            Vector cascArr = new Vector();
            cascArr.add(lastMixes.get(lastMixesIndex));
            result.add(buildCascade(cascArr));
        }

        // Enumeration k = result.elements();
        // System.err.println(new Date().toLocaleString() + " - BUILD " +
        // result.size() + " CASCADES USING " + seedForRandomGenerator +":");
        // while(k.hasMoreElements())
        // {
        // MixCascade c = (MixCascade) k.nextElement();
        // System.err.println("CASCADE: " + c.getId() + " HAS " +
        // c.getMixIds().size());
        // Enumeration h = c.getMixIds().elements();
        // int v = 1;
        // while(h.hasMoreElements()) {
        // System.err.println(v + ": " + h.nextElement().toString());
        // v++;
        // }
        // System.err.println("----------------------------------------------");
        // }
        // System.err.println
        // ("##############################################");
        return result;
    }

    /**
     * Randomizes the given <code>Vector</code>
     * 
     * @param a_vector
     * @param a_randArr
     * @return
     */
    private Vector randomizeIt(Vector a_vector, int[] a_randArr)
    {
        Vector result = new Vector(a_vector.size());

        // determine the position of the biggest number in the array
        int posOfBiggestValue = -1;
        int biggestValue = -1;
        for (int j = 0; j < a_randArr.length; j++)
        {
            posOfBiggestValue = -1;
            biggestValue = -1;
            for (int i = 0; i < a_randArr.length; i++)
            {
                if (a_randArr[i] > biggestValue)
                {
                    biggestValue = a_randArr[i];
                    posOfBiggestValue = i;
                }
            }
            result.addElement(a_vector.get(posOfBiggestValue));
            a_randArr[posOfBiggestValue] = -1;
        }
        return result;
    }

    /**
     * Randomizes the given <code>Vector</code> using the given
     * <code>Random</code>.
     * 
     * @param a_vector
     *            The <code>Vector</code> to be randomized
     * @param a_random
     *            The <code>Random</code> to use
     * @return The randomized <code>Vector</code>
     */
    private Vector randomizeVector(Vector a_vector, Random a_random)
    {
        int[] randArray = new int[a_vector.size()];
        for (int i = 0; i < randArray.length; i++)
        {
            randArray[i] = Math.abs(a_random.nextInt());
        }
        return randomizeIt(a_vector, randArray);
    }

}
