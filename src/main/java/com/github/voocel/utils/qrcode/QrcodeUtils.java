package com.github.voocel.utils.qrcode;

import com.google.common.collect.Maps;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
@Slf4j
@UtilityClass
public class QrcodeUtils {
  /**
   * 生成二维码的默认边长，因为是正方形的，所以高度和宽度一致
   */
  private static final int DEFAULT_LENGTH = 400;
  /**
   * 生成二维码的格式
   */
  private static final String FORMAT = "jpg";

  /**
   * 根据内容生成二维码数据
   *
   * @param content 二维码文字内容[为了信息安全性，一般都要先进行数据加密]
   * @param length  二维码图片宽度和高度
   */
  public static BitMatrix createQrcodeMatrix(String content, int length) {
    return createQrcodeMatrix(content, length, ErrorCorrectionLevel.H);
  }

  public static BitMatrix createQrcodeMatrix(String content, int length, ErrorCorrectionLevel level) {
    Map<EncodeHintType, Object> hints = Maps.newEnumMap(EncodeHintType.class);
    // 设置字符编码
    hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
    // 指定纠错等级
    hints.put(EncodeHintType.ERROR_CORRECTION, level);

    try {
      return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, length, length, hints);
    } catch (WriterException e) {
      throw new RuntimeException("内容为：【" + content + "】的二维码生成失败！", e);
    }
  }

  /**
   * 根据指定边长创建生成的二维码，允许配置logo属性
   *
   * @param content    二维码内容
   * @param length     二维码的高度和宽度
   * @param logoFile   logo 文件对象，可以为空
   * @param logoConfig logo配置，可设置logo展示长宽，边框颜色
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, int length, File logoFile, MatrixToLogoImageConfig logoConfig)
    throws Exception {
    if (logoFile != null && !logoFile.exists()) {
      throw new IllegalArgumentException("请提供正确的logo文件！");
    }

    try (InputStream logo = logoFile == null ? null : new FileInputStream(logoFile)) {
      return createQrcode(content, length, logo, logoConfig);
    }
  }

  public static byte[] createQrcode(String content, int length, InputStream logo, MatrixToLogoImageConfig logoConfig) throws Exception {
    BufferedImage img = generateQRCodeImage(content, length, logo, logoConfig);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, FORMAT, baos);
    return baos.toByteArray();
  }

  /**
   * 根据指定边长创建生成的二维码
   *
   * @param content  二维码内容
   * @param length   二维码的高度和宽度
   * @param logoFile logo 文件对象，可以为空
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, int length, File logoFile) throws Exception {
    return createQrcode(content, length, logoFile, new MatrixToLogoImageConfig());
  }

  /**
   * 创建生成默认高度(400)的二维码图片
   * 可以指定是否贷logo
   *
   * @param content  二维码内容
   * @param logoFile logo 文件对象，可以为空
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, File logoFile) throws Exception {
    return createQrcode(content, DEFAULT_LENGTH, logoFile);
  }

  public static BufferedImage generateQRCodeImage(String content, int length, InputStream logo, MatrixToLogoImageConfig logoConfig) throws Exception {
    // 生成二维码图像
    BitMatrix qrCodeMatrix = createQrcodeMatrix(content, length);
    BufferedImage img = MatrixToImageWriter.toBufferedImage(qrCodeMatrix);
    try {
      if (logo != null) {
        overlapImage(img, FORMAT, logo, logoConfig);
      }
    } catch (Exception e) {
      throw new RuntimeException("为二维码添加LOGO时失败！", e);
    }
    return img;
  }

  public static BufferedImage generateQRCodeImage(String content, int length, File logoFile, MatrixToLogoImageConfig logoConfig) throws Exception {
    if (logoFile != null && !logoFile.exists()) {
      throw new IllegalArgumentException("请提供正确的logo文件！");
    }
    try (InputStream logo = Files.newInputStream(logoFile.toPath())) {
      return generateQRCodeImage(content, length, logo, logoConfig);
    }
  }

  public static BufferedImage generateQRCodeImage(String content, int length, InputStream logo) throws Exception {
    return generateQRCodeImage(content, length, logo, new MatrixToLogoImageConfig());
  }

  public static BufferedImage generateQRCodeImage(String content, int length, File logoFile) throws Exception {
    return generateQRCodeImage(content, length, logoFile, new MatrixToLogoImageConfig());
  }

  public static BufferedImage generateQRCodeImage(String content, InputStream logo) throws Exception {
    return generateQRCodeImage(content, DEFAULT_LENGTH, logo);
  }

  public static BufferedImage generateQRCodeImage(String content, File logoFile) throws Exception {
    return generateQRCodeImage(content, DEFAULT_LENGTH, logoFile);
  }

  /**
   * 将logo添加到二维码中间
   *
   * @param image         生成的二维码图片对象
   * @param logo          logo文件对象
   * @param ignoredFormat 图片格式
   */
  private static void overlapImage(final BufferedImage image, String ignoredFormat, final InputStream logo,
                                   MatrixToLogoImageConfig logoConfig) throws IOException {
    BufferedImage logoImg = ImageIO.read(logo);
    logoImg = clipRound(logoImg);
    Graphics2D g = logoImg.createGraphics();
    // 考虑到logo图片贴到二维码中，建议大小不要超过二维码的1/5;
    int width = image.getWidth() / logoConfig.getLogoPart();
    int height = image.getHeight() / logoConfig.getLogoPart();
    int radius = width / 10;
    // logo起始位置，此目的是为logo居中显示
    int x = (image.getWidth() - width) / 2;
    int y = (image.getHeight() - height) / 2;

    // 创建一个支持有透明度的图像缓冲区
    BufferedImage buffer = g.getDeviceConfiguration().createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
    g.dispose();

    // 绘制阴影
    g = buffer.createGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR, 0.1f));
    g.setColor(Color.BLACK);
    g.fillRoundRect(x + 10, y + 10, width - 20, height, radius, radius);
    g.dispose();

    // 绘制LOGO到缓冲区
    g = buffer.createGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, 1));
    g.drawImage(logoImg, x, y, width, height, null);

    // 给logo画边框
    // 构造一个具有指定线条宽度以及 cap 和 join 风格的默认值的实心 BasicStroke
    g.setStroke(new BasicStroke(logoConfig.getBorder()));
    g.setColor(logoConfig.getBorderColor());
    g.drawRoundRect(x, y, width, height, radius, radius);
    g.setStroke(new BasicStroke(1));
    g.setColor(Color.GRAY);
    g.drawRoundRect(x + logoConfig.getBorder() / 2, y + logoConfig.getBorder() / 2, width - logoConfig.getBorder(),
      height - logoConfig.getBorder(), radius, radius);
    g.dispose();

    // 将带阴影的图像绘制到二维码上
    g = image.createGraphics();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1.0f));
    g.drawImage(buffer, 0, 0, image.getWidth(), image.getHeight(), null);
    g.dispose();
  }

  /**
   * 为LOGO剪出圆角
   *
   * @param srcImage LOGO图像
   */
  private static BufferedImage clipRound(BufferedImage srcImage) {
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    int radius = width / 10;

    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = image.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setClip(new RoundRectangle2D.Double(0, 0, width, height, radius, radius));
    g.drawImage(srcImage, 0, 0, null);
    g.dispose();
    return image;
  }

  /**
   * 解析二维码
   *
   * @param file 二维码文件内容
   * @return 二维码的内容
   */
  public static String decodeQrcode(File file) throws IOException, NotFoundException {
    BufferedImage image = ImageIO.read(file);
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    Binarizer binarizer = new HybridBinarizer(source);
    BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
    Map<DecodeHintType, Object> hints = Maps.newEnumMap(DecodeHintType.class);
    hints.put(DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
    return new MultiFormatReader().decode(binaryBitmap, hints).getText();
  }
}
