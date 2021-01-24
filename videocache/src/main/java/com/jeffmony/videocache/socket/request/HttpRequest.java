package com.jeffmony.videocache.socket.request;

import com.jeffmony.videocache.common.VideoCacheException;
import com.jeffmony.videocache.utils.ProxyCacheUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import javax.net.ssl.SSLException;

/**
 * @author jeffmony
 */

public class HttpRequest {
    private static final String HEADER_RANGE = "range";   //头部统一转化为小写
    private final BufferedInputStream mInputStream;
    private final String mRemoteIP;
    private final HashMap<String, String> mHeaders;
    private HashMap<String, String> mParams;
    private Method mMethod;
    private String mUri;
    private String mProtocolVersion;
    private boolean mKeepAlive;

    public HttpRequest(InputStream inputStream, InetAddress inetAddress) {
        mInputStream = new BufferedInputStream(inputStream);

        // isLoopbackAddress() : local address; 127.0.0.0 ~ 127.255.255.255
        // isAnyLocalAddress() : normal address ?
        mRemoteIP = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()
                        ? ProxyCacheUtils.LOCAL_PROXY_HOST
                        : inetAddress.getHostAddress();
        String mRemoteHostname = inetAddress.isLoopbackAddress() || inetAddress.isAnyLocalAddress()
                ? "localhost"
                : inetAddress.getHostName();
        mHeaders = new HashMap<String, String>();
        mKeepAlive = true;
    }

