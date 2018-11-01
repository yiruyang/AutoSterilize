package com.jm.checker;

/**
 * Created by zhoukan on 2018/8/9.
 *
 * @desc:
 */

public class JMByteUtil {

    /**
     * convert hex to string
     *
     * @param data
     */
    public static String convertHexToDec(byte[] data) {

        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(Byte.toString(b));
        }
        return sb.toString();
    }

    /**
     *  接收到的原始数据 是无符号类型 但java都是有符号的 所以需要先转换成 无符号的值 然后再进行计算
     * @param data
     * @return
     */
    public static float getsignedSum(byte[] data) {

        int d0;
        int d1;
        float sum;
        d0 = data[0] < 0? data[0] & 0xff : data[0];
        d1 = data[1] < 0? data[1] & 0xff : data[1];
        sum = (d0 + (d1 << 8)) / 10.0f;
        return sum;
    }

    /**

     *  前面是低位  后面是高位
<<<<<<< HEAD
     * @param a
=======
     * @param
>>>>>>> a784d68d5596b1f55a4078f87603126112c533e9
     * @return
     */
    public static float getUnSignedSum(byte[] a) {

        byte a1 = a[0];
        short a2 = a[1];

        short sa2 = (short) (a2<<8);
        short or = (short) (a1&0x00ff|sa2);
        // 最后来处理符号位
        short and = (short) (or&0xffff);
        return and/10.0f;
    }


    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }


    /**
     * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。  和bytesToInt2（）配套使用
     */
    public static byte[] intToBytes2(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24));
        return value;
    }

    /**
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {

        int value;
        value = (int) (((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF));
        return value;
    }


}
