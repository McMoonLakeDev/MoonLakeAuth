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


package com.minecraft.moonlake.auth.service.mc;

import com.minecraft.moonlake.auth.data.*;
import com.minecraft.moonlake.auth.exception.MoonLakeProfileException;
import com.minecraft.moonlake.auth.exception.MoonLakeProfileNotFoundException;
import com.minecraft.moonlake.auth.exception.MoonLakeRequestException;
import com.minecraft.moonlake.auth.response.MojangBaseResponse;
import com.minecraft.moonlake.auth.service.MoonLakeAuthBaseService;
import com.minecraft.moonlake.auth.util.UUIDSerializer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Proxy;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class MinecraftAuthService extends MoonLakeAuthBaseService {

    private final static String URL_JOIN = "https://sessionserver.mojang.com/session/minecraft/join";
    private final static String URL_HAS_JOINED = "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private final static String URL_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile";
    private final static PublicKey SIGNATURE_KEY;

    static {
        InputStream input = null;
        try {
            input = MinecraftAuthService.class.getResourceAsStream("/yggdrasil_session_pubkey.der");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int length;
            while((length = input.read(buff)) != -1)
                output.write(buff, 0, length);
            input.close();
            output.close();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(output.toByteArray());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            SIGNATURE_KEY = keyFactory.generatePublic(spec);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError("不存在或无效的 yggdrasil 公钥.");
        } finally {
            if(input != null) try {
                input.close();
            } catch (Exception e) {
            }
        }
    }

    public MinecraftAuthService() {
        super();
    }

    public MinecraftAuthService(Proxy proxy) {
        super(proxy);
    }

    public void joinServerRequest(GameProfile profile, String accessToken, String serverId) throws MoonLakeRequestException {
        validate(profile, "游戏档案对象不能为 null 值.");
        validate(accessToken, "访问令牌对象不能为 null 值.");
        validate(serverId, "目标服务器 Id 对象不能为 null 值.");
        JoinServerRequest request = new JoinServerRequest(accessToken, profile.getId(), serverId);
        makeRequest(getProxy(), URL_JOIN, request);
    }

    public GameProfile getProfileByServer(String name, String serverId) throws MoonLakeRequestException {
        validate(name, "用户名对象不能为 null 值.");
        validate(serverId, "目标服务器 Id 对象不能为 null 值.");
        HasJoinedResponse response = makeRequest(getProxy(), String.format("%1$s?username=%2$s&serverId=%3$s", URL_HAS_JOINED, name, serverId), null, HasJoinedResponse.class);
        if(response != null && response.id != null) {
            GameProfile result = new GameProfile(response.id, name);
            if(response.properties != null)
                result.getProperties().addAll(response.properties);
            return result;
        }
        return null;
    }

    public GameProfile fillProfileProperties(GameProfile profile) throws MoonLakeProfileException {
        validate(profile, "游戏档案对象不能为 null 值.");
        if(profile.getId() == null)
            return profile;
        try {
            String finalURL = String.format("%1$s/%2$s?unsigned=false", URL_PROFILE, UUIDSerializer.fromUUID(profile.getId()));
            MinecraftProfileResponse response = makeRequest(getProxy(), finalURL, null, MinecraftProfileResponse.class);
            if(response == null)
                throw new MoonLakeProfileNotFoundException("无法获取到游戏档案的属性数据, 不存在此游戏档案.");
            if(response.properties != null)
                profile.getProperties().addAll(response.properties);
            return profile;
        } catch (MoonLakeRequestException e) {
            e.printStackTrace();
            throw new MoonLakeProfileException("无法获取到游戏档案的属性数据.", e);
        }
    }

    public GameProfile fillProfileTexutes(GameProfile profile) throws MoonLakeProfileException {
        return fillProfileTexutes(profile, true);
    }

    public GameProfile fillProfileTexutes(GameProfile profile, boolean requireSecure) throws MoonLakeProfileException {
        Map<TextureType, ProfileTexture> textures = getProfileTextures(profile, requireSecure);
        profile.getTextures().putAll(textures);
        return profile;
    }

    public Map<TextureType, ProfileTexture> getProfileTextures(GameProfile profile) throws MoonLakeProfileException {
        return getProfileTextures(profile, true);
    }

    public Map<TextureType, ProfileTexture> getProfileTextures(GameProfile profile, boolean requireSecure) throws MoonLakeProfileException {
        validate(profile, "游戏档案对象不能为 null 值.");
        Property property = profile.getProperty("textures");
        if(property == null)
            return new HashMap<>();
        if(!property.hasSignature())
            throw new MoonLakeProfileException("游戏档案的属性材质数据不存在签名.");
        if(!property.validateSignature(SIGNATURE_KEY))
            throw new MoonLakeProfileException("无法验证游戏档案的属性材质数据的签名值.");
        MinecraftTexturesPayload result = null;
        try {
            String data = new String(Base64.getDecoder().decode(property.getValue().getBytes(Charset.forName("utf-8"))));
            result = getGson().fromJson(data, MinecraftTexturesPayload.class);
        } catch (Exception e) {
            throw new MoonLakeProfileException("无法将游戏档案的属性材质数据值进行解密.", e);
        }
        Map<TextureType, ProfileTexture> textures = result.textures;
        if(textures == null)
            textures = new HashMap<>();
        return textures;
    }

    private static class JoinServerRequest {
        private String accessToken;
        private UUID selectedProfile;
        private String serverId;

        protected JoinServerRequest(String accessToken, UUID selectedProfile, String serverId) {
            this.accessToken = accessToken;
            this.selectedProfile = selectedProfile;
            this.serverId = serverId;
        }
    }

    private static class HasJoinedResponse extends MojangBaseResponse {
        public UUID id;
        public List<Property> properties;
    }

    private static class MinecraftProfileResponse extends MojangBaseResponse {
        public UUID id;
        public String name;
        public List<Property> properties;
    }

    private static class MinecraftTexturesPayload {
        public long timestamp;
        public UUID profileId;
        public String profileName;
        public boolean isPublic;
        public Map<TextureType, ProfileTexture> textures;
    }
}
