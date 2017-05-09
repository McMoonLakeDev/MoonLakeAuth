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


package com.minecraft.moonlake.auth.test;

import com.minecraft.moonlake.auth.data.*;
import com.minecraft.moonlake.auth.exception.MoonLakeAuthException;
import com.minecraft.moonlake.auth.service.mc.MinecraftAuthService;
import com.minecraft.moonlake.auth.service.mojang.MojangStatusService;
import com.minecraft.moonlake.auth.service.profile.ProfileAuthService;
import com.minecraft.moonlake.auth.service.user.UserAuthService;
import com.minecraft.moonlake.auth.util.UUIDSerializer;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class MoonLakeAuthTest {

    public static void main(String[] args) throws Exception {
        // Minecraft 登录服务实现方法
        // 详情查看在线的 wiki 维基百科
        // http://wiki.vg/Mojang_API
        // http://wiki.vg/Authentication
    }

    @Test
    public void testProfileLookup() throws MoonLakeAuthException {
        // 测试查找指定玩家名的游戏档案数据
        String[] names = { "Notch", "Month_Light", "MoonLake", "jeb_", "Ni_xiaoqi" };
        ProfileAuthService authService = new ProfileAuthService();
        authService.findProfilesByName(names, new ProfileLookupCallback() {
            @Override
            public void onLookupSucceeded(GameProfile profile) {
                System.out.println("成功获取游戏档案: " + profile.toString());
            }

            @Override
            public void onLookupFailed(GameProfile profile, Exception ex) {
                System.out.println("获取游戏档案时异常: " + ex.getMessage());
            }
        });
    }

    @Test
    public void testProfileLookupTime() throws MoonLakeAuthException {
        // 测试从指定用户名和指定时间戳查询 UUID 值
        String name = "Month_Light";
        ProfileAuthService authService = new ProfileAuthService();
        authService.findProfileByTimestamp(name, new ProfileLookupCallback() {
            @Override
            public void onLookupSucceeded(GameProfile profile) {
                System.out.println("成功获取最新游戏档案: " + profile.toString());
            }

            @Override
            public void onLookupFailed(GameProfile profile, Exception ex) {
                System.out.println("获取游戏档案时异常: " + ex.getMessage());
            }
        });
    }

    @Test
    public void testNameHistory() throws MoonLakeAuthException {
        // 测试从指定用户 UUID 查询游戏名称历史记录
        ProfileAuthService authService = new ProfileAuthService();
        authService.findNameHistoryById(UUIDSerializer.fromString("e948f0b3-c9be-4909-a176-f13720d3be4c"), new ProfileHistoryCallback() {
            @Override
            public void onLookupSucceeded(UUID id, ProfileHistoryList historyList) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println("正在查询用户 '" + id + "' 的正版用户名历史记录: ");
                System.out.println("当前正在使用的用户名为: " + historyList.getLatest().getName());
                for(ProfileHistory history : historyList) {
                    System.out.print("用户名: " + history.getName());
                    for(int i = history.getName().length(); i < 16; i++)
                        System.out.print(" ");
                    System.out.print(" , 改变日期: " + (history.hasChangedToAt() ? sdf.format(new Date(history.getChangedToAt())) : "无"));
                    System.out.println();
                }
            }

            @Override
            public void onLookupFailed(UUID id, Exception ex) {
                System.out.println("获取用户 '" + id + "' 的名称历史记录时异常: " + ex.getMessage());
            }
        });
    }

    @Test
    public void testMojangStatus() throws MoonLakeAuthException {
        // 测试查询 mojang 服务器状态
        MojangStatusService statusService = new MojangStatusService();
        statusService.checkMojangStatus(new StatusServiceCallback() {
            @Override
            public void onCheckSucceeded(StatusServiceList serviceList) {
                System.out.println("请求查询 mojang 服务器状态: ");
                for(StatusService service : serviceList) {
                    System.out.printf("域名: %s, 状态: %s, 是否不可用: %s", service.getHost(), service.getType(), service.isUnavailable());
                    System.out.println();
                }
            }

            @Override
            public void onCheckFailed(Exception ex) {
                System.out.println("查询 mojang 服务器状态时异常: " + ex.getMessage());
            }
        });
    }

    @Test
    @Ignore // ignore
    public void testAuthMojang() throws MoonLakeAuthException {
        // 测试登录到 mojang 的 minecraft 账户
        String clientToken = "客户端令牌";
        String username = "mojang账户邮箱";
        String password = "mojang账户密码";
        UserAuthService authService = new UserAuthService(clientToken);
        authService.setUsername(username);
        authService.setPassword(password);
        authService.login();

        if(authService.isLoggedIn()) {
            // 如果成功登录则打印结果
            System.out.println("成功登录到 Mojang 正版账户.");
            System.out.println(authService.getSelectedProfile());

            // 获取当前用户的 minecraft 游戏档案数据
            MinecraftAuthService mcAuthService = new MinecraftAuthService();
            mcAuthService.fillProfileProperties(authService.getSelectedProfile()); // 填充游戏档案的属性数据
            mcAuthService.fillProfileTextures(authService.getSelectedProfile()); // 填充游戏档案的材质属性数据
            System.out.println(authService.getSelectedProfile()); // 打印最终游戏档案

            // 登出当前用户服务
            authService.invalidateToken(); // 将访问令牌进行失效
        }
    }

    @Test
    @Ignore // ignore
    public void testTexture() throws MoonLakeAuthException {
        // 测试获取指定玩家的正版皮肤材质源文件
        ProfileAuthService authService = new ProfileAuthService();
        authService.findSkinRawTextureByName("Month_Light", new SkinRawImageCallback<String>() {
            @Override
            public void onLookupSucceeded(String param, BufferedImage skinRawImage) {
                try {
                    File outFile = new File("src\\test\\" + param + ".png");
                    System.out.println("成功获取到玩家 '" + param + "' 的皮肤材质源文件:");
                    System.out.println("写出到: " + outFile.getAbsolutePath());
                    ImageIO.write(skinRawImage, "PNG", outFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onLookupFailed(String param, Exception ex) {
                ex.printStackTrace();
            }
        });
    }
}
