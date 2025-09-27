package com.example.nasonly.utils;

import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.FileIdBothDirectoryInformation;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * NAS工具类，用于操作SMB共享文件夹
 */
public class NASUtils {

    /**
     * 获取NAS上所有共享文件夹名称
     *
     * @param host     NAS服务器地址
     * @param username 用户名
     * @param password 密码
     * @return 共享文件夹名称列表
     * @throws NASConnectionException 当连接失败时抛出
     * @throws NASAuthenticationException 当认证失败时抛出
     */
    public static List<String> getAllShares(String host, String username, String password) 
            throws NASConnectionException, NASAuthenticationException {
        
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null) {
            throw new IllegalArgumentException("密码不能为空");
        }

        SMBClient client = new SMBClient();
        Connection connection = null;
        Session session = null;
        
        try {
            // 尝试连接到SMB服务器
            connection = client.connect(host);
            
            // 尝试认证
            com.hierynomus.smbj.auth.AuthenticationContext authContext = 
                new com.hierynomus.smbj.auth.AuthenticationContext(
                    username, 
                    password.toCharArray(), 
                    null
                );
            
            session = connection.authenticate(authContext);
            
            // 获取所有共享
            Set<String> shareNames = session.getShares();
            
            // 过滤掉系统共享（以$结尾的）
            List<String> userShares = new ArrayList<>();
            for (String shareName : shareNames) {
                if (!shareName.endsWith("$")) {
                    userShares.add(shareName);
                }
            }
            
            return userShares;
            
        } catch (com.hierynomus.smbj.common.SMBApiException e) {
            // SMB协议级别的错误，通常是认证失败
            if (e.getMessage().contains("STATUS_LOGON_FAILURE") || 
                e.getMessage().contains("STATUS_ACCESS_DENIED") ||
                e.getMessage().contains("Authentication failed")) {
                throw new NASAuthenticationException("认证失败：用户名或密码错误", e);
            } else {
                throw new NASConnectionException("SMB协议错误：" + e.getMessage(), e);
            }
        } catch (ConnectException e) {
            // 网络连接失败
            throw new NASConnectionException("无法连接到NAS服务器 " + host + "：" + e.getMessage(), e);
        } catch (IOException e) {
            // 其他IO错误
            throw new NASConnectionException("连接过程中发生IO错误：" + e.getMessage(), e);
        } catch (Exception e) {
            // 其他未预期的错误
            throw new NASConnectionException("获取共享列表时发生未知错误：" + e.getMessage(), e);
        } finally {
            // 确保资源被正确释放
            try {
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                // 记录关闭资源时的错误，但不抛出异常
                System.err.println("关闭SMB连接时发生错误：" + e.getMessage());
            }
        }
    }

    /**
     * 列出指定共享目录下的所有文件和子目录名称
     *
     * @param host      NAS服务器地址
     * @param username  用户名
     * @param password  密码
     * @param shareName 共享文件夹名称
     * @return 文件和子目录名称列表
     * @throws NASConnectionException 当连接失败时抛出
     * @throws NASAuthenticationException 当认证失败时抛出
     */
    public static List<String> listFilesInShare(String host, String username, String password, String shareName)
            throws NASConnectionException, NASAuthenticationException {

        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("主机地址不能为空");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (shareName == null || shareName.trim().isEmpty()) {
            throw new IllegalArgumentException("共享文件夹名称不能为空");
        }

        try (SMBClient client = new SMBClient();
             Connection connection = client.connect(host)) {

            // 尝试认证
            com.hierynomus.smbj.auth.AuthenticationContext authContext =
                new com.hierynomus.smbj.auth.AuthenticationContext(
                    username,
                    password.toCharArray(),
                    null
                );

            try (Session session = connection.authenticate(authContext);
                 DiskShare share = (DiskShare) session.connectShare(shareName)) {

                // 获取根目录下的所有文件和文件夹
                List<FileIdBothDirectoryInformation> fileList = share.list("");

                // 提取文件名和目录名
                List<String> result = new ArrayList<>();
                for (FileIdBothDirectoryInformation fileInfo : fileList) {
                    String fileName = fileInfo.getFileName();
                    // 跳过当前目录(.)和上级目录(..)
                    if (!".".equals(fileName) && !"..".equals(fileName)) {
                        result.add(fileName);
                    }
                }

                return result;

            } catch (ClassCastException e) {
                throw new NASConnectionException("指定的共享 '" + shareName + "' 不是磁盘共享", e);
            }

        } catch (com.hierynomus.smbj.common.SMBApiException e) {
            // SMB协议级别的错误，通常是认证失败或共享不存在
            if (e.getMessage().contains("STATUS_LOGON_FAILURE") ||
                e.getMessage().contains("STATUS_ACCESS_DENIED") ||
                e.getMessage().contains("Authentication failed")) {
                throw new NASAuthenticationException("认证失败：用户名或密码错误", e);
            } else if (e.getMessage().contains("STATUS_BAD_NETWORK_NAME") ||
                       e.getMessage().contains("STATUS_OBJECT_NAME_NOT_FOUND")) {
                throw new NASConnectionException("共享文件夹 '" + shareName + "' 不存在", e);
            } else {
                throw new NASConnectionException("SMB协议错误：" + e.getMessage(), e);
            }
        } catch (ConnectException e) {
            // 网络连接失败
            throw new NASConnectionException("无法连接到NAS服务器 " + host + "：" + e.getMessage(), e);
        } catch (IOException e) {
            // 其他IO错误
            throw new NASConnectionException("连接过程中发生IO错误：" + e.getMessage(), e);
        } catch (Exception e) {
            // 其他未预期的错误
            throw new NASConnectionException("获取文件列表时发生未知错误：" + e.getMessage(), e);
        }
    }

    /**
     * NAS连接异常
     */
    public static class NASConnectionException extends Exception {
        public NASConnectionException(String message) {
            super(message);
        }

        public NASConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * NAS认证异常
     */
    public static class NASAuthenticationException extends Exception {
        public NASAuthenticationException(String message) {
            super(message);
        }

        public NASAuthenticationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void main(String[] args) {
        String host = "192.168.31.114";
        String username = "admin";
        String password = "Xu8389765";
        try {
            List<String> shares = getAllShares(host, username, password);
            System.out.println("共享目录列表:");
            for (String share : shares) {
                System.out.println("- " + share);
            }
            if (!shares.isEmpty()) {
                String firstShare = shares.get(0);
                System.out.println("\n第一个共享目录 '" + firstShare + "' 下的文件和文件夹:");
                List<String> files = listFilesInShare(host, username, password, firstShare);
                for (String file : files) {
                    System.out.println("文件/文件夹: " + file);
                }
            } else {
                System.out.println("没有可用的共享目录。");
            }
        } catch (NASConnectionException e) {
            System.err.println("连接错误: " + e.getMessage());
        } catch (NASAuthenticationException e) {
            System.err.println("认证错误: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("其他错误: " + e.getMessage());
        }
    }
}