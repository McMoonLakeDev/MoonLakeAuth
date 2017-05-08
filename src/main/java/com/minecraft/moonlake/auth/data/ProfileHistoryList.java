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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class ProfileHistoryList implements Iterable<ProfileHistory> {

    private final List<ProfileHistory> historyList;

    public ProfileHistoryList(List<ProfileHistory> historyList) {
        this.historyList = historyList;
    }

    public ProfileHistory getLatest() {
        if(historyList.size() == 1)
            return historyList.get(0);
        Collections.sort(historyList);
        return historyList.get(historyList.size() - 1);
    }

    public int size() {
        return historyList.size();
    }

    @Override
    public Iterator<ProfileHistory> iterator() {
        return historyList.iterator();
    }
}
