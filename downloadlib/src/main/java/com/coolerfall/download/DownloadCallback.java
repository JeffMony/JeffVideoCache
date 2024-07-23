package com.coolerfall.download;

/**
 * This is a callback to be invoked when downloading.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
public interface DownloadCallback {
  /**
   * Invoked when downloading is started.
   *
   * @param downloadId download id in download request queue
   * @param totalBytes total bytes of the file
   */
  void onStart(int downloadId, long totalBytes);

  /**
   * Invoked when download retrying.
   *
   * @param downloadId download id in download request queue
   */
  void onRetry(int downloadId);

  /**
   * Invoked when downloading is in progress.
   *
   * @param downloadId download id in download request queue
   * @param bytesWritten the bytes has written to local disk
   * @param totalBytes total bytes of the file
   */
  void onProgress(int downloadId, long bytesWritten, long totalBytes);

  /**
   * Invoked when downloading successfully.
   *
   * @param downloadId download id in download request queue
   * @param filePath file path
   */
  void onSuccess(int downloadId, String filePath);

  /**
   * Invoked when downloading failed.
   *
   * @param downloadId download id in download request queue
   * @param statusCode status code
   * @param errMsg error message
   */
  void onFailure(int downloadId, int statusCode, String errMsg);
}
