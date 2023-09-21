package com.sx.server0.component;

import com.sx.server0.entity.device.InitialDataEntity;
import de.beckhoff.jni.JNIByteBuffer;
import de.beckhoff.jni.tcads.AdsCallDllFunction;
import de.beckhoff.jni.tcads.AmsAddr;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 初始数据写入：负荷数据
 */
@Component
public class AdsWriteUtil {

    private final String NET_ID = "192.168.1.15.1.1";

    public void adsStructWrite(InitialDataEntity initialDataEntity){
        try {
            long err;
            AmsAddr addr = new AmsAddr();
            int dataLength = 19;
            // 创建一个JNIByteBuffer(需要计算需要写入数据的长度)，用于存放需要写入的数据initialDataEntity
            JNIByteBuffer dataBuff = new JNIByteBuffer(dataLength);
            // Open communication
            AdsCallDllFunction.adsPortOpen();
            err = AdsCallDllFunction.getLocalAddress(addr);
            addr.setNetIdStringEx(NET_ID);
            addr.setPort(851);
            if (err != 0) {
                System.out.println("Error: Open communication: 0x"
                        + Long.toHexString(err));
            } else {
                System.out.println("Success: Open communication!");
            }
            // Use JNIByteBuffer as a backing array for ByteBuffer
            ByteBuffer byteBufferOfInitialData = ByteBuffer.wrap(dataBuff.getByteArray());
            // Write elements to buffer. Little Endian!
            byteBufferOfInitialData.order(ByteOrder.LITTLE_ENDIAN);
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerPPlain());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerQPlain());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerPSea());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerQSea());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerPMountain());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerQMountain());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerPFire());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerQFire());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerPPv());
            byteBufferOfInitialData.putFloat(initialDataEntity.getPowerQPv());
            // Write struct to PLC
            err = AdsCallDllFunction.adsSyncWriteReq(addr,
                    0x4020,     // Index Group
                    0x0,        // Index Offset
                    dataLength,
                    dataBuff);
            if(err!=0) {
                System.out.println("Error: Write request: 0x"
                        + Long.toHexString(err));
            } else {
                System.out.println("Success: Write struct!");
            }
            // Close communication
            err = AdsCallDllFunction.adsPortClose();
            if(err!=0) {
                System.out.println("Error: Close Communication: 0x"
                        + Long.toHexString(err));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
