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

public class StatusService implements Comparable<StatusService.Type> {

    private final String host;
    private final Type type;

    public StatusService(String host, Type type) {
        this.host = host;
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public Type getType() {
        return type;
    }

    public boolean isUnavailable() {
        return type == Type.RED;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof StatusService) {
            StatusService other = (StatusService) obj;
            return host != null ? host.equals(other.host) : other.host == null &&
                    type != null ? type == other.type : other.type == null;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StatusService{" +
                "host='" + host + '\'' +
                ", type=" + type +
                ", isUnavailable=" + isUnavailable() +
                '}';
    }

    @Override
    public int compareTo(Type other) {
        if(other == null)
            return 1;
        return type.compareTo(other);
    }

    public enum Type {

        GREEN,
        YELLOW,
        RED,
        ;

        public static Type fromName(String name) {
            switch (name.toLowerCase()) {
                case "green":
                    return GREEN;
                case "yellow":
                    return YELLOW;
                case "red":
                    return RED;
                default:
                    return null;
            }
        }
    }
}
