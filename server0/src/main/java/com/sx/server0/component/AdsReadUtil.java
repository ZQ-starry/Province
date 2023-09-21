package com.sx.server0.component;

import com.sx.server0.entity.device.DevCommonEntity;
import com.sx.server0.entity.result.LineResultEntity;
import com.sx.server0.entity.result.NodeResultEntity;
import com.sx.server0.entity.result.ResultListsEntity;
import de.beckhoff.jni.Convert;
import de.beckhoff.jni.JNIByteBuffer;
import de.beckhoff.jni.tcads.AdsCallDllFunction;
import de.beckhoff.jni.tcads.AmsAddr;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author: ZhangQi
 * @Date: 2023/7/31 9:45
 * @Description:
 */
@Component
public class AdsReadUtil {

    /**
     * 截取ByteBuffer的任意区间
     * @param source
     * @param position
     * @param length
     * @return
     */
    private ByteBuffer sliceByteBuffer(ByteBuffer source, int position, int length) {
        // 设置原始ByteBuffer的位置和限制，以便截取内容
        source.position(position);
        source.limit(position + length);
        // 创建一个新的ByteBuffer来存储截取的内容
        ByteBuffer slicedBuffer = source.slice();
        // 重置原始ByteBuffer的位置和限制
        source.clear();
        slicedBuffer.order(ByteOrder.LITTLE_ENDIAN);
        return slicedBuffer;
    }

    /**
     * 8.20版嘉兴 单台机器 4个线路表（单相电流+有功无功） 4个站点表（单项电压）
     * @return
     */
    public ResultListsEntity devStructRead(DevCommonEntity devInfo) {
        ResultListsEntity resultListsEntity = new ResultListsEntity();
        List<LineResultEntity> lineList = new ArrayList<>();
        List<NodeResultEntity> nodeList = new ArrayList<>();
        Float[] nodeUIFloats = new Float[devInfo.getStaNum()];
        int length = devInfo.getLineDataLen() + devInfo.getNodeDataLen() + devInfo.getILen();
        long err;
        AmsAddr addr = new AmsAddr();
        try {
            AdsCallDllFunction.adsPortOpen();
            err = AdsCallDllFunction.getLocalAddress(addr);
            addr.setNetIdStringEx(devInfo.getNetIdDev());
            addr.setPort(851);
            if (err != 0) {
                System.out.println("Read Error PLC: 0x" + Long.toHexString(err));
            } else {
                // System.out.println("Read Success: Open communication!");
                JNIByteBuffer hdlBuff = new JNIByteBuffer(4);
                JNIByteBuffer symBuff = new JNIByteBuffer(Convert.StringToByteArr("MAIN.AllData", false));
                JNIByteBuffer dataBuff = new JNIByteBuffer(length);
                err = AdsCallDllFunction.adsSyncReadWriteReq(
                        addr,
                        0xF003,//指令代码号，用于获取句柄
                        0x0,//指令号，固定
                        hdlBuff.getUsedBytesCount(),//取得句柄缓存长度
                        hdlBuff,
                        symBuff.getUsedBytesCount(),//取得变量名缓存长度
                        symBuff);
                if (err != 0) {
                    System.out.println("Error: Get handle: 0x" + Long.toHexString(err));
                }
                int hdlBuffToInt = Convert.ByteArrToInt(hdlBuff.getByteArray());
                Long dataLength = (long) length;
                err = AdsCallDllFunction.adsSyncReadReq(
                        addr,
                        0xF005,//指令代码号，用于获取数据
                        hdlBuffToInt,
                        dataLength,//数据长度
                        dataBuff);
                if (err != 0) {
                    System.out.println("Error: Read by handle: 0x" + Long.toHexString(err));
                } else {
                    ByteBuffer responseBuffer = ByteBuffer.wrap(dataBuff.getByteArray());
                    responseBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    // 切割responseBuffer，分为线路的、站点的、变电站的电流
                    ByteBuffer lineBuffer = sliceByteBuffer(responseBuffer, 0, devInfo.getLineDataLen());
                    int position = devInfo.getLineDataLen();
                    ByteBuffer nodeBuffer = sliceByteBuffer(responseBuffer, position, devInfo.getNodeDataLen());
                    position = devInfo.getLineDataLen()+devInfo.getNodeDataLen();
                    ByteBuffer iBuffer = sliceByteBuffer(responseBuffer,position , devInfo.getILen());
                    // 线路数据处理
                    for (int i = 0; i < devInfo.getLineDutNum(); i++){
                        LineResultEntity lineResultEntity = new LineResultEntity();
                        for (int j = 0; j < 45; j++) {
                            if ((j+1)%3 == 0 || (j+2)%3 == 0){
                                //有功无功单位转为MW
                                RunWithStart.LineEntity[j].set(lineResultEntity, lineBuffer.getFloat(
                                        i*AllGVar.LINE_EN_LEN + j*4)/1000000);
                            }else {
                                RunWithStart.LineEntity[j].set(lineResultEntity, lineBuffer.getFloat(
                                        i*AllGVar.LINE_EN_LEN + j*4));
                            }
                        }
                        lineList.add(lineResultEntity);
                    }
                    // 站点数据处理
                    for (int i = 0; i < devInfo.getNodeDutNum(); i++){
                        NodeResultEntity nodeResultEntity = new NodeResultEntity();
                        for (int j = 0; j < 15; j++) {
                            // 电压单位转换为KV
                            RunWithStart.NodeEntity[j].set(nodeResultEntity, nodeBuffer.getFloat(
                                    i*AllGVar.NODE_EN_LEN + j*4)/1000);
                        }
                        nodeList.add(nodeResultEntity);
                    }
                    // 变电站电流数据处理
                    for (int j = 0; j< devInfo.getStaNum(); j++){
                        nodeUIFloats[j]=iBuffer.getFloat(j*4);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            AdsCallDllFunction.adsPortClose();
        }
        resultListsEntity.setLineResultEntityList(lineList);
        resultListsEntity.setNodeResultEntityList(nodeList);
        resultListsEntity.setNodeUI(nodeUIFloats);
        return resultListsEntity;
    }
}
