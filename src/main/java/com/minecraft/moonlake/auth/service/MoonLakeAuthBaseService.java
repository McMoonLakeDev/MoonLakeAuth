/*
 * Copyright (C) 2017 The MoonLake Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.minecraft.moonlake.auth.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.minecraft.moonlake.auth.response.MojangBaseResponse;
import com.minecraft.moonlake.auth.response.MojangStatusResponse;
import com.minecraft.moonlake.auth.response.ProfileHistoryResponse;
import com.minecraft.moonlake.auth.response.ProfileSearchResponse;
import com.minecraft.moonlake.auth.exception.MoonLakeInvalidCredentialsException;
import com.minecraft.moonlake.auth.exception.MoonLakeRequestException;
import com.minecraft.moonlake.auth.exception.MoonLakeSerivceUnavailableException;
import com.minecraft.moonlake.auth.exception.MoonLakeUserMigratedException;
import com.minecraft.moonlake.auth.util.UUIDSerializer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.UUID;

public abstract class MoonLakeAuthBaseService implements MoonLakeAuthService {

    private final static Gson GSON;

    static {
        GSON = new GsonBuilder()
                .registerTypeAdapter(UUID.class, new UUIDSerializer())
                .registerTypeAdapter(ProfileSearchResponse.class, new ProfileSearchResponse.Serializer())
                .registerTypeAdapter(ProfileHistoryResponse.class, new ProfileHistoryResponse.Serializer())
                .registerTypeAdapter(MojangStatusResponse.class, new MojangStatusResponse.Serializer())
                .create();
    }

    private Proxy proxy;

    protected MoonLakeAuthBaseService() {
        this.proxy = Proxy.NO_PROXY;
    }

    public MoonLakeAuthBaseService(Proxy proxy) {
        if(proxy == null)
            throw new IllegalArgumentException("代理对象不能为 null 值.");
        this.proxy = proxy;
    }

    public Proxy getProxy() {
        return proxy;
    }

    protected static void validate(Object obj, String message) throws IllegalArgumentException {
        if(obj == null)
            throw new IllegalArgumentException(message);
    }

    private static void validateProxyAndURL(Proxy proxy, String url) throws IllegalArgumentException {
        validate(proxy, "代理对象不能为 null 值.");
        validate(url, "目标 URL 对象不能为 null 值.");
    }

    private static HttpURLConnection createURLConnection(String url) throws IOException {
        return createURLConnection(Proxy.NO_PROXY, url);
    }

    private static HttpURLConnection createURLConnection(Proxy proxy, String url) throws IOException {
        validateProxyAndURL(proxy, url);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection(proxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setUseCaches(false);
        return connection;
    }

    private static String getConnectionResult(HttpURLConnection connection) throws IOException {
        InputStream input = null;
        try {
            try {
                input = connection.getInputStream();
            } catch (Exception e) {
                input = connection.getErrorStream();
            }
            if(input != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuilder builder = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null)
                    builder.append(line).append("\n");
                return builder.toString();
            } else {
                return "";
            }
        } finally {
            if(input != null) try {
                input.close();
            } catch (Exception e) {
            }
        }
    }

    private static String fromGetRequest(String url) throws IOException {
        return fromGetRequest(Proxy.NO_PROXY, url);
    }

    private static String fromGetRequest(Proxy proxy, String url) throws IOException {
        validateProxyAndURL(proxy, url);
        HttpURLConnection connection = createURLConnection(proxy, url);
        connection.setDoInput(true);
        return getConnectionResult(connection);
    }

    private static String fromPostRequest(String url, String postData, String contentType) throws IOException {
        return fromPostRequest(Proxy.NO_PROXY, url, postData, contentType);
    }

    private static String fromPostRequest(Proxy proxy, String url, String postData, String contentType) throws IOException {
        validateProxyAndURL(proxy, url);
        byte[] bytes = postData.getBytes(Charset.forName("utf-8"));
        HttpURLConnection connection = createURLConnection(proxy, url);
        connection.setRequestProperty("Content-Type", contentType + "; charset=utf-8");
        connection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
        connection.setDoInput(true);
        connection.setDoOutput(true);

        OutputStream out = null;
        try {
            out = connection.getOutputStream();
            out.write(bytes);
        } finally {
            if(out != null) try {
                out.close();
            } catch (Exception e) {
            }
        }
        return getConnectionResult(connection);
    }

    protected static Gson getGson() {
        return GSON;
    }

    protected static boolean isBlank(String str) {
        return str == null || str.equals("");
    }

    protected static void makeRequest(Proxy proxy, String url, Object request) throws MoonLakeRequestException {
        makeRequest(proxy, url, request, MojangBaseResponse.class);
    }

    protected static <T extends MojangBaseResponse> T makeRequest(Proxy proxy, String url, Object request, Class<T> responseClass) throws MoonLakeRequestException {
        MojangBaseResponse response = null;
        try {
            String data = request == null ? fromGetRequest(proxy, url) : fromPostRequest(proxy, url, GSON.toJson(request), "application/json");
            response = GSON.fromJson(data, responseClass);
        } catch (Exception e) {
            throw new MoonLakeSerivceUnavailableException("无法创建服务请求: " + url, e);
        }
        if(response != null && !isBlank(response.getError())) {
            if(!response.getError().equals("ForbiddenOperationException"))
                throw new MoonLakeRequestException(response.getErrorMessage());
            if(response.getCause() != null && response.getCause().equals("UserMigratedException"))
                throw new MoonLakeUserMigratedException(response.getErrorMessage());
            else
                throw new MoonLakeInvalidCredentialsException(response.getErrorMessage());
        }
        return (T) response;
    }
}
