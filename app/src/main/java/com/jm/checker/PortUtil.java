package com.jm.checker;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.example.x6.serail.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.Observer;

/**
 * Created by zhoukan on 2018/7/26.
 *
 * @desc:
 */

public class PortUtil {


    private static final String TAG = "PortUtil";

    public final static byte[] readTagCMD = new byte[]{(byte) 0xBB, 0x00, 0x22, 0x00, 0x00, 0x22, 0x7E};
    //    0x5A 0xA5     0xA3       0x00        A3       0x55
    public final static byte[] readSensor = new byte[]{(byte) 0x5A, (byte) 0xA5, (byte) 0xA3, 0x00, (byte) 0xA3, 0x55};
    public final static byte[] readCMD =
            new byte[]{(byte) 0xBB, (byte) 0x00, (byte) 0x39, (byte) 0x00,
                    (byte) 0x09, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, 0x03, 0x00, 0x00, 0x00, 0x02, 0x47, 0x7E};
    private final static byte RFID_HEAD = (byte) 0XBB;
    private final static byte[] SENSOR_HEAD = new byte[]{0x5A, (byte) 0xA5};
    private final static byte[] start = new byte[]{0x5A, (byte) 0xA5, (byte) 0xA1, 0x01, 0x01, (byte) 0xA3, 0x55};
    private final static byte[] end = new byte[]{0x5A, (byte) 0xA5, (byte) 0xA1, 0x01, 0x00, (byte) 0xA2, 0x55};
    //    0x5A 0xA5     0xA6       0x01       0x23          0xCA       0x55
    // 默认值为53
    private final static byte[] density = new byte[]{0x5A, (byte) 0xA5, (byte) 0xA6, 0x01, 0x35, (byte) 0xA2, 0x55};

    // 旋转角度0x5A 0xA5     0xA4       0x04      0x1E   0x00     0xF0     0x00         0xB6       0x55
    private final static byte[] sweepRange = new byte[]{0x5A, (byte) 0xA5, (byte) 0xA4, 0x04, 0x1E, (byte) 0x00, (byte) 0xF0, 0x00, (byte) 0xB6, 0x55};


    // send select cmd
    private byte[] selectCMD;
    private byte[] selectCMDHEAD = new byte[]{(byte) 0xBB, 0x00, 0x0C, 0x00, 0x13, 0x01, 0x00, 0x00, 0x00, 0x20, 0x60, 0x00};

    private int RECURSIVE_TIMES = 5;
    private int INTERAL_TIME = 200; // ms

    private SerialPort serialPort;
    private InputStream ttyInputStream;
    private OutputStream ttyOutputStream;
    private int count = 0;

    public final static String tty0 = "/dev/ttyS0";
    public final static String tty1 = "/dev/ttyS1";
    public final static String tty2 = "/dev/ttyS2";
    public final static String tty3 = "/dev/ttyS3";
    public final static String tty4 = "/dev/ttyS4";

    //其它
    public static PortUtil port0;
    //rfid专用
    public static PortUtil port1;


    static {
        port0 = new PortUtil();
        port0.init_serial(PortUtil.tty0, 9600);

        port1 = new PortUtil();
        port1.init_serial(PortUtil.tty1, 115200);
    }

