package mx.com.adquira.tcmp;

import java.io.IOException;
import java.io.InputStream;

import mx.com.adquira.cv.util.FontDefine;
import mx.com.adquira.cv.dto.PaymentRequestDto;
import mx.com.adquira.cv.helperobjects.Constants;
import mx.com.adquira.cv.helperobjects.TCMPaymentEMVResponse;
import mx.com.adquira.cv.helperobjects.TCMPrintExtra;
import mx.com.adquira.cv.helperobjects.TCMResponse;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

public class TcmPay extends Activity implements AdapterView.OnItemSelectedListener {
	
	public static final String PAYMENT_REQUEST_DTO = "mx.com.adquira.cv.dto.PaymentRequestDto";
	
	private EditText txtOrderId;
	private EditText txtConcepto;
	private EditText txtMonto;
	private Spinner payCategory;
	private TextView payResult;
	private CheckBox checkEMV;
	private TextView btnPrintTest;
	private ProgressBar mProgress;
	
	private String token = "";
	private Intent fromIntent;
	
	private int[] payCategories = {773,120};
	
	private String monto;
	private String concepto;
	private String orderId;
	private int payCat;
	private boolean forceReconnect = false;
	private static long lStartTime;
	private static long lEndTime;
	
	private IntentFilter filter = new IntentFilter("mx.com.adquira.cv.tcmpintents.EMV_PAYMENT_ACTION");
	private ResponseReceiver receiver = new ResponseReceiver();


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		fromIntent = getIntent();
		
		setContentView(R.layout.activity_tcm_pay);
		txtOrderId = (EditText)findViewById(R.id.editPayReference);
		txtConcepto = (EditText)findViewById(R.id.editPayConcept);
		txtMonto = (EditText)findViewById(R.id.editPayAmount);
		payCategory = (Spinner) findViewById(R.id.payCategory);
		checkEMV = (CheckBox) findViewById(R.id.checkEMV);
		payResult = (TextView)findViewById(R.id.textPayResult);
		btnPrintTest = (TextView)findViewById(R.id.printTest);
		mProgress = (ProgressBar) findViewById(R.id.paymentProgressBar);
		
		btnPrintTest.setVisibility(View.INVISIBLE);
		mProgress.setVisibility(4);
		
		txtOrderId.setText("PB"+Math.rint(Math.random()*10000));
		txtConcepto.setText("TEST");
		txtMonto.setText("0.00");
		checkEMV.setChecked(true);
		
		if(fromIntent.hasExtra("user"))
			payResult.setText("Bienvenido "+fromIntent.getStringExtra("user"));
		
		if(fromIntent.hasExtra("token"))
			token = fromIntent.getStringExtra("token");
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.paymentCategory, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		payCategory.setAdapter(adapter);
		payCategory.setOnItemSelectedListener(this);
		
		final Button btnPagar = (Button) findViewById(R.id.btnPay);
		
		registerReceiver(receiver,filter);
		
		forceReconnect = true;
		
