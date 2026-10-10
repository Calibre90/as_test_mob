package com.calibre90.astestmob;

import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;

/** Launches the original Studio admin dialog without the customer license/splash flow. */
public final class StandaloneAdminActivity extends MainActivity {
  @Override public void onCreate(Bundle saved) {
    // The original admin dialog operates on these same editor data structures.
    // Do not invoke MainActivity.onCreate: that would start the customer license gate.
    superOnCreate(saved);
    for (int i=0;i<4;i++) rows[i]=new ArrayList<>();
    features.addAll(StudioSettings.load(this));
    restoreCustomTabPositions();
    restoreSelectedModule();
    seedBuiltInRows();
    restoreAdminRows();
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(Color.rgb(44,44,44));
    Button open=new Button(this);
    open.setText("Администрирование интерфейса");
    open.setOnClickListener(v->showAdminTabs());
    root.addView(open);
    setContentView(root);
    root.post(()->showAdminTabs());
  }

  private void superOnCreate(Bundle state) {
    // Android Activity lifecycle, deliberately bypassing the customer MainActivity launcher.
    // Implemented in MainActivity through a dedicated protected initialization hook.
    initializeStandaloneAdmin(state);
  }
}
