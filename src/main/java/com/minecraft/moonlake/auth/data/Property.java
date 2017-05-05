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

import com.minecraft.moonlake.auth.exception.MoonLakeProfileException;

import java.nio.charset.Charset;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

public class Property {

    private String name;
    private String value;
    private String signature;

    public Property(String name, String value) {
        this(name, value, null);
    }

    public Property(String name, String value, String signature) {
        this.name = name;
        this.value = value;
        this.signature = signature;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getSignature() {
        return signature;
    }

    public boolean hasSignature() {
        return signature != null;
    }

    public boolean validateSignature(PublicKey key) throws MoonLakeProfileException {
        if(!hasSignature())
            return false;
        try {
            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(key);
            sig.update(value.getBytes());
            return sig.verify(Base64.getDecoder().decode(signature.getBytes(Charset.forName("utf-8"))));
        } catch (Exception e) {
            throw new MoonLakeProfileException("无法验证属性材质数据的签名值.", e);
        }
    }

    @Override
    public String toString() {
        return "Property{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", signature='" + signature + '\'' +
                '}';
    }
}
