package com.kyhsgeekcode.disassembler.Utils.Olly;

import com.kyhsgeekcode.disassembler.Utils.DisasmInfo;
import com.kyhsgeekcode.disassembler.Utils.IInfoReader;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class UDDReader implements IInfoReader {
    private static final String TAG = "ADA_UDDReader";

    @Override
    public DisasmInfo Read(InputStream is) throws IOException {
        DisasmInfo info = new DisasmInfo();
        DataInputStream dis = new DataInputStream(is);
        while (dis.available() > 0) {
            int type = dis.readInt() & 0xFFffFFff;
            int length = dis.readInt() & 0xFFffFFff;
            byte[] data = new byte[length];
            dis.read(data, 0, length);

//            UddTag tag = UddTag.fromInt(type);
//            if (tag == UddTag.TAG_UNKNOWN) {
//                Log.w(TAG, "Unknown type:" + type);
//            }
//            uddData.put(tag, data);
        }

        return info;
    }
}
