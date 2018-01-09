package com.weidongjian.meitu.wheelviewdemo;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.weigan.loopview.LoopAdapter;
import com.weigan.loopview.LoopView;
import com.weigan.loopview.OnItemClickListener;
import com.weigan.loopview.OnItemSelectedListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Toast toast;
    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final LoopView loopView = (LoopView) findViewById(R.id.loopView);
        //设置渐进色
//        loopView.setTopShaderCallback(new LoopView.TextShaderCallback() {
//            @Override
//            public void setShader(Paint paint, int x0, int y0, int x1, int y1) {
//                Shader shader = new LinearGradient(x0, y0, x0, y1, new int[] {0xFFFF0000, 0xFF00FF00, 0xFF0000FF}, null, Shader.TileMode.CLAMP);
//                paint.setShader(shader);
//            }
//        });
//
//        loopView.setCenterShaderCallback(new LoopView.TextShaderCallback() {
//            @Override
//            public void setShader(Paint paint, int x0, int y0, int x1, int y1) {
//                Shader shader = new LinearGradient(x0, y0, x0, y1, new int[] {0xFFFF0000, 0xFF00FF00, 0xFF0000FF}, null, Shader.TileMode.CLAMP);
//                paint.setShader(shader);
//            }
//        });

//        loopView.setBottomTextColor(getColor(R.color.blue));
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 30  ; i++) {
            list.add("item " + i);
        }

        //设置是否循环播放
        loopView.setNotLoop();
        loopView.setItemsVisibleCount(5);
        //滚动监听
        loopView.setListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
//                if (toast == null) {
//                    toast = Toast.makeText(MainActivity.this, "item " + index, Toast.LENGTH_SHORT);
//                }
//                toast.setText("item " + index);
//                toast.show();
                Log.d(TAG, "onItemSelect index=" + index);
            }
        });

        loopView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(int index) {
                if (toast == null) {
                    toast = Toast.makeText(MainActivity.this, "click item " + index, Toast.LENGTH_SHORT);
                }
                toast.setText("click item " + index);
                toast.show();

                Log.d(TAG, "onItemClick index=" + index);
            }
        });
        //设置原始数据
//        loopView.setItems(list);
        loopView.setAdapter(new LoopAdapter() {
            @Override
            public int getCount() {
                return list.size();
            }

            @Override
            public String getDescription(int position) {
                return list.get(position);
            }

            @Override
            public Object getItem(int position) {
                return list.get(position);
            }
        });

        //设置初始位置
        loopView.setInitPosition(0);

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loopView.setCurrentPosition(0);
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ScrollViewActivity.class);
                startActivity(intent);
            }
        });
        findViewById(R.id.button3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DialogActivity.class);
                startActivity(intent);
            }
        });
    }

}
