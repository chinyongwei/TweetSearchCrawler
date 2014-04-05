import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class TweetCrawler {
	/*
	 * Change 'query' to  your search term 
	 */
	String query = "mufc";
	/*
	 * change 'scroll_cursor if you have a point to continue,
	 * else just leave it blank to get the latest
	 */
	String scroll_cursor = "";
//	String scroll_cursor = "TWEET-451791255258021888-452148919196450816";
	
	String https_url = "https://twitter.com/i/search/timeline?q="+query+"&src=typd&include_available_features=1&include_entities=1&scroll_cursor=";
	String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
	int crawlTillMonth, crawlTillDate, stopMonth, stopDate; 
	String filenameall, filenamecompile;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TweetCrawler().testIt();
	}

	private void testIt() {
		// Month(1-12) and Date(1-31) to be that of the day to stop, 
		stopMonth = 4;
		stopDate = 1;
		
		// Month(1-12) and Date(1-31) to be that of the first tweet, 
		crawlTillMonth = 4;
		crawlTillDate = 5;

		// Reset files
		filenameall = "HTTPS_RESPONSE.html";
		File f = new File(filenameall);
		f.delete();
		
		filenamecompile = "JSON_COMPILE_TWEETS.txt";
		File fc = new File(filenamecompile);
		fc.delete();
		
		try {
			//to test for one crawl
//			int round = 1;
//			while (round-- > 0) {
			while( (crawlTillMonth>stopMonth)?true:(((crawlTillMonth==stopMonth)?(crawlTillDate>=stopDate):false)) ){
				System.out.println("____________crawl start____________");
				crawlURL(scroll_cursor, scroll_cursor + ".html");
				System.out.println("____________crawl end____________");
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void crawlURL(String scroll_cursor, String filename)
			throws IOException {
		URL url;
		url = new URL(https_url + scroll_cursor);
		HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

		// dumpl all cert info
		print_https_cert(con);

		// dump all the content
		print_content(con, filename);

	}
	
	private void print_content(HttpsURLConnection con, String filename) {
		if (con != null) {

			try {

				System.out.println("****** Content of the URL ********");
				BufferedReader br = new BufferedReader(new InputStreamReader(
						con.getInputStream()));

				String input;
				
				PrintWriter out = null;
				PrintWriter outTemp = null;
				PrintWriter outCompile = null;

				try {
					// Creates a printerwriter out that writes to the compiled
					// file and tempfile(to parse date).
					out = new PrintWriter(new FileOutputStream(new File(
							filenameall), true));
					outTemp = new PrintWriter(new FileOutputStream(new File(
							filename), false));
					outCompile = new PrintWriter(new FileOutputStream(new File(
							filenamecompile), true));
				} catch (FileNotFoundException ex) {
					ex.printStackTrace();
				}

				while ((input = br.readLine()) != null) {
					JSONObject jo = new JSONObject(input);
					// print out html
					out.append(jo.getString("items_html").toString());
					outTemp.append(jo.getString("items_html").toString());
//					System.out.println(jo.getString("items_html").toString());
					System.out.println("Appended to:(HTTPS_RESPONSE.html), Printed to:("+filename+")");
					
					// get next scroll_cursor id
					scroll_cursor = jo.getString("scroll_cursor");
				}
				outTemp.close();
				br.close();

				// Check latest refresh, using file generated, 'filename'.txt
				File tempInput = new File(filename);
				Document doc = Jsoup.parse(tempInput, "UTF-8", "");
				Elements elements = doc.getElementsByClass("js-stream-item");
				for (Element element : elements) {
					// Store in JSON format
					JSONObject tempO = new JSONObject();
					
					// Get timeStamp element
					Elements dateEs = element.getElementsByClass("_timestamp");
					for (Element dateE : dateEs) {
						String date = setCurrentCrawlTillDate( dateE.text());
						
						tempO.put("date", date);
						// CANCHANGE: edit to your output format
						System.out.println("date : " + date);
						// *CANCHANGE
					}
					
					//get tweet text element
					Elements tweetEs = element.getElementsByClass("tweet-text");
					for (Element tweetE : tweetEs) {

						tempO.put("tweet-text", tweetE.text());
						// CANCHANGE: edit to your output format
						System.out.println("tweet : " + tweetE.text());
						// *CANCHANGE
					}
					if(tempO.length() != 0) outCompile.append(tempO.toString()+"\n");
				}
				out.close();
				
				outCompile.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}

		}

	}

	private void print_https_cert(HttpsURLConnection con) {

		if (con != null) {

			try {

				System.out.println("Response Code : " + con.getResponseCode());
				System.out.println("Cipher Suite : " + con.getCipherSuite());
				System.out.println("\n");

				Certificate[] certs = con.getServerCertificates();
				for (Certificate cert : certs) {
					System.out.println("Cert Type : " + cert.getType());
					System.out.println("Cert Hash Code : " + cert.hashCode());
					System.out.println("Cert Public Key Algorithm : "
							+ cert.getPublicKey().getAlgorithm());
					System.out.println("Cert Public Key Format : "
							+ cert.getPublicKey().getFormat());
					System.out.println("\n");
				}

			} catch (SSLPeerUnverifiedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private String setCurrentCrawlTillDate(String text) {
		String[] dateArr = text.split(" ");
		if(dateArr.length == 1){
			Calendar lCal = Calendar.getInstance();
			crawlTillMonth =  lCal.get(Calendar.MONTH) + 1;
			crawlTillDate =  lCal.get(Calendar.DATE);
//			System.out.println(crawlTillDate+"/"+crawlTillMonth);
			return monthNames[crawlTillMonth-1]+" "+crawlTillDate;
		}
		
		crawlTillDate = Integer.parseInt(dateArr[1]);
		if(dateArr[0].equalsIgnoreCase("Jan")) crawlTillMonth=1;
		else if(dateArr[0].equals("Feb")) crawlTillMonth=2;
		else if(dateArr[0].equals("Mar")) crawlTillMonth=3;
		else if(dateArr[0].equals("Apr")) crawlTillMonth=4;
		else if(dateArr[0].equals("May")) crawlTillMonth=5;
		else if(dateArr[0].equals("Jun")) crawlTillMonth=6;
		else if(dateArr[0].equals("Jul")) crawlTillMonth=7;
		else if(dateArr[0].equals("Aug")) crawlTillMonth=8;
		else if(dateArr[0].equals("Sep")) crawlTillMonth=9;
		else if(dateArr[0].equals("Oct")) crawlTillMonth=10;
		else if(dateArr[0].equals("Nov")) crawlTillMonth=11;
		else if(dateArr[0].equals("Dec")) crawlTillMonth=12;
		return text;
	}

}