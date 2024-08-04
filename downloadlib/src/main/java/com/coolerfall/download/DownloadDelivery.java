package com.coolerfall.download;

import android.os.Handler;
import android.support.annotation.NonNull;
import java.util.concurrent.Executor;

/**
 * This class is used to delivery callback to call back in main thread.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DownloadDelivery {
  private final Executor downloadPoster;

  public DownloadDelivery(final Handler handler) {
    downloadPoster = new Executor() {
      @Override public void execute(@NonNull Runnable command) {
          //handler.post(command);
          command.run();
      }
    };
  }

  /**
   * Post download start event.
   *
   * @param request download request
   * @param totalBytes total bytes
   */
  void postStart(final DownloadRequest request, final long totalBytes) {
    downloadPoster.execute(new Runnable() {
      @Override public void run() {
        request.downloadCallback().onStart(request.downloadId(), totalBytes);
      }
    });
  }

  /**
   * Post download retry event.
   *
   * @param request download request
   */
  void postRetry(final DownloadRequest request) {
    downloadPoster.execute(new Runnable() {
      @Override public void run() {
        request.downloadCallback().onRetry(request.downloadId());
      }
    });
  }

  /**
   * Post download progress event.
   *
   * @param request download request
   * @param bytesWritten the bytes have written to file
   * @param totalBytes the total bytes of currnet file in downloading
   */
  void postProgress(final DownloadRequest request, final long bytesWritten, final long totalBytes) {
    downloadPoster.execute(new Runnable() {
      @Override public void run() {
        request.downloadCallback().onProgress(request.downloadId(), bytesWritten, totalBytes);
      }
    });
  }

  /**
   * Post download success event.
   *
   * @param request download request
   */
  void postSuccess(final DownloadRequest request, final long totalBytes, final long time) {
    downloadPoster.execute(new Runnable() {
      @Override public void run() {
        request.downloadCallback().onSuccess(request.downloadId(), request.destinationFilePath(), totalBytes, time);
      }
    });
  }

  /**
   * Post download failure event.
   *
   * @param request download request
   * @param statusCode status code
   * @param errMsg error message
   */
  void postFailure(final DownloadRequest request, final int statusCode, final String errMsg) {
    downloadPoster.execute(new Runnable() {
      @Override public void run() {
        request.downloadCallback().onFailure(request.downloadId(), statusCode, errMsg);
      }
    });
  }
}
