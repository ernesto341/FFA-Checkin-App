package org.mhs.checkin_ffa_1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;

class Event
{
	String type = "";
	int event_id = 0;
	Date date = null;
	int user_id = 0;
	String user = "";
	String phone_number = "";
	Location loc = null;
	boolean at_farm = false;

	float feed_qty = 0.f;
	float weight = 0.f;
	String medication = "";

	public void setFeedQty (final float f)
	{
		this.feed_qty = f;
	}

	public void setWeight (final float f)
	{
		this.weight = f;
	}

	public void setMedication (final String s)
	{
		this.medication = s;
	}

	public Event (String t, String u, int uid, String n, Location location, boolean farm)
	{
		this.type = t;
		this.date = new Date();
		this.user_id = uid;
		this.user = u;
		this.phone_number = n;
		this.loc = location;
		this.at_farm = farm;
		this.event_id = (int)(Math.random() * 100.0);
		this.event_id = this.generateEventId();
	}

	public String toString()
	{
		String data = new String();
		data = "Type: ";
		data += type;
		data += "\n";
		data += "User: ";
		data += user;
		data += "\n";
		data += "User ID: ";
		data += user_id;
		data += "\n";
		data += "Event ID: ";
		data += event_id;
		data += "\n";
		data += "Date: ";
		data += date.toString();
		data += "\n";
		data += "Phone Number: ";
		data += phone_number;
		data += "\n";
		data += "Location: ";
		if (loc != null)
		{
			data += loc.getLatitude();
			data += ", ";
			data += loc.getLongitude();
		}
		else
			data += "(null)";
		data += "\n";
		data += (at_farm == true ? "At Farm" : "NOT AT FARM");
		data += "\n";
		if (feed_qty > 0.f)
		{
			data += "Feed Quantity:";
			data += feed_qty;
			data += "\n";
		}
		if (weight > 0.f)
		{
			data += "Weight:";
			data += weight;
			data += "\n";
		}
		if (medication.length() > 0)
		{
			data += "Medication:";
			data += medication;
			data += "\n";
		}
		return data;
	}

	public byte[] getBytes() throws UnsupportedEncodingException
	{
		String data = this.toString();
		return data.getBytes("UTF-8");
	}

	public String toXml()
	{
		String data = new String();
		data += "<event>";
		data += "<type>";
		data += type;
		data += "</type>";
		data += "<event_id>";
		data += event_id;
		data += "</event_id>";
		data += "<date>";
		data += date.toString();
		data += "</date>";
		data += "<user_id>";
		data += user_id;
		data += "</user_id>";
		data += "<user>";
		data += user;
		data += "</user>";
		data += "<phone_number>";
		data += phone_number;
		data += "</phone_number>";
		data += "<lat>";
		data += (loc != null ? (loc.getLatitude()) : (data += "-999.999"));
		data += "</lat>";
		data += "<lon>";
		data += (loc != null ? (loc.getLongitude()) : (data += "-999.999"));
		data += "</lon>";
		data += "<at_farm>";
		data += (at_farm == true ? "true" : "false");
		data += "</at_farm>";
		if (feed_qty > 0.f)
		{
			data += "<feed_qty>";
			data += feed_qty;
			data += "</feed_qty>";
		}
		if (weight > 0.f)
		{
			data += "<weight>";
			data += weight;
			data += "</weight>";
		}
		if (medication.length() > 0)
		{
			data += "<medication>";
			data += medication;
			data += "</medication>";
		}
		data += "</event>";

		return data;
	}

	public byte[] toXmlBytes() throws UnsupportedEncodingException
	{
		String data = this.toXml();
		return data.getBytes("UTF-8");
	}

	private int generateEventId ()
	{
		String data = this.toString();
		int id = (int)(Math.random() * 10.0), event_len = data.length(); 
		for(int i = 0; i < event_len; i++)
		{
			id = ((int)((Math.random() * 10.) + 1) * (id + (((int)(data.charAt(i)) / 2) * 3) ));
		}

		return id;
	}
}



class LocationReceiver extends BroadcastReceiver
{
	protected static final int BASE64FLAGS = (Base64.URL_SAFE | Base64.NO_WRAP);
	protected static final String CONFIGFILENAME = "config.dat";
	protected static final String LOGFILENAME = "checkins.log";
	private static Location farm = null;
	private static final float farm_lat = 35.670f;
	private static final float farm_lon = -119.235f;
	private static String user = "";
	private static int user_id = 0;
	private FileWriter log = null;
	private FileReader config = null;
	private static final float farm_radius = 150.0f;
	private static Location last_loc;
	private static boolean inbound = false;
	
