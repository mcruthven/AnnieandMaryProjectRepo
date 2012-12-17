/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.multidrawbluetooth;

import java.io.ByteArrayOutputStream;

import com.example.multidrawbluetooth.FingerPaint.MyView;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class BluetoothDraw extends GraphicsActivity implements
		ColorPickerDialog.OnColorChangedListener {
	private static final String TAG = "BluetoothChat";
	private static final boolean D = true;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// Intent request codes
	private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
	private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
	private static final int REQUEST_ENABLE_BT = 3;
	public static final int M_UP = 0;
	public static final int M_START = 1;
	public static final int M_T = 2;
	public final int EMBOSSED=1;
	public final int NOT_EMBOSSED=0;
	public final int BLURRED=1;
	public final int NOT_BLURRED=0;
	MyView v;
	// Name of the connected device
	private String mConnectedDeviceName = null;
	// Array adapter for the conversation thread
	private ArrayAdapter<String> mConversationArrayAdapter;
	// String buffer for outgoing messages
	Intent outgoingIntent;

	// Local Bluetooth adapter
	private BluetoothAdapter mBluetoothAdapter = null;
	// Member object for the chat services
	private BluetoothChatService mChatService = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		v = new MyView(this);
		setContentView(v);
		// setContentView(R.layout.activity_main);
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(0xFFFF0000);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(12);

		mEmboss = new EmbossMaskFilter(new float[] { 1, 1, 1 }, 0.4f, 6, 3.5f);

		mBlur = new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL);
		mPaint2 = new Paint();
		mPaint2.setAntiAlias(true);
		mPaint2.setDither(true);
		mPaint2.setColor(0xFFFF0000);
		mPaint2.setStyle(Paint.Style.STROKE);
		mPaint2.setStrokeJoin(Paint.Join.ROUND);
		mPaint2.setStrokeCap(Paint.Cap.ROUND);
		mPaint2.setStrokeWidth(12);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// If the adapter is null, then Bluetooth is not supported
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available",
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		if (D)
			Log.e(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		// setupChat() will then be called during onActivityResult
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			// Otherwise, setup the chat session
		} else {
			if (mChatService == null)
				setupChat();
		}
	}

	private void setupChat() {
		Log.d(TAG, "setupChat()");


		// Initialize the BluetoothChatService to perform bluetooth connections
		mChatService = new BluetoothChatService(this, mHandler);
		outgoingIntent = new Intent();
	}

	private Paint mPaint;
	private MaskFilter mEmboss;
	private MaskFilter mBlur;

	private Paint mPaint2;
	private void sendMessage(byte[] send) {
		// Check that we're actually connected before trying anything
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		} else {

			// Check that there's actually something to send

			// Get the message bytmees and tell the BluetoothChatService to
			// write
			mChatService.write(send);
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		if (D)
			Log.e(TAG, "- ON PAUSE -");
	}

	@Override
	public void onStop() {
		super.onStop();
		if (D)
			Log.e(TAG, "-- ON STOP --");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Stop the Bluetooth chat services
		if (mChatService != null)
			mChatService.stop();
		if (D)
			Log.e(TAG, "--- ON DESTROY ---");
	}

	private void ensureDiscoverable() {
		if (D)
			Log.d(TAG, "ensure discoverable");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	private final void setStatus(int resId) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(resId);
	}

	private final void setStatus(CharSequence subTitle) {
		final ActionBar actionBar = getActionBar();
		actionBar.setSubtitle(subTitle);
	}

	// The Handler that gets information back from the BluetoothChatService
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				if (D)
					Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					setStatus(getString(R.string.title_connected_to,
							mConnectedDeviceName));
					// mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					setStatus(R.string.title_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					setStatus(R.string.title_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:

				// construct a string from the buffer
				// String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				String readMessage = new String(readBuf, 0, msg.arg1);
				v.recieve(readMessage);
				// int z;
				break;
			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (D)
			Log.d(TAG, "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE_SECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, true);
			}
			break;
		case REQUEST_CONNECT_DEVICE_INSECURE:
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				connectDevice(data, false);
			}
			break;
		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {
				// Bluetooth is now enabled, so set up a chat session
				setupChat();
			} else {
				// User did not enable Bluetooth or an error occurred
				Log.d(TAG, "BT not enabled");
				// Toast.makeText(this, R.string.bt_not_enabled_leaving,
				// Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	private void connectDevice(Intent data, boolean secure) {
		// Get the device MAC address
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);
		// Get the BluetoothDevice object
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		// Attempt to connect to the device
		mChatService.connect(device, secure);
	}

	/*
	 * @Override public boolean onCreateOptionsMenu(Menu menu) { MenuInflater
	 * inflater = getMenuInflater(); inflater.inflate(R.menu.option_menu, menu);
	 * return true; }
	 */
	private static final int COLOR_MENU_ID = Menu.FIRST;
	private static final int EMBOSS_MENU_ID = Menu.FIRST + 1;
	private static final int BLUR_MENU_ID = Menu.FIRST + 2;
	private static final int ERASE_MENU_ID = Menu.FIRST + 3;
	private static final int SRCATOP_MENU_ID = Menu.FIRST + 4;
	private static final int CLEAR_MENU_ID = Menu.FIRST + 5;
	
	private static final int M_COL = 3;
	private static final int M_EMB = 4;
	private static final int M_BLUR = 5;
	private static final int M_ERASE = 6;
	private static final int M_SRC = 7;
	private static final int M_CLEAR = 8;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.option_menu, menu);
		menu.add(0, COLOR_MENU_ID, 0, "Color").setShortcut('3', 'c');
		menu.add(0, EMBOSS_MENU_ID, 0, "Emboss").setShortcut('4', 's');
		menu.add(0, BLUR_MENU_ID, 0, "Blur").setShortcut('5', 'z');
		menu.add(0, ERASE_MENU_ID, 0, "Erase").setShortcut('5', 'z');
		menu.add(0, SRCATOP_MENU_ID, 0, "SrcATop").setShortcut('5', 'z');
		menu.add(0, CLEAR_MENU_ID, 0, "CLEAR").setShortcut('5', 'z');

		/****
		 * Is this the mechanism to extend with filter effects? Intent intent =
		 * new Intent(null, getIntent().getData());
		 * intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
		 * menu.addIntentOptions( Menu.ALTERNATIVE, 0, new ComponentName(this,
		 * NotesList.class), null, intent, 0, null);
		 *****/
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		mPaint.setXfermode(null);
		mPaint.setAlpha(0xFF);
		Intent serverIntent = null;
		switch (item.getItemId()) {
		case COLOR_MENU_ID:
			new ColorPickerDialog(this, this, mPaint.getColor()).show();
			return true;
		case EMBOSS_MENU_ID:
			if (mPaint.getMaskFilter() != mEmboss) {
				mPaint.setMaskFilter(mEmboss);
				v.send(0,0,M_EMB,EMBOSSED);
			} else {
				mPaint.setMaskFilter(null);
				v.send(0,0,M_EMB,NOT_EMBOSSED);
			}
			
			return true;
		case BLUR_MENU_ID:
			if (mPaint.getMaskFilter() != mBlur) {
				mPaint.setMaskFilter(mBlur);
				v.send(0,0,M_BLUR,BLURRED);
			} else {
				mPaint.setMaskFilter(null);
				v.send(0,0,M_BLUR,NOT_BLURRED);
			}
			
			return true;
		case ERASE_MENU_ID:
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			v.send(0,0,M_ERASE,0);
			return true;
		case SRCATOP_MENU_ID:
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
			mPaint.setAlpha(0x80);
			v.send(0,0,M_SRC,0);
			return true;
		case CLEAR_MENU_ID:
			v.clear();
			return true;
		case R.id.secure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
			return true;
		case R.id.insecure_connect_scan:
			// Launch the DeviceListActivity to see devices and do scan
			serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent,
					REQUEST_CONNECT_DEVICE_INSECURE);
			return true;
		case R.id.discoverable:
			// Ensure this device is discoverable by others
			ensureDiscoverable();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/*
	 * @Override public boolean onOptionsItemSelected(MenuItem item) { Intent
	 * serverIntent = null; switch (item.getItemId()) { case
	 * R.id.secure_connect_scan: // Launch the DeviceListActivity to see devices
	 * and do scan serverIntent = new Intent(this, DeviceListActivity.class);
	 * startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
	 * return true; case R.id.insecure_connect_scan: // Launch the
	 * DeviceListActivity to see devices and do scan serverIntent = new
	 * Intent(this, DeviceListActivity.class);
	 * startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
	 * return true; case R.id.discoverable: // Ensure this device is
	 * discoverable by others ensureDiscoverable(); return true; } return false;
	 * }
	 */
	public void colorChanged(int color) {
		mPaint.setColor(color);
		v.send(0,0,M_COL,color);
	}

	public class MyView extends View {

		private static final float MINP = 0.25f;
		private static final float MAXP = 0.75f;
		int width;
		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Path mPath2;
		private Paint mBitmapPaint;
		int height;

		public MyView(Context c) {
			super(c);
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			width = size.x;
			height = size.y;
			mBitmap = Bitmap.createBitmap(width, height,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mPath = new Path();
			mPath2 = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		}
		protected void clear(){
			Paint clearPaint = new Paint();
			clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			mCanvas.drawRect(0, 0, width, height, clearPaint); 
		}
		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(0xFFFFFFFF);
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			if (!mPath.isEmpty())
				canvas.drawPath(mPath, mPaint);
			if (!mPath2.isEmpty())
				canvas.drawPath(mPath2, mPaint2);
		}

		private float mX, mY, mY2, mX2;
		private static final float TOUCH_TOLERANCE = 4;

		private void send(float x, float y, int s, int c) {
			if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
				return;
			}

			String s1 = String.format("%f %f %d %d ", x, y, s, c);
			byte arr[] = s1.getBytes();
			mChatService.write(arr);
		}

		private void recieve(String strarr) {
			String s[] = strarr.split(" ");
			int c = Integer.parseInt(s[2]);
			float x = Float.parseFloat(s[0]);
			float y = Float.parseFloat(s[1]);
			switch (c) {
			case M_COL:
				mPaint2.setXfermode(null);
				mPaint2.setAlpha(0xFF);
				mPaint2.setColor(Integer.parseInt(s[3]));
				break;
			case M_START:
				mPath2.reset();
				mPath2.moveTo(x, y);
				mX2 = x;
				mY2 = y;
				invalidate();
				break;
			case M_UP:
				mPath2.lineTo(mX2, mY2);
				mCanvas.drawPath(mPath2, mPaint2);
				invalidate();
				break;
			case M_T:
				float mx = mX2;
				float my = mY2;
				mPath2.quadTo(mx, my, (x + mx) / 2, (y + my) / 2);
				mX2 = x;
				mY2 = y;
				invalidate();
				break;
			case M_EMB:
				mPaint2.setXfermode(null);
				mPaint2.setAlpha(0xFF);
				if (Integer.parseInt(s[3]) == EMBOSSED) {
					mPaint2.setMaskFilter(mEmboss);
				} else {
					mPaint2.setMaskFilter(null);
				}
				break;
			case M_BLUR:
				mPaint2.setXfermode(null);
				mPaint2.setAlpha(0xFF);
				if (Integer.parseInt(s[3]) == BLURRED) {
					mPaint2.setMaskFilter(mBlur);
				} else {
					mPaint2.setMaskFilter(null);
				}
				break;
			case M_ERASE:
				mPaint2.setAlpha(0xFF);
				mPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
				break;
			case M_SRC:
				mPaint2.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
				mPaint2.setAlpha(0x80);
				break;

			}
		}

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			send(x, y, M_START, 0);
			mY = y;
		}

		private void touch_move(float x, float y) {

			// commit the path to our offscreen
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				send(x, y, M_T, 0);
				mX = x;
				mY = y;

			}
		}

		private void touch_up() {
			mPath.lineTo(mX, mY);
			mCanvas.drawPath(mPath, mPaint);
			send(mX, mY, M_UP, 0);
	//		mPath.reset();
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();

			switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touch_start(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_MOVE:
				touch_move(x, y);
				invalidate();
				break;
			case MotionEvent.ACTION_UP:
				touch_up();
				invalidate();
				break;
			}
			return true;
		}
	}

}
