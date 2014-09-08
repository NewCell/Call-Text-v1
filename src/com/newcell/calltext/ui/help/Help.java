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

package com.newcell.calltext.ui.help;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.newcell.calltext.R;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.newcell.calltext.api.SipConfigManager;
import com.newcell.calltext.api.SipManager;
import com.newcell.calltext.utils.CollectLogs;
import com.newcell.calltext.utils.CustomDistribution;
import com.newcell.calltext.utils.Log;
import com.newcell.calltext.utils.PreferencesProviderWrapper;

public class Help extends SherlockDialogFragment implements OnItemClickListener {
	
	
	private static final String THIS_FILE = "Help";
	private PreferencesProviderWrapper prefsWrapper;
	
	public static Help newInstance() {
        Help instance = new Help();
        Bundle args = new Bundle();
        args.putBoolean(ARG_KILL_LOADING, false);
        instance.setArguments(args);
        return instance;
    }
	
	private static final int REQUEST_SEND_LOGS = 0;
	
	// Help choices
	private final static int FAQ = 0;
	private final static int SEND_LOGS = 2;
	private final static int START_LOGS = 3;
	private final static int LEGALS = 4;
	private final static int CUST_CARE = 5;
	
	private class HelpEntry {
		public int iconRes;
		public int textRes;
		public int choiceTag;
		public HelpEntry(int icon, int text, int choice) {
			iconRes = icon;
			textRes = text;
			choiceTag = choice;
		}
	}

    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	prefsWrapper = new PreferencesProviderWrapper(getActivity());
    	
        
    }
    

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_menu_help)
                .setTitle(R.string.help)
                .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dismiss();
                        }
                    }
                )
                .setView(getCustomView(getActivity().getLayoutInflater(), null, savedInstanceState))
                .create();
    }

    
    public View getCustomView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.help, container, false);
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        
        
        ArrayList<HelpEntry> items = new ArrayList<HelpEntry>();

        // FAQ
		if(!TextUtils.isEmpty(CustomDistribution.getFaqLink())) {
			items.add(new HelpEntry(android.R.drawable.ic_menu_info_details, R.string.faq, FAQ));
		}
		
		// Issue list 
		/*
		 * 8/18/2014 Removed to simplify Help dialog
		 
		if(CustomDistribution.showIssueList()) {
			items.add(new HelpEntry(android.R.drawable.ic_menu_view, R.string.view_existing_issues, OPEN_ISSUES));
		}
		
		 * 
		 */
		
		// Log collector
		if(!TextUtils.isEmpty(CustomDistribution.getSupportEmail()) ) {
			if(isRecording()) {
		        items.add(new HelpEntry( android.R.drawable.ic_menu_send , R.string.send_logs, SEND_LOGS));
			}else {
		        items.add(new HelpEntry( android.R.drawable.ic_menu_save , R.string.record_logs, START_LOGS));
			}
		}

		// Legal info
		if(!TextUtils.isEmpty(CustomDistribution.getLegalLink())) {
			items.add(new HelpEntry(android.R.drawable.ic_menu_gallery, R.string.legal_information, LEGALS));
		}
		
		// Customer care
		/*
		 * 
		 
		if(!TextUtils.isEmpty(CustomDistribution.getCustomerCareNumber())) {
			items.add(new HelpEntry(R.drawable.ic_menu_answer_call, R.string.call_customer_care, CUST_CARE));
		}
		
		 * 
		 */
        
        lv.setAdapter(new HelpArrayAdapter(getActivity(), items));
        
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        tv.setText(
        		getString(R.string.help_application_info) + " " + 
        		getString(R.string.csipsimple) + " version: " +
        		getString(R.string.csipsimple_version_name));
        
        return v;
    }
    
    private class HelpArrayAdapter extends ArrayAdapter<HelpEntry> {
    	public HelpArrayAdapter(Context ctxt, List<HelpEntry> items) {
			super(ctxt, R.layout.help_list_row, android.R.id.text1, items);
		}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View v = super.getView(position, convertView, parent);
    		bindView(v, getItem(position));
    		return v;
    	}
    	
    	/**
    	 * Bind the fiew to the help entry content
    	 * @param v the view to bind info to
    	 * @param he the help entry to display info of
    	 */
    	private void bindView(View v, HelpEntry he) {
    		TextView tv = (TextView) v;
    		tv.setText(he.textRes);
    		tv.setCompoundDrawablesWithIntrinsicBounds(he.iconRes, 0, 0, 0);
    	}
    }
    
    

	private boolean isRecording() {
		return (prefsWrapper.getLogLevel() >= 3);
	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long id) {
        Log.i(THIS_FILE, "Item clicked : " + id +" " + position);
		HelpArrayAdapter haa = (HelpArrayAdapter) av.getAdapter();
		HelpEntry he = haa.getItem(position);
		
		SherlockDialogFragment newFragment;
		switch (he.choiceTag) {
		case FAQ:
			/*
			 * Removed to use the browser instead
			 
			newFragment = Faq.newInstance();
	        newFragment.show(getFragmentManager(), "faq");
	        
	         *
	         */
			
			if(!TextUtils.isEmpty(CustomDistribution.getFaqLink())) {
		        Intent i = new Intent(Intent.ACTION_VIEW);
		        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		        i.setData(Uri.parse(CustomDistribution.getFaqLink()));
		        startActivity(i);
			}
			break;
		case LEGALS:
			/*
			 * TODO:: Temporarily using until we can get a legal page on the website
			 * 
			 */
			newFragment = Legal.newInstance();
	        newFragment.show(getFragmentManager(), "issues");
			/*
			 * 
			 */
	        
	        /*
	         * For use if we get our own Legal page on the website.
	         
	        if(!TextUtils.isEmpty(CustomDistribution.getLegalLink())) {
	        	Intent i = new Intent(Intent.ACTION_VIEW);
	        	i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	i.setData(Uri.parse(CustomDistribution.getLegalLink()));
	        	startActivity(i);
	        }
	        
	         * 
	         */
	        
			break;
		case SEND_LOGS:
			prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "1");
			try {
				startActivityForResult(CollectLogs.getLogReportIntent("<<<PLEASE ADD THE BUG DESCRIPTION HERE>>>", getActivity()), REQUEST_SEND_LOGS);
			}catch(Exception e) {
				Log.e(THIS_FILE, "Impossible to send logs...", e);
			}
			Log.setLogLevel(1);
			break;
		case START_LOGS:
			prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "4");
			Log.setLogLevel(4);
			Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
			getActivity().sendBroadcast(intent);
			dismiss();
			break;
		case CUST_CARE:
			// Call customer care
			
			break;
		default:
			break;
		}
	}
	
	
	private final static String ARG_KILL_LOADING = "kill_loading";

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SEND_LOGS) {
			try {
				dismiss();
			} catch (IllegalStateException ex) {
				getArguments().putBoolean(ARG_KILL_LOADING, true);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResume() {
		super.onResume();
		final boolean kill = getArguments().getBoolean(ARG_KILL_LOADING, false);
		if (kill) {
			dismiss();
		}
	}

}
