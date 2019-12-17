package com.example.rovercontroller;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class Client {

    private Socket socket;
    private DataOutputStream dOut;
    private String ipAdd;
    private int fbSpeed;
    private int lrSpeed;

    Client(String ipAdd) {
        this.ipAdd = ipAdd;
        fbSpeed = 0;
        lrSpeed = 0;
    }

    void OpenConnection() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket(ipAdd, 8080);
                    dOut = new DataOutputStream(socket.getOutputStream());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void CloseConnection() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    dOut.writeUTF("over");

                    dOut.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    void UpDownClick(int i) {
        if(fbSpeed >= -100 && fbSpeed <= 100) {
            fbSpeed += i;
        }
        showSpeeds();
    }

    void LeftRightClick(int i) {
        if(lrSpeed >= -100 && lrSpeed <= 100) {
            lrSpeed += i;
        }
        showSpeeds();
    }

    private void showSpeeds() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String str = fbSpeed + "," + lrSpeed;
                    dOut.writeUTF(str);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
