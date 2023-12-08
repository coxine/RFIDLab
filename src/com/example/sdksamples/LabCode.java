package com.example.sdksamples;

import com.impinj.octane.*;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


public class LabCode {
    static Random random = new Random();

    /**
     * 将16进制字符串转换为ASCII码字符串
     *
     * @param hexString 16进制字符串
     * @return ASCII码字符串
     */
    static String hexToAscii(String hexString){
        StringBuilder asciiStringBuilder = new StringBuilder();
//        如User Memory Bank存储的16进制串为48656C6C6F2C20776F726C6421202020
//        每两个16进制数(8bit)代表一个字节，所以需要每两个16进制数进行一次处理
        for (int i = 0; i< hexString.length();i+=2){
            String hexPair = hexString.substring(i,i+2);
            asciiStringBuilder.append((char) Integer.parseInt(hexPair, 16));
        }
        return asciiStringBuilder.toString();
    }

    /**
     * 将ASCII编码的字符串转换为16进制字符串
     *
     * @param asciiString Ascii码字符串
     * @return ASCII编码后的16进制字符串
     */
    static String AsciiToHexString(String asciiString){
        StringBuilder hexStringBuilder = new StringBuilder();

        byte[] asciiStringBytes = asciiString.getBytes(StandardCharsets.US_ASCII);
        for (byte b:asciiStringBytes){
//            将一个字符转换为两位的16进制数
            hexStringBuilder.append(String.format("%02x",b));
        }
        return hexStringBuilder.toString();
    }

    /**
     * 将UTF-8编码的字符串转换为16进制字符串
     *
     * @param utf8String UTF-8字符串
     * @return UTF-8编码后的16进制字符串
     */
    static String utf8ToHexString(String utf8String){
        StringBuilder hexStringBuilder = new StringBuilder();

        byte[] ustf8Bytes = utf8String.getBytes(StandardCharsets.UTF_8);
        for (byte b:ustf8Bytes){
            hexStringBuilder.append(String.format("%02x",b));
        }

        return hexStringBuilder.toString();
    }

    /**
     * 将16进制字符串转换为UTF-8编码的字符串
     *
     * @param hexString 包含UTF-8编码的16进制字符串
     * @return 解码后的UTF-8字符串
     */
    static String hexStringToUtf8(String hexString) {
//        用于存储解码后的字符串
        StringBuilder utf8StringBuilder = new StringBuilder();
//        创建一个ByteBuffer来存储16进制字符串的字节表示
        ByteBuffer byteBuffer = ByteBuffer.allocate(hexString.length() / 2);
//        创建UTF-8解码器
        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();
//        将16进制字符串解析为字节并放入ByteBuffer中
        for (int i = 0; i < hexString.length(); i += 2) {
//        如User Memory Bank存储的16进制串为48656C6C6F2C20776F726C6421202020
//        每两个16进制数(8bit)代表一个字节，所以需要每两个16进制数进行一次处理
            String hexPair = hexString.substring(i, i + 2);
//            将每一对16进制字符解析为对应的字节值
            int decimalValue = Integer.parseInt(hexPair, 16);
            byteBuffer.put((byte) decimalValue);
        }

//        准备ByteBuffer以进行解码
        byteBuffer.flip();
//        创建一个CharBuffer来存储解码后的字符
        CharBuffer charBuffer = CharBuffer.allocate(byteBuffer.limit());
//        使用UTF-8解码器将字节转换为字符
        utf8Decoder.decode(byteBuffer, charBuffer, true);
//        准备CharBuffer以进行读取
        charBuffer.flip();
//        将解码后的字符追加到StringBuilder中
        utf8StringBuilder.append(charBuffer);

        return utf8StringBuilder.toString();
    }

    /**
     * 随机生成一个EPC
     *
     * @return 随机生成的EPC字符串
     */
    static String getRandomEpc() {
        StringBuilder epc = new StringBuilder();

        // get the length of the EPC from 1 to 6 words
        int numWords = random.nextInt(6) + 1;

        for (int i = 0; i < numWords; i++) {
            Short s = (short) random.nextInt(Short.MAX_VALUE + 1);
            epc.append(String.format("%04X", s));
        }
        return epc.toString();
    }

