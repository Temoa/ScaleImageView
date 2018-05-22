package me.temoa.scaleimageview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ScaleImageView iv = findViewById(R.id.long_iv);
        iv.setImageDrawable(getResources().getDrawable(R.drawable.test));
    }
}
