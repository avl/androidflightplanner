package se.flightplanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

public class RookieHelper {
//Stuff that can probably be done by one-liner library functions which
//I have yet to find ...
	static public void showmsg(Activity act,String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setMessage(msg)
		.setCancelable(true)
		.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int id) {
		         dialog.dismiss();
		    }
		});
		AlertDialog diag=builder.create();
		diag.show();
	}
	
}
