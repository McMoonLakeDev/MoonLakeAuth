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

import com.google.gson.annotations.Expose;

import java.util.*;

public class GameProfile {

    private UUID id;
    private String name;
    private List<Property> properties;
    private boolean legacy;

    // 定义此字段不参与 json 的序列化和反序列化
    @Expose(serialize = false, deserialize = false)
    private Map<TextureType, ProfileTexture> textures;

    public GameProfile(String id, String name) {
        this.id = id != null && !id.equals("") ? UUID.fromString(id) : null;
        this.name = name;
    }

    public GameProfile(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isLegacy() {
        return legacy;
    }

    public List<Property> getProperties() {
        if(properties == null)
            this.properties = new ArrayList<>();
        return properties;
    }

    public Property getProperty(String name) {
        for(Property property : getProperties())
            if(property.getName().equals(name))
                return property;
        return null;
    }

    public Map<TextureType, ProfileTexture> getTextures() {
        if(textures == null)
            this.textures = new HashMap<>();
        return textures;
    }

    public ProfileTexture getTexture(TextureType type) {
        return textures != null ? textures.get(type) : null;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof GameProfile) {
            GameProfile other = (GameProfile) obj;
            return id != null ? id.equals(other.id) : other.id == null &&
                    name != null ? name.equals(other.name) : other.name == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GameProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", properties=" + properties +
                ", legacy=" + legacy +
                '}';
    }
}
