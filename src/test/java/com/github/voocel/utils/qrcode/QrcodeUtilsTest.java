package com.github.voocel.utils.qrcode;

import com.beust.jcommander.internal.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class QrcodeUtilsTest {

  private String content = "abc";
  private List<Path> generatedQrcodePaths = Lists.newArrayList();

  @BeforeTest
  public void setup() {
    BasicConfigurator.configure();
  }

  @Test
  public void testCreateQrcode() throws Exception {
    byte[] bytes = QrcodeUtils.createQrcode(content, 800, null);
    Path path = Files.createTempFile("qrcode_800_", ".jpg");
    generatedQrcodePaths.add(path);
    log.info("{}", path.toAbsolutePath());
    Files.write(path, bytes);

    bytes = QrcodeUtils.createQrcode(content, null);
    path = Files.createTempFile("qrcode_400_", ".jpg");
    generatedQrcodePaths.add(path);
    log.info("{}", path.toAbsolutePath());
    Files.write(path, bytes);
  }

  @Test
  public void testCreateQrcodeWithLogo() throws Exception {
    try (InputStream inputStream = ClassLoader.getSystemResourceAsStream("logo.png")) {
      File logoFile = Files.createTempFile("logo_", ".jpg").toFile();
      FileUtils.copyInputStreamToFile(inputStream, logoFile);
      log.info("{}", logoFile);
      byte[] bytes = QrcodeUtils.createQrcode(content, 800, logoFile);
      Path path = Files.createTempFile("qrcode_with_logo_", ".jpg");
      generatedQrcodePaths.add(path);
      log.info("{}", path.toAbsolutePath());
      Files.write(path, bytes);
    }
  }

  @Test(dependsOnMethods = {"testCreateQrcode", "testCreateQrcodeWithLogo"})
  public void testDecodeQrcode() throws Exception {
    for (Path path : generatedQrcodePaths) {
      Assert.assertEquals(QrcodeUtils.decodeQrcode(path.toFile()), content);
    }
  }


  @Test
  public void testPersonCode() throws Exception {
    BufferedImage img = QrcodeUtils.generateQRCodeImage(content, 400, ClassLoader.getSystemResourceAsStream("logo.png"));
    File outFile = Files.createTempFile("qrcode_with_logo_", ".png").toFile();
    log.info("{}", outFile.getAbsolutePath());
    ImageIO.write(img, "png", outFile);
    Desktop.getDesktop().open(outFile);
  }

}
