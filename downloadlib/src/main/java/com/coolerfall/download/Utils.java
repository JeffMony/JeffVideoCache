package com.coolerfall.download;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Contains some utils used in download manager.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class Utils {
  static final int DEFAULT_READ_TIMEOUT = 25 * 1000;
  static final int DEFAULT_WRITE_TIMEOUT = 25 * 1000;
  static final int DEFAULT_CONNECT_TIMEOUT = 20 * 1000;
  static final String HTTP = "http";
  static final String HTTPS = "https";
  static final String LOCATION = "Location";
  static final String CONTENT_DISPOSITION = "Content-Disposition";
  static final int MAX_REDIRECTION = 5;
  static final int HTTP_OK = 200;
  static final int HTTP_PARTIAL = 206;
  static final int HTTP_TEMP_REDIRECT = 307;

  private Utils() {
  }

  /**
   * To check whether current network is wifi.
   *
   * @param context context
   * @return true if network if wifi, otherwise return false
   */
  static boolean isWifi(Context context) {
    if (context == null) {
      return false;
    }

    ConnectivityManager manager =
        (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    if (manager == null) {
      return false;
    }
    NetworkInfo info = manager.getActiveNetworkInfo();

    return info != null && (info.getType() == ConnectivityManager.TYPE_WIFI);
  }

  /* get uuid without '-' */
  static String getUuid() {
    return UUID.randomUUID().toString().trim().replaceAll("-", "");
  }

  /* caculate md5 for string */
  static String md5(String origin) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(origin.getBytes("UTF-8"));
      BigInteger bi = new BigInteger(1, md.digest());
      StringBuilder hash = new StringBuilder(bi.toString(16));

      while (hash.length() < 32) {
        hash.insert(0, "0");
      }

      return hash.toString();
    } catch (Exception e) {
      return getUuid();
    }
  }

  /**
   * Get filename from url.
   *
   * @param url url
   * @return filename or md5 if no available filename
   */
  static String getFilenameFromUrl(String url) {
    String filename = md5(url) + ".down";

    int index = url.lastIndexOf("/");
    if (index > 0) {
      String tmpFilename = url.substring(index + 1);
      int qmarkIndex = tmpFilename.indexOf("?");
      if (qmarkIndex > 0) {
        tmpFilename = tmpFilename.substring(0, qmarkIndex - 1);
      }

      /* if filename contains '.', then the filename has file extension */
      if (tmpFilename.contains(".")) {
        filename = tmpFilename;
      }
    }

    return filename;
  }

  /**
   * Get filename from content disposition in header.
   *
   * @param url url of current file to download
   * @param contentDisposition content disposition in header
   * @return filename in header if existed, otherwise get from url
   */
  public static String getFilenameFromHeader(String url, String contentDisposition) {
    String filename;
    if (!TextUtils.isEmpty(contentDisposition)) {
      int index = contentDisposition.indexOf("filename");
      if (index > 0) {
        filename = contentDisposition.substring(index + 9);
        return filename;
      } else {
        filename = getFilenameFromUrl(url);
      }
    } else {
      filename = getFilenameFromUrl(url);
    }

    try {
      filename = URLDecoder.decode(filename, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      /* ignore */
    }

    return filename;
  }

  /**
   * Create {@link SSLContext} for https.
   *
   * @return {@link SSLContext}
   */
  static SSLContext createSSLContext() {
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      TrustManager[] tm = {
          new X509TrustManager() {
            @SuppressLint("TrustAllX509TrustManager") @Override
            public void checkClientTrusted(X509Certificate[] paramArrayOfX509Certificate,
                String paramString) {
            }

            @SuppressLint("TrustAllX509TrustManager") @Override
            public void checkServerTrusted(X509Certificate[] paramArrayOfX509Certificate,
                String paramString) {
            }

            @Override public X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[] {};
            }
          }
      };
      sc.init(null, tm, new SecureRandom());
      return sc;
    } catch (NoSuchAlgorithmException | KeyManagementException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Create default {@link Downloader} for download manager.
   *
   * @return {@link Downloader}
   */
  static Downloader createDefaultDownloader() {
    try {
      Class.forName("okhttp3.OkHttpClient");
      return OkHttpDownloader.create();
    } catch (ClassNotFoundException ignored) {
    }

    return URLDownloader.create();
  }
}
