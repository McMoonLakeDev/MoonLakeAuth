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


package com.minecraft.moonlake.auth.data;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ProfileSearchResponse extends MojangBaseResponse {

    private GameProfile[] profiles;

    public ProfileSearchResponse() {
    }

    public GameProfile[] getProfiles() {
        return profiles;
    }

    public final static class Serializer implements JsonDeserializer<ProfileSearchResponse> {

        @Override
        public ProfileSearchResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            ProfileSearchResponse response = new ProfileSearchResponse();
            if(jsonElement instanceof JsonObject) {
                JsonObject jsonObject = (JsonObject) jsonElement;
                if(jsonObject.has("error"))
                    response.setError(jsonObject.get("error").getAsString());
                if(jsonObject.has("cause"))
                    response.setCause(jsonObject.get("cause").getAsString());
                if(jsonObject.has("errorMessage"))
                    response.setErrorMessage(jsonObject.get("errorMessage").getAsString());
            } else {
                response.profiles = jsonDeserializationContext.deserialize(jsonElement, GameProfile[].class);
            }
            return response;
        }
    }
}
