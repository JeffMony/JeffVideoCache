package com.coolerfall.download;

import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;

/**
 * A mechanism to download files from network. It's easy to use custom downloader
 * by implementing this interface, and then set it in {@link DownloadManager}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public interface Downloader {
  /**
   * Detect filename from http header/url if existed.
   *
   * @param uri uri to detect filename
   * @return filename to save
   * @throws IOException
   */
  String detectFilename(Uri uri) throws IOException;

  /**
   * Init downloader and start to download. The downloader should handle redirect
   * status code, such as 301, 302 and so on.
   *
   * @param uri {@link Uri}
   * @param breakpoint breakpoint if exists
   * @return status code
   * @throws IOException
   */
  int start(Uri uri, long breakpoint) throws IOException;

  /**
   * Get content length for current uri.
   *
   * @return content length
   */
  long contentLength();

  /**
   * Get inputstream supported by downloader.
   *
   * @throws IOException
   */
  InputStream byteStream() throws IOException;

  /**
   * Close downloader and stop downloader.
   */
  void close();

  /**
   * Make a copy for this downloader.
   */
  Downloader copy();
}
