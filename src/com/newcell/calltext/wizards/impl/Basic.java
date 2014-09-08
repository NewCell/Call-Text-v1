/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.newcell.calltext.wizards.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.newcell.calltext.R;
import com.newcell.calltext.api.SipManager;
import com.newcell.calltext.api.SipProfile;
import com.newcell.calltext.api.SipUri;
import com.newcell.calltext.api.SipUri.ParsedSipContactInfos;
import com.newcell.calltext.utils.Log;

public class Basic extends BaseImplementation {
	protected static final String TAG = "Basic W";

	private Preference accountSync;
	private EditTextPreference accountDisplayName;
	private EditTextPreference accountUserName;
	private EditTextPreference accountServer;
	private EditTextPreference accountPassword;

	private ProgressDialog dialog;
	private Context context;
	private SharedPreferences sharedPref;
	private SharedPreferences.Editor prefEditor;

	private static final String REQUEST_AUTH = 		"request_authentication";
	private static final String REQUEST_INFO = 		"request_user_info";
	private static final String REQUEST_COMPLETE = 	"request_complete";
	private static final String DEF_DISPLAY_NAME = "NO ACCOUNT FOUND";
	private static final String DEF_USER_NAME = "NOACCOUNTFOUND";
	private static final String DEF_PASSWORD = "PASSWORD";
	
	public static final int MEID_DECIMAL_NEEDED_LENGTH = 18;

