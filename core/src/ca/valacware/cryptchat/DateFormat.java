package ca.valacware.cryptchat;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

class DateFormat extends SimpleDateFormat {
	DateFormat() {
		super("yyMMdd");
		this.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
}
