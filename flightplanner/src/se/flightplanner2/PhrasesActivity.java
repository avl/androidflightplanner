package se.flightplanner2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import se.flightplanner2.Project.LatLon;
import se.flightplanner2.Project.Merc;
import se.flightplanner2.TripState.NextLanding;
import se.flightplanner2.descpos.RelDec;
import se.flightplanner2.descpos.SigPointReldec;
import se.flightplanner2.vector.BoundingBox;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Html;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class PhrasesActivity extends Activity {
	private LinearLayout layout;
	private ArrayList<View> payloads = new ArrayList<View>();
	private RelDec curreldec;
	@Override
	public void onBackPressed() {
	  super.onBackPressed();
	  overridePendingTransition(0, 0);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == DescribePosition.CHOOSE_POSITION) {
			if (resultCode == RESULT_OK) {
				RelDec rel = (RelDec) data
						.getSerializableExtra("se.flightplanner2.reldec");
				curreldec = rel;
				// RookieHelper.showmsg(this,rel.getDescr(true,false));
				update();
			}
		}
	}

	private TextView addFoldable(String name, String content) {
		TextView payload = new TextView(this);
		payload.setText(Html.fromHtml(content));
		payload.setTextSize(20);
		addFoldable(name, payload);
		return payload;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	enum Service {
		GROUND, TOWER, CONTROL
	}

	enum Phase {
		GROUND, FLIGHT
	}

	private String sel_station = "[ATS unit]";
	private String sel_alt = "[altitude]";

	private String getgnd() {
		return sel_station;
	}

	private String gettwr() {
		return sel_station;
	}

	private String getctl() {
		return sel_station;
	}

	private String getreg() {
		return registration;
	}

	private String gettype() {
		return atsradioname;
	}

	private String getpos(boolean detailed) {
		if (curreldec != null)
			return curreldec.getDescr(true, detailed) + ", " + format_sel_alt();
		return "[position], " + format_sel_alt();
	}

	private String format_sel_alt() {
		if (sel_alt.startsWith("FL"))
			return "FLIGHT LEVEL " + sel_alt.substring(2);
		return sel_alt;
	}

	// private void add(Service service,Phase phase,
	// )
	String destination = "destination";
	String departure = "departure field";
	String eta = "(ETA)";

	private String get_enroute_text() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Enroute Phraseology</h1>");
		sb.append("<h2>Enroute</h2>");
		sb.append("<p>" + getctl() + " " + getreg() + " " + getpos(true)
				+ "</p>");
		sb.append("<p>" + getctl() + " " + getreg()
				+ " REQUEST CHANGE TO [freq]</p>");
		sb.append("<p>" + getctl() + " " + getreg()
				+ " REQUEST ? FEET (FLIGHT LEVEL ?)</p>");
		sb.append("<p>" + getctl() + " " + getreg() + " " + getpos(false)
				+ " REQUEST CROSSING AIRSPACE TO [position] "
				+ format_sel_alt() + "</p>");
		sb.append("<p>"
				+ getctl()
				+ " "
				+ getreg()
				+ " REQUEST WEATHER DEVIATION [x MILES] LEFT (or RIGHT) OF ROUTE</p>");
		sb.append("<p>" + getctl() + " " + getreg()
				+ " REQUEST WEATHER DEVIATION TO [position] VIA [route]</p>");

		sb.append("<p>" + getgnd() + " " + getreg() + " VFR FLIGHT PLAN FROM "
				+ departure + " TO " + destination + " WE ARE DELAYED, "
				+ "NOW ESTIMATING " + destination + " " + eta + "</p>");

		SimpleDateFormat formatter = new SimpleDateFormat("mm");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		String nowtime = formatter.format(new Date());
		sb.append("<p>" + getgnd() + " " + getreg() + " VFR FLIGHT PLAN FROM "
				+ departure + " TO " + destination + ", " + getpos(false));

		sb.append("<p>"
				+ getgnd()
				+ " "
				+ getreg()
				+ " VFR FLIGHT PLAN FROM "
				+ departure
				+ " TO "
				+ destination
				+ " "
				+ ", DIVERTING TO (new destination), ESTIMATED TIME OF ARRIVAL (time)</p>");

		sb.append("</div>");

		return sb.toString();
	}

	private void update_ats() {
		TripState state = GlobalTripState.tripstate;
		NextLanding nl = null;

		SimpleDateFormat formatter2 = new SimpleDateFormat("HH:mm");
		formatter2.setTimeZone(TimeZone.getTimeZone("UTC"));

		destination = "destination";
		departure = "departure field";
		eta = "(ETA)";

		if (state != null)
			nl = state.getNextLanding();
		if (nl != null && nl.when != null) {
			eta = formatter2.format(nl.when);
			destination = nl.where;
		}

		NextLanding pt = null;
		if (state != null)
			pt = state.getPrevTakeoff();
		if (pt != null) {
			departure = pt.where;
		}
	}

	private String get_departure_text() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Departure Phraseology</h1>");
		sb.append("<h2>Taxi</h2>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " [location] REQUEST START UP, INFORMATION [ATIS letter]</p>");

		sb.append("<p>" + getgnd() + " " + getreg() + " " + gettype()
				+ " [location] VFR TO " + destination + " REQUEST TAXI</p>");

		sb.append("<small>When there is no ATIS:</small><br/>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST DEPARTURE INFORMATION</p>");

		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST DETAILED TAXI INSTRUCTIONS</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST CROSS RUNWAY [number]</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST CHANGE TO [freq]</p>");

		sb.append("<h2>Take-off</h2>");
		sb.append("<small>When run-up complete:</small><br/>");
		sb.append("<p>" + gettwr() + " " + getreg() + " READY</p>");

		sb.append("<small>When asked to line-up on runway:</small><br/>");
		sb.append("<p>" + getreg() + " LINING UP</p>");
		sb.append("<small>When given a conditional line-up instruction (e.g., \"Lineup behind landing Cessna, behind\"):</small><br/>");
		sb.append("<p>" + getreg() + "(condition) LINING UP</p>");

		sb.append("<small>To ask permission to turn right (or left) once airborne:</small><br/>");
		sb.append("<p>" + gettwr() + " " + getreg()
				+ " REQUEST RIGHT (or LEFT) TURN</p>");

		sb.append("<h2>Departing from uncontrolled field:</h2>");
		sb.append("<p>" + getgnd() + " " + getreg() + " DEPARTED " + departure
				+ ", (time-of-departure) ACTIVATING MY VFR FLIGHT PLAN TO "
				+ destination+ " </p>");

		sb.append("<h2>Departing</h2>");
		sb.append("<small>For help to avoid traffic or for navigation aid:</small><br/>");
		sb.append("<p>" + gettwr() + " " + getreg() + " REQUEST VECTORS</p>");
		// sb.append("<p>"+get
		// who()+" "+getreg()+" READY (at holding point, when ready departure)</p>");
		// asdf
		return sb.toString();
	}

	private String get_emergency_text() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Emergency</h1>");
		sb.append("<p>MAYDAY MAYDAY MAYDAY " + gettwr() + " " + getreg() + " "
				+ gettype() + " [kind of emergency] [intention] "
				+ getpos(true) + " [people onboard] [aircraft color]</p>");
		sb.append("<small>When pilot wishes to cancel emergency condition:</small><br/>");
		sb.append("<p>" + gettwr() + " " + getreg() + " CANCEL EMERGENCY</p>");
		return sb.toString();
	}

	private String get_arrival_text() {
		StringBuilder sb = new StringBuilder();
		sb.append("<h1>Arrival Phraseology</h1>");

		sb.append("<h2>Approach</h2>");
		sb.append("<p>" + gettwr() + " " + getreg() + " " + gettype() + " "
				+ getpos(false) + " INFORMATION [ATIS letter] FOR LANDING</p>");
		sb.append("<p>" + gettwr() + " " + getreg()
				+ " REQUEST HOLDING INSTRUCTIONS</p>");
		sb.append("<small>For help to avoid traffic or for navigation aid:</small><br/>");
		sb.append("<p>" + gettwr() + " " + getreg() + " REQUEST VECTORS</p>");
		sb.append("<p>" + gettwr() + " " + getreg()
				+ " REQUEST STRAIGHT-IN APPROACH</p>");
		sb.append("<p>" + gettwr() + " " + getreg()
				+ " REQUEST VISUAL APPROACH</p>");

		sb.append("<small>If not \"clear-to-land\"):</small><br/>");
		sb.append("<p>" + getgnd() + " " + getreg() + " FINAL</p>");
		sb.append("<small>If more than 4NM from touchdown, or 8NM for straight-in approach:</small><br/>");
		sb.append("<p>" + getgnd() + " " + getreg() + " LONG FINAL</p>");

		sb.append("<h2>Landing</h2>");
		sb.append("<p>" + getgnd() + " " + getreg() + " GOING AROUND</p>");

		sb.append("<h2>On uncontrolled field</h2>");
		sb.append("<p>" + getgnd() + " " + getreg() + " VFR FLIGHT PLAN FROM "
				+ departure + " TO " + destination + " " + getpos(false)
				+ " CLOSING MY FLIGHT PLAN</p>");

		sb.append("<h2>Taxi-in</h2>");
		sb.append("<small>To ask permission to taxi back along runway:</small><br/>");
		sb.append("<p>" + getgnd() + " " + getreg() + " REQUEST BACKTRACK</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " ON RUNWAY REQUEST TAXI TO FUEL STATION</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " ON RUNWAY REQUEST TAXI TO PARKING</p>");
		sb.append("<small>When entire aircraft is past holding point:</small><br/>");
		sb.append("<p>" + getgnd() + " " + getreg() + " RUNWAY VACATED</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST CROSS RUNWAY [number]</p>");
		sb.append("<p>" + getgnd() + " " + getreg()
				+ " REQUEST CHANGE TO [freq]</p>");

		return sb.toString();
	}

	LayoutInflater layinf;

	private View addFoldable(String name, final View payload) {
		Button but = (Button) layinf.inflate(R.layout.phrase_button, null,
				false);
		// v = vi.inflate(R.layout.game_list_item, null,false);
		but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				for (View pay : payloads)
					pay.setVisibility(View.GONE);
				payload.setVisibility(View.VISIBLE);
			}
		});
		but.setText(name);
		layout.addView(but);
		layout.addView(payload);
		payloads.add(payload);
		payload.setVisibility(View.GONE);
		return payload;
	}

	private TextView payload_enroute;
	private TextView payload_arrival;
	private TextView payload_departure;
	private TextView payload_emergency;

	private SigPoint getClosestSigp() {
		ArrayList<SigPoint> founditems = new ArrayList<SigPoint>();
		LatLon latlon = GlobalPosition.getLastLatLonPosition();
		if (latlon == null)
			return null;
		AirspaceLookup lookup = GlobalLookup.lookup;
		if (lookup == null)
			return null;
		for (int wider_net = 2; wider_net < 300; wider_net = (wider_net * 3) / 2 + 1) {

			founditems = lookup.allSigPoints.findall(BoundingBox.nearby(latlon,
					wider_net));
			if (founditems.size() >= 1)
				break;
			// Log.i("fplan","Looking around "+m.x+","+m.y+" size: "+Project.approx_scale(m,
			// 13,wider_net)+" found: "+bigairfs.size());
		}
		float closest_dist = 1e10f;
		SigPoint closest = null;
		for (SigPoint p : founditems) {
			float d = (float) Project.exacter_distance(p.latlon, latlon);
			if (d < closest_dist) {
				closest_dist = d;
				closest = p;
			}
		}
		return closest;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.phrases);
		layinf = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);

		layout = (LinearLayout) findViewById(R.id.main);

		SigPoint sp = getClosestSigp();
		if (sp != null)
			curreldec = new SigPointReldec(sp);

		Button but_pos = (Button) findViewById(R.id.but_position);

		but_pos.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(PhrasesActivity.this,
						DescribePosition.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
				intent.putExtra("se.flightplanner2.selectpos", true);
				startActivityForResult(intent, DescribePosition.CHOOSE_POSITION);
			}
		});
		Spinner spn_chn = (Spinner) findViewById(R.id.spn_channel);
		final Spinner spn_alt = (Spinner) findViewById(R.id.spn_alt);
		final Spinner spn_lvl = (Spinner) findViewById(R.id.spn_lvl);

		{
			final ArrayList<String> alts = new ArrayList<String>();
			alts.add("[altitude]");
			for (int i = 500; i < 10000;) {
				alts.add("" + i + " FEET");
				if (i < 3000)
					i += 100;
				else
					i += 500;
			}
			fill_spinner(spn_alt, alts.toArray(new String[alts.size()]));

			String cand = fetch_stored_presel(spn_alt, alts, "presel_altitude");
			if (cand != null)
				sel_alt = cand;

			spn_alt.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int pos, long arg3) {
					sel_alt = alts.get(pos);
					store_setting("presel_altitude", sel_alt);
					spn_lvl.setSelection(0);
					update();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					sel_alt = "[altitude]";
					update();
				}
			});

		}
		{
			final ArrayList<String> lvls = new ArrayList<String>();
			lvls.add("[FL]");
			for (int i = 35; i < 100; i += 5) {
				lvls.add("FL" + i);
			}
			fill_spinner(spn_lvl, lvls.toArray(new String[lvls.size()]));

			String cand = fetch_stored_presel(spn_lvl, lvls, "presel_altitude");
			if (cand != null)
				sel_alt = cand;

			spn_lvl.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int pos, long arg3) {
					sel_alt = lvls.get(pos);
					store_setting("presel_altitude", sel_alt);
					spn_alt.setSelection(0);
					update();
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					sel_alt = "[altitude]";
					update();
				}
			});

		}

		AirspaceLookup lookup = GlobalLookup.lookup;
		if (lookup != null) {
			LatLon latlon = GlobalPosition.getLastLatLonPosition();
			if (latlon != null) {
				HashSet<String> stations = new HashSet<String>();
				ArrayList<AirspaceArea> foundareas = new ArrayList<AirspaceArea>();
				for (int around = 5; around < 200; around *= 2) {
					foundareas = lookup.areas.get_areas(BoundingBox.nearby(
							latlon, around));
					if (foundareas.size() > 10)
						break;
				}
				for (AirspaceArea area : foundareas) {
					for (String freq : area.freqs) {
						int colon = freq.lastIndexOf(":");
						if (colon <= 1)
							stations.add(freq);
						else
							stations.add(freq.substring(0, colon));
					}
				}
				final ArrayList<String> stations2 = new ArrayList<String>();
				stations2.addAll(stations);
				Collections.sort(stations2);
				fill_spinner(spn_chn,
						stations2.toArray(new String[stations2.size()]));

				final String setting_name = "presel_station";
				String presel = fetch_stored_presel(spn_chn, stations2,
						setting_name);
				if (presel == null) {
					String best = "?";
					int best_score = -100000;
					int best_index = -1;
					int index = 0;
					for (String stat : stations2) {
						int score = 0;
						if (stat.toUpperCase().contains("ATIS"))
							score -= 100;
						if (stat.toUpperCase().contains("CONTROL"))
							score += 20;
						if (stat.toUpperCase().contains("TWR"))
							score += 10;
						if (stat.toUpperCase().contains("TOWER"))
							score += 10;
						if (score > best_score) {
							best = stat;
							best_score = score;
							best_index = index;
						}
						index += 1;
					}
					if (best_index != -1)
						spn_chn.setSelection(best_index);
				}
				if (presel != null)
					sel_station = presel;

				spn_chn.setOnItemSelectedListener(new OnItemSelectedListener() {
					@Override
					public void onItemSelected(AdapterView<?> arg0, View arg1,
							int pos, long arg3) {
						sel_station = stations2.get(pos);
						update();
						store_setting("presel_station", sel_station);

					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
						sel_station = "[ATS Callsign]";
						update();
					}
				});

			}
		}

		// addFoldable("Departure",
		// get_departure_texts()
		// );
		payload_enroute = addFoldable("En-Route", "");
		payload_arrival = addFoldable("Arrival", "");
		payload_departure = addFoldable("Departure", "");
		payload_emergency = addFoldable("Emergency", "");

		addFoldable("Phonetic Alphabet",
				"<p>Read digits indvidually, words 'thousand'/'hundred' can only be used for altitudes (four-thousand feet) and visibility (visibility 10 kilometers).</p>"+
				"<p>Alphabet:</p>" +
				"<p>"+
				"ALPHA<br/>" + "BRAVO<br/>"
				+ "CHARLIE<br/>" + "DELTA<br/>" + "ECHO<br/>" + "FOXTROT<br/>"
				+ "GOLF<br/>" + "HOTEL<br/>" + "INDIA<br/>" + "JULIET<br/>"
				+ "KILO<br/>" + "LIMA<br/>" + "MIKE<br/>" + "NOVEMBER<br/>"
				+ "OSCAR<br/>" + "PAPA<br/>" + "QUEBEC<br/>" + "ROMEO<br/>"
				+ "SIERRA<br/>" + "TANGO<br/>" + "UNIFORM<br/>" + "VICTOR<br/>"
				+ "WHISKEY<br/>" + "X-RAY<br/>" + "YANKEE<br/>" + "ZULU<br/>"
				+ "</p>"
				);

		update();
	}

	private String fetch_stored_presel(Spinner spn,
			final ArrayList<String> items, final String setting_name) {
		String presel = getPreferences(MODE_PRIVATE).getString(setting_name,
				null);

		if (presel != null) {
			int index = 0;
			boolean found = false;
			for (String stat : items) {
				Log.i("fplan.presel", "Applying presel: " + presel
						+ " to item: " + stat);
				if (presel.equals(stat)) {
					spn.setSelection(index);
					found = true;
					break;
				}
				index += 1;
			}
			if (!found)
				presel = null;
		}
		return presel;
	}

	String atsradioname = "Cessna";
	String registration = "SE-ABC";

	private void update() {
		TripState ts = GlobalTripState.tripstate;
		if (ts != null) {
			registration = ts.get_registration();
			atsradioname = ts.get_atsradioname();
		}
		update_ats();

		payload_enroute.setText(Html.fromHtml(get_enroute_text()));
		payload_arrival.setText(Html.fromHtml(get_arrival_text()));
		payload_departure.setText(Html.fromHtml(get_departure_text()));
		payload_emergency.setText(Html.fromHtml(get_emergency_text()));
		/*
		 * private TextView payload_arrival; private TextView payload_departure;
		 * private TextView payload_emergency;
		 */
	}

	private void fill_spinner(Spinner spn_pos, String[] contents) {
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(
				this, android.R.layout.simple_spinner_item, contents);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spn_pos.setAdapter(adapter);
	}

	private void store_setting(String setting_name, String value) {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor pedit = prefs.edit();
		pedit.putString(setting_name, value);
		pedit.commit();
	}
}
