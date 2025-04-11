package com.example.akherapp;

import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import com.example.akherapp.utils.NotificationHelper;

public class BaseActivity extends AppCompatActivity {
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_notifications) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String userId = prefs.getString("id", null);
            if (userId != null) {
                NotificationHelper.showNotificationsDialog(this, "admin");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