    public void parseRequest() throws Exception {
        byte[] buf = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
        int splitByteIndex = 0;
        int readLength = 0;

        int read = -1;
        this.mInputStream.mark(ProxyCacheUtils.DEFAULT_BUFFER_SIZE);
        try {
            read = this.mInputStream.read(buf, 0, ProxyCacheUtils.DEFAULT_BUFFER_SIZE);
        } catch (SSLException e) {
            ProxyCacheUtils.close(this.mInputStream);
            throw e;
        } catch (IOException e) {
            ProxyCacheUtils.close(this.mInputStream);
            throw new SocketException("Socket Shutdown");
        } catch (Exception e) {
            ProxyCacheUtils.close(this.mInputStream);
            throw new VideoCacheException("Other exception");
        }
        if (read == -1) {
            ProxyCacheUtils.close(this.mInputStream);
            throw new SocketException("Can't read inputStream");
        }
        while (read > 0) {
            readLength += read;
            splitByteIndex = findResponseHeaderEnd(buf, readLength);
            if (splitByteIndex > 0) {
                break;
            }
            read = mInputStream.read(buf, readLength, ProxyCacheUtils.DEFAULT_BUFFER_SIZE - readLength);
        }

        if (splitByteIndex < readLength) {
            mInputStream.reset();
            mInputStream.skip(splitByteIndex);
        }

        mParams = new HashMap<String, String>();
        mHeaders.clear();

        // Create a BufferedReader for parsing the header.
        BufferedReader headerReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(buf, 0, readLength)));

        // Decode the header into params and header java properties
        Map<String, String> extraInfo = new HashMap<String, String>();
        decodeHeader(headerReader, extraInfo, this.mParams, this.mHeaders);

        if (null != this.mRemoteIP) {
            this.mHeaders.put("remote-addr", this.mRemoteIP);
            this.mHeaders.put("http-client-ip", this.mRemoteIP);
        }

        this.mMethod = Method.lookup(extraInfo.get("method"));
        if (this.mMethod == null) {
            throw new VideoCacheException("BAD REQUEST: Syntax error. HTTP verb " + extraInfo.get("method") + " unhandled.");
        }

        this.mUri = extraInfo.get("uri");

        String connection = this.mHeaders.get("connection");
        mKeepAlive = "HTTP/1.1".equals(mProtocolVersion) &&
                (connection == null || !connection.matches("(?i).*close.*"));
    }

    // GET / HTTP/1.1\r\nHost: www.sina.com.cn\r\nConnection: close\r\n\r\n
    //'\r\n\r\n'
    private int findResponseHeaderEnd(final byte[] buf, int readLength) {
        int splitByteIndex = 0;
        while (splitByteIndex + 1 < readLength) {

            // RFC2616
            if (buf[splitByteIndex] == '\r' && buf[splitByteIndex + 1] == '\n' &&
                    splitByteIndex + 3 < readLength && buf[splitByteIndex + 2] == '\r' &&
                    buf[splitByteIndex + 3] == '\n') {
                return splitByteIndex + 4;
            }

            // tolerance
            if (buf[splitByteIndex] == '\n' && buf[splitByteIndex + 1] == '\n') {
                return splitByteIndex + 2;
            }
            splitByteIndex++;
        }
        return 0;
    }

    private void decodeHeader(BufferedReader headerReader, Map<String, String> extraInfo,
                 Map<String, String> params, Map<String, String> headers)
            throws VideoCacheException {
        try {
            // Read the request line
            String readLine = headerReader.readLine();
            if (readLine == null) {
                return;
            }

            StringTokenizer st = new StringTokenizer(readLine);
            if (!st.hasMoreTokens()) {
                throw new VideoCacheException("Bad request, syntax error, correct format: GET /example/file.html");
            }

            extraInfo.put("method", st.nextToken());

            if (!st.hasMoreTokens()) {
                throw new VideoCacheException("Bad request, syntax error, correct format: GET /example/file.html");
            }

            String uri = st.nextToken();

            // Decode parameters from the URI
            int questionMaskIndex = uri.indexOf('?');
            if (questionMaskIndex >= 0 && questionMaskIndex < uri.length()) {
                decodeParams(uri.substring(questionMaskIndex + 1), params);
                uri = ProxyCacheUtils.decodeUri(uri.substring(0, questionMaskIndex));
            } else {
                uri = ProxyCacheUtils.decodeUri(uri);
            }

            // If there's another token, its protocol version,
            // followed by HTTP headers.
            // NOTE: this now forces header names lower case since they are
            // case insensitive and vary by client.
            if (st.hasMoreTokens()) {
                mProtocolVersion = st.nextToken();
            } else {
                // default protocol version
                mProtocolVersion = "HTTP/1.1";
            }

            // parse headers:
            String line = headerReader.readLine();
            while (line != null && !line.trim().isEmpty()) {
                int index = line.indexOf(':');
                if (index >= 0 && index < line.length()) {
                    headers.put(line.substring(0, index).trim().toLowerCase(Locale.US),
                            line.substring(index + 1).trim());
                }
                line = headerReader.readLine();
            }

            extraInfo.put("uri", uri);
        } catch (IOException e) {
            throw new VideoCacheException("Parsing Header Exception: " + e.getMessage(), e);
        }
    }

    private void decodeParams(String params, Map<String, String> paramsMap) {
        if (params == null) {
            return;
        }

        StringTokenizer st = new StringTokenizer(params, "&");
        while (st.hasMoreTokens()) {
            String item = st.nextToken();
            int index = item.indexOf('=');
            if (index >= 0 && index < item.length()) {
                paramsMap.put(
                        ProxyCacheUtils.decodeUri(item.substring(0, index)).trim(),
                        ProxyCacheUtils.decodeUri(item.substring(index + 1)));
            } else {
                paramsMap.put(ProxyCacheUtils.decodeUri(item).trim(), "");
            }
        }
    }

    public String getMimeType() {
        return "video/mpeg";
    }

    public String getProtocolVersion() {
        return mProtocolVersion;
    }

    public String getUri() {
        return String.valueOf(mUri);
    }

    public boolean keepAlive() {
        return mKeepAlive;
    }

    public Method requestMethod() {
        return mMethod;
    }

    public String getRangeString() {
        return mHeaders.get(HEADER_RANGE);
    }
}
