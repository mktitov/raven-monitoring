/*
 * Copyright 2015 Mikhail Titov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.raven;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import org.junit.Test;

/**
 *
 * @author Mikhail Titov
 */
public class SocketTest {
    private final static int PORT = 1234;
    
    @Test
    public void test() throws Exception {
        ServerAcceptThread server = new ServerAcceptThread();
        Client client = new Client();
        server.start();
        Thread.sleep(100);
        client.start();
        server.join();
        client.join();
    }
    
    private ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket socket = new ServerSocket(port, 50, getAddress() );
        return socket;
    }
    
    private Socket createClientSocket(int port) throws IOException {
        Socket socket = new Socket(getAddress(), port);
        return socket;
    }

    private InetAddress getAddress() throws UnknownHostException {
        return Inet4Address.getLocalHost();
    }
    
    private void clientLog(String message) {
        log("CLIENT", message);
    }
    
    private void serverLog(String message) {
        log("SERVER", message);
    }
    
    private void log(String who, String message) {
        System.out.println(String.format("%tH:%<tM:%<tS.%<tL [%s] %s", new Date(), who, message));
    }
    
    private class Client extends Thread {

        @Override
        public void run() {
            try {
                Socket socket = createClientSocket(PORT);
                InputStream in = socket.getInputStream();                
                int data;
                while ( (data = in.read())!=-1 )
                    clientLog("Received data: "+data);
                clientLog("Channel closed by server");
                Thread.sleep(1000);
                clientLog("Closing CLIENT socket");
                socket.close();
            } catch (Exception e) {
                clientLog("Exception in client socket: "+e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
    
    private class ServerAcceptThread extends Thread {
        

        @Override
        public void run() {
            try {
                ServerSocket server = createServerSocket(PORT);
                Socket socket = server.accept();
                OutputStream out = socket.getOutputStream();
                serverLog("Sending 1");
                out.write(1);
                Thread.sleep(1000);
                serverLog("Sending 2");
                out.write(2);
                Thread.sleep(1000);
                serverLog("Sending 3");
                out.write(3);
                serverLog("Closing output stream...");
                socket.shutdownOutput();
                Thread.sleep(2000);
                serverLog("Closing socket and server socket");
                socket.close();
                server.close();
                serverLog("Server accept process stopped");
            } catch (Exception e) {
                serverLog("Exception in SERVER socket: "+e.getMessage());
                e.printStackTrace(System.out);
                
            }
        }
    }
}
