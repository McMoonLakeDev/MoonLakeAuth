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


package com.minecraft.moonlake.auth.response;

import com.google.gson.*;
import com.minecraft.moonlake.auth.data.StatusService;

import java.lang.reflect.Type;
import java.util.Map;

public class MojangStatusResponse extends MojangBaseResponse {

    private StatusService[] services;

    public MojangStatusResponse(StatusService[] services) {
        this.services = services;
    }

    public StatusService[] getServices() {
        return services;
    }

    public final static class Serializer implements JsonDeserializer<MojangStatusResponse> {

        @Override
        public MojangStatusResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if(jsonElement instanceof JsonArray) {
                JsonArray jsonArray = (JsonArray) jsonElement;
                StatusService[] services = new StatusService[jsonArray.size()];
                for(int i = 0; i < services.length; i++) {
                    JsonObject service = jsonArray.get(i).getAsJsonObject();
                    String host = "", status = "";
                    for(Map.Entry<String, JsonElement> entry : service.entrySet()) {
                        host = entry.getKey();
                        status = entry.getValue().getAsString();
                    }
                    services[i] = new StatusService(host, StatusService.Type.fromName(status));
                }
                return new MojangStatusResponse(services);
            }
            return null;
        }
    }
}
