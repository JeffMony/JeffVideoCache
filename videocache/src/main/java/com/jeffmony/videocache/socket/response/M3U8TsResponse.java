package com.jeffmony.videocache.socket.response;

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
 * https://iqiyi.cdn9-okzy.com/20210217/22550_b228d68b/1000k/hls/6f2ac117eac000000.ts&jeffmony_ts&/c462e3fd379ce23333aabed0a3837848/0.ts&jeffmony_ts&unknown
 */
public class M3U8TsResponse extends BaseResponse {

    private static final String TAG = "M3U8TsResponse";
    private File mTsFile;
    private String mTsUrl;
    private String mM3U8Md5;    //对应M3U8 url的md5值
    private int mTsIndex;       //M3U8 ts对应的索引位置
    private long mTsLength;

    public M3U8TsResponse(HttpRequest request, String videoUrl, Map<String, String> headers, String fileName) throws Exception {
        super(request, videoUrl, headers);
        mTsUrl = videoUrl;
        mTsFile = new File(mCachePath, fileName);
        mM3U8Md5 = getM3U8Md5(fileName);
        if (mHeaders == null) {
            mHeaders = new HashMap<>();
        }
        mHeaders.put("Connection", "close");
        mTsIndex = getTsIndex(fileName);
        mResponseState = ResponseState.OK;
    }

    private String getM3U8Md5(String str) throws VideoCacheException {
        str = str.substring(1);
        int index = str.indexOf("/");
        if (index == -1) {
            throw new VideoCacheException("Error index during getMd5");
        }
        return str.substring(0, index);
    }

    private int getTsIndex(String str) throws VideoCacheException {
        int idotIndex = str.lastIndexOf(".");
        if (idotIndex == -1) {
            throw new VideoCacheException("Error index during getTcd sIndex");
        }
        str = str.substring(0, idotIndex);
        int seperatorIndex = str.lastIndexOf("/");
        if (seperatorIndex == -1) {
            throw new VideoCacheException("Error index during getTsIndex");
        }
        str = str.substring(seperatorIndex + 1);
        return Integer.parseInt(str);
    }

    @Override
    public void sendBody(Socket socket, OutputStream outputStream, long pending) throws Exception {
        boolean isM3U8TsCompleted = VideoProxyCacheManager.getInstance().isM3U8TsCompleted(mM3U8Md5, mTsIndex, mTsFile.getAbsolutePath());
        while (!isM3U8TsCompleted) {
            downloadTsFile(mTsUrl, mTsFile);
            isM3U8TsCompleted = VideoProxyCacheManager.getInstance().isM3U8TsCompleted(mM3U8Md5, mTsIndex, mTsFile.getAbsolutePath());
            if (mTsLength > 0 && mTsLength == mTsFile.length()) {
                break;
            }
        }
        LogUtils.d(TAG, "FilePath="+mTsFile.getAbsolutePath()+", FileLength="+mTsFile.length()+", tsLength="+mTsLength);

        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(mTsFile, "r");
            if (randomAccessFile == null) {
                throw new VideoCacheException("M3U8 ts file not found, this=" + this);
            }
            int bufferedSize = StorageUtils.DEFAULT_BUFFER_SIZE;
            byte[] buffer = new byte[bufferedSize];
            long offset = 0;

            while(shouldSendResponse(socket, mM3U8Md5)) {
                randomAccessFile.seek(offset);
                int readLength;
                while((readLength = randomAccessFile.read(buffer, 0, buffer.length)) != -1) {
                    offset += readLength;
                    outputStream.write(buffer, 0, readLength);
                    randomAccessFile.seek(offset);
                }
                LogUtils.d(TAG, "Send M3U8 ts file end, this="+this);
                break;
            }
        } catch (Exception e) {
            throw e;
        } finally {
            ProxyCacheUtils.close(randomAccessFile);
        }
    }

    private void downloadTsFile(String url, File file) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            connection = HttpUtils.getConnection(url, mHeaders);
            int responseCode = connection.getResponseCode();
            if (responseCode == ResponseState.OK.getResponseCode() || responseCode == ResponseState.PARTIAL_CONTENT.getResponseCode()) {
                inputStream = connection.getInputStream();
                mTsLength = connection.getContentLength();
                saveTsFile(inputStream, file);
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

    private void saveTsFile(InputStream inputStream, File file) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int readLength;
            byte[] buffer = new byte[StorageUtils.DEFAULT_BUFFER_SIZE];
            while ((readLength = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, readLength);
            }
        } catch (Exception e) {
            if (file.exists() && mTsLength > 0 && mTsLength == file.length()) {
                //说明此文件下载完成
            } else {
                file.delete();
            }
            throw e;
        } finally {
            ProxyCacheUtils.close(fos);
            ProxyCacheUtils.close(inputStream);
        }
    }


}
