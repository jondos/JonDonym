/*
 * This is only an additional class for viewing some generated zip binary image captchas. This class
 * is no needed part of the infoservice source tree. So no documentation is available here.
 */
package infoservice.japforwarding.captcha.test;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import anon.util.Base64;
import anon.util.ZLibTools;
import captcha.ZipBinaryImageCaptchaGenerator;

public class TestZipBinaryImageCaptcha {

  private Random textRandom;

  private int wrongCaptchas;

  private int correctCaptchas;

  private JLabel wrongLabel;

  private JLabel correctLabel;

  private JLabel lastLabel;

  private JButton nextButton;

  private JPanel mainPanel;

  private JLabel captchaLabel;

  private JTextField codeField;

  private String lastString;

  private JLabel codeLabel;

  private JLabel sizeLabel;

  public TestZipBinaryImageCaptcha() throws Exception {
    textRandom = new Random();
    correctCaptchas = 0;
    wrongCaptchas = 0;
    mainPanel = new JPanel();
    lastString = getRandomString(8);
    String currentCaptcha = (new ZipBinaryImageCaptchaGenerator(300, 100)).createCaptcha(lastString);
    captchaLabel = new JLabel(new ImageIcon(BinaryImageExtractor.binaryToImage(ZLibTools.decompress(Base64.decode(currentCaptcha)))));
    sizeLabel = new JLabel("Base64-encoded-size: " + NumberFormat.getInstance().format(currentCaptcha.length()) + " Bytes");
    codeField = new JTextField(10);
    nextButton = new JButton("Next");
    lastLabel = new JLabel("Last Captcha-Code:");
    wrongLabel = new JLabel("Wrong: 0");
    correctLabel = new JLabel("Correct: 0");
    nextButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event)
      {
        try {
          if (lastString.toLowerCase().equals(codeField.getText().trim().toLowerCase())) {
            correctCaptchas++;
          }
          else {
            wrongCaptchas++;
          }
          lastLabel.setText("Last Captcha-Code: " + lastString);
          lastString = getRandomString(8);
          String currentCaptcha = (new ZipBinaryImageCaptchaGenerator(300, 100)).createCaptcha(lastString);
          captchaLabel.setIcon(new ImageIcon(BinaryImageExtractor.binaryToImage(ZLibTools.decompress(Base64.decode(currentCaptcha)))));
          sizeLabel.setText("Base64-encoded-size: " + NumberFormat.getInstance().format(currentCaptcha.length()) + " Bytes");
          codeField.setText("");
          correctLabel.setText("Correct: " + Integer.toString(correctCaptchas));
          wrongLabel.setText("Wrong: " + Integer.toString(wrongCaptchas));
        }
        catch (Exception e) {
          System.out.println(e.toString());
        }
      }
    });
    codeLabel = new JLabel("Captcha-Code:");

    GridBagLayout mainPanelLayout = new GridBagLayout();
    mainPanel.setLayout(mainPanelLayout);

    GridBagConstraints mainPanelConstraints = new GridBagConstraints();
    mainPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    mainPanelConstraints.fill = GridBagConstraints.NONE;
    mainPanelConstraints.weightx = 1.0;
    mainPanelConstraints.weighty = 0.0;

    mainPanelConstraints.gridx = 0;
    mainPanelConstraints.gridy = 0;
    mainPanelConstraints.gridwidth = 3;
    mainPanelLayout.setConstraints(captchaLabel, mainPanelConstraints);
    mainPanel.add(captchaLabel);

    mainPanelConstraints.insets = new Insets(5, 0, 0, 0);
    mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    mainPanelConstraints.gridx = 0;
    mainPanelConstraints.gridy = 1;
    mainPanelLayout.setConstraints(sizeLabel, mainPanelConstraints);
    mainPanel.add(sizeLabel);

    mainPanelConstraints.insets = new Insets(20, 0, 0, 0);
    mainPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    mainPanelConstraints.weightx = 0.0;
    mainPanelConstraints.gridx = 0;
    mainPanelConstraints.gridy = 2;
    mainPanelConstraints.gridwidth = 1;
    mainPanelLayout.setConstraints(codeLabel, mainPanelConstraints);
    mainPanel.add(codeLabel);

    mainPanelConstraints.gridx = 1;
    mainPanelConstraints.gridy = 2;
    mainPanelConstraints.insets = new Insets(10, 10, 0, 0);
    mainPanelConstraints.weightx = 1.0;
    mainPanelLayout.setConstraints(codeField, mainPanelConstraints);
    mainPanel.add(codeField);

    mainPanelConstraints.gridx = 2;
    mainPanelConstraints.gridy = 2;
    mainPanelLayout.setConstraints(nextButton, mainPanelConstraints);
    mainPanel.add(nextButton);

    mainPanelConstraints.insets = new Insets(20, 0, 0, 0);
    mainPanelConstraints.weightx = 1.0;
    mainPanelConstraints.gridx = 0;
    mainPanelConstraints.gridy = 3;
    mainPanelConstraints.gridwidth = 2;
    mainPanelLayout.setConstraints(correctLabel, mainPanelConstraints);
    mainPanel.add(correctLabel);

    mainPanelConstraints.insets = new Insets(20, 10, 0, 0);
    mainPanelConstraints.gridx = 2;
    mainPanelConstraints.gridy = 3;
    mainPanelConstraints.gridwidth = 1;
    mainPanelLayout.setConstraints(wrongLabel, mainPanelConstraints);
    mainPanel.add(wrongLabel);

    mainPanelConstraints.insets = new Insets(10, 0, 0, 0);
    mainPanelConstraints.gridx = 0;
    mainPanelConstraints.gridy = 4;
    mainPanelConstraints.gridwidth = 3;
    mainPanelLayout.setConstraints(lastLabel, mainPanelConstraints);
    mainPanel.add(lastLabel);
  }

  public JPanel getMainPanel() {
    return mainPanel;
  }

  public String getRandomString(int stringLength) {
    String randomString = "";
    while (stringLength > 0) {
      String charString = (new ZipBinaryImageCaptchaGenerator(300, 100)).getValidCharacters();
      int position = textRandom.nextInt(charString.length());
      randomString = randomString + charString.substring(position, position + 1);
      stringLength--;
    }
    return randomString;
  }

  public static void main(String[] args) {
    try {
      JFrame testFrame = new JFrame("CaptchaTest");
      TestZipBinaryImageCaptcha captchaTest = new TestZipBinaryImageCaptcha();
      testFrame.getContentPane().add(captchaTest.getMainPanel());
      testFrame.setLocation(300, 200);
      testFrame.pack();
      testFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      testFrame.setVisible(true);
    }
    catch (Exception e) {
      System.out.println(e.toString());
      e.printStackTrace();
    }
  }
}
