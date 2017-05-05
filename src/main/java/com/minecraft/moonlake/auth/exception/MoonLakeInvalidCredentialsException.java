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


package com.minecraft.moonlake.auth.exception;

public class MoonLakeInvalidCredentialsException extends MoonLakeRequestException {

    private static final long serialVersionUID = 1647656324928893833L;

    public MoonLakeInvalidCredentialsException() {
    }

    public MoonLakeInvalidCredentialsException(String message) {
        super(message);
    }

    public MoonLakeInvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public MoonLakeInvalidCredentialsException(Throwable cause) {
        super(cause);
    }
}
