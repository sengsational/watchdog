package com.sengsational.wd;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class WatchdogMain implements Runnable {

	static private int periodMinutes;
	static private String url;
	static private String checkFor;
	static private int beatSeconds;
	static private Emailer emailer;

	static boolean running = false;
    static String response = "";
	
	static WatchdogMain singleton;
	static Thread runningThread;
	static long lastCheck = new Date(0).getTime();

	
	public static WatchdogMain getInstance(int periodMinutes, String url, String checkFor, int beatSeconds, Emailer emailer) {
		if (singleton != null) {
			WatchdogMain.finish();
		}
		singleton = new WatchdogMain(periodMinutes, url, checkFor, beatSeconds, emailer);
		return singleton;
	}

	private WatchdogMain(int periodMinutes, String url, String checkFor, int beatSeconds, Emailer emailer) {
		WatchdogMain.periodMinutes = periodMinutes;
		WatchdogMain.url = url;
		WatchdogMain.checkFor = checkFor;
		WatchdogMain.beatSeconds = beatSeconds;
		WatchdogMain.emailer = emailer;
        running = true;
        runningThread = new Thread(this, "Thread-WatchdogMain");
        System.out.println("running thread " + runningThread.getName());
        runningThread.start();
	}

	@Override
	public void run() {
		int dynamicMinutes = periodMinutes;
		while (running) {
			if (timeToCheck(dynamicMinutes, lastCheck)) {
				lastCheck = new Date().getTime();
				System.out.print(new Date() + " Checking... ");
				if (!websiteIsLive(url, checkFor)) {
					sendEmailAlert(emailer, url);
					// After sending email, start increasing the period so we don't flood the user
					if (dynamicMinutes < 60) dynamicMinutes = 60;
					else dynamicMinutes = dynamicMinutes * 2;
				} else {
					//System.out.println("found.");
					dynamicMinutes = periodMinutes; 
				}
			} else {
				try {
					Thread.sleep(beatSeconds * 1000);
				} catch (InterruptedException e) {
					System.out.println("Program interrupted");
					break;
				}
			}
		}
	}

	private boolean websiteIsLive(String url, String checkFor) {
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        HttpClient httpclient = new DefaultHttpClient(httpParams);
        try {
            String responseBody = "(uninitialized)";
            HttpGet httpget = new HttpGet(url); 
            System.out.print(" Executing request " + httpget.getURI() + "...");
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            responseBody = httpclient.execute(httpget, responseHandler);
            System.out.println("... finished executing request with " + responseBody.length() + " characters received.");
            //if (!quiet && responseBody.length() < 100) System.out.println("[" + responseBody + "]");
            if (responseBody != null) {
            	return responseBody.contains(checkFor);
            }
        } catch (Exception e) {
            System.out.println(new Date() + " Failed to get http page. " + url + " " + e.getMessage());
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return false;
	}

	private static void sendEmailAlert(Emailer emailer, String failedUrl) {
        if (failedUrl != null) {
            emailer.sendEmailMessage("CwHelper Monitor - Site Not Responding", "The computer on " + failedUrl +  " did not respond.  Scheduled recordings will probably not work.");
        } else {
            emailer.sendEmailMessage("CwHelper Monitor Test Email", "This is just a test to see if the monitoring program will send an email.");
        }
	}

	private boolean timeToCheck(int periodMinutes, long lastCheck) {
		long durationSinceLast = new Date().getTime() - lastCheck;
		if ((durationSinceLast + beatSeconds) >= (periodMinutes * 60 * 1000)) return true;
		return false;
	}

	public static boolean isRunning() {
        return running;
    }

    public static void finish() {
        runningThread.interrupt();
    }

	public static final String HELP_TEXT = "Usage:\nwatchdog -url=http://192.168.3.65:8181/ping " +
			"-checkEveryMinutes=15 -checkFor=\"Version: 5\" -sendAddresses=abc@gmail.com;def@gmail.com " +
    		"-smtpServer=smtp.googlemail.com -smtpPort=587 -logonUser=sendfrom@gmail.com -logonPassword=S33cr#t " + 
			"-sendStartupEmail=true\nor to shorten:\nwatchdog -url=http://192.168.3.65:8181/ping " +
			"-cf=\"Version: 5\" -sa=abc@gmail.com;def@gmail.com " +
			"-ss=smtp.googlemail.com -sp=587 -lu=sendfrom@gmail.com -lp=S33cr#t";
	public static final String LONG_HELP = "The url is the page you want to check and the checkFor is text on that page you expect to be there.\n" + 
			"checkEveryMinutes is how often you want to check, in minutes. sendStartupEmail is to send a test message when this program starts, just to make sure the email configuration is right.\n" +
			"The rest is for to configure the outbound email.\n";

	public static void main(String[] args) {

		Map<String, String> shortens = new HashMap<>();
		Set<String> parameterKeys = new HashSet<>();
		parameterKeys.add("url");
		parameterKeys.add("checkfor");shortens.put("cf", "checkfor");
		parameterKeys.add("sendaddresses");shortens.put("sa", "sendaddresses");
		parameterKeys.add("smtpserver");shortens.put("ss", "smtpserver");
		parameterKeys.add("logonuser");shortens.put("lu", "logonuser");
		parameterKeys.add("logonpassword");shortens.put("lp", "logonpassword");
		
		Set<String> optionalParameterKeys = new HashSet<>();
		optionalParameterKeys.add("smtpport");shortens.put("sp", "smtpport");
		optionalParameterKeys.add("sendstartupemail");shortens.put("se", "sendstartupemail");
		optionalParameterKeys.add("checkeveryminutes");shortens.put("ce", "checkeveryminutes");
		
		HashMap<String, String> params = convertToKeyValuePair(args, shortens);
		Set<String> set = params.keySet();
		for (String key : set) {
			parameterKeys.remove(key);
			System.out.println(key + ":" + params.get(key));
		}

		String missingArguments = "Missing Arguments: ";
		for (String key : parameterKeys) {
			if (key.equals("smtpport")) params.put("smtpport", "587");
			else if (key.equals("sendstartupemail")) params.put("sendstartupemail", "false");
			else if (key.equals("checkeveryminutes")) params.put("checkeveryminutes", "15");
			else missingArguments += key + ", ";
		}
		if (missingArguments.endsWith(", ")) {
			System.out.println("\nUnable to start the program. " + missingArguments);
			System.out.println(HELP_TEXT);
			System.exit(-1);
		}

        Emailer emailer = Emailer.getInstance();
        emailer.initialize(params.get("smtpserver"),params.get("smtpport"), params.get("logonuser"), params.get("logonpassword"), params.get("sendaddresses"));
		if ("true".equals(params.get("sendstartupemail"))) {
			System.out.println("Sending Test Email on Startup to " + params.get("sendaddresses"));
			WatchdogMain.sendEmailAlert(emailer, null);
		}
		
		System.out.println("\nHIT ANY KEY AT ANY TIME TO EXIT PROGRAM\n");

		int beatSeconds = 5;
		int checkEveryMinutes = 15;
		try {
			int checkEveryMinutesInput = Integer.parseInt(params.get("checkeveryminutes"));
			if (checkEveryMinutesInput > 0) checkEveryMinutes = checkEveryMinutesInput;
			else throw new Exception("checkEveryMinutes input must be greater than zero.");
		} catch (Throwable t) {
			System.out.println("Ignoring checkEveryMinutes input [" + params.get("checkeveryminutes") + "].  Using default " + checkEveryMinutes);
		}

		WatchdogMain.getInstance(checkEveryMinutes, params.get("url"), params.get("checkfor"), beatSeconds, emailer); // starts it's thread
		Scanner scanner = new Scanner(System.in);
		scanner.nextLine();
		scanner.close();
		WatchdogMain.finish();
	}
	
	
	private static HashMap<String, String> convertToKeyValuePair(String[] args, Map<String, String> shortens) {
		boolean needsHelp = false;
		if (args.length == 0) needsHelp = true;
	    HashMap<String, String> params = new HashMap<>();
	    for (String arg: args) {
	        if (!arg.startsWith("-")) {
	        	System.out.println("argument [" + arg + "] did not start with '-'. Ignored.");
	        	needsHelp = true;
	        	continue;
	        }
	        if (!arg.contains("=") || arg.startsWith("=") || arg.endsWith("=")) {
	        	System.out.println("argument [" + arg + "] did not contain '=', or not in the right place. Ignored.");
	        	needsHelp = true;
	        	continue;
	        }
	        
	        String[] splitFromEqual = arg.split("=");
	        String key = splitFromEqual[0].substring(1);
	        if (shortens.containsKey(key)) {
	        	key = shortens.get(key);
	        	System.out.println(key);
	        }
		    String value = splitFromEqual[1];

	        params.put(key.toLowerCase(), value);
	    }
	    if (needsHelp) {
	    	System.out.println(HELP_TEXT);
	    	System.out.println(LONG_HELP);
	    }
	    return params;
	}	
}
