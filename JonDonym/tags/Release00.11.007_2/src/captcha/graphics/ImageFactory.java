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
package captcha.graphics;

import java.awt.Image;
import java.awt.image.ImageProducer;
import java.lang.reflect.Field;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
/**
 * This class creates Images using the BufferedImage implementation from Java 1.2 and above. This
 * class can be compiled also within Java 1.1 but cannot be used there (because BufferedImage is
 * not available within Java 1.1).
 */
public class ImageFactory {

  /**
   * Constructs a new Image (BufferedImage implementation) with the default color model (ARGB). If
   * Java < 1.2 (no BufferedImage available) is used, null is returned.
   *
   * @param a_width The width of the new Image.
   * @param a_height The height of the new Image.
   *
   * @return The created Image (BufferedImage implementation) or null, if there was an error while
   *         creating the new Image (e.g. Java < 1.2 is used).
   */
  public static Image createImage(int a_width, int a_height) {
    Image returnImage = null;
    try {
      /* get the BufferedImage class */
      Class bufferedImageClass = Class.forName("java.awt.image.BufferedImage");
      /* create the types of the parameters needed by the BufferedImage constructor */
      Class[] bufferedImageConstructorParamTypes = new Class[3];
      bufferedImageConstructorParamTypes[0] = int.class;
      bufferedImageConstructorParamTypes[1] = int.class;
      bufferedImageConstructorParamTypes[2] = int.class;
      /* get the correct image type value (default RGBA color model) */
      Field argbImageType = bufferedImageClass.getField("TYPE_INT_ARGB");
      /* initialize the parameters for the constructor */
      Object[] bufferedImageConstructorParams = new Object[3];
      bufferedImageConstructorParams[0] = new Integer(a_width);
      bufferedImageConstructorParams[1] = new Integer(a_height);
      bufferedImageConstructorParams[2] = new Integer(argbImageType.getInt(bufferedImageClass));
      returnImage = (Image)(bufferedImageClass.getConstructor(bufferedImageConstructorParamTypes).newInstance(bufferedImageConstructorParams));
    }
    catch(Exception e) {
      LogHolder.log(LogLevel.EMERG, LogType.MISC, "Error while creating empty Image: " + e.toString());
    }
    return returnImage;
  }

  /**
   * Constructs a new Image (BufferedImage implementation) with the default color model (ARGB). If
   * Java < 1.2 (no BufferedImage available) is used, null is returned. The Image is filled with the
   * data obtained from the specified ImageProducer.
   *
   * @param a_imageProducer The producer, which produces the image data.
   *
   * @return The created Image (BufferedImage implementation) or null, if there was an error while
   *         creating the new Image (e.g. Java < 1.2 is used).
   */
  public static Image createImage(ImageProducer a_imageProducer) {
    Image returnImage = null;
    try {
      RgbMemoryImageConsumer memoryImage = new RgbMemoryImageConsumer(a_imageProducer);
      returnImage = createImage(memoryImage.getWidth(), memoryImage.getHeight());
      Class bufferedImageClass = Class.forName("java.awt.image.BufferedImage");
      Class[] bufferedImageSetRgbParamTypes = new Class[7];
      bufferedImageSetRgbParamTypes[0] = int.class;
      bufferedImageSetRgbParamTypes[1] = int.class;
      bufferedImageSetRgbParamTypes[2] = int.class;
      bufferedImageSetRgbParamTypes[3] = int.class;
      bufferedImageSetRgbParamTypes[4] = int[].class;
      bufferedImageSetRgbParamTypes[5] = int.class;
      bufferedImageSetRgbParamTypes[6] = int.class;
      Object[] setRgbParams = new Object[7];
      setRgbParams[0] = new Integer(0);
      setRgbParams[1] = new Integer(0);
      setRgbParams[2] = new Integer(memoryImage.getWidth());
      setRgbParams[3] = new Integer(memoryImage.getHeight());
      setRgbParams[4] = memoryImage.getRgbPixels();
      setRgbParams[5] = new Integer(0);
      setRgbParams[6] = new Integer(memoryImage.getWidth());
      bufferedImageClass.getMethod("setRGB", bufferedImageSetRgbParamTypes).invoke(returnImage, setRgbParams);
    }
    catch (Exception e) {
      LogHolder.log(LogLevel.EMERG, LogType.MISC, "Error while creating Image from ImageProducer: " + e.toString());
      returnImage = null;
    }
    return returnImage;
  }

}
