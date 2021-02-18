package com.getcapacitor.plugin.http;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class HttpRequestHandler {
    /**
     * Internal builder class for building a CapacitorHttpUrlConnection
     */
    private static class HttpURLConnectionBuilder {
        private Integer connectTimeout;
        private Integer readTimeout;
        private JSObject headers;
        private String method;
        private URL url;

        private CapacitorHttpUrlConnection connection;

        public HttpURLConnectionBuilder setConnectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public HttpURLConnectionBuilder setReadTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public HttpURLConnectionBuilder setHeaders(JSObject headers) {
            this.headers = headers;
            return this;
        }

        public HttpURLConnectionBuilder setMethod(String method) {
            this.method = method;
            return this;
        }

        public HttpURLConnectionBuilder setUrl(URL url) {
            this.url = url;
            return this;
        }

        public HttpURLConnectionBuilder openConnection() throws IOException {
            connection = new CapacitorHttpUrlConnection((HttpURLConnection) url.openConnection());

            connection.setAllowUserInteraction(false);
            connection.setRequestMethod(method);

            if (connectTimeout != null)  connection.setConnectTimeout(connectTimeout);
            if (readTimeout != null) connection.setReadTimeout(readTimeout);

            connection.setRequestHeaders(headers);
            return this;
        }

        public  HttpURLConnectionBuilder setUrlParams(JSObject params) throws MalformedURLException, URISyntaxException {
            return this.setUrlParams(params, true);
        }

        public HttpURLConnectionBuilder setUrlParams(JSObject params, boolean shouldEncode) throws URISyntaxException, MalformedURLException {
            String initialQuery = url.getQuery();
            String initialQueryBuilderStr = initialQuery == null ? "" : initialQuery;

            Iterator<String> keys = params.keys();
            StringBuilder urlQueryBuilder = new StringBuilder(initialQueryBuilderStr);

            // Build the new query string
            while (keys.hasNext()) {
                String key = keys.next();
                String value = params.getString(key);
                if (urlQueryBuilder.length() > 0) {
                    urlQueryBuilder.append("&");
                }
                urlQueryBuilder.append(key)
                        .append("=")
                        .append(value);
            }

            String urlQuery = urlQueryBuilder.toString();

            URI uri = url.toURI();
            if (shouldEncode) {
                URI encodedUri = new URI(uri.getScheme(),
                        uri.getAuthority(),
                        uri.getPath(),
                        urlQuery,
                        uri.getFragment());
                this.url = encodedUri.toURL();
            } else {
                String unEncodedUrlString = uri.getScheme() +
                        uri.getAuthority() +
                        uri.getPath() +
                        urlQuery +
                        uri.getFragment();
                this.url = new URL(unEncodedUrlString);
            }

            return this;
        }

        public CapacitorHttpUrlConnection build() {
            return connection;
        }
    }

    /**
     * See https://developer.mozilla.org/en-US/docs/Web/API/XMLHttpRequest/responseType
     */
    public enum ResponseType {
        ARRAY_BUFFER("arraybuffer"),
        BLOB("blob"),
        DOCUMENT("document"),
        JSON("json"),
        TEXT("text");

        private final String name;

        ResponseType(String name) {
            this.name = name;
        }

        static final ResponseType DEFAULT = TEXT;

        static ResponseType parse(String value) {
            for (ResponseType responseType: values()) {
                if (responseType.name.equalsIgnoreCase(value)) {
                    return responseType;
                }
            }
            return DEFAULT;
        }
    }

    private static void buildResponse(CapacitorHttpUrlConnection conn) throws IOException, JSONException {
        buildResponse(conn, ResponseType.DEFAULT);
    }

    private static JSObject buildResponse(CapacitorHttpUrlConnection connection, ResponseType responseType) throws IOException, JSONException {
        int statusCode = connection.getResponseCode();

        JSObject output = new JSObject();
        output.put("status", statusCode);
        output.put("headers", buildResponseHeaders(connection));
        output.put("url", connection.getURL());

        // Log.d(getLogTag(), "Request completed, got data");

        InputStream errorStream = connection.getErrorStream();
        String contentType = connection.getHeaderField("Content-Type");

        if (contentType != null && contentType.contains("application/json")) {
            // backward compatibility
            InputStream stream = (errorStream != null ? errorStream : connection.getInputStream());
            output.put("data", parseJSON(readStreamAsString(stream)));
        } else {
            InputStream inputStream = connection.getInputStream();
            switch (responseType) {
                case ARRAY_BUFFER:
                case BLOB:
                    output.put("data", readStreamAsBase64(inputStream));
                    break;
                case JSON:
                    output.put("data", parseJSON(readStreamAsString(inputStream)));
                    break;
                case DOCUMENT:
                case TEXT:
                    output.put("data", readStreamAsString(inputStream));
                    break;
            }
        }

        return output;
    }

    private static JSObject buildResponseHeaders(CapacitorHttpUrlConnection connection) {
        JSObject output = new JSObject();

        for (Map.Entry<String, List<String>> entry : connection.getHeaderFields().entrySet()) {
            String valuesString = TextUtils.join(", ", entry.getValue());
            output.put(entry.getKey(), valuesString);
        }

        return output;
    }

    /**
     * Returns a JSObject or a JSArray based on a string-ified input
     * @param input String-ified JSON that needs parsing
     * @return A JSObject or JSArray
     * @throws JSONException thrown if the JSON is malformed
     */
    private static Object parseJSON(String input) throws JSONException {
        try {
            if ("null".equals(input)) {
                return JSONObject.NULL;
            } else {
                try {
                    return new JSObject(input);
                } catch (JSONException e) {
                    return new JSArray(input);
                }
            }
        } catch (JSONException e) {
            return new JSArray(input);
        }
    }

    private static String readStreamAsBase64(InputStream in) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int readBytes;
            while ((readBytes = in.read(buffer)) != -1) {
                out.write(buffer, 0, readBytes);
            }
            byte[] result = out.toByteArray();
            return Base64.encodeToString(result, 0, result.length, Base64.DEFAULT);
        }
    }

    private static String readStreamAsString(InputStream in) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append(System.getProperty("line.separator"));
            }
            return builder.toString();
        }
    }

    /**
     * Makes an Http Request based on the PluginCall parameters
     * @param call The Capacitor PluginCall that contains the options need for an Http request
     * @throws IOException throws an IO request when a connection can't be made
     * @throws URISyntaxException thrown when the URI is malformed
     * @throws JSONException thrown when the incoming JSON is malformed
     */
    public static JSObject request(PluginCall call) throws IOException, URISyntaxException, JSONException {
        String urlString = call.getString("url");
        String method = call.getString("method").toUpperCase();
        JSObject headers = call.getObject("headers");
        JSObject params = call.getObject("params");
        Integer connectTimeout = call.getInt("connectTimeout");
        Integer readTimeout = call.getInt("readTimeout");
        ResponseType responseType = ResponseType.parse(call.getString("responseType"));

        boolean isHttpMutate = method.equals("DELETE") ||
                method.equals("PATCH") ||
                method.equals("POST") ||
                method.equals("PUT");

        URL url = new URL(urlString);
        HttpURLConnectionBuilder connectionBuilder = new HttpURLConnectionBuilder()
                .setUrl(url)
                .setMethod(method)
                .setHeaders(headers)
                .setUrlParams(params)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .openConnection();

        CapacitorHttpUrlConnection connection = connectionBuilder.build();

        // Set HTTP body on a non GET or HEAD request
        if (isHttpMutate) {
            JSObject data = call.getObject("data");
            connection.setDoOutput(true);
            connection.setRequestBody(data);
            connection.connect();
        }

        return buildResponse(connection, responseType);
    }

    /**
     * Makes an Http Request to download a file based on the PluginCall parameters
     * @param call The Capacitor PluginCall that contains the options need for an Http request
     * @param context The Android Context required for writing to the filesystem
     * @throws IOException throws an IO request when a connection can't be made
     * @throws URISyntaxException thrown when the URI is malformed
     */
    public static JSObject downloadFile(PluginCall call, Context context) throws IOException, URISyntaxException {
        String urlString = call.getString("url");
        String method = call.getString("method").toUpperCase();
        String filePath = call.getString("filePath");
        String fileDirectory = call.getString("fileDirectory", FilesystemUtils.DIRECTORY_DOCUMENTS);
        JSObject headers = call.getObject("headers");
        JSObject params = call.getObject("params");
        Integer connectTimeout = call.getInt("connectTimeout");
        Integer readTimeout = call.getInt("readTimeout");

        final URL url = new URL(urlString);
        final File file = FilesystemUtils.getFileObject(context, filePath, fileDirectory);

        HttpURLConnectionBuilder connectionBuilder = new HttpURLConnectionBuilder()
                .setUrl(url)
                .setMethod(method)
                .setHeaders(headers)
                .setUrlParams(params)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .openConnection();

        CapacitorHttpUrlConnection connection = connectionBuilder.build();
        InputStream connectionInputStream = connection.getInputStream();

        FileOutputStream fileOutputStream = new FileOutputStream(file, false);

        byte[] buffer = new byte[1024];
        int len;

        while ((len = connectionInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, len);
        }

        connectionInputStream.close();
        fileOutputStream.close();

        return new JSObject() {
            {
                put("path", file.getAbsolutePath());
            }
        };
    }

    /**
     * Makes an Http Request to upload a file based on the PluginCall parameters
     * @param call The Capacitor PluginCall that contains the options need for an Http request
     * @param context The Android Context required for writing to the filesystem
     * @throws IOException throws an IO request when a connection can't be made
     * @throws URISyntaxException thrown when the URI is malformed
     * @throws JSONException thrown when malformed JSON is passed into the function
     */
    public static JSObject uploadFile(PluginCall call, Context context) throws IOException, URISyntaxException, JSONException {
        String urlString = call.getString("url");
        String method = call.getString("method").toUpperCase();
        String filePath = call.getString("filePath");
        String fileDirectory = call.getString("fileDirectory", FilesystemUtils.DIRECTORY_DOCUMENTS);
        String name = call.getString("name", "file");
        Integer connectTimeout = call.getInt("connectTimeout");
        Integer readTimeout = call.getInt("readTimeout");
        JSObject headers = call.getObject("headers");
        JSObject params = call.getObject("params");
        JSObject data = call.getObject("data");
        ResponseType responseType = ResponseType.parse(call.getString("responseType"));

        URL url = new URL(urlString);

        File file = FilesystemUtils.getFileObject(context, filePath, fileDirectory);

        HttpURLConnectionBuilder connectionBuilder = new HttpURLConnectionBuilder()
                .setUrl(url)
                .setMethod(method)
                .setHeaders(headers)
                .setUrlParams(params)
                .setConnectTimeout(connectTimeout)
                .setReadTimeout(readTimeout)
                .openConnection();

        CapacitorHttpUrlConnection connection = connectionBuilder.build();
        connection.setDoOutput(true);

        FormUploader builder = new FormUploader(connection.getHttpConnection());
        builder.addFilePart(name, file, data);
        builder.finish();

        return buildResponse(connection, responseType);
    }
}
