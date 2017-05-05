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

import com.minecraft.moonlake.auth.data.GameProfile;
import com.minecraft.moonlake.auth.data.ProfileLookupCallback;
import com.minecraft.moonlake.auth.data.ProfileSearchResponse;
import com.minecraft.moonlake.auth.exception.MoonLakeProfileNotFoundException;
import com.minecraft.moonlake.auth.exception.MoonLakeRequestException;
import com.minecraft.moonlake.auth.service.MoonLakeAuthBaseService;

import java.net.Proxy;
import java.util.*;

public class ProfileAuthService extends MoonLakeAuthBaseService {

    private final static String URL_PROFILES = "https://api.mojang.com/profiles/minecraft";
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
}