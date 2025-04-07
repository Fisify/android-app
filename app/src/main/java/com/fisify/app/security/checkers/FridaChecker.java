package com.fisify.app.security.checkers;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class FridaChecker {
    public boolean checkFridaFiles() {
        String[] fridaPaths = {
                "/data/local/tmp/frida-server",
                "/data/local/tmp/re.frida.server",
                "/data/local/tmp/frida-server-debug"
        };

        for (String path : fridaPaths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    public boolean checkFridaPort() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 27042), 100); // 100ms timeout
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean quickFridaDBusCheck() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 27042), 100);
            OutputStream out = socket.getOutputStream();
            out.write("AUTH\r\n".getBytes());
            out.flush();

            byte[] response = new byte[5]; // Solo lee los primeros bytes
            socket.setSoTimeout(100);
            int read = socket.getInputStream().read(response);
            return read > 0;
        } catch (Exception e) {
            return false;
        }
    }
}