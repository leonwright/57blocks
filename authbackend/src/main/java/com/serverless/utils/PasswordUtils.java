package com.serverless.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PasswordUtils {
  public static String hashPassword(String password) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    byte[] hashedBytes = md.digest(password.getBytes());
    StringBuilder sb = new StringBuilder();
    for (byte b : hashedBytes) {
        sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  public static boolean validatePassword(String password, String hashedPassword) throws NoSuchAlgorithmException {
    String hashedInput = hashPassword(password);
    return hashedInput.equals(hashedPassword);
  }
  
}
