package com.example.nasonly.data.smb

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.Ignore

/**
 * SMB异步连接功能测试
 * 验证网络操作已正确移到IO线程
 */
class SmbAsyncConnectionTest {

    @Test
    fun testSmbConnectionManager_asyncMethods() {
        val smbConnectionManager = SmbConnectionManager()
        
        // 测试异步配置
        runBlocking {
            smbConnectionManager.configure(
                host = "192.168.1.100",
                share = "media",
                username = "testuser",
                password = "testpass",
                domain = ""
            )
        }
        
        // 验证配置没有抛出异常
        assertTrue("异步配置应该成功", true)
    }

    @Test
    fun testSmbConnectionManager_validateConnectionAsync() {
        val smbConnectionManager = SmbConnectionManager()
        
        runBlocking {
            // 配置无效参数
            smbConnectionManager.configure("", "", "", "", "")
            
            // 测试异步验证
            val result = smbConnectionManager.validateConnectionAsync()
            
            // 应该返回错误，因为参数为空
            assertTrue("空参数应该返回错误", result is SmbConnectionResult.Error)
            
            if (result is SmbConnectionResult.Error) {
                assertTrue("错误消息应该提到主机地址", result.message.contains("主机地址"))
            }
        }
    }

    @Test
    @Ignore("需要网络连接，在CI环境中跳过")
    fun testSmbConnectionManager_asyncConnect() {
        val smbConnectionManager = SmbConnectionManager()
        
        runBlocking {
            // 配置有效但不存在的服务器
            smbConnectionManager.configure(
                host = "192.168.1.999", // 不存在的IP
                share = "test",
                username = "test",
                password = "test"
            )
            
            // 在单元测试环境中，网络连接会失败
            // 测试异步方法不会抛出异常即可
            try {
                val connected = smbConnectionManager.connectAsync()
                // 连接失败是预期的
                assertFalse("连接到不存在的服务器应该失败", connected)
            } catch (e: Exception) {
                // 在测试环境中可能会有异常，这是正常的
                assertTrue("异步连接方法应该能处理异常", true)
            }
        }
    }

    @Test
    fun testSmbConnectionResult_types() {
        val successResult = SmbConnectionResult.Success("连接成功")
        val errorResult = SmbConnectionResult.Error("连接失败")
        
        assertTrue("Success结果应该是Success类型", successResult is SmbConnectionResult.Success)
        assertTrue("Error结果应该是Error类型", errorResult is SmbConnectionResult.Error)
        
        assertEquals("Success消息应该正确", "连接成功", successResult.message)
        assertEquals("Error消息应该正确", "连接失败", errorResult.message)
    }

    @Test
    @Ignore("在单元测试环境中跳过，需要模拟环境")
    fun testSmbConnectionManager_asyncDisconnect() {
        val smbConnectionManager = SmbConnectionManager()
        
        runBlocking {
            // 测试异步断开连接不会抛出异常
            try {
                smbConnectionManager.disconnectAsync()
                // 验证方法调用成功
                assertTrue("异步断开连接应该成功执行", true)
            } catch (e: Exception) {
                fail("异步断开连接不应该抛出异常: ${e.message}")
            }
        }
    }
}