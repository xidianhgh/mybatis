package com.ruijie.listenevent.utils;

import com.ruijie.listenevent.common.constant.Constant;
import org.springframework.util.StringUtils;

import java.io.*;

public class FileUtils {


    public static int countFileTotalLine(String sourceFilePath) throws IOException {
        File file = new File(sourceFilePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
        StringBuilder strBuilder = new StringBuilder();
        String sLine = null;
        while ((sLine = br.readLine()) != null) {
            strBuilder.append(sLine);
            strBuilder.append("\r\n");
        }
        br.close();
        return countLines(strBuilder.toString(), Constant.linefeed);
    }

    public static void readFile(String sourceFilePath, String encode) throws IOException {
        File file = new File(sourceFilePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encode));
        StringBuilder strBuilder = new StringBuilder();
        String sLine = null;
        while ((sLine = br.readLine()) != null) {
            strBuilder.append(sLine);
            strBuilder.append("\r\n");
        }
        br.close();
        System.out.println(strBuilder.toString());
        System.out.println("行数" + countLines(strBuilder.toString(), "\r\n"));
    }

    /**
     * 读文件某一行的内容
     *
     * @param sourceFilePath
     * @param line           行数，从1开始
     * @return
     * @throws Exception
     */
    public static String readFile(String sourceFilePath, int line) throws Exception {
        File file = new File(sourceFilePath);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
        StringBuilder strBuilder = new StringBuilder();
        String sLine = null;
        int currentLine = 0;
        while ((sLine = br.readLine()) != null) {
            strBuilder.append(sLine);
            strBuilder.append("\r\n");
            if (++currentLine == line) {
                break;
            }
        }
        br.close();
        return sLine;
    }


    private static int countLines(String txt, String target) {
        //统计文件行数
        int num = 0;
        while (!StringUtils.isEmpty(txt) && txt.contains(target)) {
            txt = txt.substring(txt.indexOf(target) + target.length());
            num++;
        }
        return num;
    }
}
