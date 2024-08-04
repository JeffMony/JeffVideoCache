package com.coolerfall.download;

import android.os.Process;
import android.os.SystemClock;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.coolerfall.download.Utils.HTTP_OK;
import static com.coolerfall.download.Utils.HTTP_PARTIAL;

/**
 * This class used to dispatch downloader, this is desinged according to NetworkDispatcher in
 * Android-Volley.
 *
 * @author Vincent Cheung (coolingfall@gmail.com)
 */
final class DownloadDispatcher extends Thread {
  private static final int SLEEP_BEFORE_DOWNLOAD = 500;
  private static final int BUFFER_SIZE = 8192; //4096;
  private static final int END_OF_STREAM = -1;
  private static final String DEFAULT_THREAD_NAME = "DownloadDispatcher";
  private static final String IDLE_THREAD_NAME = "DownloadDispatcher-Idle";

  private final BlockingQueue<DownloadRequest> queue;
  private final DownloadDelivery delivery;
  private final Logger logger;
  private long lastProgressTimestamp;
  private volatile boolean quit = false;

  /**
   * Default constructor, with queue and delivery.
   *
   * @param queue a {@link BlockingQueue} with {@link DownloadRequest}
   * @param delivery {@link DownloadDelivery}
   * @param logger {@link Logger}
   */
  public DownloadDispatcher(BlockingQueue<DownloadRequest> queue, DownloadDelivery delivery,
      Logger logger) {
    this.queue = queue;
    this.delivery = delivery;
    this.logger = logger;

    /* set thread name to idle */
    setName(IDLE_THREAD_NAME);
  }

  @Override public void run() {
    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
    DownloadRequest request = null;

    while (true) {
      try {
        //setName(IDLE_THREAD_NAME);
        //request = queue.take();
        request = queue.poll(60, TimeUnit.SECONDS);
        if (quit || isInterrupted()) {
          logger.log("thread is interrupted, quit.isInterrupted:" + isInterrupted());
          if (request != null) {
            request.finish();
          }
          return;
        }
        if (request == null) {
          continue;
        }
        if (request.isCanceled()) {
          request.finish();
          continue;
        }

        logger.log("A new download request taken, download id: " + request.downloadId());
        //sleep(SLEEP_BEFORE_DOWNLOAD);
        //setName(DEFAULT_THREAD_NAME);

        /* start download */
        executeDownload(request);
      } catch (InterruptedException e) {
        e.printStackTrace();
        /* we may have been interrupted because it was time to quit */
        if (quit) {
          if (request != null) {
            request.finish();
          }

          return;
        }
      }
    }
  }

  /* update download state */
  private void updateState(DownloadRequest request, DownloadState state) {
    request.updateDownloadState(state);
  }

  /* update download start state */
  private void updateStart(DownloadRequest request, long totalBytes) {
    /* if the request has failed before, donnot deliver callback */
    if (request.downloadState() == DownloadState.FAILURE) {
      updateState(request, DownloadState.RUNNING);
      return;
    }

    /* set the download state of this request as running */
    updateState(request, DownloadState.RUNNING);
    delivery.postStart(request, totalBytes);
  }

  /* update download retrying */
  private void updateRetry(DownloadRequest request) {
    delivery.postRetry(request);
  }

  /* update download progress */
  private void updateProgress(DownloadRequest request, long bytesWritten, long totalBytes) {
    long currentTimestamp = SystemClock.uptimeMillis();
    if (bytesWritten != totalBytes
        && currentTimestamp - lastProgressTimestamp < request.progressInterval()) {
      return;
    }

    /* save progress timestamp */
    lastProgressTimestamp = currentTimestamp;

    if (!request.isCanceled()) {
      delivery.postProgress(request, bytesWritten, totalBytes);
    }
  }

  /* update download success */
  @SuppressWarnings("ResultOfMethodCallIgnored") private void updateSuccess(
      DownloadRequest request, long totalBytes, long time) {
    logger.log("onSuccess updateSuccess:" + request.destinationFilePath());
    updateState(request, DownloadState.SUCCESSFUL);

    /* notify the request download finish */
    request.finish();

    File file = new File(request.tempFilePath());
    if (file.exists()) {
      file.renameTo(new File(request.destinationFilePath()));
    }

    /* deliver success message */
    delivery.postSuccess(request, totalBytes, time);
  }

  /* update download failure */
  private void updateFailure(DownloadRequest request, int statusCode, String errMsg) {
    updateState(request, DownloadState.FAILURE);

    /* if the status code is 0, may be cause by the net error */
    int leftRetryTime = request.retryTime();
    if (leftRetryTime >= 0) {
      try {
        /* sleep a while before retrying */
        sleep(request.retryInterval());
      } catch (InterruptedException e) {
        /* we may have been interrupted because it was time to quit */
        if (quit) {
          request.finish();
          return;
        }
      }

      /* retry downloading */
      if (!request.isCanceled()) {
        logger.log("Retry DownloadRequest: "
            + request.downloadId()
            + " left retry time: "
            + leftRetryTime);
        updateRetry(request);
        executeDownload(request);
      }

      return;
    }

    /* notify the request that downloading has finished */
    request.finish();

    /* deliver failure message */
    delivery.postFailure(request, statusCode, errMsg);
  }

