package com.prembros.artooproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 *
 * Created by Prem $ on 9/12/2017.
 */

public class ResetTablesOnEndOfDay extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatabaseHolder db = new DatabaseHolder(context);
                db.open();
                db.resetTables();
                db.close();
            }
        });
    }
}