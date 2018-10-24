package cc.noharry.bleserver.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import cc.noharry.bleserver.L;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author NoHarry
 * @date 2018/03/27
 */

public class BLEAdmin {
  private static BLEAdmin INSTANCE = null;
  private Context mContext = null;
  private final BluetoothAdapter mBluetoothAdapter;
  private final Handler mHandler;
  private BTStateReceiver btStateReceiver = null;
  private final BluetoothManager mBluetoothManager;
  private UUID UUID_SERVER=UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
  private UUID UUID_CHARREAD=UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
  private UUID UUID_DESCRIPTOR=UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");
  private UUID UUID_CHARWRITE=UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
  private BluetoothGattCharacteristic mCharacteristicWrite;
  private AdvertiseSettings mSettings;
  private AdvertiseData mAdvertiseData;
  private AdvertiseData mScanResponseData;
  private AdvertiseCallback mCallback;
  private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
  private boolean isFirstRead=true;


  private BLEAdmin(Context context) {
    mContext = context.getApplicationContext();
    mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
    mBluetoothAdapter = mBluetoothManager.getAdapter();
    mHandler = new Handler(mContext.getMainLooper());
    btStateReceiver = new BTStateReceiver();
  }

  public static BLEAdmin getINSTANCE(Context context){
    if (INSTANCE == null){
      synchronized (BLEAdmin.class){
        INSTANCE = new BLEAdmin(context);
      }
    }
    return INSTANCE;
  }


  /**
   *
   * @param isEnableLog whther enable the debug log
   * @return BLEAdmin
   */
  public BLEAdmin setLogEnable(boolean isEnableLog){
    if (isEnableLog){
      L.isDebug=true;
    }else {
      L.isDebug=false;
    }
    return this;
  }

  /**
   * Return true if Bluetooth is currently enabled and ready for use.
   * @return true if the local adapter is turned on
   */
  public boolean isEnable(){
   return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
  }

  /**
   * Turn on the local Bluetooth adapter
   * @return true to indicate adapter startup has begun, or false on immediate error
   */
  public boolean openBT(){
    if (mBluetoothAdapter!=null && !mBluetoothAdapter.isEnabled()){
      return mBluetoothAdapter.enable();
    }
    return false;
  }

