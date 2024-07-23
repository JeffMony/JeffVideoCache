package com.coolerfall.download;

/**
 * An implementation of {@link DownloadCallback} with empty methods allowing
 * subclass to override only the mothod they're interested in.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public abstract class DownloadCallbackAdapter implements DownloadCallback {

  static final DownloadCallbackAdapter EMPTY_CALLBACK = new DownloadCallbackAdapter() {
  };

  /**
   * {@inheritDoc}
   */
  public void onStart(int downloadId, long totalBytes) {

  }

  /**
   * {@inheritDoc}
   */
  public void onRetry(int downloadId) {

  }

  /**
   * {@inheritDoc}
   */
  public void onProgress(int downloadId, long bytesWritten, long totalBytes) {

  }

  /**
   * {@inheritDoc}
   */
  public void onSuccess(int downloadId, String filePath) {

  }

  /**
   * {@inheritDoc}
   */
  public void onFailure(int downloadId, int statusCode, String errMsg) {

  }
}
