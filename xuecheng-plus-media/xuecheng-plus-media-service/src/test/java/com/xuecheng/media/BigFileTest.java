package com.xuecheng.media;

import com.alibaba.nacos.common.utils.CollectionUtils;
import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BigFileTest {
    @Test
    public void testChunk() throws IOException {
        File fileSourse = new File("C:\\Users\\yy\\Videos\\1.mp4");
        String chunkPath="C:\\Users\\yy\\Videos\\chunk\\";
        int filesize=1024*1024*5;
        int chunkNum = (int) Math.ceil(fileSourse.length()*1.0/filesize);
        //缓存区
        byte[] bytes=new byte[1024];
        RandomAccessFile raf_r = new RandomAccessFile(fileSourse, "r");
        for (int i = 0; i < chunkNum; i++) {
            File file = new File(chunkPath + i);
            int lenthg=-1;
            RandomAccessFile raf_rw = new RandomAccessFile(file, "rw");
            while ((lenthg=raf_r.read(bytes))!=-1){


                raf_rw.write(bytes,0,lenthg);
                if (file.length()>=filesize){
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();



    }

    @Test
    public void testMerge() throws IOException {
        File filesourse=new File("C:\\Users\\yy\\Videos\\chunk");
        File mergedFile=new File("C:\\Users\\yy\\Videos\\1merge.mp4");
        File fileSourse = new File("C:\\Users\\yy\\Videos\\1.mp4");
        File[] files = filesourse.listFiles();
        List<File> list = Arrays.asList(files);
        Collections.sort(list, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName());
            }
        });
        RandomAccessFile raf_rw = new RandomAccessFile(mergedFile, "rw");
        for (File file : list) {
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int lenthg=-1;
            byte[] bytes=new byte[1024];

            while ((lenthg=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,lenthg);
            }
            raf_r.close();
        }
        raf_rw.close();
        FileInputStream fileInputStream = new FileInputStream(mergedFile);
        String s = DigestUtils.md5DigestAsHex(fileInputStream);
        FileInputStream fileInputStream1 = new FileInputStream(fileSourse);
        String s2 = DigestUtils.md5DigestAsHex(fileInputStream1);
        if (s.equals(s2)) {
            System.out.println("合并成功");
        }

    }
}
