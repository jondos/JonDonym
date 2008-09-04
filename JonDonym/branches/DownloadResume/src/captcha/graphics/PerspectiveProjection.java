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
import java.awt.image.MemoryImageSource;
import java.awt.image.PixelGrabber;

import Jama.Matrix;



/**
 * This class is the implementation for a perspective projection between two images of the same
 * size (the areas of projection can be chosen within the constructor).
 */
public class PerspectiveProjection {

  /**
   * Stores the projection matrix needed for the trnasformation of the image pixels.
   */
  private Matrix m_projectionMatrix;

  /**
   * Creates a new instance of PerspectiveProjection.
   *
   * @param a_orig A quadrangle (needs not to be a rectangle) of coordinates in the original. All
   *               points within that quadrangle are projected within the image quadrangle.
   *
   * @param a_image The image quadrangle (needs not to be a rectangle). All pixels within area
   *                of the original quadrangle are projected within that image quadrangle (in a
   *                new created Image with the same size as the original Image).
   */
  public PerspectiveProjection(Quadrangle a_orig, Quadrangle a_image) {
    /* we have to calculate the transformation matrix M which fulfills the following equations:
     *
     * | m00 m01 m02 |   |     1     |        |      1     |
     * | m10 m11 m12 | * | a_orig.x0 | = p0 * | a_image.x0 |
     * | m20 m21 m22 |   | a_orig.y0 |        | a_image.y0 |
     *
     * | m00 m01 m02 |   |     1     |        |      1     |
     * | m10 m11 m12 | * | a_orig.x1 | = p1 * | a_image.x1 |
     * | m20 m21 m22 |   | a_orig.y1 |        | a_image.y1 |
     *
     * | m00 m01 m02 |   |     1     |        |      1     |
     * | m10 m11 m12 | * | a_orig.x2 | = p2 * | a_image.x2 |
     * | m20 m21 m22 |   | a_orig.y2 |        | a_image.y2 |
     *
     * | m00 m01 m02 |   |     1     |        |      1     |
     * | m10 m11 m12 | * | a_orig.x3 | = p3 * | a_image.x3 |
     * | m20 m21 m22 |   | a_orig.y3 |        | a_image.y3 |
     *
     * Because everything is calculated using homogeneous coordinates, p0, p1, p2 and p3 are
     * relative to each other, so we can choose one of them as a fixed value -> we choose p0 = 1.
     *
     * Solve the equations to get m00, ..., m22 and p1, p2, p3, which means that we have 12
     * variables and 12 equations (12x12 matrix).
     *
     * First create the 12x12 coefficient matrix for that equations. The columns are:
     * m00 m01 m02 m10 m11 m12 m20 m21 m22 p1 p2 p3
     *
     * Also create the vector of absolute values (p0 = 1).
     */
    double[][] coefficients = new double[12][12];
    for (int i = 0; i < 12; i++) {
      for (int j = 0; j < 12; j++) {
        coefficients[i][j] = (double)0;
      }
    }
    double[][] absolute = new double[12][1];
    for (int i = 0; i < 12; i++) {
      absolute[i][0] = (double)0;
    }
    coefficients[0] [0]  = (double)1;
    coefficients[0] [1]  = (double)(a_orig.getX0());
    coefficients[0] [2]  = (double)(a_orig.getY0());
    absolute    [0] [0]  = (double)1;
    coefficients[1] [3]  = (double)1;
    coefficients[1] [4]  = (double)(a_orig.getX0());
    coefficients[1] [5]  = (double)(a_orig.getY0());
    absolute    [1] [0]   = (double)(a_image.getX0());
    coefficients[2] [6]  = (double)1;
    coefficients[2] [7]  = (double)(a_orig.getX0());
    coefficients[2] [8]  = (double)(a_orig.getY0());
    absolute    [2] [0]  = (double)(a_image.getY0());
    coefficients[3] [0]  = (double)1;
    coefficients[3] [1]  = (double)(a_orig.getX1());
    coefficients[3] [2]  = (double)(a_orig.getY1());
    coefficients[3] [9]  = (double)-1;
    coefficients[4] [3]  = (double)1;
    coefficients[4] [4]  = (double)(a_orig.getX1());
    coefficients[4] [5]  = (double)(a_orig.getY1());
    coefficients[4] [9]  = (double)(-a_image.getX1());
    coefficients[5] [6]  = (double)1;
    coefficients[5] [7]  = (double)(a_orig.getX1());
    coefficients[5] [8]  = (double)(a_orig.getY1());
    coefficients[5] [9]  = (double)(-a_image.getY1());

    coefficients[6] [0]  = (double)1;
    coefficients[6] [1]  = (double)(a_orig.getX2());
    coefficients[6] [2]  = (double)(a_orig.getY2());
    coefficients[6] [10] = (double)-1;
    coefficients[7] [3]  = (double)1;
    coefficients[7] [4]  = (double)(a_orig.getX2());
    coefficients[7] [5]  = (double)(a_orig.getY2());
    coefficients[7] [10] = (double)(-a_image.getX2());
    coefficients[8] [6]  = (double)1;
    coefficients[8] [7]  = (double)(a_orig.getX2());
    coefficients[8] [8]  = (double)(a_orig.getY2());
    coefficients[8] [10] = (double)(-a_image.getY2());

    coefficients[9] [0]  = (double)1;
    coefficients[9] [1]  = (double)(a_orig.getX3());
    coefficients[9] [2]  = (double)(a_orig.getY3());
    coefficients[9] [11] = (double)-1;
    coefficients[10][3]  = (double)1;
    coefficients[10][4]  = (double)(a_orig.getX3());
    coefficients[10][5]  = (double)(a_orig.getY3());
    coefficients[10][11] = (double)(-a_image.getX3());
    coefficients[11][6]  = (double)1;
    coefficients[11][7]  = (double)(a_orig.getX3());
    coefficients[11][8]  = (double)(a_orig.getY3());
    coefficients[11][11] = (double)(-a_image.getY3());

    /* now solve the equations by calculating the inverse matrix for the coefficients and
     * multiply it with the absolute values
     */
    Matrix solution = (new Matrix(coefficients)).solve(new Matrix(absolute));

    /* rows 0 .. 8 are the values m00, m01, m02, m10, m11, m12, m20, m21, m22 we need for the
     * projection matrix
     */
    double[][] projectionMatrix = new double[3][3];
    projectionMatrix[0][0] = solution.get(0, 0);
    projectionMatrix[0][1] = solution.get(1, 0);
    projectionMatrix[0][2] = solution.get(2, 0);
    projectionMatrix[1][0] = solution.get(3, 0);
    projectionMatrix[1][1] = solution.get(4, 0);
    projectionMatrix[1][2] = solution.get(5, 0);
    projectionMatrix[2][0] = solution.get(6, 0);
    projectionMatrix[2][1] = solution.get(7, 0);
    projectionMatrix[2][2] = solution.get(8, 0);

    m_projectionMatrix = new Matrix(projectionMatrix);
  }

