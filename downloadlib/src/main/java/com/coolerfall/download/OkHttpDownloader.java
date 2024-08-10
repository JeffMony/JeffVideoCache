package com.coolerfall.download;

import android.net.Uri;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.coolerfall.download.Utils.CONTENT_DISPOSITION;
import static com.coolerfall.download.Utils.DEFAULT_CONNECT_TIMEOUT;
import static com.coolerfall.download.Utils.DEFAULT_READ_TIMEOUT;
import static com.coolerfall.download.Utils.DEFAULT_WRITE_TIMEOUT;
import static com.coolerfall.download.Utils.HTTP_TEMP_REDIRECT;
import static com.coolerfall.download.Utils.LOCATION;
import static com.coolerfall.download.Utils.MAX_REDIRECTION;
import static com.coolerfall.download.Utils.getFilenameFromHeader;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * A downloader implemented by {@link OkHttpClient}.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public final class OkHttpDownloader implements Downloader {
  private final OkHttpClient client;
  private Response response;
  private final AtomicInteger redirectionCount = new AtomicInteger();

  private static OkHttpClient defaultOkHttpClient() {
    return new OkHttpClient.Builder().connectTimeout(DEFAULT_CONNECT_TIMEOUT, MILLISECONDS)
        .readTimeout(DEFAULT_READ_TIMEOUT, MILLISECONDS)
        .writeTimeout(DEFAULT_WRITE_TIMEOUT, MILLISECONDS)
        .build();
  }

  /**
   * Create an instance using a default {@link OkHttpClient}.
   *
   * @return {@link OkHttpDownloader}
   */
  public static OkHttpDownloader create() {
    return new OkHttpDownloader(null);
  }

  /**
   * Create an instance using a {@code client}.
   *
   * @return {@link OkHttpDownloader}
   */
  public static OkHttpDownloader create(OkHttpClient client) {
    return new OkHttpDownloader(client);
  }

  private OkHttpDownloader(OkHttpClient client) {
    this.client = client == null ? defaultOkHttpClient() : client;
  }

  @Override public String detectFilename(Uri uri) throws IOException {
    redirectionCount.set(MAX_REDIRECTION);
    Response response = innerRequest(client, uri, 0);
    String url = response.request().url().toString();
    String contentDisposition = response.header(CONTENT_DISPOSITION);
    response.close();
    return getFilenameFromHeader(url, contentDisposition);
  }

  @Override public int start(Uri uri, long breakpoint) throws IOException {
    redirectionCount.set(MAX_REDIRECTION);
    response = innerRequest(client, uri, breakpoint);
    return response.code();
  }

  @Override public long contentLength() {
    return response == null || response.body() == null ? -1 : response.body().contentLength();
  }

  @Override public InputStream byteStream() {
    return response == null || response.body() == null ? null : response.body().byteStream();
  }

  @Override public void close() {
    if (response != null) {
      response.close();
    }
  }

  @Override public Downloader copy() {
    return create(client);
  }

  Response innerRequest(OkHttpClient client, Uri uri, long breakpoint) throws IOException {
    Request.Builder builder = new Request.Builder().url(uri.toString());
    if (breakpoint > 0) {
      builder.header("Accept-Encoding", "identity")
          .header("Range", "bytes=" + breakpoint + "-")
          .build();
    }
    Response response = client.newCall(builder.build()).execute();
    int statusCode = response.code();
    switch (statusCode) {
      case 301:
      case 302:
      case 303:
      case HTTP_TEMP_REDIRECT:
        response.close();
        if (redirectionCount.decrementAndGet() >= 0) {
          /* take redirect url and call start recursively */
          String redirectUrl = response.header(LOCATION);
          if (redirectUrl == null) {
            throw new DownloadException(statusCode, "redirects got no `Location` header");
          }
          return innerRequest(client, Uri.parse(redirectUrl), breakpoint);
        } else {
          throw new DownloadException(statusCode, "redirects too many times");
        }
    }

    return response;
  }
}
