package com.jeffmony.videocache.socket.response;

import com.jeffmony.videocache.VideoLockManager;
import com.jeffmony.videocache.VideoProxyCacheManager;
import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.socket.request.HttpRequest;
import com.jeffmony.videocache.socket.request.ResponseState;
import com.jeffmony.videocache.utils.HttpUtils;
import com.jeffmony.videocache.utils.LogUtils;
import com.jeffmony.videocache.utils.ProxyCacheUtils;
import com.jeffmony.videocache.utils.StorageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * @author jeffmony
 * M3U8-TS视频的local server端
 *
 * https://iqiyi.cdn9-okzy.com/20210217/22550_b228d68b/1000k/hls/6f2ac117eac000000.ts&jeffmony_seg&/c462e3fd379ce23333aabed0a3837848/0.ts&jeffmony_seg&unknown
 */
public class M3U8SegResponseNew extends BaseResponse {

    private static final String TAG = "M3U8SegResponseNew";

    private static final String TEMP_POSTFIX = ".downloading";

    private String mParentUrl;
    private final File mSegFile;
    private final String mSegUrl;
    private final String mM3U8Md5;    //对应M3U8 url的md5值
    private final int mSegIndex;      //M3U8 ts对应的索引位置
    private long mSegLength;
    private final String mFileName;

    public M3U8SegResponseNew(HttpRequest request, String parentUrl, String videoUrl, Map<String, String> headers, long time, String fileName) throws Exception {
        super(request, videoUrl, headers, time);
        mParentUrl = parentUrl;
        mSegUrl = videoUrl;
        mSegFile = new File(mCachePath, fileName);//fileName:/d6035ce66faad6d47b1ec371ba2d7672/1.ts
        mFileName = mSegFile.getName();
        LogUtils.i(TAG, "SegFilePath="+mSegFile.getAbsolutePath());
        mM3U8Md5 = getM3U8Md5(fileName);
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mHeaders.put("Connection", "close");
        mSegIndex = getSegIndex(fileName);
        mResponseState = ResponseState.OK;
        LogUtils.i(TAG, "start M3U8SegResponse: index=" + mSegIndex +", parentUrl=" + mParentUrl + "\n, segUrl=" + mSegUrl);
        VideoProxyCacheManager.getInstance().notifyCurSegIndex(mParentUrl, mSegIndex, time);
    }

    private String getM3U8Md5(String str) throws VideoCacheException {
        str = str.substring(1);
        int index = str.indexOf("/");
        if (index == -1) {
            throw new VideoCacheException("Error index during getMd5");
        }
        return str.substring(0, index);
    }

    private int getSegIndex(String str) throws VideoCacheException {
        int dotIndex = str.lastIndexOf(".");
        if (dotIndex == -1) {
            throw new VideoCacheException("Error index during getTcd sIndex");
        }
        str = str.substring(0, dotIndex);
        int separatorIndex = str.lastIndexOf("/");
        if (separatorIndex == -1) {
            throw new VideoCacheException("Error index during getTsIndex");
        }
        str = str.substring(separatorIndex + 1);
        if (str.startsWith(ProxyCacheUtils.INIT_SEGMENT_PREFIX)) {
            str = str.substring(ProxyCacheUtils.INIT_SEGMENT_PREFIX.length());
            LogUtils.i(TAG, "str = " + str);
        }
        return Integer.parseInt(str);
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {
        Object lock = VideoLockManager.getInstance().getLock(mM3U8Md5);
        long wait = 0;
        //todo 如何感知播放器已经断开，避免占用线程池
        while(!mSegFile.exists() && wait < TIME_OUT) {
            synchronized (lock) {
                lock.wait(WAIT_TIME);
            }
            if (wait == 0) {
                LogUtils.d(TAG,  "wait " + mSegFile.getName() + " available");
            }
            wait += WAIT_TIME;
        }
        //播放器可能超时
        if (!mSegFile.exists()) {
            LogUtils.e(TAG, "wait " + mSegFile.getName() + " timeout" + " socket.isClosed:" + socket.isClosed() + ",socket.isOutputShutdown:" + socket.isOutputShutdown());
            return;
        }
        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(mSegFile, "r");
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];
            long offset = 0;

            if(shouldSendResponse(socket, mM3U8Md5)) {
                randomAccessFile.seek(offset);
                int readLength;
                while((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                    offset += readLength;
                    outputStream.write(buffer, 0, readLength);
                    randomAccessFile.seek(offset);
                }
                LogUtils.d(TAG, "Send M3U8 ts file end, this="+this);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }

    private void downloadSegFile(String url, File file) throws Exception {
        LogUtils.i(TAG, "downloadSegFile file:" + file);
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(url, mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseState.OK.getResponseCode() || responseCode == ResponseState.PARTIAL_CONTENT.getResponseCode()) {
                inputStream = connection.getInputStream();
                mSegLength = connection.getContentLength();
                saveSegFile(inputStream, file);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            ProxyCacheUtils.close(inputStream);
        }
    }

    private void saveSegFile(InputStream inputStream, File file) throws Exception {
        FileOutputStream fos = null;
        long totalLength = 0;
        File tmpFile = new File(file.getParentFile(), file.getName() + TEMP_POSTFIX);
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
        try {
            fos = new FileOutputStream(tmpFile);
            int readLength;
            byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            while ((readLength = inputStream.read(buffer)) != -1) {
                totalLength += readLength;
                fos.write(buffer, 0, readLength);
            }
            tmpFile.renameTo(file);
        } catch (Exception e) {
            if (tmpFile.exists() && ((mSegLength > 0 && mSegLength == tmpFile.length()) || (mSegLength == -1 && tmpFile.length() == totalLength))) {
                //说明此文件下载完成
                tmpFile.renameTo(file);
            } else {
                tmpFile.delete();
            }
            throw e;
        } finally {
            ProxyCacheUtils.close(fos);
            ProxyCacheUtils.close(inputStream);
        }
    }


}
