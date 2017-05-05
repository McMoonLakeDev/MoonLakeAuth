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

import com.minecraft.moonlake.auth.data.GameProfile;
import com.minecraft.moonlake.auth.data.ProfileLookupCallback;
import com.minecraft.moonlake.auth.exception.MoonLakeAuthException;
import com.minecraft.moonlake.auth.service.mc.MinecraftAuthService;
import com.minecraft.moonlake.auth.service.profile.ProfileAuthService;
import com.minecraft.moonlake.auth.service.user.UserAuthService;
import org.junit.Test;

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
            mcAuthService.fillProfileTexutes(authService.getSelectedProfile()); // 填充游戏档案的材质属性数据
            System.out.println(authService.getSelectedProfile()); // 打印最终游戏档案

            // 登出当前用户服务
            authService.invalidateToken(); // 将访问令牌进行失效
        }
    }
}
