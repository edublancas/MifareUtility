package com.ebr.mifareutility;

/**
 * Created by Edu on 07/11/14.
 */
public class SectorBlockUtils {
    public static Integer getAbsoluteBlock(Integer sector, Integer block){
        Integer NBLOCKPERSECTOR = 4;
        return sector*NBLOCKPERSECTOR+block;
    }
}
