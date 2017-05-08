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
import com.minecraft.moonlake.auth.data.ProfileHistory;

import java.lang.reflect.Type;

public class ProfileHistoryResponse extends MojangBaseResponse {

    private ProfileHistory[] histories;

    public ProfileHistoryResponse(ProfileHistory[] histories) {
        this.histories = histories;
    }

    public ProfileHistory[] getHistories() {
        return histories;
    }

    public final static class Serializer implements JsonDeserializer<ProfileHistoryResponse> {

        @Override
        public ProfileHistoryResponse deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            if(jsonElement instanceof JsonArray) {
                JsonArray jsonArray = (JsonArray) jsonElement;
                ProfileHistory[] histories = new ProfileHistory[jsonArray.size()];
                for(int i = 0; i < histories.length; i++) {
                    JsonObject history = jsonArray.get(i).getAsJsonObject();
                    String name = history.get("name").getAsString();
                    long changeToAt = history.has("changedToAt") ? history.get("changedToAt").getAsLong() : -1L;
                    histories[i] = new ProfileHistory(name, changeToAt);
                }
                return new ProfileHistoryResponse(histories);
            }
            return null;
        }
    }
}
