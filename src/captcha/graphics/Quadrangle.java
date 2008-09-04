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

/**
 * This class is the representation of an quadrangle (not necessarily a rectangle).
 */
public class Quadrangle {

  /**
   * Stores the 4 corners of the quadrangle (4 x 2 matrix).
   */
  private int[][] m_corners;


  /**
   * Creates a new quadrangle given by the 4 corners (x and y values). Attention: The order of the
   * corners can matter in some cases.
   *
   * @param a_x0 X position of the first corner.
   * @param a_y0 Y position of the first corner.
   * @param a_x1 X position of the second corner.
   * @param a_y1 Y position of the second corner.
   * @param a_x2 X position of the third corner.
   * @param a_y2 Y position of the third corner.
   * @param a_x3 X position of the fourtht corner.
   * @param a_y3 Y position of the fourth corner.
   */
  public Quadrangle(int a_x0, int a_y0, int a_x1, int a_y1, int a_x2, int a_y2, int a_x3, int a_y3) {
    m_corners = new int[4][2];
    m_corners[0][0] = a_x0;
    m_corners[0][1] = a_y0;
    m_corners[1][0] = a_x1;
    m_corners[1][1] = a_y1;
    m_corners[2][0] = a_x2;
    m_corners[2][1] = a_y2;
    m_corners[3][0] = a_x3;
    m_corners[3][1] = a_y3;
  }


  /**
   * Returns the X position of the first corner.
   *
   * @return The X position of the first corner.
   */
  public int getX0() {
    return m_corners[0][0];
  }

  /**
   * Returns the Y position of the first corner.
   *
   * @return The Y position of the first corner.
   */
  public int getY0() {
    return m_corners[0][1];
  }

  /**
   * Returns the X position of the second corner.
   *
   * @return The X position of the second corner.
   */
  public int getX1() {
    return m_corners[1][0];
  }

  /**
   * Returns the Y position of the second corner.
   *
   * @return The Y position of the second corner.
   */
  public int getY1() {
    return m_corners[1][1];
  }

  /**
   * Returns the X position of the third corner.
   *
   * @return The X position of the third corner.
   */
  public int getX2() {
    return m_corners[2][0];
  }

  /**
   * Returns the Y position of the third corner.
   *
   * @return The Y position of the third corner.
   */
  public int getY2() {
    return m_corners[2][1];
  }

  /**
   * Returns the X position of the fourth corner.
   *
   * @return The X position of the fourth corner.
   */
  public int getX3() {
    return m_corners[3][0];
  }

  /**
   * Returns the Y position of the fourth corner.
   *
   * @return The Y position of the fourth corner.
   */
  public int getY3() {
    return m_corners[3][1];
  }

}
