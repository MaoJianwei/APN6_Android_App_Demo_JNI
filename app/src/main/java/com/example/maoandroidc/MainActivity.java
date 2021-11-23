package com.example.maoandroidc;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maoandroidc.databinding.ActivityMainBinding;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketImpl;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'maoandroidc' library on application startup.
    static {
        System.loadLibrary("maoandroidc");
    }

    private ActivityMainBinding binding;

    private MainActivity me = this;

    private ExecutorService threadPool;

    private Socket client;
    private Runnable connectTask = new Runnable() {
        @Override
        public void run() {
            me.runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), "connectTask run", Toast.LENGTH_SHORT).show();
            });
            if (client == null) {
                client = new Socket();
                try {
                    EditText addr = binding.ConnectToAddr;
                    EditText port = binding.ConnectToPort;

                    client.connect(new InetSocketAddress(addr.getText().toString(), Integer.parseInt(port.getText().toString())));
//                    client.connect(new InetSocketAddress("127.0.0.1", 8768));
//                    client.connect(new InetSocketAddress("pi-china.maojianwei.com", 22));
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "connect ok", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
//                    e.printStackTrace();
                    client = null;
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), String.format("connect fail: %s", e.toString()), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    };

    int count = 1;
    private Runnable sendTask = new Runnable() {
        @Override
        public void run() {
            me.runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), "sendTask run", Toast.LENGTH_SHORT).show();
            });
            if (client != null) {
                try {
//                    setSockOptAPN(client);

                    OutputStream os = client.getOutputStream();
                    String str = String.format("Send count %d", count++);
                    os.write(str.getBytes());
                    me.runOnUiThread(() -> {
                        TextView clientText = binding.ClientResult;
                        clientText.setText(str);
                        Toast.makeText(getApplicationContext(), "send ok", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), String.format("send fail: %s", e.toString()), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    };
    private Runnable closeTask = new Runnable() {
        @Override
        public void run() {
            me.runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), "closeTask run", Toast.LENGTH_SHORT).show();
            });
            if (client != null) {
                try {
                    client.close();
                    client = null;
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "close ok", Toast.LENGTH_SHORT).show();
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), String.format("close fail: %s", e.toString()), Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }
    };


    private ServerSocket ss;
    private Runnable acceptRecvSocket = new Runnable() {
        @Override
        public void run() {
            me.runOnUiThread(() -> {
                Toast.makeText(getApplicationContext(), "acceptRecvSocket run", Toast.LENGTH_SHORT).show();
            });

            while (true) {
                Socket s = null;
                try {
                    s = ss.accept();
                    s.setTrafficClass(6);
                    try {
                        Field implField = s.getClass().getDeclaredField("impl");
                        implField.setAccessible(true);
                        SocketImpl impl = (SocketImpl)implField.get(s);

                        Class socksSocketImpl = impl.getClass();
                        Class dualStackPlainSocketImpl = impl.getClass().getSuperclass();
                        Class abstractPlainSocketImpl = impl.getClass().getSuperclass().getSuperclass();
                        Class socketImpl = impl.getClass().getSuperclass().getSuperclass().getSuperclass();

                        Field fdField = socketImpl.getDeclaredField("fd");
                        fdField.setAccessible(true);
                        FileDescriptor fd = (FileDescriptor)fdField.get(impl);

                        Field fdIntField = fd.getClass().getDeclaredField("descriptor");
                        fdIntField.setAccessible(true);
                        int fdInt = (Integer) fdIntField.get(fd);

                        int errno = setSockOptAPN(fdInt);
                        me.runOnUiThread(() -> {
                            Toast.makeText(getApplicationContext(), String.format("set APN errno: %d", errno), Toast.LENGTH_SHORT).show();
                        });
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }

                    InputStream in = s.getInputStream();

                    while (true) {
                        byte buf[] = new byte[4096];
                        int count = in.read(buf, 0, 4096);
                        if (count == -1) {
                            throw new IOException("client closed");
                        }

                        me.runOnUiThread(() -> {
                            TextView serverResult = binding.ServerResult;
                            String recv = new String(buf, StandardCharsets.UTF_8);
                            serverResult.setText(recv);
                            Toast.makeText(getApplicationContext(), String.format("Server receive: %d, %s", count, recv), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if (s != null){
                        try {
                            s.close();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                    me.runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Server thread hold on", Toast.LENGTH_SHORT).show();
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.ClientResult;
        tv.setText(stringFromJNI());

        client = null;
        ss = null;

        threadPool = Executors.newCachedThreadPool();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Toast.makeText(getApplicationContext(), "onResume Mao", Toast.LENGTH_SHORT).show();

        Button buttonBindAndListen = binding.BindAndListen;
        buttonBindAndListen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText port = binding.ConnectToPort;
                    ss = new ServerSocket();
                    ss.bind(new InetSocketAddress("0.0.0.0", Integer.parseInt(port.getText().toString())));
                    Toast.makeText(getApplicationContext(), "buttonBindAndListen clicked - bind", Toast.LENGTH_SHORT).show();

                    threadPool.submit(acceptRecvSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), String.format("IOException: %s", e.toString()), Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonConnect = binding.Connect;
        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadPool.submit(connectTask);
//                Toast.makeText(getApplicationContext(), "buttonConnect clicked", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonSendAndIncrease = binding.SendAndIncrease;
        buttonSendAndIncrease.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadPool.submit(sendTask);
//                Toast.makeText(getApplicationContext(), "buttonSendAndIncrease clicked", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonClose = binding.Close;
        buttonClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                threadPool.submit(closeTask);
//                Toast.makeText(getApplicationContext(), "buttonClose clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (ss != null) {
            try {
                ss.close();
                ss = null;
                Toast.makeText(getApplicationContext(), "onPause release ss", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), String.format("onPause release ss faild, IOException: %s", e.toString()), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * A native method that is implemented by the 'maoandroidc' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native int setSockOptAPN(int sockFd);
}