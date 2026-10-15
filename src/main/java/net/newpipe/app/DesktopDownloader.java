/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.schabi.newpipe.extractor.downloader.Downloader
 *  org.schabi.newpipe.extractor.downloader.Request
 *  org.schabi.newpipe.extractor.downloader.Response
 *  org.schabi.newpipe.extractor.exceptions.ReCaptchaException
 */
package net.newpipe.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

final class DesktopDownloader
extends Downloader {
    private static final int CONNECT_TIMEOUT_MILLIS = 15000;
    private static final int READ_TIMEOUT_MILLIS = 30000;
    private static final int MAX_RESPONSE_BYTES = 0x1000000;
    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0";

    DesktopDownloader() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Response execute(Request request) throws IOException, ReCaptchaException {
        HttpURLConnection connection = (HttpURLConnection)new URL(request.url()).openConnection();
        try {
            int statusCode;
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(request.httpMethod());
            connection.setRequestProperty("User-Agent", USER_AGENT);
            for (Map.Entry<String, List<String>> header : request.headers().entrySet()) {
                if (header.getKey() == null || header.getValue() == null) continue;
                boolean firstValue = true;
                for (String string : header.getValue()) {
                    if (string == null) continue;
                    if (firstValue) {
                        connection.setRequestProperty(header.getKey(), string);
                        firstValue = false;
                        continue;
                    }
                    connection.addRequestProperty(header.getKey(), string);
                }
            }
            byte[] body = request.dataToSend();
            if (body != null) {
                connection.setDoOutput(true);
                try (OutputStream output = connection.getOutputStream();){
                    output.write(body);
                }
            }
            if ((statusCode = connection.getResponseCode()) == 429) {
                throw new ReCaptchaException("reCaptcha challenge requested", request.url());
            }
            HashMap responseHeaders = new HashMap();
            for (Map.Entry entry : connection.getHeaderFields().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) continue;
                responseHeaders.put((String)entry.getKey(), new ArrayList((Collection)entry.getValue()));
            }
            InputStream responseStream = statusCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String string = responseStream == null ? "" : DesktopDownloader.readBody(responseStream);
            Response response = new Response(statusCode, connection.getResponseMessage(), responseHeaders, string, connection.getURL().toString());
            return response;
        }
        finally {
            connection.disconnect();
        }
    }

    private static String readBody(InputStream input) throws IOException {
        try (InputStream response = input;){
            String string;
            try (ByteArrayOutputStream output = new ByteArrayOutputStream();){
                int bytesRead;
                byte[] buffer = new byte[8192];
                int totalBytes = 0;
                while ((bytesRead = response.read(buffer)) != -1) {
                    if ((totalBytes += bytesRead) > 0x1000000) {
                        throw new IOException("Extractor response exceeded 16 MiB");
                    }
                    output.write(buffer, 0, bytesRead);
                }
                string = new String(output.toByteArray(), StandardCharsets.UTF_8);
            }
            return string;
        }
    }
}