	private void bindFields() {
		accountSync = (Preference) findPreference("sync");
		accountDisplayName = (EditTextPreference) findPreference("display_name");
		accountUserName = (EditTextPreference) findPreference("username");
		accountServer = (EditTextPreference) findPreference("server");
		accountPassword = (EditTextPreference) findPreference("password");

		dialog = new ProgressDialog(context);

		// Open shared preferences for the app to retrieve AuthKey and AuthPass
		sharedPref = context.getSharedPreferences(
				context.getString(R.string.preference_file_key), 
				Context.MODE_PRIVATE);

		prefEditor = sharedPref.edit();


		accountSync.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				/*
				 * Old AuthKey/AuthPass method

				String temp_auth_key = 
						sharedPref.getString(context.getString(R.string.preference_auth_key), "");
				String temp_auth_pass = 
						sharedPref.getString(context.getString(R.string.preference_auth_pass), "");

				if(temp_auth_key == null
						|| temp_auth_key == ""
						|| temp_auth_pass == null
						|| temp_auth_pass == "") {
					// User AuthKey and AuthPass preference files 
					// don't exist, request them
					Log.i(TAG, "No user authentication key/password, starting REQUEST_AUTH...");
					new AccountDownloader().execute(REQUEST_AUTH);
				} else {
					// User AuthKey and AuthPass preference files already exist, 
					// just request account info
					Log.i(TAG, "User authentication key/password found, starting REQUEST_INFO...");
					new AccountDownloader().execute(REQUEST_INFO);
				}

				 * Old AuthKey/AuthPass method
				 */

				new AccountDownloader().execute(REQUEST_INFO);

				return true;
			}
		});

	}

	public void fillLayout(final SipProfile account) {

		context = parent;

		bindFields();

		accountDisplayName.setText(account.display_name);


		String serverFull = account.reg_uri;
		if (serverFull == null) {
			//serverFull = "";
			serverFull = SipManager.SERVER_DOMAIN;
		}else {
			serverFull = serverFull.replaceFirst("sip:", "");
		}

		ParsedSipContactInfos parsedInfo = SipUri.parseSipContact(account.acc_id);		
		accountUserName.setText(parsedInfo.userName);
		accountServer.setText(serverFull);
		accountPassword.setText(account.data);
	}

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("username");
		setStringFieldSummary("server");
		setPasswordFieldSummary("password");
	}

	private static HashMap<String, Integer>SUMMARIES = new  HashMap<String, Integer>(){/**
	 * 
	 */
		private static final long serialVersionUID = -5743705263738203615L;

		{
			put("display_name", R.string.w_common_display_name_desc);
			put("username", R.string.w_basic_username_desc);
			put("server", R.string.w_common_server_desc);
			put("password", R.string.w_basic_password_desc);
		}};

		@Override
		public String getDefaultFieldSummary(String fieldName) {
			Integer res = SUMMARIES.get(fieldName);
			if(res != null) {
				return parent.getString( res );
			}
			return "";
		}

		public boolean canSave() {
			boolean isValid = true;

			isValid &= checkField(accountDisplayName, isEmpty(accountDisplayName));
			isValid &= checkField(accountPassword, isEmpty(accountPassword));
			isValid &= checkField(accountServer, isEmpty(accountServer));
			isValid &= checkField(accountUserName, isEmpty(accountUserName));

			return isValid;
		}

		public SipProfile buildAccount(SipProfile account) {
			Log.d(TAG, "begin of save ....");
			account.display_name = accountDisplayName.getText().trim();

			//String[] serverParts = accountServer.getText().split(":");
			//			String serverDomain = SipManager.SERVER_DOMAIN;
			String serverDomain = accountServer.getText().toString();
			//account.acc_id = "<sip:" + SipUri.encodeUser(accountUserName.getText().trim()) + "@"+serverParts[0].trim()+">";
			account.acc_id = "<sip:" + SipUri.encodeUser(accountUserName.getText().trim()) + "@" + serverDomain + ">";

			//String regUri = "sip:" + accountServer.getText();
			String regUri = "sip:" + serverDomain;
			account.reg_uri = regUri;
			account.proxies = new String[] { regUri } ;


			account.realm = "*";
			account.username = getText(accountUserName).trim();
			account.data = getText(accountPassword);
			account.scheme = SipProfile.CRED_SCHEME_DIGEST;
			account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
			//By default auto transport
			account.transport = SipProfile.TRANSPORT_UDP;
			return account;
		}

		@Override
		public int getBasePreferenceResource() {
			return R.xml.w_basic_preferences;
		}

		@Override
		public boolean needRestart() {
			return false;
		}

		/**
		 * Class to download the account details for the user from the NewCell server
		 * @author jerredmoss
		 *
		 */
		public class AccountDownloader extends AsyncTask<String, Integer, HttpResponse> {

			private static final int TIMEOUT = 20000;
			private static final String TAG = "AccountDownloader";

			private DefaultHttpClient httpClient;
			private HttpParams httpParams;
			private HttpPost httpPostRequest;
			private HttpEntity response;

			private JSONObject jsonObject;
			private TelephonyManager telephonyManager;

			private String mode;


			public AccountDownloader() {

			}

			@Override
			protected void onPreExecute() {
				super.onPreExecute();

				dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				dialog = ProgressDialog.show(parent, "Syncing", "Downloading account details...");
			}

			@Override
			protected HttpResponse doInBackground(String... params) {

				mode = params[0];

				if(SipManager.API_USER_INFO == null 
						|| SipManager.API_USER_INFO == ""
						|| SipManager.API_REQUEST_AUTH == null 
						|| SipManager.API_REQUEST_AUTH == "") {
					Log.e(TAG, "Could not retrieve API URL");
					return null;
				}

				jsonObject = new JSONObject();

				httpParams = new BasicHttpParams();
				HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT);

				httpClient = new DefaultHttpClient(httpParams);

				telephonyManager = 
						( TelephonyManager ) context.getSystemService(Context.TELEPHONY_SERVICE);

				String myHexDeviceId = telephonyManager.getDeviceId();
				
				Log.i(TAG, "Original Device ID::" + myHexDeviceId);
				
				String myDeviceId = myHexDeviceId;
				int phoneType = telephonyManager.getPhoneType();
				
				Log.i(TAG,  "Phone type::" + phoneType);
				
				if(phoneType == TelephonyManager.PHONE_TYPE_CDMA) {
					// CDMA uses an 18 character MEID-DEC
					// Derived from the MEID-HEX

					myDeviceId = getMeidDecimalFromHex(myHexDeviceId);
					Log.i(TAG, "Got MEID-DEC for CDMA::" + myDeviceId);

				} else if(phoneType == TelephonyManager.PHONE_TYPE_GSM) {
					// GSM uses a 15 character IMEI
					// No need to convert

					myDeviceId = myHexDeviceId;
					Log.i(TAG, "Using IMEI for GSM::" + myDeviceId);
				} else {
					Log.e(TAG, "Different phone type::" + phoneType);
				}

//				String myAuthKey = "";
//				String myAuthPass = "";
//				String concatHashes = "";

				if(mode == REQUEST_AUTH) {
					// First step, request AuthKey and AuthPass by passing ESN

					httpPostRequest = new HttpPost(SipManager.API_REQUEST_AUTH);

					try {
						jsonObject.put(context.getString(R.string.json_esn), myDeviceId);
					} catch(JSONException e) {
						Log.e(TAG, "Error adding esn to JSON");
						e.printStackTrace();
					}

				} else if(mode == REQUEST_INFO) {
					// Second step, request user info by passing ESN

					httpPostRequest = new HttpPost(SipManager.API_USER_INFO);

					//					myAuthKey = sharedPref.getString(context.getString(R.string.preference_auth_key), "");
					//					myAuthPass = sharedPref.getString(context.getString(R.string.preference_auth_pass), "");

					//					Log.i(TAG, "AuthKey from preferences::" + myAuthKey);
					//					Log.i(TAG, "AuthPass from preferences::" + myAuthPass);


					try {
						jsonObject.put(context.getString(R.string.json_esn), myDeviceId);
						//						jsonObject.put(context.getString(R.string.json_auth_key), myAuthKey);
						//						jsonObject.put(context.getString(R.string.json_auth_pass), myAuthPass);
					} catch (JSONException e) {
						Log.e(TAG, "Error adding esn/authkey/authpass to JSON");
						e.printStackTrace();
					}
				} else if(mode == REQUEST_COMPLETE) {
					// Third step, received user info. 
					// TODO:: Might not need

				}
				try {
					Log.i(TAG, "JSON Sending::\n" + jsonObject.toString(2));

					StringEntity se;
					se = new StringEntity(jsonObject.toString());

					httpPostRequest.setEntity(se);

					return httpClient.execute(httpPostRequest);

				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "Error downloading account details");
					Log.e(TAG, e.toString());
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					Log.e(TAG, "Error downloading account details");
					Log.e(TAG, e.toString());
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(TAG, "Error downloading account details");
					Log.e(TAG, e.toString());
					e.printStackTrace();
				} catch (JSONException e) {
					Log.e(TAG, "Error downloading account details");
					Log.e(TAG, e.toString());
					e.printStackTrace();
				}

				// Return null if there was an error
				return null;
			}

			@Override
			protected void onProgressUpdate(Integer... values) {
				super.onProgressUpdate(values);

				dialog.setProgress(values[0]);
			}

			@Override
			protected void onPostExecute(HttpResponse result) {
				super.onPostExecute(result);

				if(result == null) {
					Log.i(TAG, "Received NULL from server");

					accountDisplayName.setText(DEF_DISPLAY_NAME);
					accountUserName.setText(DEF_USER_NAME);
					accountPassword.setText(DEF_PASSWORD);
				}
				else {

					response = result.getEntity();

					try {
						if(response != null) {
							InputStream inputStream = response.getContent();

							String resultString = convertStreamToString(inputStream);
							inputStream.close();

							Log.i(TAG, "String Received::\n" + resultString);

							JSONObject jsonObjectRecv = new JSONObject(resultString);

							Log.i(TAG, "JSONObject Received::\n " + jsonObjectRecv.toString(2));

							if(mode == REQUEST_AUTH) {
								// First step, received authkey and authpass, now request user info
								updateSharedPrefs(jsonObjectRecv);

								new AccountDownloader().execute(REQUEST_INFO);

							} else if(mode == REQUEST_INFO) {
								// Second step, received user info, update account details
								updateAccountDetails(jsonObjectRecv);

							} else if(mode == REQUEST_COMPLETE) {
								// TODO:: Might not need
							}


						} else {
							Log.e(TAG, "Received NULL from the server");
						}
					} catch(IOException e) {
						Log.e(TAG, "Error getting data from server");
						Log.e(TAG, e.toString());
						e.printStackTrace();
					} catch(JSONException e) {
						Log.e(TAG, "Error converting response to JSON");
						Log.e(TAG, e.toString());
						e.printStackTrace();
					}
				}
				
				dialog.dismiss();
			}

		}

		/**
		 * Converts a Hex MEID to a Decimal MEID
		 * @param hex The hex version of the MEID
		 * @return The Decimal version of the MEID
		 */
		private static String getMeidDecimalFromHex(String hex) {
			/*
			 * How to do it:
			 * 1. Convert first 8 numbers from Hex, store in dec1
			 * 2. Convert last 6 numbers from Hex, store in dec2
			 * 3. If length of dec1 + dec2 == 18
			 *        Decimal = dec1 + dec2
			 *    ElseIf length of dec1 + dec2 < 18
			 *        Decimal = dec1 + "0" + dec2
			 *        Add as many 0s needed to get to 18 characters
			 *
			 * Example:
			 *     MEID-Hex: 45685023     858729
			 *     MEID-Dec: 1164464163 0 8750889
			 *     or
			 *     MEID-Dec: 1164464163 87508896
			 */
			
			String hex1 = hex.substring(0, 8);
			String hex2 = hex.substring(8, hex.length());

			String dec1 = String.valueOf(Long.parseLong(hex1, 16));
			String dec2 = String.valueOf(Long.parseLong(hex2, 16));

			int length = dec1.length() + dec2.length();

			Log.i(TAG, " Dec1::" + dec1 + "\n Dec2::" + dec2 + "\n Length::" + length);

			StringBuilder decimal = new StringBuilder("");

			if(length == 18) {
				decimal.append(dec1);
				decimal.append(dec2);
			} else if(length < 18) {
				decimal.append(dec1);
				for(int i = length; i < MEID_DECIMAL_NEEDED_LENGTH; i++) {
					decimal.append("0");
				}
				decimal.append(dec2);
			} else {
				Log.e(TAG,"MEID-DEC length is not <= " 
						+ MEID_DECIMAL_NEEDED_LENGTH 
						+ ". Length = " + length);
			}
			
			if(decimal.length() != MEID_DECIMAL_NEEDED_LENGTH) {
				Log.e(TAG, "Decimal length != " 
						+ MEID_DECIMAL_NEEDED_LENGTH 
						+ ". " + decimal.length());
			}
			Log.i(TAG, " Decimal::" + decimal);

			return decimal.toString();
		}

		/**
		 * Converts an InputStream to a String
		 * @param is The InputStream to convert
		 * @return The converted String
		 */
		private static String convertStreamToString(InputStream is) {

			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			StringBuilder sb = new StringBuilder();

			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return sb.toString();
		}

		/**
		 * Updates the users account settings for them from the account details
		 * pulled from the server
		 * @param account The JSON Object of the account received
		 */
		private void updateAccountDetails(JSONObject account) {

			String mAccountName = "";
			String mUser = "";
			String mPassword = "";

			try {
				if(account.getString(context.getString(R.string.json_esn)).equals("") ||
						account.getString(context.getString(R.string.json_sip_username)).equals("") ||
						account.getString(context.getString(R.string.json_sip_password)).equals("")) {
					Log.i(TAG, "No account was found that matched this phone");
					
					accountDisplayName.setText(DEF_DISPLAY_NAME);
					accountUserName.setText(DEF_USER_NAME);
					accountPassword.setText(DEF_PASSWORD);
					
					/**
					 * TODO::
					 * Change the text here to be a R.string.fdasflkdjfa for language support
					 */
					Toast.makeText(parent, "No account was found that matched this phone", 
							Toast.LENGTH_LONG).show();
					return;
				}
			} catch(JSONException e) {
				/**
				 * TODO::
				 * Change the text here to be a R.string.fsgkjgdfgh for language support
				 */
				Toast.makeText(context, "Error downloading account details", Toast.LENGTH_LONG).show();
				Log.e(TAG, "Could not get JSON values, none found");
				e.printStackTrace();
				return;
			}
			
			try {
				mAccountName = account.getString(context.getString(R.string.json_mobile_number));
			} catch (JSONException e) {
				Log.e(TAG, "Could not get mobile_number, none found");
				mAccountName = DEF_DISPLAY_NAME;
				e.printStackTrace();
			}
			try {
				mUser = account.getString(context.getString(R.string.json_sip_username));
			} catch (JSONException e) {
				Log.e(TAG, "Could not get sip_username, none found");
				mUser = DEF_USER_NAME;
				e.printStackTrace();
			}
			try {
				mPassword = account.getString(context.getString(R.string.json_sip_password));
			} catch (JSONException e) {
				Log.e(TAG, "Could not get sip_password, none found");
				mPassword = DEF_PASSWORD;
				e.printStackTrace();
			}
			
			if(mAccountName.equals(null) || mAccountName.equals("")) {
				mAccountName = mUser;
			}

			accountDisplayName.setText(mAccountName);
			accountUserName.setText(mUser);
			accountPassword.setText(mPassword);
		}

		/**
		 * Updates the shared preferences for the app for future use
		 * @param result The JSON object we received with the AuthKey and AuthPass
		 */
		private void updateSharedPrefs(JSONObject result) {
			String authkey = "";
			String authpass = "";

			try {
				if(result.getString(context.getString(R.string.json_auth_key)) == null
						|| result.getString(context.getString(R.string.json_auth_key)) == ""
						|| result.getString(context.getString(R.string.json_auth_pass)) == null
						|| result.getString(context.getString(R.string.json_auth_pass)) == "") {
					Log.i(TAG, "No authentication credentials were found for this account");
					/*
					 * TODO::
					 * Change the text here to be a R.string.fsgkjgdfgh for language support
					 */
					Toast.makeText(context, 
							"No authentication credentials were returned for this account", 
							Toast.LENGTH_LONG).show();
					return;
				}

			} catch(JSONException e) {
				/*
				 * TODO::
				 * Change the text here to be a R.string.fsgkjgdfgh for language support
				 */
				Toast.makeText(context, 
						"Error updating authentication credentials", 
						Toast.LENGTH_SHORT).show();
				Log.e(TAG, "Could not get JSON values from auth_request, none found");
				e.printStackTrace();
				return;
			}

			try {
				authkey = result.getString(context.getString(R.string.json_auth_key));
			} catch (JSONException e) {
				Log.e(TAG, "Could not get AuthKey, none found");
				e.printStackTrace();
				return;
			}
			try {
				authpass = result.getString(context.getString(R.string.json_auth_pass));
			} catch (JSONException e) {
				Log.e(TAG, "Could not get AuthPass, none found");
				e.printStackTrace();
				return;
			}

			// Open shared preferences for the app to retrieve AuthKey and AuthPass
			//			sharedPref = context.getSharedPreferences(
			//					context.getString(R.string.preference_file_key), 
			//					Context.MODE_PRIVATE);

			//			prefEditor = sharedPref.edit();
			prefEditor.putString(context.getString(R.string.preference_auth_key), authkey);
			prefEditor.putString(context.getString(R.string.preference_auth_pass), authpass);

			prefEditor.commit();

			Log.i(TAG, "Stored to preferences:\n    AuthKey::" + authkey
					+ "\n    AuthPass::" + authpass);
		}

		/**
		 * Create an MD5 hash from a string
		 * @param str The original string
		 * @return The MD5 hash of the string
		 */
		private String createMD5Hash(String str) {
			final String MD5 = "MD5";
			try {
				MessageDigest digest = java.security.MessageDigest.getInstance(MD5);
				digest.update(str.getBytes());
				byte messageDigest[] = digest.digest();
				// Create hex string
				StringBuilder hexString = new StringBuilder();
				for(byte aMessageDigest : messageDigest) {
					String h = Integer.toHexString(0xFF & aMessageDigest);
					while(h.length() < 2)
						h = "0" + h;
					hexString.append(h);
				}
				return hexString.toString();
			} catch(NoSuchAlgorithmException e) {
				Log.e(TAG, "Error in createMD5Hash()");
				Log.e(TAG, e.toString());
			}
			return null;
		}

}





