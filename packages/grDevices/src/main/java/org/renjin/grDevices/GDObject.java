/*

GDObject.java
Java graphics device

Created by Simon Urbanek on Thu Aug 05 2004.
Copyright (c) 2004-2009 Simon Urbanek. All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation;
version 2.1 of the License.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

*/

package org.renjin.grDevices;

import org.renjin.gcc.runtime.BytePtr;
import org.renjin.gcc.runtime.Ptr;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.*;

/**
 * GDObject is an arbitrary object that can be painted
 */
interface GDObject {
  void paint(Component c, GDState gs, Graphics g);
}

/**
 * object storing the current graphics state
 */
class GDState {
  private Color col;
  private Color fill;
  private Font font;

  public Color getCol() {
    return col;
  }

  public void setCol(Color col) {
    this.col = col;
  }

  public Color getFill() {
    return fill;
  }

  public void setFill(Color fill) {
    this.fill = fill;
  }

  public Font getFont() {
    return font;
  }

  public void setFont(Font font) {
    this.font = font;
  }
}

class GDLine implements GDObject {
  private double x1;
  private double y1;
  private double x2;
  private double y2;

  public GDLine(double x1, double y1, double x2, double y2) {
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    if (gs.getCol() != null) {
      g.drawLine((int) (x1 + 0.5), (int) (y1 + 0.5), (int) (x2 + 0.5), (int) (y2 + 0.5));
    }
  }
}

class GDRect implements GDObject {
  private double x1;
  private double y1;
  private double x2;
  private double y2;

  public GDRect(double x1, double y1, double x2, double y2) {
    double tmp;
    if (x1 > x2) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y1 > y2) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    int x = (int) (x1 + 0.5);
    int y = (int) (y1 + 0.5);
    int w = (int) (x2 + 0.5) - x;
    int h = (int) (y2 + 0.5) - y;
    if (gs.getFill() != null) {
      g.setColor(gs.getFill());
      g.fillRect(x, y, w + 1, h + 1);
      if (gs.getCol() != null) {
        g.setColor(gs.getCol());
      }
    }
    if (gs.getCol() != null) {
      g.drawRect(x, y, w, h);
    }
  }
}

class GDClip implements GDObject {
  private double x1;
  private double y1;
  private double x2;
  private double y2;

  public GDClip(double x1, double y1, double x2, double y2) {
    double tmp;
    if (x1 > x2) {
      tmp = x1;
      x1 = x2;
      x2 = tmp;
    }
    if (y1 > y2) {
      tmp = y1;
      y1 = y2;
      y2 = tmp;
    }
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    g.setClip((int) (x1 + 0.5), (int) (y1 + 0.5), (int) (x2 - x1 + 1.7), (int) (y2 - y1 + 1.7));
  }
}

class GDCircle implements GDObject {
  private double x;
  private double y;
  private double r;

  public GDCircle(double x, double y, double r) {
    this.x = x;
    this.y = y;
    this.r = r;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    if (gs.getFill() != null) {
      g.setColor(gs.getFill());
      g.fillOval((int) (x - r + 0.5), (int) (y - r + 0.5), (int) (r + r + 1.5), (int) (r + r + 1.5));
      if (gs.getCol() != null) {
        g.setColor(gs.getCol());
      }
    }
    if (gs.getCol() != null) {
      g.drawOval((int) (x - r + 0.5), (int) (y - r + 0.5), (int) (r + r + 1.5), (int) (r + r + 1.5));
    }
  }
}

class GDText implements GDObject {
  private double x;
  private double y;
  private double r;
  private double h;
  private String txt;

  public GDText(double x, double y, double r, double h, String txt) {
    this.x = x;
    this.y = y;
    this.r = r;
    this.h = h;
    this.txt = txt;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    if (gs.getCol() != null) {
      double rx = x;
      double ry = y;
      double hc = 0d;

      if (h != 0d) {
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(txt);
        hc = ((double) w) * h;
        rx = x - (((double) w) * h);
      }
      int ix = (int) (rx + 0.5);
      int iy = (int) (ry + 0.5);

      if (r != 0d) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(x, y);
        double rr = -r / 180d * Math.PI;
        g2d.rotate(rr);
        if (hc != 0d) {
          g2d.translate(-hc, 0d);
        }
        g2d.drawString(txt, 0, 0);
        if (hc != 0d) {
          g2d.translate(hc, 0d);
        }
        g2d.rotate(-rr);
        g2d.translate(-x, -y);
      } else {
        g.drawString(txt, ix, iy);
      }
    }
  }
}


class GDFont implements GDObject {

  private Font font;

  /**
   * this is to work around a bug in Java on Windows where the Symbol font is incorrectly mapped and
   * requires us to force another font for the Symbol characters. According to
   * Simon Urbanek, Mac OS X is fine with Symbol, Windows is not, so we'll fix this for Windows only
   */
  private static final boolean USE_SYMBOL_FONT = !System.getProperty("os.name", "").startsWith("Win");

  public GDFont(double cex, double ps, double lineheight, int face, String family) {
    int jFT = Font.PLAIN;
    if (face == 2) {
      jFT = Font.BOLD;
    }
    if (face == 3) {
      jFT = Font.ITALIC;
    }
    if (face == 4) {
      jFT = Font.BOLD | Font.ITALIC;
    }
    if (face == 5 && USE_SYMBOL_FONT) {
      family = "Symbol";
    }
    font = new Font(family.equals("") ? null : family, jFT, (int) (cex * ps + 0.5));
  }

  public Font getFont() {
    return font;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    g.setFont(font);
    gs.setFont(font);
  }
}

class GDPolygon implements GDObject {
  private int n;
  private int[] xi;
  private int[] yi;
  private boolean isPolyline;

