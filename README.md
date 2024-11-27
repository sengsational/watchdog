This is a small Java program that takes command line input for 1) a web page, and 2) a "send from" smpt email user and server, and 3) a "send to" email address.  The program runs continuously, periodically checking to make sure the web page responds with a small bit of expected text.  If the text is present, it waits the specified amount of time before checking again, but doesn't do anything.  If the web site fails to respond with the expected text, then an email is sent using the SMTP specifics specified.

Usage:
watchdog -url=http://192.168.3.65:8181/ping -checkEveryMinutes=15 -checkFor="Version: 5" -sendAddresses=abc@gmail.com;def@gmail.com -smtpServer=smtp.googlemail.com -smtpPort=587 -logonUser=sendfrom@gmail.com -logonPassword=S33cr#t -sendStartupEmail=true

"checkFor" is the text the program looks for from the response from the url
"sendStartupEmail" will send an email at program startup so you can see that the SMTP configuration is working.

Usage with shortened argument keys:
watchdog -url=http://192.168.3.65:8181/ping -ce=15 -cf="Version: 5" -sa=abc@gmail.com;def@gmail.com -ss=smtp.googlemail.com -sp=587 -lu=sendfrom@gmail.com -lp=S33cr#t se=true
