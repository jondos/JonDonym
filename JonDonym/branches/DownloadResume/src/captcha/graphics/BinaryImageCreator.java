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

import java.awt.Color;
import java.awt.Image;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * This is a helper class for creating binary images.
 */
public class BinaryImageCreator {

  /**
   * Transforms the specified image to our own binary image format. Every pixel, which is not
   * 100% white, is transformed to black.
   *
   * @param a_image The source image. The image must be 100% in memory already.
   *
   * @return Our own binary image format. If there was something wrong with the input, null is
   *         returned.
   */
  public static byte[] imageToBinary(Image a_image) {
    byte[] resultData = null;
    try {
      /* get the data from the original image */
      int width = a_image.getWidth(null);
      int height = a_image.getHeight(null);
      PixelGrabber originalImage = new PixelGrabber(a_image, 0, 0, width, height, true);
      originalImage.grabPixels();
      int[] originalImagePixels = (int[])(originalImage.getPixels());
      ByteArrayOutputStream binaryData = new ByteArrayOutputStream();
      DataOutputStream transformer = new DataOutputStream(binaryData);
      /* write the size information */
      transformer.writeInt(width);
      transformer.writeInt(height);
      transformer.flush();
      /* write the binary image map */
      int currentByte = 0;
      for (int i = 0; i < width * height; i++) {
        int currentPixel = originalImagePixels[i];
        currentByte = currentByte << 1;
        if (currentPixel != Color.white.getRGB()) {
          /* we write every non-white color as black */
          currentByte = currentByte + 1;
        }
        if ((i % 8) == 7) {
          /* we have one full byte */
          binaryData.write(currentByte);
          currentByte = 0;
        }
      }
      if ((width * height) % 8 != 0) {
        /* we have to pad the last byte and write it to the output */
        currentByte = currentByte << (8 - ((width * height) % 8));
        binaryData.write(currentByte);
      }
      binaryData.flush();
      resultData = binaryData.toByteArray();
    }
    catch (Exception e) {
      /* should not happen */
    }
    return resultData;
  }

}
