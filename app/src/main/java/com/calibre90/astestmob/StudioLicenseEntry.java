package com.calibre90.astestmob;

import android.app.Activity;

/** Shared entry point for the existing Studio activation dialog. */
public final class StudioLicenseEntry {
  private StudioLicenseEntry() {}
  public static void requireActivation(Activity activity, Runnable onActivated) {
    LicenseGate.enforce(activity, onActivated);
  }
}
