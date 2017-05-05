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


package com.minecraft.moonlake.auth.service.user;

import com.minecraft.moonlake.auth.data.GameProfile;
import com.minecraft.moonlake.auth.data.MojangBaseResponse;
import com.minecraft.moonlake.auth.data.Property;
import com.minecraft.moonlake.auth.exception.MoonLakeAuthException;
import com.minecraft.moonlake.auth.exception.MoonLakeRequestException;
import com.minecraft.moonlake.auth.service.MoonLakeAuthBaseService;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class UserAuthService extends MoonLakeAuthBaseService {

    private final static String URL_REFRESH = "https://authserver.mojang.com/refresh";
    private final static String URL_SIGNOUT = "https://authserver.mojang.com/signout";
    private final static String URL_VALIDATE = "https://authserver.mojang.com/validate";
    private final static String URL_INVALIDATE = "https://authserver.mojang.com/invalidate";
    private final static String URL_AUTHENTICATE = "https://authserver.mojang.com/authenticate";

    private String id;
    private String username;
    private String password;
    private String clientToken;
    private String accessToken;
    private boolean loggedIn;
    private GameProfile selectedProfile;
    private List<Property> properties = new ArrayList<>();
    private List<GameProfile> profiles = new ArrayList<>();

    public UserAuthService() {
        this(Proxy.NO_PROXY);
    }

    public UserAuthService(Proxy proxy) {
        this(UUID.randomUUID().toString(), proxy);
    }

    public UserAuthService(String clientToken) {
        this(clientToken, Proxy.NO_PROXY);
    }

    public UserAuthService(String clientToken, Proxy proxy) {
        super(proxy);
        this.clientToken = clientToken;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<GameProfile> getAvailableProfiles() {
        return profiles;
    }

    public GameProfile getSelectedProfile() {
        return selectedProfile;
    }

    public void setUsername(String username) {
        checkLoginState("用户名");
        this.username = username;
    }

    public void setPassword(String password) {
        checkLoginState("密码");
        this.password = password;
    }

    public void setAccessToken(String accessToken) {
        checkLoginState("访问令牌");
        this.accessToken = accessToken;
    }

    public void login() throws MoonLakeAuthException {
        checkStringBlank(username, new MoonLakeAuthException("无效的用户名."));
        if(!isBlank(accessToken)) {
            loginWithToken();
        } else {
            checkStringBlank(password, new MoonLakeAuthException("无效的密码."));
            loginWithPassword();
        }
    }

    public void logout() throws MoonLakeRequestException {
        if(!loggedIn)
            throw new IllegalStateException("无法登出, 因为当前没有处于登录状态.");
        this.id = null;
        this.loggedIn = false;
        this.accessToken = null;
        this.selectedProfile = null;
        this.profiles.clear();
        this.properties.clear();
    }

    public boolean signoutToken() throws MoonLakeAuthException {
        checkStringBlank(username, new MoonLakeAuthException("无效的用户名."));
        checkStringBlank(password, new MoonLakeAuthException("无效的密码."));
        try {
            SignoutRequest request = new SignoutRequest(username, password);
            makeRequest(getProxy(), URL_SIGNOUT, request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean invalidateToken() throws MoonLakeAuthException {
        checkStringBlank(accessToken, new MoonLakeAuthException("无效的访问令牌."));
        try {
            InvalidateRequest request = new InvalidateRequest(clientToken, accessToken);
            makeRequest(getProxy(), URL_INVALIDATE, request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateToken() throws MoonLakeAuthException {
        checkStringBlank(accessToken, new MoonLakeAuthException("无效的访问令牌."));
        try {
            ValidateRequest request = new ValidateRequest(clientToken, accessToken);
            makeRequest(getProxy(), URL_VALIDATE, request);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void selectGameProfile(GameProfile profile) throws MoonLakeAuthException {
        if(!loggedIn)
            throw new IllegalStateException("无法选择游戏档案, 因为当前没有登录.");
        if(selectedProfile == null)
            throw new IllegalStateException("已选择游戏档案时无法再次选择.");
        if(profile != null && profiles.contains(profile)) {
            RefreshRequest request = new RefreshRequest(clientToken, accessToken, profile);
            RefreshResponse response = makeRequest(getProxy(), URL_REFRESH, request, RefreshResponse.class);
            if(response != null && clientToken.equals(response.clientToken)) {
                this.accessToken = response.accessToken;
                this.selectedProfile = response.selectedProfile;
            }
            throw new MoonLakeAuthException("错误: 服务器请求更改客户端令牌.");
        }
        throw new IllegalStateException("无效的游戏档案.");
    }

    @Override
    public String toString() {
        return "UserAuthService{" +
                "username='" + username + '\'' +
                ", clientToken='" + clientToken + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", loggedIn=" + loggedIn +
                ", selectedProfile=" + selectedProfile +
                ", profiles=" + profiles +
                '}';
    }

    private void checkStringBlank(String str, MoonLakeAuthException e) throws MoonLakeAuthException {
        if(isBlank(str))
            throw e;
    }

    private void checkLoginState(String type) throws IllegalStateException {
        if(loggedIn && selectedProfile != null)
            throw new IllegalStateException("已处于登录状态并已选择游戏档案, 不能再修改" + type + "属性.");
    }

    private void loginWithPassword() throws MoonLakeAuthException {
        checkStringBlank(username, new MoonLakeAuthException("无效的用户名."));
        checkStringBlank(password, new MoonLakeAuthException("无效的密码."));
        AuthenticationRequest request = new AuthenticationRequest(username, password, clientToken);
        AuthenticationResponse response = makeRequest(getProxy(), URL_AUTHENTICATE, request, AuthenticationResponse.class);
        if(response != null && clientToken.equals(response.clientToken))
            loginProperty(response.user, response.accessToken, response.selectedProfile, response.availableProfiles);
        else
            throw new MoonLakeAuthException("错误: 服务器请求更改客户端令牌.");
    }

    private void loginWithToken() throws MoonLakeAuthException {
        if(id == null || id.isEmpty()) {
            checkStringBlank(username, new MoonLakeAuthException("无效的用户名."));
            this.id = username;
        }
        checkStringBlank(accessToken, new MoonLakeAuthException("无效的访问令牌."));
        RefreshRequest request = new RefreshRequest(clientToken, accessToken, null);
        RefreshResponse response = makeRequest(getProxy(), URL_REFRESH, request, RefreshResponse.class);
        if(response != null && clientToken.equals(response.clientToken))
            loginProperty(response.user, response.accessToken, response.selectedProfile, response.availableProfiles);
        else
            throw new MoonLakeAuthException("错误: 服务器请求更改客户端令牌.");
    }

    private void loginProperty(User user, String accessToken, GameProfile selectedProfile, GameProfile[] availableProfiles) {
        if(user != null && user.id != null)
            this.id = user.id;
        else
            this.id = username;
        this.loggedIn = true;
        this.accessToken = accessToken;
        this.selectedProfile = selectedProfile;
        this.profiles = availableProfiles != null ? Arrays.asList(availableProfiles) : new ArrayList<>();
        this.properties.clear();
        if(user != null && user.properties != null)
            this.properties.addAll(user.properties);
    }

    private static class Agent {
        private String name;
        private int version;

        protected Agent(String name, int version) {
            this.name = name;
            this.version = version;
        }
    }

    private static class User {
        public String id;
        public List<Property> properties;
    }

    private static class AuthenticationRequest {
        private Agent agent;
        private String username;
        private String password;
        private boolean requestUser;
        private String clientToken;

        protected AuthenticationRequest(String username, String password, String clientToken) {
            this.agent = new Agent("Minecraft", 1);
            this.username = username;
            this.password = password;
            this.requestUser = true;
            this.clientToken = clientToken;
        }
    }

    private static class RefreshRequest {
        private String clientToken;
        private String accessToken;
        private GameProfile selectedProfile;
        private boolean requestUser;

        protected RefreshRequest(String clientToken, String accessToken, GameProfile selectedProfile) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.requestUser = true;
        }
    }

    private static class ValidateRequest {
        private String clientToken;
        private String accessToken;

        protected ValidateRequest(String clientToken, String accessToken) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }
    }

    private static class InvalidateRequest {
        private String clientToken;
        private String accessToken;

        protected InvalidateRequest(String clientToken, String accessToken) {
            this.clientToken = clientToken;
            this.accessToken = accessToken;
        }
    }

    private static class SignoutRequest {
        private String username;
        private String password;

        protected SignoutRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }

    private static class AuthenticationResponse extends MojangBaseResponse {
        public String accessToken;
        public String clientToken;
        public GameProfile selectedProfile;
        public GameProfile[] availableProfiles;
        public User user;
    }

    private static class RefreshResponse extends MojangBaseResponse {
        public String accessToken;
        public String clientToken;
        public GameProfile selectedProfile;
        public GameProfile[] availableProfiles;
        public User user;
    }
}
