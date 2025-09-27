package com.example.nasonly.utils;

import java.util.List;

/**
 * NASUtils使用示例
 */
public class NASUtilsExample {

    public static void main(String[] args) {
        // 示例使用方法
        demonstrateNASUtils();
    }

    /**
     * 演示NASUtils的使用方法
     */
    public static void demonstrateNASUtils() {
        String host = "192.168.1.100";  // NAS服务器IP地址
        String username = "your_username";  // 用户名
        String password = "your_password";  // 密码

        try {
            // 获取所有共享文件夹
            List<String> shares = NASUtils.getAllShares(host, username, password);
            
            System.out.println("成功连接到NAS服务器: " + host);
            System.out.println("找到 " + shares.size() + " 个共享文件夹:");
            
            for (String share : shares) {
                System.out.println("- " + share);
            }

            // 如果有共享文件夹，演示列出第一个共享中的文件
            if (!shares.isEmpty()) {
                String firstShare = shares.get(0);
                System.out.println("\n列出共享文件夹 '" + firstShare + "' 中的文件和目录:");

                List<String> files = NASUtils.listFilesInShare(host, username, password, firstShare);
                System.out.println("找到 " + files.size() + " 个文件/目录:");

                for (String file : files) {
                    System.out.println("  - " + file);
                }
            }
            
        } catch (NASUtils.NASConnectionException e) {
            System.err.println("连接错误: " + e.getMessage());
            // 可以根据需要添加重试逻辑或其他处理
            
        } catch (NASUtils.NASAuthenticationException e) {
            System.err.println("认证错误: " + e.getMessage());
            // 提示用户检查用户名和密码
            
        } catch (IllegalArgumentException e) {
            System.err.println("参数错误: " + e.getMessage());
            // 提示用户检查输入参数
        }
    }

    /**
     * 在Android应用中的异步使用示例
     */
    public static void asyncExample(String host, String username, String password) {
        // 在Android中应该在后台线程执行网络操作
        new Thread(() -> {
            try {
                List<String> shares = NASUtils.getAllShares(host, username, password);
                
                // 在主线程更新UI
                // runOnUiThread(() -> {
                //     // 更新UI显示共享列表
                //     updateUI(shares);
                // });
                
            } catch (NASUtils.NASConnectionException e) {
                // 处理连接错误
                handleConnectionError(e);
                
            } catch (NASUtils.NASAuthenticationException e) {
                // 处理认证错误
                handleAuthenticationError(e);
            }
        }).start();
    }

    private static void handleConnectionError(NASUtils.NASConnectionException e) {
        System.err.println("连接失败: " + e.getMessage());
        // 在实际应用中，这里应该通知UI显示错误信息
    }

    private static void handleAuthenticationError(NASUtils.NASAuthenticationException e) {
        System.err.println("认证失败: " + e.getMessage());
        // 在实际应用中，这里应该提示用户重新输入凭据
    }
}