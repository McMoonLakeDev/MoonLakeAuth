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


package com.minecraft.moonlake.auth.service.profile;

import com.minecraft.moonlake.auth.data.*;
import com.minecraft.moonlake.auth.exception.*;
import com.minecraft.moonlake.auth.response.MojangBaseResponse;
import com.minecraft.moonlake.auth.response.ProfileHistoryResponse;
import com.minecraft.moonlake.auth.response.ProfileSearchResponse;
import com.minecraft.moonlake.auth.service.MoonLakeAuthBaseService;
import com.minecraft.moonlake.auth.service.mc.MinecraftAuthService;
import com.minecraft.moonlake.auth.util.UUIDSerializer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.net.Proxy;
import java.net.URL;
import java.util.*;
import java.util.List;

public class ProfileAuthService extends MoonLakeAuthBaseService {

    private final static String URL_PROFILES = "https://api.mojang.com/profiles/minecraft";
    private final static String URL_PROFILE_TIME = "https://api.mojang.com/users/profiles/minecraft";
    private final static String URL_PROFILE_HISTORY = "https://api.mojang.com/user/profiles/%1$s/names";
    private final static int MAX_FAIL_COUNT = 3;
    private final static int DELAY_BETWEEN_PAGES = 100;
    private final static int DELAY_BETWEEN_FAILURES = 750;
    private final static int PROFILES_PER_REQUEST = 100;

    public ProfileAuthService() {
        super();
    }

    public ProfileAuthService(Proxy proxy) {
        super(proxy);
    }

    public void findProfileByName(final String name, final ProfileLookupCallback callback) {
        findProfileByName(name, callback, false);
    }

    public void findProfileByName(final String name, final ProfileLookupCallback callback, boolean async) {
        validate(name, "名称对象不能为 null 值.");
        findProfilesByName(new String[] { name }, callback, async);
    }

    public void findProfilesByName(final String[] names, final ProfileLookupCallback callback) {
        findProfilesByName(names, callback, false);
    }