  /* execute downloading */
  private void executeDownload(DownloadRequest request) {
    if (quit || isInterrupted()) {
      return;
    }

    Downloader downloader = request.downloader();
    RandomAccessFile raf = null;
    InputStream is = null;

    try {
      if (request.destinationFilePath() == null) {
        request.updateDestinationFilePath(downloader.detectFilename(request.uri()));
      }

      File file = new File(request.tempFilePath());
      boolean fileExist = file.exists();
      long breakpoint = file.length();
      long bytesWritten = 0;

      int statusCode = downloader.start(request.uri(), breakpoint);
      is = downloader.byteStream();
      if (statusCode != HTTP_OK && statusCode != HTTP_PARTIAL) {
        logger.log("Incorrect http code got: " + statusCode);
        if (fileExist) {
          file.delete();
        }
        throw new DownloadException(statusCode, "download fail");
      }

      if (fileExist) {
        if (statusCode == HTTP_PARTIAL) {
          /* set the range to continue the downloading */
          raf = new RandomAccessFile(file, "rw");
          raf.seek(breakpoint);
          bytesWritten = breakpoint;
          logger.log(
                  "Detect existed file with " + breakpoint + " bytes, start breakpoint downloading");
        } else {
          boolean ret = file.delete();
          raf = new RandomAccessFile(file, "rw");
          logger.log("file:" + file.getName() + " exists, but server don't support breakpoint downloading, delete file:" + ret);
        }
      } else {
        raf = new RandomAccessFile(file, "rw");
      }

      long contentLength = downloader.contentLength();
      if (contentLength <= 0 && is == null) {
        throw new DownloadException(statusCode, "content length error");
      }
      boolean noContentLength = contentLength <= 0;
      contentLength += bytesWritten;

      updateStart(request, contentLength);
      logger.log("Start to download, content length: " + contentLength + " bytes");

      if (is != null) {
        byte[] buffer = new byte[BUFFER_SIZE];
        int length;

        long start = SystemClock.uptimeMillis();
        long hasReadBytes = 0;
        while (true) {
          /* if the request has canceld, stop the downloading */
          if (quit || isInterrupted() || request.isCanceled()) {
            request.finish();
            return;
          }

          /* if current is not wifi and mobile network is not allowed, stop */
          if (request.allowedNetworkTypes() != 0
              && !Utils.isWifi(request.context())
              && (request.allowedNetworkTypes() & DownloadRequest.NETWORK_MOBILE) == 0) {
            throw new DownloadException(statusCode, "allowed network error");
          }

          /* read data into buffer from input stream */
          length = readFromInputStream(buffer, is);
          long fileSize = raf.length();
          long totalBytes = noContentLength ? fileSize : contentLength;

          if (length == END_OF_STREAM) {
            updateSuccess(request, hasReadBytes, SystemClock.uptimeMillis() - start);
            return;
          } else if (length == Integer.MIN_VALUE) {
            throw new DownloadException(statusCode, "transfer data error");
          }
          hasReadBytes += length;
          bytesWritten += length;
          /* write buffer into local file */
          raf.write(buffer, 0, length);

          /* deliver progress callback */
          updateProgress(request, bytesWritten, totalBytes);
        }
      } else {
        throw new DownloadException(statusCode, "input stream error");
      }
    } catch (IOException e) {
      logger.log("Caught new exception: name:" + request.destinationFilePath(), e);

      if (e instanceof DownloadException) {
        DownloadException exception = (DownloadException) e;
        updateFailure(request, exception.getCode(), exception.getMessage());
      } else {
        //如果range超出范围，会报java.io.FileNotFoundException，比如tmp文件来不及改成原名字时
        File file = new File(request.tempFilePath());
        if (file.exists()) {
          file.delete();
        }
        updateFailure(request, 0, e.getMessage());
      }
    } finally {
      downloader.close();
      silentCloseFile(raf);
      silentCloseInputStream(is);
    }
  }

  /* read data from input stream */
  int readFromInputStream(byte[] buffer, InputStream is) {
    try {
      return is.read(buffer);
    } catch (IOException e) {
      return Integer.MIN_VALUE;
    }
  }

  /* a utility function to close a random access file without raising an exception */
  static void silentCloseFile(RandomAccessFile raf) {
    if (raf != null) {
      try {
        raf.close();
      } catch (IOException ignore) {
      }
    }
  }

  /* a utility function to close an input stream without raising an exception */
  static void silentCloseInputStream(InputStream is) {
    try {
      if (is != null) {
        is.close();
      }
    } catch (IOException ignore) {
    }
  }

  /**
   * Forces this dispatcher to quit immediately. If any download requests are still in
   * the queue, they are not guaranteed to be processed.
   */
  void quit() {
    logger.log("Download dispatcher quit");
    quit = true;

    /* interrupt current thread */
    interrupt();
  }
}