    public static void main(String[] args) {
        try {
//            设置控制台字符集为UTF-8
            System.setProperty("file.encoding", "UTF-8");
            System.out.println("Default Charset: " + Charset.defaultCharset());


//            连接阅读器
//            需要手动修改hostname为所用阅读器的MAC地址
            String hostname = "SpeedwayR-10-AB-5A.local";
            ImpinjReader reader = new ImpinjReader();
//            连接阅读器
            System.out.println("Connecting...");
            reader.connect(hostname);




//            修改设置
            Settings settings = reader.queryDefaultSettings();
            ReportConfig report = settings.getReport();
            report.setMode(ReportMode.Individual);
            settings.setRfMode(1002);
            settings.setSearchMode(SearchMode.DualTarget);
            settings.setSession(2);
//            天线设置
            AntennaConfigGroup antennas = settings.getAntennas();
//            首先需要禁用所有天线端口，需要使用哪个天线端口再将其开启
            antennas.disableAll();
//            连接的是哪个天线端口就设置为对应的端口号
            antennas.enableById(new short[]{1});
//            设置天线的接收功率为最大值
            antennas.getAntenna((short) 1).setIsMaxRxSensitivity(true);
//            设置天线的发送功率为最小值10Dbm，此值设置的过大容易导致读取到其他不想读的标签
            antennas.getAntenna((short) 1).setTxPowerinDbm(10);
//            应用设置
            reader.applySettings(settings);




//            设置要执行的读和写操作
//            建立阅读器operation sequence
            TagOpSequence sequence = new TagOpSequence();
            sequence.setOps(new ArrayList<TagOp>());
            sequence.setExecutionCount((short) 0);
            sequence.setState(SequenceState.Active);
            sequence.setId(1);

//            加入读取EPC的命令
            TagReadOp readOp = new TagReadOp();
//            设置读取的内容为MemoryBank的EPC部分
            readOp.setMemoryBank(MemoryBank.Epc);
//            设置需要读取的内容长度，以字word为单位进行读取
//            注意EPC最长为96bit，即6word，所以此处只能设置1-6之间的数字
            readOp.setWordCount((short) 6);
//            设置开始读取的位置，此为相对于前面设置的EPC的初始位置的偏移，也以word为单位，一般设置为0或者WordPointers.xxx，表示从头开始读
//            最终读取到的信息即为从偏移位置开始，读取wordcount个字的信息
//            例如：若wordcount设置为2，wordpointer设置为1，则最终读取到的内容为EPC第1、2个word的信息
            readOp.setWordPointer(WordPointers.Epc);
//            将命令加入到operation sequence中
            sequence.getOps().add(readOp);

//            加入读取User Memory Bank的命令
            readOp = new TagReadOp();
//            设置读取的内容为MemoryBank的User Memory Bank部分
            readOp.setMemoryBank(MemoryBank.User);
//            注意User Memory Bank最长为8word,只能设置1-8之间的数字
            readOp.setWordCount((short) 8);
            readOp.setWordPointer((short) 0);
            sequence.getOps().add(readOp);

//            加入写EPC的命令
            TagWriteOp writeOp = new TagWriteOp();
//            设置写入的内容为MemoryBank的EPC部分
            writeOp.setMemoryBank(MemoryBank.Epc);
//            writeOp.setData(TagData.fromHexString(getRandomEpc()));
            writeOp.setData(TagData.fromHexString("20231209AAAABBBBCCCCDDDD"));
//            Starting writing at word 2 (word 0 = CRC, word 1 = PC bits)
            writeOp.setWordPointer(WordPointers.Epc);
            sequence.getOps().add(writeOp);

//            设置为NULL表示应用到所有tags，所有接收到信息的tag都会回复
            sequence.setTargetTag(null);
//            将operation sequence加入到阅读器中，阅读器可以加入多个operation sequence
            reader.addOpSequence(sequence);





//            设置监听器来持续监听，在RFID标签操作完成时被调用
            reader.setTagOpCompleteListener(new TagOpCompleteListenerImplementation(){
                @Override
                public void onTagOpComplete(ImpinjReader reader, TagOpReport results){
                    String epc = null;
                    String userMemoryString = "";
                    for (TagOpResult tagOpResult : results.getResults()) {
//                        读取Tag操作
                        if (tagOpResult instanceof TagReadOpResult){
//                            类型转换，将命令转换为读取类型的操作
                            TagReadOpResult tagReadOpResult = (TagReadOpResult) tagOpResult;
//                            由于读取EPC是最先加入进来的op，所以对应的opId应该为1
                            if (tagReadOpResult.getOpId() == (short) 1){
                                epc = tagReadOpResult.getData().toString();
                                System.out.println("Read EPC result: "+epc);
                            }
//                            由于读取User Memory Bank是第2个加入进来的op，所以对应的opId应该为2
                            else if (tagReadOpResult.getOpId() == (short) 2){
//                                读取的是User Memory Bank的数据
//                                userMemoryString = hexToAscii(tagReadOpResult.getData().toHexString());
                                userMemoryString = hexStringToUtf8(tagReadOpResult.getData().toHexString());
                                System.out.println("Read UserMemoryBank result:"+userMemoryString);
                            }

                        }

//                        写入Tag操作
                        if (tagOpResult instanceof TagWriteOpResult){
                            TagWriteOpResult tagWriteOpResult = (TagWriteOpResult) tagOpResult;
//                            由于写入EPC是第3个加入进来的op，所以对应的opId应该为3
                            if (tagWriteOpResult.getOpId() == (short) 3){
                                System.out.println("Write EPC result: " + tagWriteOpResult.getResult().toString());
//                                System.out.println(" EPC_written_numWords: " + tagWriteOpResult.getNumWordsWritten());
                            }
                        }
                    }


                }
            });


            System.out.println("Starting...");
            reader.start();
            System.out.println("Press Enter to exit.");
            Scanner s = new Scanner(System.in);
            s.nextLine();
            System.out.println("Stopping");
            reader.stop();

//            和阅读器解除连接
            System.out.println("Disconnecting");
            reader.disconnect();

        } catch (OctaneSdkException ex){
            System.out.println("Octane SDK exception: "+ ex.getMessage());
        } catch (Exception ex){
            System.out.println("Exception: " + ex.getMessage());
            ex.printStackTrace(System.out);
        }
    }
}