		btnPrintTest.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				printTest();
			}

		});
		
		btnPagar.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mProgress.setVisibility(0); // Visible
				monto = txtMonto.getText().toString();
				concepto = txtConcepto.getText().toString();
				orderId = txtOrderId.getText().toString();
				
				payResult.setText("Vas a pagar "+monto+", ref. "+orderId+
						", un "+concepto+" de cat."+payCat);
				
				pago(Float.parseFloat(monto),orderId, concepto,""+payCat);
			}
		});
	}
	
	private void pago(float monto, String orderId, String concepto, String category){
		
		Intent myIntent = new Intent();		
		lStartTime = System.currentTimeMillis();
		if(checkEMV.isChecked()) {
			myIntent.setComponent(new ComponentName("mx.com.adquira.cv.tcmpintents","mx.com.adquira.cv.tcmpintents.TCMEMVPaymentService")); 
		} else {
			myIntent.setComponent(new ComponentName("mx.com.adquira.cv.tcmpintents","mx.com.adquira.cv.tcmpintents.TCMSwipePaymentService")); 
		}
		myIntent.addCategory("android.intent.category.LAUNCHER");		
		myIntent.putExtra(Constants.PAYMENT_REQUEST_DTO, new PaymentRequestDto(token, orderId, concepto, monto, category));
		myIntent.putExtra("FORCE_RECONNECT",forceReconnect);
		forceReconnect = false;
		TcmPay.this.startService(myIntent);		
	}
	
	// Broadcast receiver for receiving status updates from the IntentService
	private class ResponseReceiver extends BroadcastReceiver
	{
	    // Called when the BroadcastReceiver gets an Intent it's registered to receive
	    public void onReceive(Context context, Intent intent) {
	    	mProgress.setVisibility(8); //8=GONE - 4=INVISIBLE
	    	String approval = "";
    		String trxID = "";
    		lEndTime = System.currentTimeMillis();
    		long difference = lEndTime - lStartTime;
	    	if(intent.hasExtra(Constants.EMV_RESPONSE)){
	    		TCMPaymentEMVResponse resp = (TCMPaymentEMVResponse)intent.getSerializableExtra(Constants.EMV_RESPONSE);
	    		approval = resp.getAuthCode();
	    		trxID = resp.getTransactionId();
	    		payResult.setText("Aprobaci—n: "+approval+", ref. "+trxID+"\nTiempo: "+difference+"ms.");
	    	} else {
	    		if(intent.hasExtra(Constants.ERROR_RESPONSE)) {
	    			TCMResponse errorResponse = (TCMResponse)intent.getSerializableExtra(Constants.ERROR_RESPONSE);
	    			payResult.setText("ERROR: "+errorResponse.getResponse());
	    		} else {
	    			payResult.setText(intent.getStringExtra("BT_STATUS_RESPONSE"));
	    		}
	    	}
	    		
			/*if(token!=null){
				Intent myIntent = new Intent(TcmLogin.this, TcmPay.class);
				myIntent.putExtra("token", token);
				myIntent.putExtra("user", username);
				TcmLogin.this.startActivity(myIntent); 
			} else {
				txtMessage.setText("USUARIO/PWD INCORRECTOS");
			} */
	    }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.tcm_pay, menu);
		return true;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
		payCat = payCategories[position];
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		payCat = 0;
	}

	private void printTest() {
		Intent myIntent = new Intent();		
		myIntent.setComponent(new ComponentName("mx.com.adquira.cv.tcmpintents","mx.com.adquira.cv.tcmpintents.TCMPrintService"));    
		myIntent.addCategory("android.intent.category.LAUNCHER");	
		
		TCMPrintExtra print = new TCMPrintExtra();
		print.setPrintExtras("Prueba de impresion unica linea");
		myIntent.putExtra("tcm_print_extras", print );
		this.startService(myIntent);

		TCMPrintExtra[] printArray = new TCMPrintExtra[3];
		print = new TCMPrintExtra();
		print.setPrintExtras("- 1) Print multilinea -");
		printArray[0] = print;
		
		print = new TCMPrintExtra();
		print.setImage(true);
		
		InputStream openRawResource = getResources().openRawResource(R.drawable.pay_logo);
		try {
			byte[] data = new byte[openRawResource.available()];
			openRawResource.read(data);
			Log.d("TcmPay", "pay logo size: " + data.length);
			print.setImage_resource(data);
		} catch (IOException e) {
			e.printStackTrace();
			Log.e("TcmPay", "", e);
		}
		printArray[1] = print;
		
		print = new TCMPrintExtra();
		print.setPrintExtras("- 3) ultima...... -");
		print.setCharPrintSize(FontDefine.FONT_48PX);
		print.setPrintJutification(FontDefine.Align_CENTER);
		printArray[2] = print;
		
		myIntent.putExtra("tcm_print_extras", printArray );
		this.startService(myIntent);
		
		
	}
	
	@Override
	protected void onDestroy()
	{
	    unregisterReceiver(receiver);
	    super.onDestroy();
	}
}
