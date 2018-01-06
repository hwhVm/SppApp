package beini.com.sppapp.blue;

import android.util.Log;

import beini.com.sppapp.SystemUtil;

/**
 * Created by beini on 2018/1/3.
 */

public class SppDeco {
    public static final int PACK_PREFIX = 0x01;
    public static final int PACK_SUFFIX = 0x02;
    public static final int PACK_ACCEPT = 0x55;

    public static byte[] newEncodeCmd(int cmd1, byte[] cmd_data) {
        //DLog.i(TAG, "初级数据 : " + cmd_type + " data: " + StringUtils.getHex(cmd_data));
        boolean cmdData_f = true;
        byte[] cmd;
        if (cmd_data == null) {
            cmdData_f = false;
        }

        // 七个基本数据
        if (cmdData_f) {
            cmd = new byte[7 + cmd_data.length];
        } else {
            cmd = new byte[7];
        }
        cmd[0] = PACK_SUFFIX; // 前缀
        cmd[1] = (byte) ((cmd.length - 4) & 0xFF); // 数据位低位
        cmd[2] = (byte) ((cmd.length - 4) >>> 8 & 0xFF); // 数据位高位
        cmd[3] = (byte) (cmd1 & 0xFF); // 数据类型

        // 将命令数据放入包中, 从第 4 位开始放
        if (cmdData_f) {
            for (int i = 0; i < cmd_data.length; i++) {
                cmd[i + 4] = cmd_data[i];
            }
        }

        // 校验和 : 长度 (序号1.2) + 命令类型(序号3) + 命令数据(序号 4 ~ 长度 - 2) 累加结果
        // 注意是字节累加结果
        int CheckSum = 0;
        for (int i = 1; i < cmd.length - 2; i++) {
            CheckSum += (cmd[i] & 0xFF);
            CheckSum &= 0xFFFF;
        }
        cmd[cmd.length - 3] = (byte) (CheckSum & 0xFF); // 校验和低位
        cmd[cmd.length - 2] = (byte) ((CheckSum >>> 8) & 0xFF); // 校验和高位

        cmd[cmd.length - 1] = PACK_SUFFIX; // 包尾

        //打印发送的数据
//		LogUtil.e("发送的数据 : " + StringUtils.getHex(cmd));

        return cmd;
    }

    public static void decodeData(byte[] bytes, int length) {
        Log.d("com.beini", "--->decodeData");
        byte[] readBufk = new byte[length];
        for (int i = 0; i < length; i++) {
            readBufk[i] = bytes[i];
        }
        int len = (SystemUtil.getUnsignedByte(readBufk[1])
                | SystemUtil.getUnsignedByte(readBufk[2]) << 8);
        int offset = 0;
        //可能有两个包
        if (readBufk.length - 1 - 2 - 1 > len) {
            while (true) {
                int dataLen = 1 + 2 + len + 1;
                byte[] newData = new byte[dataLen];
                System.arraycopy(readBufk, offset, newData, 0, dataLen);

                handCmd(newData);

                offset += dataLen;
                if (readBufk.length - offset < (1 + 2 + 1)) {
                    break;
                }
                len = (SystemUtil.getUnsignedByte(readBufk[offset + 1])
                        | SystemUtil.getUnsignedByte(readBufk[offset + 2]) << 8);
            }
        } else {
            handCmd(readBufk);
        }
    }

    public static void handCmd(byte[] readBufk) {
        if (readBufk == null || readBufk.length < 6) {
            return;
        }
        // 打印解码后的数据
//        Log.d("com.beini", "接收的原始数据 : " + StringUtils.getHex(readBufk));

        if (readBufk[0] != PACK_PREFIX
                || readBufk[readBufk.length - 1] != PACK_SUFFIX) {
            return;
        }

        int packetlength = getIntInByteArray(readBufk, 1);
        if (packetlength == -1) {
            return;
        }
        // 获取命令类型
        int Cmd = getByteInByteArray(readBufk, 3);
        Log.d("com.beini", "------>cmd=" + Cmd);
        // 命令回应确认包 0x04
        if (Cmd == 0x04) {
            boolean isSuccess = (getByteInByteArray(readBufk, 5) == PACK_ACCEPT);
            int cmd_type = getByteInByteArray(readBufk, 4);
            {
                byte[] cmd_type_array = new byte[1];
                cmd_type_array[0] = (byte) cmd_type;
            }
            Log.d("com.beini", "cmd_type=" + cmd_type+"  isSuccess="+isSuccess);
            if (isSuccess) {
                switch (cmd_type) {

                }
            }

        }
    }

    /**
     * 获取字节数组
     *
     * @param Data
     * @param point
     * @return
     */
    private static int getByteInByteArray(byte[] Data, int point) {
        if (Data == null) {
            return -1;
        }
        if (Data.length <= point) {
            return -1;
        }
        return (Data[point] & 0xFF);
    }

    /**
     * 获取 point 及 point + 1 两个字节的数据, point 低位, point + 1 高位
     *
     * @param Data
     * @param point
     * @return
     */
    private static int getIntInByteArray(byte[] Data, int point) {
        // 注意多字节数据小端对齐, 低位在前(左), 高位在后(右)
        int numl = getByteInByteArray(Data, point); // 低位
        int numh = getByteInByteArray(Data, point + 1);// 高位
        if (numl == -1 || numh == -1) {
            return -1;
        }
        return (numh * 256 + numl);
    }
}
