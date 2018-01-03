package beini.com.sppapp.blue;

/**
 * Created by beini on 2018/1/3.
 */

public class SppDeco {
    public static final int PACK_PREFIX = 0x01;
    public static final int PACK_SUFFIX = 0x02;

    /**
     * /**新的蓝牙传输模式不带有转义
     *
     * @param cmd_data
     * @return
     */
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
}