  /**
   * Turn on the local Bluetooth adapter with a listener on {@link BluetoothAdapter#STATE_ON}
   * @param listener listen to the state of bluetooth adapter
   * @return true to indicate adapter startup has begun, or false on immediate error
   */
  public boolean openBT(OnBTOpenStateListener listener){
    btOpenStateListener=listener;
    registerBtStateReceiver(mContext);
    if (mBluetoothAdapter.isEnabled()){
      btOpenStateListener.onBTOpen();
      return true;
    }
    if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()){
      return mBluetoothAdapter.enable();
    }
    return false;
  }


  /**
   * Turn off the local Bluetooth adapter
    */
  public void closeBT() {
    if (null != mBluetoothAdapter && mBluetoothAdapter.isEnabled()) {
      mBluetoothAdapter.disable();
    }
  }

  public BluetoothAdapter getBluetoothAdapter() {
    return mBluetoothAdapter;
  }

  /*public void scan(BleScanConfig config,BleScanCallback callback){
      L.e("开始扫描");
    if (!BleScanner.isScanning.get()){
      BleScanner.getINSTANCE(mContext).scan(config,callback);
    }

  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  public void stopScan(){
    L.e("停止扫描");
    BleScanner.getINSTANCE(mContext).cancelScan();
  }*/

  private void registerBtStateReceiver(Context context) {
    IntentFilter filter = new IntentFilter();
    filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
    context.registerReceiver(btStateReceiver, filter);
  }

  private void unRegisterBtStateReceiver(Context context) {
    try {
      context.unregisterReceiver(btStateReceiver);
    } catch (Exception e) {
    } catch (Throwable e) {
    }

  }

  private OnBTOpenStateListener btOpenStateListener = null;

  public interface OnBTOpenStateListener {
    void onBTOpen();
  }

  private class BTStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context arg0, Intent intent) {
      String action = intent.getAction();
      L.i("action=" + action);
      if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
        int state = intent
            .getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
        L.i("state=" + state);
        switch (state) {
          case BluetoothAdapter.STATE_TURNING_ON:
            L.i("ACTION_STATE_CHANGED:  STATE_TURNING_ON");
            break;
          case BluetoothAdapter.STATE_ON:
            L.i("ACTION_STATE_CHANGED:  STATE_ON");
            if (null != btOpenStateListener){
              btOpenStateListener.onBTOpen();
            }
            unRegisterBtStateReceiver(mContext);
            break;
          default:
        }
      }
    }
  }



  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public void initGATTServer() {
    mSettings = new AdvertiseSettings.Builder()
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .build();

    mAdvertiseData = new AdvertiseData.Builder()
        .setIncludeDeviceName(true)
        .setIncludeTxPowerLevel(true)
        .build();

    //        .addServiceUuid(new ParcelUuid(UUID_SERVER))
    /*,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40*/
    mScanResponseData = new AdvertiseData.Builder()
//        .addServiceUuid(new ParcelUuid(UUID_SERVER))
        .setIncludeTxPowerLevel(true)
        .addServiceData(new ParcelUuid(UUID_SERVER),new byte[]{0,2,3/*,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40*/})
        .build();

//    mBluetoothAdapter.setName("android test huawei");
    mBluetoothAdapter.setName("Ble Server");
    mCallback = new AdvertiseCallback() {

      @Override
      public void onStartSuccess(AdvertiseSettings settingsInEffect) {
        L.i("BLE advertisement added successfully");
        initServices(mContext);

      }

      @Override
      public void onStartFailure(int errorCode) {
        L.e("Failed to add BLE advertisement, reason: " + errorCode);
      }
    };

    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
    mBluetoothLeAdvertiser.startAdvertising(mSettings, mAdvertiseData, mScanResponseData, mCallback);


  }

  public void changeData(){
    mScanResponseData = new AdvertiseData.Builder()
//        .addServiceUuid(new ParcelUuid(UUID_SERVER))
        .setIncludeTxPowerLevel(true)
        .addServiceData(new ParcelUuid(UUID_SERVER),new byte[]{1,2,3/*,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40*/})
        .build();
    mBluetoothLeAdvertiser.stopAdvertising(mCallback);
    mBluetoothLeAdvertiser.startAdvertising(mSettings,mAdvertiseData,mScanResponseData,mCallback);
  }

  private BluetoothGattServer bluetoothGattServer;
  private BluetoothGattCharacteristic characteristicRead;
  private BluetoothDevice currentDevice;
  private void initServices(Context context) {
    bluetoothGattServer = mBluetoothManager.openGattServer(context, bluetoothGattServerCallback);
    BluetoothGattService service = new BluetoothGattService(UUID_SERVER, BluetoothGattService.SERVICE_TYPE_PRIMARY);

    //add a read characteristic.
    characteristicRead = new BluetoothGattCharacteristic(UUID_CHARREAD,
        BluetoothGattCharacteristic.PROPERTY_READ+BluetoothGattCharacteristic.PROPERTY_NOTIFY,
        BluetoothGattCharacteristic.PERMISSION_READ);
    //add a descriptor
    BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(UUID_DESCRIPTOR, BluetoothGattCharacteristic.PERMISSION_WRITE);
//    characteristicRead.addDescriptor(descriptor);
//    service.addCharacteristic(characteristicRead);

    //add a write characteristic.
    mCharacteristicWrite = new BluetoothGattCharacteristic(UUID_CHARWRITE,
        BluetoothGattCharacteristic.PROPERTY_WRITE
            + BluetoothGattCharacteristic.PROPERTY_READ
            + BluetoothGattCharacteristic.PROPERTY_NOTIFY
        ,
        BluetoothGattCharacteristic.PERMISSION_WRITE
            + BluetoothGattCharacteristic.PERMISSION_READ
    );
    service.addCharacteristic(mCharacteristicWrite);
    mCharacteristicWrite.addDescriptor(descriptor);
    bluetoothGattServer.addService(service);
    L.e("2. initServices ok");
  }


  /**
   * 服务事件的回调
   */
  private BluetoothGattServerCallback bluetoothGattServerCallback = new BluetoothGattServerCallback() {

    /**
     * 1.连接状态发生变化时
     * @param device
     * @param status
     * @param newState
     */
    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      L.e(String.format("1.onConnectionStateChange：device name = %s, address = %s", device.getName(), device.getAddress()));
      L.e(String.format("1.onConnectionStateChange：status = %s, newState =%s ", status, newState));
      super.onConnectionStateChange(device, status, newState);
      currentDevice = device;
    }

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
      super.onServiceAdded(status, service);
      L.e(String.format("onServiceAdded：status = %s", status));
    }
