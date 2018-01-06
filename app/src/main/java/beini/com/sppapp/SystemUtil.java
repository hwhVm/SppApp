package beini.com.sppapp;

/**
 * Created by beini on 2018/1/4.
 */

public class SystemUtil {
    static public int getUnsignedByte(byte data) {      //将data字节型数据转换为0~255 (0xFF 即BYTE)。
        return data & 0x0FF;
    }
}
