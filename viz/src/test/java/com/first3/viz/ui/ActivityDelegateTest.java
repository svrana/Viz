package com.first3.viz.ui;

import android.app.Activity;
import com.first3.viz.ui.ActivityDelegate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertTrue;

@Config(emulateSdk = 18, manifest = "./src/main/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class ActivityDelegateTest {
    @Test
    public void testActivityDelegate() throws Exception {
        //Activity activity = Robolectric.buildActivity(ActivityDelegate.class).create().get();
        //assertTrue(true);
    }
}
