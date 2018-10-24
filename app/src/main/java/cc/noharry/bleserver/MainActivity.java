package cc.noharry.bleserver;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import cc.noharry.bleserver.ble.BLEAdmin;
import cc.noharry.bleserver.ble.BLEAdmin.OnBTOpenStateListener;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity implements OnClickListener {

  private Button mBtInit;
  private UUID UUID_SERVER=UUID.fromString("0000ffe5-0000-1000-8000-00805f9b34fb");
  private TextView mTv_times;
  public AtomicInteger times=new AtomicInteger(0);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    initView();
    initEvent();
  }

  private void initEvent() {
    mBtInit.setOnClickListener(this);
  }

  private void initView() {
    mBtInit = findViewById(R.id.bt_init);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()){
      case R.id.bt_init:
        openBt();
        break;

      default:
    }
  }


  private void openBt() {
    BLEAdmin.getINSTANCE(this).openBT(new OnBTOpenStateListener() {
      @Override
      public void onBTOpen() {
       L.i("开启蓝牙");
       initGatt();
      }
    });
  }

  private void initGatt() {
    BLEAdmin.getINSTANCE(this).initGATTServer();
  }



}