    /* 打开串口 */
    private void init_serial(String port, int baudrate) {
        try {
            serialPort = new SerialPort(new File(port), baudrate, 0);
            ttyInputStream = serialPort.getInputStream();
            ttyOutputStream = serialPort.getOutputStream();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // rfid
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Observable<byte[]> readTag() {


        Exception t = null;
        byte[] dest = new byte[0];
        try {
            // 1.readTag CMD
            byte[] epcWrap = write(readTagCMD, RFID_HEAD);
            // sample: BB 02 22 00 11 C9 30 00 [E2 00 00 17 27 02 02 67 12 80 EE 17] 45 76 0B 7E
            // 2.fetch epc
            byte[] epc = new byte[12];
            System.arraycopy(epcWrap, 8, epc, 0, 12);
            // 3.create select cmd
            selectCMD = new byte[epc.length + selectCMDHEAD.length + 2];
            System.arraycopy(selectCMDHEAD, 0, selectCMD, 0, selectCMDHEAD.length);
            System.arraycopy(epc, 0, selectCMD, selectCMDHEAD.length, epc.length);
            // 4.calc the sum and add HEAD & END
            int length = selectCMD.length;
            byte sum = 0;
            for (int i = 1; i < length - 2; i++) {
                //    System.out.println(Byte.valueOf(data[i]));
                sum = (byte) (sum + selectCMD[i]);
            }
            selectCMD[epc.length + selectCMDHEAD.length] = sum;
            selectCMD[epc.length + selectCMDHEAD.length + 1] = 0x7E;
            System.out.println(Arrays.toString(selectCMD));
            // 5.send selectCMD
            write(selectCMD, RFID_HEAD);
            byte[] data = write(readCMD, RFID_HEAD);
            // 6.return data area []
            dest = new byte[4];
            System.arraycopy(data, 20, dest, 0, 4);
        } catch (Exception e) {
            t = e;
        }

        final byte[] finalDest = dest;
        final Exception finalT = t;
        return new Observable<byte[]>() {
            @Override
            protected void subscribeActual(Observer<? super byte[]> observer) {
                observer.onNext(finalDest);
                if (finalT != null) {
                    observer.onError(finalT);
                }

            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private byte[] write(byte[] bytes, byte head) {
        boolean flag = true;
        try {
            ttyOutputStream.write(bytes);
            byte[] data = readData();
            // check data
            // 1.first check head
            if (Byte.compare(data[0], head) != 0) {
                flag = false;
            }

            // 2.check checkSum
            int length = data.length;
            byte sum = 0;
            for (int i = 1; i < length - 2; i++) {
                //    System.out.println(Byte.valueOf(data[i]));
                sum = (byte) (sum + data[i]);
            }
            if (Byte.valueOf(sum).compareTo(data[length - 2]) != 0) {
                flag = false;
            }
            // 3.any turn is error invoke write again && invoker self 5 timers
            Log.d("xxx", "write: " + flag);
            if (!flag && count < 5) {
                count++;
                Thread.sleep(20);
                return write(bytes, head);
            }
            return data;

        } catch (Exception e) {
//            e.printStackTrace();
            return null;
        }
    }


    /**
     * 报头为数组的
     *
     * @param bytes
     * @param head
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private byte[] write(byte[] bytes, byte[] head) {
        boolean flag = true;
        try {
            ttyOutputStream.write(bytes);
            byte[] data = readData();
            // check data
            // 1.first check head
            if (Byte.compare(data[0], head[0]) != 0 && Byte.compare(data[1], head[1]) != 0) {
                flag = false;
            }
            // 2.check checkSum
            int length = data.length;
            byte sum = 0;
            for (int i = 2; i < length - 2; i++) {
                // System.out.println(Byte.valueOf(data[i]));
                sum = (byte) (sum + data[i]);
            }
            if (Byte.valueOf(sum).compareTo(data[length - 2]) != 0) {
                flag = false;
            }
            // 3.any turn is error invoke write again && invoker self 5 timers
            if (!flag && count < 5) {
                count++;
                Thread.sleep(20);
                return write(bytes, head);
            }
            return data;

        } catch (Exception e) {
            return null;
        }
    }


    // open
    public Observable<byte[]> start() {

        Exception exe = null;
        byte[] res;

        try {
            ttyOutputStream.write(start);
            res = readData();
        } catch (Exception e) {
            e.printStackTrace();
            exe = e;
            res = null;
        }

        return createObservable(res, exe);
    }

    // close
    public Observable<byte[]> end() {
        Exception exe = null;
        byte[] res;

        try {
            ttyOutputStream.write(end);
            res = readData();
        } catch (Exception e) {
            e.printStackTrace();
            exe = e;
            res = null;
        }
        return createObservable(res, exe);
    }


    private byte[] readData() throws Exception {
        byte[] responseData = null;
        byte[] response = null;
        int available = 0;
        int index = 0;
        int headIndex = 0;


        while (index < 10) {
            Thread.sleep(50L);
            available = this.ttyInputStream.available();
            if (available > 7) {
                break;
            }

            ++index;
        }

        if (available > 0) {
            responseData = new byte[available];
            this.ttyInputStream.read(responseData);

            for (int i = 0; i < available; ++i) {
                if (responseData[i] == -69) {
                    headIndex = i;
                    break;
                }
            }

            response = new byte[available - headIndex];
            System.arraycopy(responseData, headIndex, response, 0, response.length);
        }
        return response;
    }

    /**
     * read sensor data polling
     *
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Observable<byte[]> readSensorData() {

        final byte[] sensorData = write(readSensor, SENSOR_HEAD);

        return new Observable<byte[]>() {
            @Override
            protected void subscribeActual(Observer<? super byte[]> observer) {
                if (sensorData != null) {
                    observer.onNext(sensorData);
                } else {
                    observer.onError(new NullPointerException("sensor data is null,maybe port is inValid!"));
                }

            }
        };

    }

    /**
     * modify density of liquid
     *
     * @param value
     */
    public Observable<byte[]> changeDensity(int value) {

        byte[] data;
        Exception exe = null;

        density[4] = (byte) value;
        caclSum(density);
        try {
            ttyOutputStream.write(density);
            data = readData();
            System.out.println("data:" + Arrays.toString(data));
        } catch (Exception e) {
            e.printStackTrace();
            data = null;
            exe = e;
        }

        return createObservable(data, exe);
    }

    /**
     * @param startAngle
     * @param endAngle
     */
    public Observable<byte[]> sendAngelRange(int startAngle, int endAngle) {

        byte[] data = null;
        Exception exception = null;
        // 0x5A  0xA5  0xA4  0x04  0x1E  0x00  0xF0  0x00  0xB6  0x55
        byte[] startByte = JMByteUtil.intToBytes(startAngle);
        byte[] endByte = JMByteUtil.intToBytes(endAngle);

        sweepRange[4] = startByte[0];
        sweepRange[5] = startByte[1];

        sweepRange[6] = endByte[0];
        sweepRange[7] = endByte[1];

        caclSum(sweepRange);

        Log.d(TAG, "sendAngelRange: " + Arrays.toString(sweepRange));

        try {
            ttyOutputStream.write(sweepRange);
            data = readData();
        } catch (Exception e) {
            data = null;
            exception = e;
        }

        return createObservable(data, exception);
    }

    private void caclSum(byte[] command) {
        int length = command.length;
        byte sum = 0;
        for (int i = 2; i < length - 2; i++) {
            sum = (byte) (sum + command[i]);
        }
        command[length - 2] = sum;
    }


    /**
     * 创建Observable
     *
     * @param inputData
     * @param e
     * @return
     */
    private Observable<byte[]> createObservable(final byte[] inputData, final Exception e) {

        return new Observable<byte[]>() {
            @Override
            protected void subscribeActual(Observer<? super byte[]> observer) {
                if (inputData != null && inputData.length > 0) {
                    observer.onNext(inputData);
                } else {
                    observer.onError(new NullPointerException("data is null , please check whether port's connect is broken!"));
                }

                if (e != null) {
                    observer.onError(e);
                }

            }
        };
    }
}