  /**
   * Performs the projection between original and image as configured in the constructor. The
   * image of the projection is a new Image with the same size as the original Image. This method
   * should not throw an Exception, but it's possible.
   *
   * @param a_input The original Image.
   * @param a_defaultColor The default color used for every pixel in the image of the projection,
   *                       which is not the image of a projected original pixel.
   *
   * @return The image of the projection, which has the same size as the original.
   */
  public Image transform(Image a_input, Color a_defaultColor) throws Exception {
    int height = a_input.getHeight(null);
    int width = a_input.getWidth(null);

    /* create the matrix with the original coordinates (homogenous format) */
    double[][] coordinates = new double [3][height*width];
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        coordinates[0][i * width + j] = 1;
        coordinates[1][i * width + j] = j;
        coordinates[2][i * width + j] = i;
      }
    }
    Matrix imageCoordinates = m_projectionMatrix.times(new Matrix(coordinates));

    /* normalize the image coordinates */
    double[][] normalImageCoordinates = new double[2][width * height];
    for (int i = 0; i < width * height; i++) {
      normalImageCoordinates[0][i] = imageCoordinates.get(1, i) / imageCoordinates.get(0, i);
      normalImageCoordinates[1][i] = imageCoordinates.get(2, i) / imageCoordinates.get(0, i);
    }

    /* now create the transformation map for the pixels, we take for every image point the best
     * matching original pixel
     */
    int[] imageMap = new int[width * height];
    double[] distanceMap = new double[width * height];
    for (int i = 0; i < width * height; i++) {
      imageMap[i] = -1;
    }
    for (int i = 0; i < width * height; i++) {
      double exactX = normalImageCoordinates[0][i];
      double exactY = normalImageCoordinates[1][i];
      int x = (int)Math.round(exactX);
      int y = (int)Math.round(exactY);
      if ((x >= 0) && (x < width) && (y >= 0) && (y < height)) {
        /* the image pixel is within the destination image -> calculate the distance from the
         * next pixel
         */
        double distance = Math.sqrt(((exactX - (double)x) * (exactX - (double)x)) + ((exactY - (double)y) * (exactY - (double)y)));
        if (imageMap[y * width + x] == -1) {
          /* no souce pixel has clamed that image point yet */
          imageMap[y * width + x] = i;
          distanceMap[y * width + x] = distance;
        }
        else {
          /* already one pixel there, check if we are closer to discrete position */
          if (distance < distanceMap[y * width + x]) {
            /* we are closer */
            imageMap[y * width + x] = i;
            distanceMap[y * width + x] = distance;
          }
        }
      }
    }
    /* now we have the transformation pixel mapping -> transform the image */
    PixelGrabber originalImage = new PixelGrabber(a_input, 0, 0, width, height, true);
    originalImage.grabPixels();
    int[] originalImagePixels = (int[])(originalImage.getPixels());
    int[] outputImagePixels = new int[width * height];
    for (int i = 0; i < width * height; i++) {
      if (imageMap[i] > -1) {
        /* get the specified pixel from the original image */
        outputImagePixels[i] = originalImagePixels[imageMap[i]];
      }
      else {
        /* this pixel has no mapping, use the default color */
        outputImagePixels[i] = a_defaultColor.getRGB();
      }
    }
    MemoryImageSource outputImage = new MemoryImageSource(width, height, outputImagePixels, 0, width);

    return ImageFactory.createImage(outputImage);
  }
}