//[65, 84, 49, 164, 61, 254, 128, 158, 178, 121, 6, 23, 53, 223, 194, 105, 237, 195, 209]
    //[65, 84, 67, 46, 14, 214, 187, 196, 83, 207, 45, 223, 6, 164, 227, 179, 139, 129, 67]
    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
      L.e(String.format("onCharacteristicReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
      L.e(String.format("onCharacteristicReadRequest：requestId = %s, offset = %s", requestId, offset));
//      SystemClock.sleep(5000);
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
                  super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                  byte[] firstRead=new byte[]{65, 84, 49, (byte) 164, 61, (byte) 254, (byte) 128,
                      (byte) 158, (byte) 178, 121, 6, 23, 53, (byte) 223, (byte) 194, 105,
                      (byte) 237, (byte) 195, (byte) 209};
                  byte[] success=new byte[]{65, 84, 67, 46, 14, (byte) 214, (byte) 187, (byte) 196, 83,
                      (byte) 207, 45, (byte) 223, 6, (byte) 164, (byte) 227, (byte) 179, (byte) 139,
                      (byte) 129, 67};
      /*if (isFirstRead){
        sendMessage(firstRead);
      }else {
        sendMessage(success);
      }*/
      /*for (int i=0;i<5;i++){
        sendMessage("read success"+i);
      }*/
      sendMessage("read success");
    }

    /**
     * 3. onCharacteristicWriteRequest,接收具体的字节
     * @param device
     * @param requestId
     * @param characteristic
     * @param preparedWrite
     * @param responseNeeded
     * @param offset
     * @param requestBytes
     */
    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] requestBytes) {
      L.e(String.format("3.onCharacteristicWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
//      SystemClock.sleep(5000);
      L.i("收到数据:"+Arrays.toString(requestBytes)+" 长度:"+requestBytes.length);
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, requestBytes);
      byte[] success=new byte[]{65, 84, 67, 46, 14, (byte) 214, (byte) 187, (byte) 196, 83,
          (byte) 207, 45, (byte) 223, 6, (byte) 164, (byte) 227, (byte) 179, (byte) 139,
          (byte) 129, 67};
      //4.处理响应内容
      onResponseToClient(requestBytes, device, requestId, characteristic);
//      onResponseToClient(success, device, requestId, characteristic);
    }

    /**
     * 2.描述被写入时，在这里执行 bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS...  收，触发 onCharacteristicWriteRequest
     * @param device
     * @param requestId
     * @param descriptor
     * @param preparedWrite
     * @param responseNeeded
     * @param offset
     * @param value
     */
    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
      L.e(String.format("2.onDescriptorWriteRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
      //            L.e(TAG, String.format("2.onDescriptorWriteRequest：requestId = %s, preparedWrite = %s, responseNeeded = %s, offset = %s, value = %s,", requestId, preparedWrite, responseNeeded, offset, OutputStringUtil.toHexString(value)));

      // now tell the connected device that this was all successfull
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
    }

    /**
     * 5.特征被读取。当回复响应成功后，客户端会读取然后触发本方法
     * @param device
     * @param requestId
     * @param offset
     * @param descriptor
     */
    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
      L.e(String.format("onDescriptorReadRequest：device name = %s, address = %s", device.getName(), device.getAddress()));
      L.e(String.format("onDescriptorReadRequest：requestId = %s", requestId));
      //            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
      bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
    }

    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);
      L.e(String.format("5.onNotificationSent：device name = %s, address = %s", device.getName(), device.getAddress()));
      L.e(String.format("5.onNotificationSent：status = %s", status));
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu) {
      super.onMtuChanged(device, mtu);
      L.e(String.format("onMtuChanged：mtu = %s", mtu));
    }

    @Override
    public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
      super.onExecuteWrite(device, requestId, execute);
      L.e(String.format("onExecuteWrite：requestId = %s", requestId));
    }
  };

  /**
   * 4.处理响应内容
   *
   * @param reqeustBytes
   * @param device
   * @param requestId
   * @param characteristic
   */
  private void onResponseToClient(byte[] reqeustBytes, BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic) {
    L.e(String.format("4.onResponseToClient：device name = %s, address = %s", device.getName(), device.getAddress()));
    L.e(String.format("4.onResponseToClient：requestId = %s", requestId));
    //        String msg = OutputStringUtil.transferForPrint(reqeustBytes);
    String msg = new String(reqeustBytes);
    L.i("4.收到:" + Arrays.toString(reqeustBytes));
    currentDevice = device;
//    sendMessage("success");
    sendMessage(reqeustBytes);
    /*for (int i=0;i<10;i++){
      SystemClock.sleep(1000);
      sendMessage("success"+i);
    }*/

  }

  private void sendMessage(String message) {
    byte[] i=new byte[]{6};
//    characteristicRead.setValue(message.getBytes());
//    mCharacteristicWrite.setValue(message.getBytes());
    characteristicRead.setValue(i);
    mCharacteristicWrite.setValue(i);
    if (currentDevice != null){
      bluetoothGattServer.notifyCharacteristicChanged(currentDevice, characteristicRead, false);
      bluetoothGattServer.notifyCharacteristicChanged(currentDevice, mCharacteristicWrite, false);
    }


    L.i("4.发送:" + message);
  }

  private void sendMessage(byte[] message) {
//    byte[] i=new byte[]{6};
//    characteristicRead.setValue(message.getBytes());
//    mCharacteristicWrite.setValue(message.getBytes());
    characteristicRead.setValue(message);
    mCharacteristicWrite.setValue(message);
    if (currentDevice != null){
      bluetoothGattServer.notifyCharacteristicChanged(currentDevice, characteristicRead, false);
      bluetoothGattServer.notifyCharacteristicChanged(currentDevice, mCharacteristicWrite, false);
    }


    L.i("4.发送:" + Arrays.toString(message));
  }


}
