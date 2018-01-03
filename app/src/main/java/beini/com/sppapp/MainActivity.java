package beini.com.sppapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import beini.com.sppapp.adapter.BaseAdapter;
import beini.com.sppapp.adapter.BaseBean;
import beini.com.sppapp.adapter.BluetoothDeviceAdapter;
import beini.com.sppapp.blue.SppDeco;

public class MainActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 0x110;
    private BluetoothDeviceAdapter mBluetoothDeviceAdapter;
    private List<BluetoothDevice> mBluetoothDevices = new ArrayList<>();
    private RecyclerView recycle_blue_devices;
    private static final UUID BLUE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private BluetoothSocket bluetoothSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recycle_blue_devices = findViewById(R.id.recycle_blue_devices);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 获得和当前Android已经配对的蓝牙设备。
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            // 遍历
            for (BluetoothDevice device : pairedDevices) {
                // 把已经取得配对的蓝牙设备名字和地址打印出来。
                Log.d("com.beini", " 已经配对的蓝牙设备 " + device.getName() + " : " + device.getAddress());
                device.describeContents();

                if (!TextUtils.isEmpty(device.getName())) {
                    mBluetoothDevices.add(device);
                }
            }
        }
        //从BluetoothDevice获得BluetoothSocket，取出输入输出流，进而进行数据传输
        //Android Bluetooth蓝牙设备的连接编程模型和Java socket网络连接编程模型类型。

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycle_blue_devices.setLayoutManager(linearLayoutManager);
        mBluetoothDeviceAdapter = new BluetoothDeviceAdapter(new BaseBean<>(R.layout.item_blue_devices, mBluetoothDevices));
        recycle_blue_devices.setAdapter(mBluetoothDeviceAdapter);
        mBluetoothDeviceAdapter.setItemClick(new BaseAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mBluetoothAdapter != null && mBluetoothAdapter.enable() && mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
                BluetoothDevice bluetoothDevice = mBluetoothDevices.get(position);
                try {
                    Log.d("com.beini", "开始建立连接...");
                    bluetoothSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(BLUE_UUID);
                    bluetoothSocket.connect();
                    Log.d("com.beini", "连接建立...");
                    new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            writeDataFromServer(bluetoothSocket);
                        }
                    }.start();

                } catch (IOException e) {
                    Log.d("com.beini", "建立连接出现异常..." + e.getLocalizedMessage());
                    e.printStackTrace();
                }

            }
        });
        findViewById(R.id.btn_serch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("com.beini", "OnClick");
                if (mBluetoothAdapter == null) {
                    //不支持蓝牙
                    return;
                }
                Log.d("com.beini", "!mBluetoothAdapter.isEnabled()=" + !mBluetoothAdapter.isEnabled());
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    // 接下去，在onActivityResult回调判断
                } else {
                    startDiscovery();
                }

            }
        });
        findViewById(R.id.btn_stop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled() && mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.cancelDiscovery();
                }
            }
        });

    }

    private void writeDataFromServer(BluetoothSocket bluetoothSocket) {
        InputStream inputStream;
        OutputStream outputStream;
        try {
            Log.d("com.beini", "开始发送...");
            inputStream = bluetoothSocket.getInputStream();
            outputStream = bluetoothSocket.getOutputStream();
            byte[] data = new byte[2];
            data[0] = 0;
            data[1] = (byte) 1;
            byte[] cmd = SppDeco.newEncodeCmd(0x72, data);
            outputStream.write(cmd);
            outputStream.flush();
            Log.d("com.beini", "发送结束...");
        } catch (IOException e) {
            Log.d("com.beini", "发送异常  " + e.getLocalizedMessage());
            e.printStackTrace();
        }

    }

    // 广播接收发现蓝牙设备
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("com.beini", " action=" + action);
            switch (action) {
                case BluetoothDevice.ACTION_FOUND://name为null过滤
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.d("com.beini", "设备名字:" + device.getName() + "   设备地址:" + device.getAddress());
                    if (!TextUtils.isEmpty(device.getName())) {
                        mBluetoothDevices.add(device);
                        mBluetoothDeviceAdapter.notifyDataSetChanged();
                    }
                    device.getBondState();//获取设备绑定状态
                    device.getType();//传统蓝牙,低功耗
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED://12秒
                    Log.d("com.beini", "蓝牙设备搜索结束...  mBluetoothAdapter.isDiscovering()=" + mBluetoothAdapter.isDiscovering());
                    break;
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("com.beini", "打开蓝牙成功！");
                startDiscovery();
            }

            if (resultCode == RESULT_CANCELED) {
                Log.d("com.beini", "放弃打开蓝牙！");
            }

        } else {
            Log.d("com.beini", "打开蓝牙异常！");
            return;
        }
    }

    public void startDiscovery() {
        Log.d("com.beini", "!mBluetoothAdapter.isDiscovering()=" + !mBluetoothAdapter.isDiscovering());
        if (!mBluetoothAdapter.isDiscovering()) {
            if (mBluetoothAdapter.startDiscovery()) {
                Log.d("com.beini", "启动蓝牙扫描设备...");
                mBluetoothDevices.clear();
//                mBluetoothAdapter.startLeScan()
//                mBluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
//                    @Override
//                    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//                        Log.d("com.beini", "device.getName()=" + device.getName() + " device.getAddress()=" + device.getAddress());
//                    }
//                });
            } else {
                Log.d("com.beini", "启动蓝牙扫描设备出现异常...");
            }
        } else {
            Log.d("com.beini", "正在搜索...");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // 可选方法，非必需
    // 此方法使自身的蓝牙设备可以被其他蓝牙设备扫描到，
    // 注意时间阈值。0 - 3600 秒。0将一直保持可被发现、可被扫描状态，但会很消耗电力资源。
    // 通常设置时间为120秒。
    private void enablingDiscoverability() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        // 0，自身设备始终可以被发现（意味着将十分消耗设备资源，如电源）
        // 第二个参数可设置的范围是0~3600秒，在此时间区间（窗口期）内可被发现
        // 任何不在此区间的值都将被自动设置成120秒。
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
        startActivity(discoverableIntent);
    }
}
