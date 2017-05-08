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

public class ProfileHistory implements Comparable<ProfileHistory> {

    private final String name;
    private final long changedToAt;

    public ProfileHistory(String name) {
        this(name, 0L);
    }

    public ProfileHistory(String name, long changedToAt) {
        this.name = name;
        this.changedToAt = changedToAt;
    }

    public String getName() {
        return name;
    }

    public long getChangedToAt() {
        return changedToAt;
    }

    public boolean hasChangedToAt() {
        return changedToAt > 0L;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj instanceof ProfileHistory) {
            ProfileHistory other = (ProfileHistory) obj;
            return name != null ? name.equals(other.name) : other.name == null && changedToAt == other.changedToAt;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (int) (changedToAt ^ (changedToAt >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "ProfileHistory{" +
                "name='" + name + '\'' +
                ", changedToAt=" + changedToAt +
                ", hasChangedToAt=" + hasChangedToAt() +
                '}';
    }

    @Override
    public int compareTo(ProfileHistory other) {
        if(other == null)
            return 1;
        return Long.compare(changedToAt, other.changedToAt);
    }
}
