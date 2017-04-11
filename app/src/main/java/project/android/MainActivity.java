package project.android;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import project.android.security.EKEProvider;
import project.android.sensor.orientation.OrientationProvider;
import project.edu.android.remouse.R;

/**
 * @author Sayantan Majumdar
 */

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static ArrayList<Fragment> sFragmentList = new ArrayList<>();
    private static final int REQUEST_RW_STORAGE = 2909;

    public static File sRemouseDir = null;
    public static byte[] sPublicKey;
    public static final String DEVICE_NAME = Build.MANUFACTURER + " " + Build.MODEL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        makeRemouseDirectory();

        new Thread(new Runnable() {
            @Override
            public void run() {
                sPublicKey = new EKEProvider().getBase64EncodedPubKey();
            }
        }).start();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
            @Override
            public void onDrawerOpened(View view) {
                super.onDrawerOpened(view);
                view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        sFragmentList.add(new ConnectionFragment());
        sFragmentList.add(new AboutFragment());

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public void onPause() {
        super.onPause();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stopService(new Intent(this, NetworkService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        String title = getString(R.string.app_name);
        if (id == R.id.nav_mouse) {
            // Handle the 3D mouse action
            if(OrientationProvider.checkGyro(this)) {
                fragment = new MouseFragment();
                title = "Remote Mouse";
            } else {
                Toast.makeText(this, "Gyrosope not present!", Toast.LENGTH_LONG).show();
            }

        } else if(id == R.id.nav_touchpad) {
            // 2D Mouse
            fragment = new TouchpadFragment();
            title = "Remote Mouse";

        } else if (id == R.id.nav_keyboard) {
            // Handle the keyboard action
            fragment = new KeyboardFragment();
            title="Remote Keyboard";

        } else if (id == R.id.nav_connect) {
            // connection module
            fragment = sFragmentList.get(0);
            title="Connect to PC";

        } else if (id == R.id.nav_about) {
            // App info
            fragment = sFragmentList.get(1);
            title="About app";
        }
        if(fragment != null)    displayFragment(fragment, title);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public static Fragment getConnectionFragment() { return sFragmentList.get(0); }

    private void displayFragment(Fragment fragment, String title) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, fragment)
                    .commit();
        }
        // set the toolbar title
        if (getSupportActionBar() != null)  getSupportActionBar().setTitle(title);
    }

    private void makeRemouseDirectory() {
        sRemouseDir = getDir("Remouse", Context.MODE_PRIVATE);
        if(!sRemouseDir.exists())   sRemouseDir.mkdir();
    }
}