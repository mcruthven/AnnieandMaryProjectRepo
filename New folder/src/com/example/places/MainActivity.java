package com.example.places;


import java.util.HashMap;

import com.example.hometownhappenings.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;

public class MainActivity extends Activity {
	ListView listView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		listView= (ListView) findViewById(R.id.listView1);
		String[] from = new String[] { "title", "description" ,"distance"};
	    int[] to = new int[] { R.id.image };
	    // Add some rows

	    map.put("title", "First title"); // This will be shown in R.id.title
	    map.put("description", "description 1"); // And this in R.id.description
	    map.put("distance", "3 mi"); // And this in R.id.description
	    fillMaps.add(map);

	    map = new HashMap<String, Object>();
	    map.put("title", "Second title");
	    map.put("description", "description 2");
	    map.put("distance", "2 mi"); 
	    fillMaps.add(map);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
}
