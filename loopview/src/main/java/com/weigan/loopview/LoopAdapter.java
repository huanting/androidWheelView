package com.weigan.loopview;

/**
 * Created by huanting on 2018/1/8.
 */

public interface LoopAdapter {
    int getCount();
    String getDescription(int position);
    Object getItem(int position);
}
