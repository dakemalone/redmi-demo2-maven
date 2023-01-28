package com.example.demo.util;

import com.example.demo.pojo.OptionFtp;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;



import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FTPUtil {
    private static final Logger log= LoggerFactory.getLogger(FTPUtil.class);
    /**设置缓冲区大小
     * */
    private static final int BUFFER_SIZE = 1024*1024*4;
    /**本地字符编码
     * */
    private static String LOCAL_CHARSET = "UTF-8";
    /**
     * utf-8字符编码
     * */
    private static final String CHARSET_UTF8 = "UTF-8";
    /**
     * OPTS UTF8字符串常量
     **/
    private static final String OPTS_UTF8 = "OPTS UTF8";

    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     **/
    private static final String SERVER_CHARSET = "ISO-8859-1";

    private static FTPClient ftpClient = null;

    /**
     * 连接ftp服务器
     * */
    private static void connection(OptionFtp optionFtp){
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(optionFtp.getIp(),Integer.valueOf(optionFtp.getPort()));
            boolean isLogin = ftpClient.login(optionFtp.getUser(),optionFtp.getPassword());
            System.out.println("ftp服务器登录"+isLogin);
            ftpClient.sendNoOp();
            ftpClient.setStrictReplyParsing(false);
            ftpClient.setBufferSize(BUFFER_SIZE);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            int reply = ftpClient.getReply();
            System.out.println(reply);
            if (!FTPReply.isProtectedReplyCode(reply)) {
                closeConnection();
            }
        } catch (Exception e) {
            log.error("",e);
            throw new RuntimeException(e);
        }
    }
    /**
     * 关闭ftp client*/
    private static void closeConnection() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e){
                log.error("",e);
            }
        }
    }
    private static String changeEncoding(String ftpPath){
        String directory = null;
        try{
            if(FTPReply.isPositiveCompletion(ftpClient.sendCommand(OPTS_UTF8,"NO"))){
                LOCAL_CHARSET = CHARSET_UTF8;
            }
            directory = new String(ftpPath.getBytes(LOCAL_CHARSET),SERVER_CHARSET);
        } catch (Exception e){
            log.error("",e);
        }
        return directory;
    }
    /**
     * 改变工作目录
     * 如果没有，则创建工作目录
     * @param path
     */
    private static void changeAndMakeWorkingDir(String path) {
        try {
            ftpClient.changeWorkingDirectory("/");
            path = path.replaceAll("\\\\","/");
            String[] path_array = path.split("/");
            for (String s : path_array) {
                boolean b = ftpClient.changeWorkingDirectory(s);
                if (!b) {
                    ftpClient.makeDirectory(s);
                    ftpClient.changeWorkingDirectory(s);
                }
            }
        } catch (IOException e) {
            log.error("",e);
            throw new RuntimeException(e);
        }
    }
    /**
     *
     * @author dake malone
     * @date 26/1/2023 下午 4:04
     * @param
     * @param optionFtp
     * @param filename
     * @param dirPath
     * @param fos
     * @return
     */

    public static void download(OptionFtp optionFtp, String filename, String dirPath, FileOutputStream fos){
        /*登录*/
        connection(optionFtp);
        if(ftpClient != null){
            try{
                String path = changeEncoding(dirPath);
                changeAndMakeWorkingDir(path);
                String[] fileNames = ftpClient.listNames();
                if(fileNames == null || fileNames.length == 0){
                    return;
                }
                for (String fileName :
                        fileNames) {
                    String ftpName = new String(fileName.getBytes(SERVER_CHARSET), LOCAL_CHARSET);
                    if(StringUtils.equals(ftpName,filename)){
                        InputStream in = ftpClient.retrieveFileStream(fileName);
                        IOUtils.copy(in,fos);
                    }
                }

            } catch (IOException e){
                log.error("",e);
            }finally {
                closeConnection();
            }

        }
    }

    public static void main(String[] args) throws IOException{
//        ftpClient = new FTPClient();
//
        OptionFtp optionFtp = new OptionFtp();
        optionFtp.setIp("192.168.206.177");
        optionFtp.setPort("21");
        optionFtp.setUser("ftpuser");
        optionFtp.setPassword("123456");
//        ftpClient.connect(optionFtp.getIp(),Integer.valueOf(optionFtp.getPort()));
//        boolean isLogin = ftpClient.login(optionFtp.getUser(),optionFtp.getPassword());
//        ftpClient.setStrictReplyParsing(false);
//        ftpClient.setBufferSize(BUFFER_SIZE);
//        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//        System.out.println(isLogin);
        FTPUtil.connection(optionFtp);
    }
}
