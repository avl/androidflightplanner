package se.flightplanner2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;


public class RookieHelper {
//Stuff that can probably be done by one-liner library functions which
//I have yet to find ...
	static public void showmsg(Context act,String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		builder.setMessage(Html.fromHtml(msg,null,null))
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
