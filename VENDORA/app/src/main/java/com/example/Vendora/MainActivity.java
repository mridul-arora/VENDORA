package com.example.Vendora;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cab.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      Thread thread =new Thread()
      {
          @Override
          public void run()
          {
              try
              {
               sleep(4000);
              }

              catch(Exception e)
              {
                e.printStackTrace();
              }

              finally
              {
                  Intent WelcomeIntent = new Intent(MainActivity.this,WelcomeActivity.class);
                  startActivity(WelcomeIntent);
              }
          }

      };
      thread.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();


    }
}



