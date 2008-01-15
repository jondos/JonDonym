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

import java.awt.image.ColorModel;
import java.awt.image.ImageConsumer;
import java.awt.image.ImageProducer;
import java.util.Hashtable;

/**
 * This class grabs the pixels of an image from an ImageProducer.
 */
public class RgbMemoryImageConsumer implements ImageConsumer {

  /**
   * Stores the pixels of the image in the RGB format.
   */
  private int[] m_rgbPixels;

  /**
   * Stores the ImageProducer, which produces the image data.
   */
  private ImageProducer m_imageProducer;

  /**
   * Stores the width of the produced image.
   */
  private int m_width;

  /**
   * Stores the height of the produced image.
   */
  private int m_height;

  /**
   * This Object is needed for internal synchronization.
   */
  private Object m_productionLock;

  /**
   * Stores whether the image production is ready. This is needed for internal synchronization.
   */
  private boolean m_productionComplete;


  /**
   * Creates a ne RgbMemoryImageConsumer. This method blocks until the image is produced by the
   * ImageProducer.
   *
   * @param a_imageProducer The ImageProducer which produces the image.
   */
  public RgbMemoryImageConsumer(ImageProducer a_imageProducer) {
    m_imageProducer = a_imageProducer;
    m_width = 0;
    m_height = 0;
    m_rgbPixels = new int[0];
    m_productionLock = new Object();
    m_productionComplete = false;
    m_imageProducer.startProduction(this);
    synchronized (m_productionLock) {
      if (m_productionComplete == false) {
        try {
          m_productionLock.wait();
        }
        catch (Exception e) {
        }
      }
    }
  }

  /**
   * This method is called by the ImageProducer, if the image production is ready. Nobody else
   * than the ImageProducer should call this method.
   *
   * @param a_status The error code of the image production.
   */
  public void imageComplete(int a_status) {
    m_imageProducer.removeConsumer(this);
    synchronized (m_productionLock) {
      m_productionComplete = true;
      m_productionLock.notify();
    }
  }

  /**
   * This method is only for compatibility with the ImageConsumer interface.
   *
   * @param a_colorModel The default color model of the produced pixels.
   */
  public void setColorModel(ColorModel a_colorModel) {
  }

  /**
   * This method sets the dimensions of the produced image. Only the ImageProducer should call
   * this method.
   *
   * @param a_width The width of the produced image.
   * @param a_height The height of the produced image.
   */
  public void setDimensions(int a_width, int a_height) {
    m_width = a_width;
    m_height = a_height;
    m_rgbPixels = new int[m_width * m_height];
  }

  /**
   * This method is only for compatibility with the ImageConsumer interface.
   *
   * @param a_hints Some hints about the pixel transfer between ImageProducer and ImageConsumer.
   */
  public void setHints(int a_hints) {
  }

  /**
   * This method transfers the pixels between ImageProducer and ImageConsumer. Only the
   * ImageProducer should call this method.
   *
   * @param a_posX The X position of the transfer rectangle.
   * @param a_posY The Y position of the transfer rectangle.
   * @param a_width The width of the transfer rectangle.
   * @param a_height The height of the transfer rectangle.
   * @param a_pixels The pixel data of the transfer rectangle.
   * @param a_offset The offset in the pixel data array.
   * @param a_scansize The length of each line in the pixel data array.
   */
  public void setPixels(int a_posX, int a_posY, int a_width, int a_height, ColorModel a_colorModel, int[] a_pixels, int a_offset, int a_scansize) {
    for (int y = 0; y < a_height; y++) {
      for (int x = 0; x < a_width; x++) {
        m_rgbPixels[((y + a_posY) * m_width) + (x + a_posX)] = a_colorModel.getRGB(a_pixels[(y * a_scansize) + x + a_offset]);
      }
    }
  }

  /**
   * This method transfers the pixels between ImageProducer and ImageConsumer. Only the
   * ImageProducer should call this method.
   *
   * @param a_posX The X position of the transfer rectangle.
   * @param a_posY The Y position of the transfer rectangle.
   * @param a_width The width of the transfer rectangle.
   * @param a_height The height of the transfer rectangle.
   * @param a_pixels The pixel data of the transfer rectangle.
   * @param a_offset The offset in the pixel data array.
   * @param a_scansize The length of each line in the pixel data array.
   */
  public void setPixels(int a_posX, int a_posY, int a_width, int a_height, ColorModel a_colorModel, byte[] a_pixels, int a_offset, int a_scansize) {
    for (int y = 0; y < a_height; y++) {
      for (int x = 0; x < a_width; x++) {
        m_rgbPixels[((y + a_posY) * m_width) + (x + a_posX)] = a_colorModel.getRGB((int)(a_pixels[(y * a_scansize) + x + a_offset]));
      }
    }
  }

  /**
   * This method is only for compatibility with the ImageConsumer interface.
   *
   * @param a_properties Some properties for the produced image.
   */
  public void setProperties(Hashtable a_properties) {
  }

  /**
   * Returns the width of the produced image.
   *
   * @return The width of the produced image.
   */
  public int getWidth() {
    return m_width;
  }

  /**
   * Returns the height of the produced image.
   *
   * @return The height of the produced image.
   */
  public int getHeight() {
    return m_height;
  }

  /**
   * Returns a copy of the pixel data of the produced image in the default RGB color model.
   *
   * @return The pixel data in the default RGB color model.
   */
  public int[] getRgbPixels() {
    int[] pixelData = new int[m_rgbPixels.length];
    System.arraycopy(m_rgbPixels, 0, pixelData, 0, m_rgbPixels.length);
    return pixelData;
  }

}
