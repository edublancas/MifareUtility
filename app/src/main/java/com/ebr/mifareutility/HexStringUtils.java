package com.ebr.mifareutility;

import java.util.Locale;

/**
 * Created by Edu on 07/11/14.
 */


public class HexStringUtils {
    public static String getHexString(byte[] b, int length)
    {
        String result = "";
        Locale loc = Locale.getDefault();

        for (int i = 0; i < length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            result += " ";
        }
        return result.toUpperCase(loc);
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public static long byteArrayToInt(byte[] value){
        long valueInt = 0;
        for (int i = 0; i < value.length; i++)
        {
            valueInt += ((long) value[i] & 0xffL) << (8 * i);
        }

        return valueInt;

    }


}
