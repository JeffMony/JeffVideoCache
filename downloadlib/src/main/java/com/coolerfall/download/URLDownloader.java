package com.coolerfall.download;

import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import static com.coolerfall.download.Utils.CONTENT_DISPOSITION;
import static com.coolerfall.download.Utils.DEFAULT_CONNECT_TIMEOUT;
import static com.coolerfall.download.Utils.DEFAULT_READ_TIMEOUT;
import static com.coolerfall.download.Utils.HTTPS;
import static com.coolerfall.download.Utils.HTTP_TEMP_REDIRECT;
import static com.coolerfall.download.Utils.LOCATION;
import static com.coolerfall.download.Utils.MAX_REDIRECTION;
import static com.coolerfall.download.Utils.createSSLContext;
import static com.coolerfall.download.Utils.getFilenameFromHeader;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

/**
 * A default downloader implemented by {@link URLConnection}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class URLDownloader implements Downloader {
  private static final String ACCPET_ENCODING = "Accept-Encoding";
  private static final String TRANSFER_ENCODING = "Transfer-Encoding";
  private static final String CONTENT_LENGTH = "Content-Length";

  private HttpURLConnection httpURLConnection;
  private final AtomicInteger redirectionCount = new AtomicInteger();

  private URLDownloader() {
  }

  /**
   * Create an instance using {@link URLConnection}.
   *
   * @return {@link URLDownloader}
   */
  public static URLDownloader create() {
    return new URLDownloader();
  }

  @Override public String detectFilename(Uri uri) throws IOException {
    redirectionCount.set(MAX_REDIRECTION);
    HttpURLConnection httpURLConnection = innerRequest(uri, 0);
    String url = httpURLConnection.getURL().toString();
    String contentDispisition = httpURLConnection.getHeaderField(CONTENT_DISPOSITION);
    httpURLConnection.disconnect();
    return getFilenameFromHeader(url, contentDispisition);
  }

  @Override public int start(Uri uri, long breakpoint) throws IOException {
    redirectionCount.set(MAX_REDIRECTION);
    httpURLConnection = innerRequest(uri, breakpoint);
    return httpURLConnection.getResponseCode();
  }

  @Override public long contentLength() {
    return getContentLength(httpURLConnection);
  }

  @Override public InputStream byteStream() throws IOException {
    return httpURLConnection.getInputStream();
  }

  @Override public void close() {
    if (httpURLConnection != null) {
      httpURLConnection.disconnect();
    }
  }

  @Override public Downloader copy() {
    return create();
  }

  HttpURLConnection innerRequest(Uri uri, long breakpoint) throws IOException {
    HttpURLConnection httpURLConnection;
    URL url = new URL(uri.toString());
    if (HTTPS.equals(uri.getScheme())) {
      HttpsURLConnection httpsURLConnection = (HttpsURLConnection) url.openConnection();
      SSLContext sslContext = createSSLContext();
      if (sslContext != null) {
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        httpsURLConnection.setSSLSocketFactory(sslSocketFactory);
      }
      httpURLConnection = httpsURLConnection;
    } else {
      httpURLConnection = (HttpURLConnection) url.openConnection();
    }

    httpURLConnection.setInstanceFollowRedirects(true);
    httpURLConnection.setUseCaches(false);
    httpURLConnection.setRequestProperty(ACCPET_ENCODING, "identity");
    httpURLConnection.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
    httpURLConnection.setReadTimeout(DEFAULT_READ_TIMEOUT);
    if (breakpoint > 0) {
      httpURLConnection.setRequestProperty("Range", "bytes=" + breakpoint + "-");
    }

    int statusCode = httpURLConnection.getResponseCode();
    switch (statusCode) {
      case HTTP_MOVED_PERM:
      case HTTP_MOVED_TEMP:
      case HTTP_SEE_OTHER:
      case HTTP_TEMP_REDIRECT:
        if (redirectionCount.decrementAndGet() >= 0) {
          /* take redirect url and call start recursively */
          String redirectUrl = httpURLConnection.getHeaderField(LOCATION);
          httpURLConnection.disconnect();
          if (redirectUrl == null) {
            throw new DownloadException(statusCode, "redirects got no `Location` header");
          }
          return innerRequest(Uri.parse(redirectUrl), breakpoint);
        } else {
          throw new DownloadException(statusCode, "redirects too many times");
        }

      default:
        return httpURLConnection;
    }
  }

  /* read response content length from server */
  int getContentLength(HttpURLConnection conn) {
    String transferEncoding = conn.getHeaderField(TRANSFER_ENCODING);
    if (transferEncoding == null || transferEncoding.equalsIgnoreCase("chunked")) {
      return conn.getHeaderFieldInt(CONTENT_LENGTH, -1);
    } else {
      return -1;
    }
  }
}
