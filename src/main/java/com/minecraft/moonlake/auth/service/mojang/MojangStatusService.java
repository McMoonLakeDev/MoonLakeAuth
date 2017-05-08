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


package com.minecraft.moonlake.auth.service.mojang;

import com.minecraft.moonlake.auth.data.StatusServiceCallback;
import com.minecraft.moonlake.auth.data.StatusServiceList;
import com.minecraft.moonlake.auth.response.MojangStatusResponse;
import com.minecraft.moonlake.auth.service.MoonLakeAuthBaseService;

import java.net.Proxy;
import java.util.Arrays;

public class MojangStatusService extends MoonLakeAuthBaseService {

    private final static String URL_STATUS = "https://status.mojang.com/check";

    public MojangStatusService() {
        super();
    }

    public MojangStatusService(Proxy proxy) {
        super(proxy);
    }

    public void checkMojangStatus(StatusServiceCallback callback) {
        checkMojangStatus(callback, false);
    }

    public void checkMojangStatus(StatusServiceCallback callback, boolean async) {
        validate(callback, "状态服务回调对象不能为 null 值.");
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    MojangStatusResponse response = makeRequest(getProxy(), URL_STATUS, null, MojangStatusResponse.class);
                    callback.onCheckSucceeded(new StatusServiceList(Arrays.asList(response.getServices())));
                } catch (Exception e) {
                    callback.onCheckFailed(e);
                }
            }
        };
        start(runnable, async);
    }

    private static void start(final Runnable runnable, boolean async) {
        if(async)
            new Thread(runnable, "MojangStatusService").start();
        else
            runnable.run();
    }
}