	@Override
	public void onReceive (Context context, Intent intent)
	{
		if (loadConfig(context) == false)
		{
			return;
		}
		farm = new Location(LocationManager.GPS_PROVIDER);
		farm.setLatitude(farm_lat);
		farm.setLongitude(farm_lon);
		last_loc = (Location)intent.getExtras().get(LocationManager.KEY_LOCATION_CHANGED);
		inbound = intent.getExtras().getBoolean(LocationManager.KEY_PROXIMITY_ENTERING);
		Event event = new Event((inbound ? (String)"Entering Farm" : (String)"Leaving Farm"), user, user_id, ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number(), last_loc, isAtFarm());
		postEvent(event);
		localLog(event, context);
		//NotificationManager nm = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		//PendingIntent pi = PendingIntent.getActivity(context, 0, null, 0);
		//Notification n = createNotification();
		//n.setLatestEventInfo(context, "Proximity Alert", (inbound ? "You've Arrived at the Farm" : "You've Left the Farm"), pi);
		//nm.notify(0, n);
	}

	private boolean localLog(Event event, Context context)
	{
		try {
			log = new FileWriter (context.getFilesDir() + "/" + LOGFILENAME, true);
		} catch (FileNotFoundException e) {
			try {
				new File(context.getFilesDir(), CONFIGFILENAME);
			} catch (NullPointerException e7) {
				return false;
			}
		} catch (IOException e8) {
			return false;
		}
		try {
			log.append((Base64.encodeToString((event.toString() + (isAtFarm() ? "At the farm\n" : "NOT AT THE FARM\n")).getBytes(), BASE64FLAGS)) + "\n");
			log.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	public boolean postEvent (Event e)
	{
		String request = "http://www.cs.csubak.edu/~erichards/acceptEvent.php";
		String urlParameters = "?e=";
		URL url = null;
		//urlParameters += Base64.encodeToString((e.toString() + (isAtFarm() ? "At the farm\n" : "NOT AT THE FARM\n")).getBytes(), BASE64FLAGS);
		//try {
		//	urlParameters += Base64.encodeToString((e.toXmlBytes()), BASE64FLAGS);
		//} catch (UnsupportedEncodingException e3) {
		// TODO Auto-generated catch block
		//	e3.printStackTrace();
		//}
		urlParameters += Base64.encodeToString((e.toString()).getBytes(), BASE64FLAGS);
		request += urlParameters;
		try {
			url = new URL(request);
		} catch (MalformedURLException e2) {
			return false;
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
		} catch (IOException e2) {
			return false;
		}
		conn.setDoOutput( true );
		conn.setDoInput( true );
		conn.setInstanceFollowRedirects( true );
		conn.setReadTimeout(5000);
		conn.setConnectTimeout(5000);
		conn.setUseCaches( false );
		try {
			conn.setRequestMethod( "POST" );
		} catch (ProtocolException e1) {
			return false;
		}
		try {
			conn.connect();
		} catch (MalformedURLException f) {
			return false;
		} catch (IOException e1) {
			return false;
		}
		try {
			conn.getResponseCode();
		} catch (IOException e1) {
		}

		conn.disconnect();
		return true;
	}

	private boolean loadConfig (Context context)
	{
		byte[] t = null;
		try {
			config = new FileReader(context.getFilesDir().getAbsolutePath() + "/" + CONFIGFILENAME);
		} catch (FileNotFoundException e) {
			return false;
		}
		String data = null;
		String raw = new String();
		BufferedReader textReader = new BufferedReader(config);
		try {
			if (!textReader.ready())
			{
				return false;
			}
		} catch (IOException e1) {
			return false;
		}
		try {
			raw = textReader.readLine();
		} catch (IOException e) {
			return false;
		}
		if (raw == null)
		{
			return false;
		}
		try {
			textReader.close();
		} catch (IOException e) {
			return false;
		}
		try
		{
			t = (Base64.decode(raw, BASE64FLAGS));
			data = new String(t, "UTF-8");
		}
		catch (IllegalArgumentException e)
		{
			return false;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
		user = data;
		setUserId();
		return true;
	}

	private int setUserId ()
	{
		int id = 3, user_len = user.length(); 
		for(int i = 0; i < user_len; i++)
		{
			id += (5) * (int)(user.charAt(i));
		}
		id = 7 * id + user_len;

		user_id = id;
		return id;
	}

	private boolean isAtFarm()
	{
		if (last_loc == null)
		{
			return false;
		}
		float acc = last_loc.getAccuracy();
		if (Math.abs((last_loc.distanceTo(farm) - acc)) <= farm_radius)
		{
			return true;
		}

		return false;
	}

	private Notification createNotification()
	{
		Notification n = new Notification();
		n.icon = R.drawable.mhs_ffa;
		n.when = System.currentTimeMillis();
		n.flags |= Notification.DEFAULT_ALL;
		n.defaults |= Notification.DEFAULT_ALL;
		n.ledARGB = Color.WHITE;
		n.ledOnMS = 1500;
		n.ledOffMS = 1500;
		
		return n;
	}
}



public class MainActivity extends Activity {
	private static final String PROX_ALERT = "org.mhs.checkin_ffa_1.PROXIMITY_ALERT";
	private LocationReceiver proxReceiver = null;
	protected static final int BASE64FLAGS = (Base64.URL_SAFE | Base64.NO_WRAP);
	protected static final String CONFIGFILENAME = "config.dat";
	protected static final String LOGFILENAME = "checkins.log";
	private static final float LOCATION_REFRESH_DISTANCE = 1000.f;
	private Location last_loc;
	private LocationManager loc_man;
	protected String user = "";
	protected int user_id = 0;
	private FileWriter log = null;
	private FileReader config = null;
	private static final float farm_radius = 150.0f;
	private static Location farm = null;
	private static final float farm_lat = 35.670f;
	private static final float farm_lon = -119.235f;
	protected static final long MAX_UPDATE_TIME = 1800000;
	private static final int PROXIMITY_ALERT = 7743;
	public static PendingIntent prox_alert = null;
	private static boolean inbound = true;
	private static boolean at_farm = false;
	private static final float UNSET = -99.99f;
	private static String line1_number = null;
	private float qty = UNSET;
	private String med = "";
	boolean repeat = false;
	Intent intent = null;

	private final LocationListener gps_loc_list = new LocationListener() {
		@Override
		public void onLocationChanged(final Location location) {
			if (location == null)
			{
				return;
			}
			if (last_loc == null)
			{
				setCurrentLocation(location);
				return;
			}
			if (location.getTime() > last_loc.getTime()) /* elapsedRealTimeNanos requires a later version of android */
			{
				setCurrentLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if (provider == LocationManager.GPS_PROVIDER)
			{
				loc_man.removeUpdates(gps_loc_list);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (provider == LocationManager.GPS_PROVIDER)
			{
				loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, gps_loc_list);
			}
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
		}
	};

	private final LocationListener pas_loc_list = new LocationListener() {
		@Override
		public void onLocationChanged(final Location location) {
			if (location == null)
			{
				return;
			}
			if (last_loc == null)
			{
				setCurrentLocation(location);
				return;
			}
			if (location.getTime() > last_loc.getTime()) /* elapsedRealTimeNanos requires a later version of android */
			{
				setCurrentLocation(location);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if (provider == LocationManager.PASSIVE_PROVIDER)
			{
				loc_man.removeUpdates(pas_loc_list);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (provider == LocationManager.PASSIVE_PROVIDER)
			{
				loc_man.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, pas_loc_list);
			}
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
		}
	};

	private final LocationListener net_loc_list = new LocationListener() {
		@Override
		public void onLocationChanged(final Location location) {
			if (location == null)
			{
				return;
			}
			if (last_loc == null)
			{
				setCurrentLocation(location);
				return;
			}
			if (location.getTime() > last_loc.getTime()) /* elapsedRealTimeNanos requires a later version of android */
			{
				if (location.getAccuracy() /* better */ <= last_loc.getAccuracy())
				{
					setCurrentLocation(location);
				}
				else if (location.getAccuracy() /* worse */ > last_loc.getAccuracy() && !(loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER)))
				{
					setCurrentLocation(location);
				}
				else if (Math.abs(last_loc.getTime() - location.getTime()) > MAX_UPDATE_TIME) /* about an hour */
				{
					setCurrentLocation(location);
				}
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if (provider == LocationManager.NETWORK_PROVIDER)
			{
				loc_man.removeUpdates(net_loc_list);
			}
		}

		@Override
		public void onProviderEnabled(String provider) {
			if (provider == LocationManager.NETWORK_PROVIDER)
			{
				loc_man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, net_loc_list);
			}
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO Auto-generated method stub
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		line1_number = ((TelephonyManager)MainActivity.this.getBaseContext().getSystemService(Context.TELEPHONY_SERVICE)).getLine1Number();
		setContentView(R.layout.activity_main);
		if (prox_alert != null)
		{
			prox_alert = null;
		}
		StrictMode.ThreadPolicy policy = null;
		policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
		StrictMode.setThreadPolicy(policy);
		farm = new Location(LocationManager.GPS_PROVIDER);
		farm.setLatitude(farm_lat);
		farm.setLongitude(farm_lon);
		loc_man = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, gps_loc_list);
			if (last_loc == null)
			{
				last_loc = loc_man.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			}
		}
		else if (loc_man.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, net_loc_list);
			if (last_loc == null)
			{
				last_loc = loc_man.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}
		else if (loc_man.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, pas_loc_list);
			if (last_loc == null)
			{
				last_loc = loc_man.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			}
		}
		if ((loadConfig()) == false)
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("What's your name?");
			alert.setMessage("Enter your FULL First and Last name (like 'James Gum')");

			final EditText input = new EditText(this);
			input.setFocusable(true);
			alert.setView(input);
			alert.setCancelable(true);
			alert.setOnKeyListener(new DialogInterface.OnKeyListener() {

				@Override
				public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
					switch(keyCode)
					{
					case KeyEvent.KEYCODE_ENTER:
						if (event.getAction() == KeyEvent.ACTION_UP)
						{
							/* TODO
							 * Accept current text
							 */
							return true;
						}
						else if (event.getAction() == KeyEvent.ACTION_DOWN)
						{
							return true;
						}
						else if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
						{
							return true;
						}
						break;
					}
					return false;
				}
			});
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					user = new String();
					user = input.getText().toString();

					new File(MainActivity.this.getFilesDir(), CONFIGFILENAME);
					try {
						setUserId();
						PrintWriter new_config = new PrintWriter(MainActivity.this.getFilesDir().getAbsolutePath() + "/" + CONFIGFILENAME);
						new_config.println((Base64.encodeToString(user.getBytes("UTF-8"), BASE64FLAGS)));
						new_config.close();
					} catch (FileNotFoundException x) {
						System.exit(-1);
					} catch (UnsupportedEncodingException e1) {
						System.exit(-1);
					}
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					System.exit(-1);
				}
			});
			input.bringToFront();
			alert.show();
		}
		//String geo = "geo:"+farm_lat+","+farm_lon;
		//intent = new Intent(this, LocationReceiver.class);
		//intent = new Intent(PROX_ALERT, Uri.parse(geo));
		//prox_alert = MainActivity.this.createPendingResult(PROXIMITY_ALERT, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		//prox_alert = PendingIntent.getBroadcast(getApplicationContext(), PROXIMITY_ALERT, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		//loc_man.addProximityAlert(farm_lat, farm_lon, farm_radius, -1, prox_alert);
		//proxReceiver = new LocationReceiver();
		//IntentFilter ifilter = new IntentFilter(PROX_ALERT);
		//ifilter.addDataScheme("geo");
		//registerReceiver(proxReceiver, ifilter);
		//loc_man.requestSingleUpdate(LocationManager.GPS_PROVIDER, prox_alert);
		//loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, prox_alert);
		/* TODO 
		 * if at farm and last event was not at the farm, post entering farm
		 */
		if (isAtFarm())
		{
			
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void resetUpdates()
	{
		if (last_loc == null)
		{
			return;
		}
		if (!at_farm)
		{
			if (loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				loc_man.removeUpdates(gps_loc_list);
				loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, (Math.abs((last_loc.distanceTo(farm)) - (last_loc.getAccuracy()))), gps_loc_list);
			}
			if (loc_man.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			{
				loc_man.removeUpdates(net_loc_list);
				loc_man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, (Math.abs((last_loc.distanceTo(farm)) - (last_loc.getAccuracy()))), net_loc_list);
			}
			if (loc_man.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
			{
				loc_man.removeUpdates(pas_loc_list);
				loc_man.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MAX_UPDATE_TIME, (Math.abs((last_loc.distanceTo(farm)) - (last_loc.getAccuracy()))), pas_loc_list);
			}
		}
		else
		{
			if (loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				loc_man.removeUpdates(gps_loc_list);
				loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, (Math.abs((farm_radius) - (last_loc.distanceTo(farm)))), gps_loc_list);
			}
			if (loc_man.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			{
				loc_man.removeUpdates(net_loc_list);
				loc_man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, (Math.abs((farm_radius) - (last_loc.distanceTo(farm)))), net_loc_list);
			}
			if (loc_man.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
			{
				loc_man.removeUpdates(pas_loc_list);
				loc_man.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MAX_UPDATE_TIME, (Math.abs((farm_radius) - (last_loc.distanceTo(farm)))), pas_loc_list);
			}
		}
	}

	private void setCurrentLocation (Location l)
	{
		if (l == null)
		{
			return;
		}
		else if (last_loc == null)
		{
			inbound = true;
			last_loc = l;
			at_farm = isAtFarm();
			if (at_farm)
			{
				Event event = new Event("Entering Farm", user, user_id, line1_number, l, at_farm);
				postEvent(event);
				localLog(event);
			}
			resetUpdates();
			return;
		}
		if (Math.abs((last_loc.distanceTo(farm) - last_loc.getAccuracy())) >= Math.abs((l.distanceTo(farm) - l.getAccuracy())))
		{
			inbound = true;
		}
		else
		{
			inbound = false;
		}
		boolean tmp = isAtFarm();
		if (!at_farm && tmp)
		{
			at_farm = tmp;
			Event event = new Event("Entering Farm", user, user_id, line1_number, l, at_farm);
			postEvent(event);
			localLog(event);
		}
		else if (at_farm && !tmp)
		{
			at_farm = tmp;
			Event event = new Event("Leaving Farm", user, user_id, line1_number, l, at_farm);
			postEvent(event);
			localLog(event);
		}
		resetUpdates();
		last_loc = l;
	}

	private boolean isAtFarm(Location l)
	{
		if (l == null)
		{
			return false;
		}
		float acc = l.getAccuracy();
		if (Math.abs((l.distanceTo(farm) - acc)) <= farm_radius)
		{
			return true;
		}

		return false;
	}

	private boolean isAtFarm()
	{
		if (last_loc == null)
		{
			return false;
		}
		float acc = last_loc.getAccuracy();
		if (Math.abs((last_loc.distanceTo(farm) - acc)) <= farm_radius)
		{
			return true;
		}

		return false;
	}

	private boolean loadConfig ()
	{
		byte[] t = null;
		try {
			config = new FileReader(MainActivity.this.getFilesDir().getAbsolutePath() + "/" + CONFIGFILENAME);
		} catch (FileNotFoundException e) {
			return false;
		}
		String data = null;
		String raw = new String();
		BufferedReader textReader = new BufferedReader(config);
		try {
			if (!textReader.ready())
			{
				return false;
			}
		} catch (IOException e1) {
			return false;
		}
		try {
			raw = textReader.readLine();
		} catch (IOException e) {
			return false;
		}
		if (raw == null)
		{
			return false;
		}
		try {
			textReader.close();
		} catch (IOException e) {
			return false;
		}
		try
		{
			t = (Base64.decode(raw, BASE64FLAGS));
			data = new String(t, "UTF-8");
		}
		catch (IllegalArgumentException e)
		{
			return false;
		} catch (UnsupportedEncodingException e) {
			return false;
		}
		user = data;
		setUserId();
		return true;
	}

	public void onClick (final View v)
	{
		qty = UNSET;
		med = "";
		final String button_label = v.getContentDescription().toString();

		if (button_label.contains("Feed"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Details");
			alert.setMessage("How many pounds of feed?");

			final SeekBar slider = new SeekBar(alert.getContext());
			alert.setView(slider);
			alert.setCancelable(false);
			slider.setBackgroundResource(R.drawable.scale_black9);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					repeat = false;
					qty = ((float)(slider.getProgress()) / 10.f);
					if (qty < .2f)
					{
						repeat = true;
					}
					if (repeat == false)
					{
						Event event = new Event(button_label, user, user_id, line1_number, last_loc, at_farm);
						event.setFeedQty(qty);
						localLog(event);

						if (postEvent(event))
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.DKGRAY, Color.rgb(27, 178, 30));
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
						else
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.LTGRAY, Color.RED);
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
					}
				}
			});
			slider.bringToFront();
			alert.show();
		}

		else if (button_label.contains("Weigh"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Details");
			alert.setMessage("Enter the new weight");

			final EditText input = new EditText(this);
			input.setFocusable(true);
			alert.setView(input);
			alert.setCancelable(false);
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					repeat = false;
					try
					{
						qty = Float.parseFloat(input.getText().toString());
					}
					catch(NumberFormatException nfe)
					{
						printMessage("Invalid Input");
						repeat = true;
					}
					if (qty < 10.f)
					{
						repeat = true;
					}
					if (repeat == false)
					{
						Event event = new Event(button_label, user, user_id, line1_number, last_loc, at_farm);
						event.setWeight(qty);
						localLog(event);

						if (postEvent(event))
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.DKGRAY, Color.rgb(27, 178, 30));
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
						else
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.LTGRAY, Color.RED);
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
					}
				}
			});

			input.bringToFront();
			alert.show();
		}

		else if (button_label.contains("Medicate"))
		{
			repeat = false;

			AlertDialog.Builder alert2 = new AlertDialog.Builder(this);
			alert2.setTitle("Details");
			alert2.setMessage("Enter the Quantity and Type of Medication");

			final EditText input2 = new EditText(this);
			input2.setFocusable(true);
			alert2.setView(input2);
			alert2.setCancelable(false);
			alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					med = input2.getText().toString();
					if (med.length() < 1)
					{
						repeat = true;
					}

					if (repeat == false)
					{
						Event event = new Event(button_label, user, user_id, line1_number, last_loc, at_farm);
						event.setMedication(med);
						localLog(event);

						if (postEvent(event))
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.DKGRAY, Color.rgb(27, 178, 30));
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
						else
						{
							ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.LTGRAY, Color.RED);
							colorFade.setDuration(300);
							colorFade.setRepeatCount(2);
							colorFade.setRepeatMode(ObjectAnimator.REVERSE);
							ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
							textFade.setDuration(300);
							textFade.setRepeatCount(2);
							textFade.setRepeatMode(ObjectAnimator.REVERSE);
							colorFade.start();
							textFade.start();
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
							v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
						}
					}

				}
			});
			input2.bringToFront();
			alert2.show();
		}

		else
		{
			try {
				if (logEvent(button_label))
				{
					ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.DKGRAY, Color.rgb(27, 178, 30));
					colorFade.setDuration(300);
					colorFade.setRepeatCount(2);
					colorFade.setRepeatMode(ObjectAnimator.REVERSE);
					ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
					textFade.setDuration(300);
					textFade.setRepeatCount(2);
					textFade.setRepeatMode(ObjectAnimator.REVERSE);
					colorFade.start();
					textFade.start();
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				}
				else
				{
					ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.LTGRAY, Color.RED);
					colorFade.setDuration(300);
					colorFade.setRepeatCount(2);
					colorFade.setRepeatMode(ObjectAnimator.REVERSE);
					ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
					textFade.setDuration(300);
					textFade.setRepeatCount(2);
					textFade.setRepeatMode(ObjectAnimator.REVERSE);
					colorFade.start();
					textFade.start();
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				}
			} catch (FileNotFoundException e) {
				ObjectAnimator colorFade = ObjectAnimator.ofObject(v, "backgroundColor", new ArgbEvaluator(), Color.LTGRAY, Color.RED);
				colorFade.setDuration(300);
				colorFade.setRepeatCount(2);
				colorFade.setRepeatMode(ObjectAnimator.REVERSE);
				ObjectAnimator textFade = ObjectAnimator.ofObject(v, "textColor", new ArgbEvaluator(), Color.WHITE, Color.BLACK);
				textFade.setDuration(300);
				textFade.setRepeatCount(2);
				textFade.setRepeatMode(ObjectAnimator.REVERSE);
				colorFade.start();
				v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
				v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			}
		}

		med = "";
		qty = UNSET;
	}

	public void onProximityAlert(Bundle extras)
	{
		if (extras == null || !(extras.containsKey(LocationManager.KEY_PROXIMITY_ENTERING)))
		{
			return;
		}
		inbound = extras.getBoolean(LocationManager.KEY_PROXIMITY_ENTERING);
		Location tmp = ((loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER)) ? (new Location(LocationManager.GPS_PROVIDER)) : (loc_man.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ? (new Location(LocationManager.NETWORK_PROVIDER)) : (new Location(LocationManager.PASSIVE_PROVIDER))));
		tmp = loc_man.getLastKnownLocation(tmp.getProvider());
		Event event = new Event((inbound ? (String)"Entering Farm" : (String)"Leaving Farm"), user, user_id, line1_number, tmp, isAtFarm(tmp));
		postEvent(event);
		localLog(event);
	}

	public void onBackPressed()
	{
		//loc_man.removeUpdates(gps_loc_list);
		//loc_man.removeUpdates(net_loc_list);
		//loc_man.removeUpdates(pas_loc_list);
		super.onBackPressed();
		//loc_man.addProximityAlert(farm_lat, farm_lon, farm_radius, -1, prox_alert);
		if (loc_man.isProviderEnabled(LocationManager.GPS_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, gps_loc_list);
		}
		if (loc_man.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, net_loc_list);
		}
		if (loc_man.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
		{
			loc_man.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MAX_UPDATE_TIME, LOCATION_REFRESH_DISTANCE, pas_loc_list);
		}
	}

	public boolean logEvent (String type) throws FileNotFoundException
	{
		Event event = new Event(type, user, user_id, line1_number, last_loc, at_farm);
		if (!(postEvent(event)))
		{
			return false;
		}
		if (!(localLog(event)))
		{
			return false;
		}
		return true;
	}

	private boolean localLog(Event event)
	{
		try {
			log = new FileWriter (MainActivity.this.getFilesDir() + "/" + LOGFILENAME, true);
		} catch (FileNotFoundException e) {
			try {
				new File(MainActivity.this.getFilesDir(), CONFIGFILENAME);
			} catch (NullPointerException e7) {
				return false;
			}
		} catch (IOException e8) {
			return false;
		}
		try {
			log.append((Base64.encodeToString((event.toString() + (isAtFarm() ? "At the farm\n" : "NOT AT THE FARM\n")).getBytes(), BASE64FLAGS)) + "\n");
			log.close();
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	public boolean postEvent (Event e)
	{
		String request = "http://www.cs.csubak.edu/~erichards/acceptEvent.php";
		String urlParameters = "?e=";
		URL url = null;
		//urlParameters += Base64.encodeToString((e.toString() + (isAtFarm() ? "At the farm\n" : "NOT AT THE FARM\n")).getBytes(), BASE64FLAGS);
		//try {
		//	urlParameters += Base64.encodeToString((e.toXmlBytes()), BASE64FLAGS);
		//} catch (UnsupportedEncodingException e3) {
		// TODO Auto-generated catch block
		//	e3.printStackTrace();
		//}
		urlParameters += Base64.encodeToString((e.toString()).getBytes(), BASE64FLAGS);
		request += urlParameters;
		try {
			url = new URL(request);
		} catch (MalformedURLException e2) {
			return false;
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();
		} catch (IOException e2) {
			return false;
		}
		conn.setDoOutput( true );
		conn.setDoInput( true );
		conn.setInstanceFollowRedirects( true );
		conn.setReadTimeout(5000);
		conn.setConnectTimeout(5000);
		conn.setUseCaches( false );
		try {
			conn.setRequestMethod( "POST" );
		} catch (ProtocolException e1) {
			return false;
		}
		try {
			conn.connect();
		} catch (MalformedURLException f) {
			return false;
		} catch (IOException e1) {
			return false;
		}
		try {
			conn.getResponseCode();
		} catch (IOException e1) {
		}

		conn.disconnect();
		return true;
	}

	private int setUserId ()
	{
		int id = 3, user_len = user.length(); 
		for(int i = 0; i < user_len; i++)
		{
			id += (5) * (int)(user.charAt(i));
		}
		id = 7 * id + user_len;

		user_id = id;
		return id;
	}

	private void printMessage (String s)
	{
		AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
		alertDialog.setTitle("Info");
		alertDialog.setMessage(s);
		alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		alertDialog.show();
	}
}
