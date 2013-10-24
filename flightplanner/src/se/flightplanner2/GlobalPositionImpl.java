package se.flightplanner2;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Date;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.util.Xml.Encoding;
import se.flightplanner2.GlobalPosition.PositionIf;
import se.flightplanner2.GlobalPosition.PositionSubscriberIf;
import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;

public class GlobalPositionImpl implements PositionIf, LocationListener {
	
	private WeakHashMap<PositionSubscriberIf,Void> subs=new WeakHashMap<GlobalPosition.PositionSubscriberIf, Void>();
	
	private Location last_location;
	private BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
	private long when;
	private LocationManager locman;
	private Handler handler;

	private DatagramSocket sock;
	private Runnable gpsRunnable;

	private DatagramChannel channel;
	public GlobalPositionImpl(LocationManager locman)
	{
		this.locman=locman;
		locman.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0,this);
		last_location=bearingspeed.calcBearingSpeed(null);
		handler=new Handler();
		
		
	}
												   //   $GPGGA,032829.00,5921.6877,N,01757.4762,E,1,12,1,303.7,M,,,,*24
	static final Pattern gpsregex=Pattern.compile(".*\\$GPGGA,(\\d+)\\.(\\d*),(\\d+)\\.?(\\d*),([NS]),(\\d+)\\.?(\\d*),([EW]),[^,]*,[^,]*,[^,]*,(\\d+\\.?\\d*),M.*");
	private long lastUdpOverride=0; 
	private double lastGpsLat,lastGpsLon;
	@SuppressWarnings("deprecation")
	private void receiveGpsPacket() throws IOException {
		ByteBuffer buf=ByteBuffer.allocate(1500);
		for(;;)
		{
			try
			{
				if (channel.receive(buf)==null)
				{
					return;
				}
				//sock.receive(pack);			
			}
			catch(SocketTimeoutException e)
			{
				return;
			}
			//Log.w("udpgps","Received packet with size: "+pack.getLength()+" matching against: "+gpsregex.pattern());
			String s=new String(buf.array(),0,buf.remaining(),"ISO-8859-1").trim();
			
			Matcher matcher=gpsregex.matcher(s);
			//Log.w("udpgps","Got: "+s);
			if (matcher.find())
			{
				//Log.w("udpgps","MAtch!");
				String timehms=matcher.group(1);
				String timesecdec=matcher.group(2);
				String hour=timehms.substring(0,2);
				String min=timehms.substring(2,4);
				String sec=timehms.substring(4);
				String subsec="0";
				if (timesecdec.length()!=0)
					subsec="0."+timesecdec;
				//Log.i("udpgps","Parsed "+timehms+" + "+subsec+" as "+hour+"-"+min+"-"+sec+" + "+subsec);
				double timesec=Double.parseDouble(hour)*3600.0+
						Double.parseDouble(min)*60.0+
						Double.parseDouble(sec);
				
				String latdegmin=matcher.group(3);
				String latdeg=latdegmin.substring(0,latdegmin.length()-2);
				String latmin=latdegmin.substring(latdegmin.length()-2);
				String latmindec=matcher.group(4);
				if (latmindec.length()!=0)
					latmin+="."+latmindec;
				
				String ns=matcher.group(5);
				
				String londegmin=matcher.group(6);
				String londeg=londegmin.substring(0,londegmin.length()-2);
				String lonmin=londegmin.substring(londegmin.length()-2);
				String lonmindec=matcher.group(7);
				if (lonmindec.length()!=0)
					lonmin+="."+lonmindec;
				
				String ew=matcher.group(8);
				
				String alts=matcher.group(9);

				double lat=Double.parseDouble(latdeg)+Double.parseDouble(latmin)/60.0;
				double lon=Double.parseDouble(londeg)+Double.parseDouble(lonmin)/60.0;
				double alt=Double.parseDouble(alts);

				if (ew.startsWith("W"))
					lon=-lon;
				if (ns.startsWith("S"))
					lat=-lat;
				Location loc=new Location("gps");
				//, "alt": 30, "lon": 
				loc.setLatitude(lat);
				loc.setLongitude(lon);
				loc.setAltitude(alt/0.3048);
				Date d = new Date();
				d.setHours(Integer.parseInt(hour));
				d.setMinutes(Integer.parseInt(min));
				d.setSeconds((int)Math.floor(Double.parseDouble(sec)));
				long ms=d.getTime();
				ms=ms-(ms%1000);
				ms+=(long)(Double.parseDouble(subsec)*1000.0);
				
				loc.setTime(ms);
				loc.removeBearing();
				loc.removeSpeed();
				
				lastUdpOverride=SystemClock.elapsedRealtime();
				if (Math.abs(lastGpsLat-lat)>1e-9 ||
						Math.abs(lastGpsLon-lon)>1e-9)
					onLocationChangedImpl(loc);			
				lastGpsLat=lat;
				lastGpsLon=lon;
				//Log.w("udpgps","Found: "+lat+","+lon);
			}
			else
			{
				//Log.e("udpgps","No match");
			}
			
		}
			
		
	}
	private Runnable lost_gps=new Runnable()
	{
		@Override
		public void run() {
			for(PositionSubscriberIf pos:subs.keySet())
				pos.gps_disabled();			
		}		
	};
	public void stop()
	{
		if (channel!=null)
		{
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		handler.removeCallbacks(lost_gps);
		handler.removeCallbacks(debugRunnable);
		locman.removeUpdates(this);
	}
	public void onLocationChanged(Location loc) {		
		if (debugRunner) return;
		long overrideAge=SystemClock.elapsedRealtime()-lastUdpOverride;
		if (overrideAge>30000)
			onLocationChangedImpl(loc);
	}
	public void onLocationChangedImpl(Location loc) {
		Location location = bearingspeed.calcBearingSpeed(loc);
		last_location = location;
		//Log.i("fplan.sensor","Number of subscribers: "+subs.size());
		for(PositionSubscriberIf pos:subs.keySet())
			pos.gps_update(last_location);
		when=SystemClock.elapsedRealtime();
		handler.removeCallbacks(lost_gps);
		handler.postDelayed(lost_gps, 7500);
		
	}

	@Override
	public void onProviderDisabled(String provider) {
		if (debugRunner) return;
		for(PositionSubscriberIf pos:subs.keySet())
			pos.gps_disabled();
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status != LocationProvider.AVAILABLE)
			{
			for(PositionSubscriberIf pos:subs.keySet())
				pos.gps_disabled();			
			}
		

	}	
	@Override
	public Location getLastPosition() {
		return last_location;
	}

	@Override
	public long getLastPositionUpdate() {
		return when;
	}

	@Override
	public void registerSubscriber(PositionSubscriberIf ref) {
		subs.put(ref,null);
		
	}
	@Override
	public void unRegisterSubscriber(PositionSubscriberIf ref) {
		subs.remove(ref);
		
	}
	private Runnable debugRunnable;
	private boolean debugRunner=false;
	private Merc debugMerc;
	private float debugSpeed;
	private float debugHdgRad;
	private float debugTurn;
	private float debugElev;

	private boolean nmea_udp;
	
	@Override
	public void setDebugTurn(float f) {
		debugTurn=f;
		if (!debugRunner) enableDriving();
	}
	@Override
	public void setDebugSpeed(float f) {
		debugSpeed=f;
		if (!debugRunner) enableDriving();
	}
	
	public void enableDriving() 
	{
		if (!Config.debugMode()) return;
		if (debugRunner==true)
			return;
		handler.removeCallbacks(lost_gps);
		debugRunner=true;
		if (debugMerc==null)
		{
			debugMerc=Project.latlon2merc(new LatLon(
					last_location.getLatitude(),last_location.getLongitude()), 13);
		}
		debugRunnable=new Runnable() {
			BearingSpeedCalc bearingspeed=new BearingSpeedCalc();
			@Override	
			public void run() {
				//Log.i("fplan.sensor","Debugspeed: "+debugSpeed+" debugHdg: "+debugHdgRad+" debugTurn: "+debugTurn);
				
				
				float debugSpeedMerc=(float)(Project.approx_scale(debugMerc, 13, debugSpeed)/3600.0);
				
				debugMerc=new
					Merc(debugMerc.x+debugSpeedMerc*Math.cos(debugHdgRad+Math.PI/2.0),
							debugMerc.y+debugSpeedMerc*Math.sin(debugHdgRad+Math.PI/2.0));
				debugHdgRad+=debugTurn/(180.0/Math.PI);
				LatLon l=Project.merc2latlon(debugMerc, 13);
				Location loc=new Location("gps");
				//, "alt": 30, "lon": 
				loc.setLatitude(l.lat);
				loc.setLongitude(l.lon);
				loc.setAltitude(debugElev);
				Date d = new Date();
				loc.setTime(d.getTime());
				loc.removeBearing();
				loc.removeSpeed();
				
				onLocationChangedImpl(loc);									
				handler.postDelayed(debugRunnable, 1000);
			}
		};
		handler.postDelayed(debugRunnable, 1000);		
	}
	@Override
	public void debugElev(float i) {
		debugElev+=i;
	}
	public void enableUdpNmea(boolean enabled) {
		if (!nmea_udp && enabled)
		{
			nmea_udp=enabled;
			//DatagramChannel channel;
			Log.w("udpgps","Starting UDP GPS listener");
			try {
				//channel = DatagramChannel.open();
		        //DatagramSocket socket = channel.socket()
				if (channel!=null)
				{
					try {
						channel.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					channel=null;
				}
			
				channel = DatagramChannel.open();
				channel.socket().setReuseAddress(true);
				channel.socket().bind(new InetSocketAddress(9877));
				channel.configureBlocking(false);
				
				//sock=new DatagramSocket(9877);
				//sock.setSoTimeout(1);	
				//sock.
				//sock.setReuseAddress(true);
			} catch (IOException e) {
				e.printStackTrace();
				Log.e("udpgps","Could not start GPS listener");
			}								
			
			if (gpsRunnable!=null)
				handler.removeCallbacks(gpsRunnable);

			gpsRunnable=new Runnable() {
				@Override	
				public void run() {
					if (!nmea_udp)
					{
						if (channel!=null)
						{
							Log.i("udpgps","Closing UDP channel");
							try {
								channel.close();
							} catch (IOException e) {
								e.printStackTrace();
							}
							channel=null;
						}
						return;
					}
					try
					{
						//Log.w("udpgps","About to receive");
						receiveGpsPacket();
		
						//Log.w("udpgps","Receive done");
						handler.postDelayed(gpsRunnable, 100);
					}
					catch(Throwable e)
					{
						Log.e("udpgps","Error");
						e.printStackTrace();
						handler.postDelayed(gpsRunnable, 20000);	
					}
				}
			};
			handler.postDelayed(gpsRunnable, 1000);			
		}
		nmea_udp=enabled;
	}
	

}
