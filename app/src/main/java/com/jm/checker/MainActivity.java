package com.jm.checker;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;


public class MainActivity extends AppCompatActivity {
    private Switch fan, potion;
    private Button turnLeft, turnRight;
    private TextView temp, humi, hasPotion, rfid;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
//        readRFID();//读取rfid
//        readSensorData();//循环读取传感器数据
    }

    private void initView() {

        fan = findViewById(R.id.fan);
        potion = findViewById(R.id.potion);
        turnLeft = findViewById(R.id.turn_left);
        turnRight = findViewById(R.id.turn_right);
        temp = findViewById(R.id.temp);
        humi = findViewById(R.id.humi);
        hasPotion = findViewById(R.id.has_potion);
        rfid = findViewById(R.id.rfid);
    }

    private void initListener(){

        fan.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked){
                    //风机开
                    PortUtil.port0.start();
                }else {
                    PortUtil.port0.end();
                }
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void readRFID(){
        PortUtil.port1.readTag()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<byte[]>() {
                    @Override
                    public void accept(byte[] bytes) throws Exception {
                        String type = JMByteUtil.convertHexToDec(bytes);
                        rfid.setText("药水编号: " + type);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d("readRFIDERROR", throwable.getMessage());
                        Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void readSensorData(){
        Observable.interval(5000, TimeUnit.MILLISECONDS)
                .flatMap(new Function<Long, Observable<byte[]>>() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override
                    public Observable<byte[]> apply(Long aLong) throws Exception {
                        return PortUtil.port0.readSensorData();
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<byte[]>() {
                    @Override
                    public void accept(byte[] bytes) throws Exception {
                        fetchSensorData(bytes);
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        Log.d("readSensorError", throwable.getMessage());
                        Toast.makeText(MainActivity.this,throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void fetchSensorData(byte[] data){

        DecimalFormat df = new DecimalFormat("0.0");
        if (data == null || data.length != 13) {
            return;
        }

        byte[] pm = new byte[]{data[4], data[5]};
        byte[] tem = new byte[]{data[6], data[7]};
        byte[] humi = new byte[]{data[8], data[9]};
        byte hasLiquid = data[10];

        float pmValue = JMByteUtil.getsignedSum(pm);
        float humiValue = JMByteUtil.getsignedSum(humi);
        float tempValue = JMByteUtil.getUnSignedSum(tem);
        float liquidValue = hasLiquid;

        temp.setText("温度: "+ df.format(tempValue) + "℃");
        this.humi.setText("湿度: " + df.format(humiValue) + "RH");
        hasPotion.setText(liquidValue > 0 ? "有药水" : "无药水");
    }
}