    public void findProfilesByName(final String[] names, final ProfileLookupCallback callback, boolean async) {
        validate(names, "名称数组不能为 null 值.");
        validate(callback, "游戏档案查询回调对象不能为 null 值.");
        final Set<String> nameSet = new HashSet<>();
        for(String name : names)
            if(name != null && !name.isEmpty())
                nameSet.add(name.toLowerCase());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for(Set<String> request : fromRequest(nameSet, PROFILES_PER_REQUEST)) {
                    int failedCount = 0;
                    boolean tryAgain = true;
                    while(failedCount < MAX_FAIL_COUNT && tryAgain) {
                        tryAgain = false;
                        try {
                            ProfileSearchResponse response = makeRequest(getProxy(), URL_PROFILES, request, ProfileSearchResponse.class);
                            failedCount = 0;
                            Set<String> missing = new HashSet<>(request);
                            for(GameProfile profile : response.getProfiles()) {
                                missing.remove(profile.getName().toLowerCase());
                                callback.onLookupSucceeded(profile);
                            }
                            for(String name : missing)
                                callback.onLookupFailed(new GameProfile((UUID) null, name), new MoonLakeProfileNotFoundException("服务器没有存在名为 '" + name + "' 的游戏档案."));
                            try {
                                Thread.sleep(DELAY_BETWEEN_PAGES);
                            } catch (InterruptedException e) {
                            }
                        } catch (MoonLakeRequestException e) {
                            failedCount++;
                            if(failedCount >= MAX_FAIL_COUNT) {
                                for(String name : request)
                                    callback.onLookupFailed(new GameProfile((UUID) null, name), e);
                            } else {
                                try {
                                    Thread.sleep(DELAY_BETWEEN_FAILURES);
                                } catch (InterruptedException e1) {
                                }
                                tryAgain = true;
                            }
                        }
                    }
                }
            }
        };
        start(runnable, async);
    }

    public void findProfileByTimestamp(String name, ProfileLookupCallback callback) {
        findProfileByTimestamp(name, callback, false);
    }

    public void findProfileByTimestamp(String name, ProfileLookupCallback callback, boolean async) {
        findProfileByTimestamp(name, -1L, callback, async);
    }

    public void findProfileByTimestamp(String name, long timestamp, ProfileLookupCallback callback) {
        findProfileByTimestamp(name, timestamp, callback, false);
    }

    public void findProfileByTimestamp(String name, long timestamp, ProfileLookupCallback callback, boolean async) {
        validate(name, "名称对象不能为 null 值.");
        validate(callback, "游戏档案查询回调对象不能为 null 值.");
        final String finalUrl = URL_PROFILE_TIME + "/" + name + (timestamp < 0L ? "" : ("?at=" + timestamp));
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ProfileTimestampResponse response = makeRequest(getProxy(), finalUrl, null, ProfileTimestampResponse.class);
                    callback.onLookupSucceeded(new GameProfile(response.id, response.name));
                } catch (Exception e) {
                    callback.onLookupFailed(new GameProfile((UUID) null, name), e);
                }
            }
        };
        start(runnable, async);
    }

    public void findNameHistoryByProfile(GameProfile profile, ProfileHistoryCallback callback) {
        findNameHistoryByProfile(profile, callback, false);
    }

    public void findNameHistoryByProfile(GameProfile profile, ProfileHistoryCallback callback, boolean async) {
        validate(profile, "游戏档案对象不能为 null 值.");
        validate(callback, "档案历史回调对象不能为 null 值.");
        findNameHistoryById(profile.getId(), callback, async);
    }

    public void findNameHistoryById(UUID id, ProfileHistoryCallback callback) {
        findNameHistoryById(id, callback, false);
    }

    public void findNameHistoryById(UUID id, ProfileHistoryCallback callback, boolean async) {
        validate(id, "目标 UUID 对象不能为 null 值.");
        final String finalUrl = String.format(URL_PROFILE_HISTORY, UUIDSerializer.fromUUID(id));
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    ProfileHistoryResponse response = makeRequest(getProxy(), finalUrl, null, ProfileHistoryResponse.class);
                    callback.onLookupSucceeded(id, new ProfileHistoryList(Arrays.asList(response.getHistories())));
                } catch (Exception e) {
                    callback.onLookupFailed(id, e);
                }
            }
        };
        start(runnable, async);
    }

    public void findSkinRawTextureByName(String name, SkinRawImageCallback<String> callback) throws MoonLakeAuthException {
        findSkinRawTextureByName(name, callback, false);
    }

    public void findSkinRawTextureByName(String name, SkinRawImageCallback<String> callback, boolean async) throws MoonLakeAuthException {
        validate(name, "用户名对象不能为 null 值.");
        validate(callback, "皮肤源图片回调对象不能为 null 值.");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                findProfileByName(name, new ProfileLookupCallback() {
                    @Override
                    public void onLookupSucceeded(GameProfile profile) {
                        try {
                            findSkinRawTextureByProfile(profile, new SkinRawImageCallback<GameProfile>() {
                                @Override
                                public void onLookupSucceeded(GameProfile param, BufferedImage skinRawImage) {
                                    callback.onLookupSucceeded(param.getName(), skinRawImage);
                                }

                                @Override
                                public void onLookupFailed(GameProfile param, Exception ex) {
                                    callback.onLookupFailed(param.getName(), ex);
                                }
                            }, false); // false 不再使用新的线程, 而在当前线程
                        } catch (MoonLakeAuthException e) {
                            callback.onLookupFailed(name, e);
                        }
                    }

                    @Override
                    public void onLookupFailed(GameProfile profile, Exception ex) {
                        callback.onLookupFailed(profile.getName(), ex);
                    }
                });
            }
        };
        start(runnable, async);
    }

    public void findSkinRawTextureByProfile(GameProfile profile, SkinRawImageCallback<GameProfile> callback) throws MoonLakeAuthException {
        findSkinRawTextureByProfile(profile, callback, false);
    }

    public void findSkinRawTextureByProfile(GameProfile profile, SkinRawImageCallback<GameProfile> callback, boolean async) throws MoonLakeAuthException {
        validate(profile, "游戏档案对象不能为 null 值.");
        validate(callback, "皮肤源图片回调对象不能为 null 值.");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean existGetSucceed = false;
                ProfileTexture skinTexture = null;
                Map<TextureType, ProfileTexture> textures = profile.getTextures();
                if(!textures.isEmpty() && (skinTexture = textures.get(TextureType.SKIN)) != null && !isBlank(skinTexture.getUrl())) {
                    try {
                        BufferedImage image = getSkinRawTextureByProfile(profile);
                        callback.onLookupSucceeded(profile, image);
                        existGetSucceed = true;
                    } catch (MoonLakeSkinException e) {
                    }
                }
                if(existGetSucceed)
                    return;
                // 当前游戏档案不存在材质属性数据则进行获取
                try {
                    MinecraftAuthService minecraftAuthService = new MinecraftAuthService(getProxy());
                    minecraftAuthService.fillProfileProperties(profile);
                    minecraftAuthService.fillProfileTextures(profile);
                    BufferedImage image = getSkinRawTextureByProfile(profile);
                    callback.onLookupSucceeded(profile, image);
                } catch (Exception e) {
                    callback.onLookupFailed(profile, e);
                }
            }
        };
        start(runnable, async);
    }

    public BufferedImage getSkinRawTextureByProfile(GameProfile profile) throws MoonLakeSkinException {
        validate(profile, "游戏档案对象不能为 null 值.");
        try {
            ProfileTexture skinTexture = null;
            Map<TextureType, ProfileTexture> textures = profile.getTextures();
            if(textures.isEmpty() || (skinTexture = textures.get(TextureType.SKIN)) == null || isBlank(skinTexture.getUrl()))
                throw new MoonLakeSkinNotFoundException("游戏档案对象不存在任何皮肤材质数据.");
            return ImageIO.read(new URL(skinTexture.getUrl()));
        } catch (Exception e) {
            throw new MoonLakeSkinException("获取游戏档案的皮肤材质数据时错误.", e);
        }
    }

    public void findSkinHeadTextureByName(String name, SkinRawImageCallback<String> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByName(name, 8, callback);
    }

    public void findSkinHeadTextureByName(String name, int zoom, SkinRawImageCallback<String> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByName(name, zoom, true, callback);
    }

    public void findSkinHeadTextureByName(String name, int zoom, boolean helmet, SkinRawImageCallback<String> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByName(name, zoom, helmet, callback, false);
    }

    public void findSkinHeadTextureByName(String name, int zoom, boolean helmet, SkinRawImageCallback<String> callback, boolean async) throws MoonLakeSkinException {
        validate(name, "用户名对象不能为 null 值.");
        validate(callback, "皮肤源图片回调对象不能为 null 值.");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                findProfileByName(name, new ProfileLookupCallback() {
                    @Override
                    public void onLookupSucceeded(GameProfile profile) {
                        try {
                            findSkinHeadTextureByProfile(profile, zoom, helmet, new SkinRawImageCallback<GameProfile>() {
                                @Override
                                public void onLookupSucceeded(GameProfile param, BufferedImage headImage) {
                                    callback.onLookupSucceeded(param.getName(), headImage);
                                }

                                @Override
                                public void onLookupFailed(GameProfile param, Exception ex) {
                                    callback.onLookupFailed(param.getName(), ex);
                                }
                            });
                        } catch (MoonLakeAuthException e) {
                            callback.onLookupFailed(name, e);
                        }
                    }

                    @Override
                    public void onLookupFailed(GameProfile profile, Exception ex) {
                        callback.onLookupFailed(profile.getName(), ex);
                    }
                });
            }
        };
        start(runnable, async);
    }

    public void findSkinHeadTextureByProfile(GameProfile profile, SkinRawImageCallback<GameProfile> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByProfile(profile, 8, callback);
    }

    public void findSkinHeadTextureByProfile(GameProfile profile, int zoom, SkinRawImageCallback<GameProfile> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByProfile(profile, zoom, true, callback);
    }

    public void findSkinHeadTextureByProfile(GameProfile profile, int zoom, boolean helmet, SkinRawImageCallback<GameProfile> callback) throws MoonLakeSkinException {
        findSkinHeadTextureByProfile(profile, zoom, helmet, callback, false);
    }

    public void findSkinHeadTextureByProfile(GameProfile profile, int zoom, boolean helmet, SkinRawImageCallback<GameProfile> callback, boolean async) throws MoonLakeSkinException {
        validate(profile, "游戏档案对象不能为 null 值.");
        validate(callback, "皮肤源图片回调对象不能为 null 值.");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean existGetSucceed = false;
                ProfileTexture skinTexture = null;
                Map<TextureType, ProfileTexture> textures = profile.getTextures();
                if(!textures.isEmpty() && (skinTexture = textures.get(TextureType.SKIN)) != null && !isBlank(skinTexture.getUrl())) {
                    try {
                        BufferedImage image = getSkinRawTextureByProfile(profile);
                        BufferedImage headImage = getSkinHeadTextureByRaw(image, zoom, helmet);
                        callback.onLookupSucceeded(profile, headImage);
                        existGetSucceed = true;
                    } catch (MoonLakeSkinException e) {
                    }
                }
                if(existGetSucceed)
                    return;
                // 当前游戏档案不存在材质属性数据则进行获取
                try {
                    MinecraftAuthService minecraftAuthService = new MinecraftAuthService(getProxy());
                    minecraftAuthService.fillProfileProperties(profile);
                    minecraftAuthService.fillProfileTextures(profile);
                    BufferedImage image = getSkinRawTextureByProfile(profile);
                    BufferedImage headImage = getSkinHeadTextureByRaw(image, zoom, helmet);
                    callback.onLookupSucceeded(profile, headImage);
                } catch (Exception e) {
                    callback.onLookupFailed(profile, e);
                }
            }
        };
        start(runnable, async);
    }

    public BufferedImage getSkinHeadTextureByProfile(GameProfile profile) throws MoonLakeSkinException {
        return getSkinHeadTextureByProfile(profile, 8);
    }

    public BufferedImage getSkinHeadTextureByProfile(GameProfile profile, int zoom) throws MoonLakeSkinException {
        return getSkinHeadTextureByProfile(profile, zoom, true);
    }

    public BufferedImage getSkinHeadTextureByProfile(GameProfile profile, int zoom, boolean helmet) throws MoonLakeSkinException {
        validate(profile, "游戏档案对象不能为 null 值.");
        BufferedImage skinRawImage = getSkinRawTextureByProfile(profile);
        return getSkinHeadTextureByRaw(skinRawImage, zoom, helmet);
    }

    public BufferedImage getSkinHeadTextureByRaw(BufferedImage skinRawImage) throws MoonLakeSkinException {
        return getSkinHeadTextureByRaw(skinRawImage, 8);
    }

    public BufferedImage getSkinHeadTextureByRaw(BufferedImage skinRawImage, int zoom) throws MoonLakeSkinException {
        return getSkinHeadTextureByRaw(skinRawImage, zoom, true);
    }

    public BufferedImage getSkinHeadTextureByRaw(BufferedImage skinRawImage, int zoom, boolean helmet) throws MoonLakeSkinException {
        validate(skinRawImage, "皮肤材质源图片对象不能为 null 值.");
        Boolean skinVer = getSkinRawImageVer(skinRawImage);
        if(skinVer == null)
            throw new MoonLakeSkinException("错误的皮肤材质源图片大小, 应为 64x64 或 64x32 大小.");
        if(zoom <= 0)
            zoom = 1;
        if(skinRawImage.getType() != BufferedImage.TYPE_INT_ARGB)
            skinRawImage = conversionImage(skinRawImage, BufferedImage.TYPE_INT_ARGB);
        // 如果皮肤源图片的类型不为 INT_ARGB 通道则进行重绘并转换
        BufferedImage skinHeadImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        skinHeadImage.setRGB(0, 0, 8, 8, skinRawImage.getRGB(8, 8, 8, 8, null, 0, 8), 0, 8);
        int[] helmetRgb = helmet ? skinRawImage.getRGB(40, 8, 8, 8, null, 0, 8) : null;
        if(helmet)
            for(int i = 0; i < helmetRgb.length; i++)
                if(!isInvalidColorRgb(helmetRgb[i])) // 如果该像素点不是无效的则填充
                    skinHeadImage.setRGB(i % 8, i / 8, helmetRgb[i]);
        // 将 8x8 像素的头像图片进行放大处理
        BufferedImage finalImage = new BufferedImage(8 * zoom, 8 * zoom, BufferedImage.TYPE_INT_ARGB);
        Graphics g = finalImage.createGraphics();
        g.drawImage(skinHeadImage, 0, 0, finalImage.getWidth(), finalImage.getHeight(), null);
        g.dispose();
        return finalImage;
    }

    private static void start(final Runnable runnable, boolean async) {
        if(async)
            new Thread(runnable, "ProfileAuthService").start();
        else
            runnable.run();
    }

    private static Set<Set<String>> fromRequest(Set<String> set, int size) {
        List<String> list = new ArrayList<>(set);
        Set<Set<String>> request = new HashSet<>();
        for(int i = 0; i < list.size(); i+= size) {
            Set<String> value = new HashSet<>();
            value.addAll(list.subList(i, Math.min(i + size, list.size())));
            request.add(value);
        }
        return request;
    }

    private static Boolean getSkinRawImageVer(BufferedImage skinRawImage) {
        // 获取皮肤材质源图片的版本
        // false: 旧版本 64x32 像素
        // true: 新版本 64x64 像素
        // null: 错误的像素大小
        int width = skinRawImage.getWidth();
        int height = skinRawImage.getHeight();
        if(width == 64) {
            if(height == 64)
                return true;
            if(height == 32)
                return false;
        }
        return null;
    }

    private static BufferedImage conversionImage(BufferedImage image, int type) {
        BufferedImage conversion = new BufferedImage(image.getWidth(), image.getHeight(), type);
        conversion.getGraphics().drawImage(image, 0, 0, null);
        return conversion;
    }

    private static boolean isInvalidColorRgb(int rgb) {
        int r = (rgb & 0xff0000) >> 16;
        int g = (rgb & 0xff00) >> 8;
        int b = (rgb & 0xff);
        return (r == 0 && g == 0 && b == 0) || (r == 0xff && g == 0xff && b == 0xff);
    }

    private static class ProfileTimestampResponse extends MojangBaseResponse {
        private UUID id;
        private String name;

        protected ProfileTimestampResponse(UUID id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
