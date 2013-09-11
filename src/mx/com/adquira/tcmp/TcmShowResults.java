package mx.com.adquira.tcmp;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class TcmShowResults extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tcm_show_results);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tcm_show_results, menu);
		return true;
	}

}