  public GDPolygon(int n, Ptr x, Ptr y, boolean isPolyline) {
    this.n = n;
    this.isPolyline = isPolyline;
    int i = 0;
    xi = new int[n];
    yi = new int[n];
    while (i < n) {
      xi[i] = (int) (x.getAlignedDouble(i) + 0.5);
      yi[i] = (int) (y.getAlignedDouble(i) + 0.5);
      i++;
    }
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    if (gs.getFill() != null && !isPolyline) {
      g.setColor(gs.getFill());
      g.fillPolygon(xi, yi, n);
      if (gs.getCol() != null) {
        g.setColor(gs.getCol());
      }
    }
    if (gs.getCol() != null) {
      if (isPolyline) {
        g.drawPolyline(xi, yi, n);
      } else {
        g.drawPolygon(xi, yi, n);
      }
    }
  }
}

class GDPath implements GDObject {

  private GeneralPath path;

  public GDPath(int npoly, Ptr numberOfPointsPerPath, Ptr x, Ptr y, boolean winding) {

    path = new GeneralPath(winding ? GeneralPath.WIND_NON_ZERO : GeneralPath.WIND_EVEN_ODD,
        countPoints(npoly, numberOfPointsPerPath));
    int k = 0;
    int end = 0;
    for (int i = 0; i < npoly; i++) {
      end += numberOfPointsPerPath.getAlignedInt(i);
      path.moveTo((float) x.getAlignedDouble(k), (float) y.getAlignedDouble(k));
      k++;
      for (; k < end; k++) {
        path.lineTo((float) x.getAlignedDouble(k), (float) y.getAlignedDouble(k));
      }
      path.closePath();
    }
  }

  private int countPoints(int npoly, Ptr numberOfPointsPerPath) {
    int count = 0;
    for (int i = 0; i < npoly; i++) {
      count += numberOfPointsPerPath.getAlignedInt(i);
    }
    return count;
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    if (gs.getFill() != null) {
      g2.setColor(gs.getFill());
      g2.fill(path);
      if (gs.getCol() != null) {
        g2.setColor(gs.getCol());
      }
    }
    if (gs.getCol() != null) {
      g2.draw(path);
    }
  }
}

class GDColor implements GDObject {
  private Color gc;

  public GDColor(int col) {
    if ((col & 0xff000000) == 0) {
      gc = null; // opacity=0 -> no color -> don't paint
    } else {
      gc = Colors.valueOf(col);
    }
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    gs.setCol(gc);
    if (gc != null) {
      g.setColor(gc);
    }
  }
}

class GDFill implements GDObject {
  private Color gc;

  public GDFill(int col) {
    if ((col & 0xff000000) == 0) {
      gc = null; // opacity=0 -> no color -> don't paint
    } else {
      gc = new Color(((float) (col & 255)) / 255f,
          ((float) ((col >> 8) & 255)) / 255f,
          ((float) ((col >> 16) & 255)) / 255f,
          ((float) ((col >> 24) & 255)) / 255f);
    }
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    gs.setFill(gc);
  }
}

class GDLinePar implements GDObject {
  private BasicStroke bs;

  public GDLinePar(double lwd, int lty) {
    bs = null;
    if (lty == 0) {
      bs = new BasicStroke((float) lwd);
    } else if (lty == -1) {
      bs = new BasicStroke(0f);
    } else {
      int l = 0;
      int dt = lty;
      while (dt > 0) {
        dt >>= 4;
        l++;
      }
      float[] dash = new float[l];
      dt = lty;
      l = 0;
      while (dt > 0) {
        int rl = dt & 15;
        dash[l++] = (float) rl;
        dt >>= 4;
      }
      bs = new BasicStroke((float) lwd, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 3f, dash, 0f);
    }
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    if (bs != null) {
      ((Graphics2D) g).setStroke(bs);
    }
  }
}

class GDRaster implements GDObject {
  private boolean interpolate;
  private Image image;
  private AffineTransform atrans;

  public GDRaster(Ptr image, int imageWidth, int imageHeight, double x, double y, double w, double h, double rot, boolean interpolate) {
    this.interpolate = interpolate;
    atrans = new AffineTransform();
    // R seems to use flipped y coordinates
    y += h;
    h = -h;

    double sx = w / (double) imageWidth;
    double sy = h / (double) imageHeight;
    atrans.translate(x, y);
    atrans.rotate(-rot / 180 * Math.PI, 0, y);
    atrans.scale(sx, sy);

    DataBuffer dbuf = toDataBuffer(image, imageWidth * imageHeight * 4);

    int[] compOffsets = {0, 1, 2, 3};
    SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, imageWidth, imageHeight,
        4, imageWidth * 4, compOffsets);
    WritableRaster raster = Raster.createWritableRaster(sm, dbuf, null);
    ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
        true, false, Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
    this.image = new BufferedImage(cm, raster, false, null);
  }

  private DataBuffer toDataBuffer(Ptr image, int size) {
    if(image instanceof BytePtr) {
      // Fast path
      BytePtr bytePtr = (BytePtr) image;
      return new DataBufferByte(bytePtr.array, size, bytePtr.offset);

    } else {
      // Need to make a copy...
      byte[] buffer = new byte[size];
      for (int i = 0; i < buffer.length; i++) {
        buffer[i] = image.getByte(i);
      }
      return new DataBufferByte(buffer, size, 0);
    }
  }

  @Override
  public void paint(Component c, GDState gs, Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    Object oh = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
    try {
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolate ?
          RenderingHints.VALUE_INTERPOLATION_BILINEAR :
          RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g2.drawImage(image, atrans, null);

    } finally {
      if (oh != null) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oh);
      }
    }
  }
}

