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
 *  
 *  This file and this file only is also released under Apache license as an API file
 */

package com.newcell.calltext.api;

import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import android.text.TextUtils;

/**
 * Helper class for Sip uri manipulation in java space.
 * Allows to parse sip uris and check it.
 *
 */
public final class SipUri {

    private SipUri() {
        // Singleton
    }

    private final static String SIP_SCHEME_RULE = "sip(?:s)?|tel";
    private final static String DIGIT_NBR_RULE = "^[0-9\\-#\\+\\*\\(\\)]+$";
    private final static Pattern SIP_CONTACT_ADDRESS_PATTERN = Pattern
            .compile("^([^@:]+)@([^@]+)$");
    private final static Pattern SIP_CONTACT_PATTERN = Pattern
            .compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?("+SIP_SCHEME_RULE+"):([^@]+)@([^>]+)(?:>)?$");
    private final static Pattern SIP_HOST_PATTERN = Pattern
            .compile("^(?:\")?([^<\"]*)(?:\")?[ ]*(?:<)?("+SIP_SCHEME_RULE+"):([^@>]+)(?:>)?$");
    private final static String API_PHONE_NBR_RULE = 
    		"^\"(1?(?:\\([2-9]\\d{2}\\)\\ ?|[2-9]\\d{2}(?:\\-?|\\ ?))[2-9]\\d{2}[- ]?\\d{4})\"";

    // Contact related
    /**
     * Holder for parsed sip contact information.<br/>
     * Basically wrap AoR.
     * We should have something like "{@link ParsedSipContactInfos#displayName} <{@link ParsedSipContactInfos#scheme}:{@link ParsedSipContactInfos#userName}@{@link ParsedSipContactInfos#domain}>
     */
    public static class ParsedSipContactInfos {
        /**
         * Contact display name.
         */
        public String displayName = "";
        /**
         * User name of AoR
         */
        public String userName = "";
        /**
         * Domaine name
         */
        public String domain = "";
        /**
         * Scheme of the protocol
         */
        public String scheme = "";
        

        @Override
        public String toString() {
            return toString(true);
        }
        
        public String toString(boolean includeDisplayName) {
            StringBuffer buildString = new StringBuffer();
            buildString.append("<");
            buildString.append(getReadableSipUri());
            buildString.append(">");
            
            // Append display name at beggining if necessary
            if (includeDisplayName && !TextUtils.isEmpty(displayName)) {
                // Prepend with space
                buildString.insert(0, " ");
                // Start with display name
                // qdtext         =  LWS / %x21 / %x23-5B / %x5D-7E  / UTF8-NONASCII
                String encodedName = displayName.replace("\"", "%22");
                encodedName = encodedName.replace("\\", "%5C");
                buildString.insert(0, "\""+encodedName+"\" ");
            }
            return buildString.toString();
        }
        
        public String getReadableSipUri() {
            StringBuffer buildString = new StringBuffer();
            if(TextUtils.isEmpty(scheme)) {
                buildString.append("sip:");
            }else {
                buildString.append(scheme + ":");
            }
            if(!TextUtils.isEmpty(userName)) {
                buildString.append(encodeUser(userName) + "@");
            }
            buildString.append(domain);
            return buildString.toString();
        }
        
        public String getContactAddress() {
            StringBuffer buildString = new StringBuffer();

            if(!TextUtils.isEmpty(userName)) {
                buildString.append(encodeUser(userName) + "@");
            }
            buildString.append(domain);
            return buildString.toString();
        }

        /**
         * @return parsed sip server uri
         */
        public ParsedSipUriInfos getServerSipUri() {
            String pScheme = scheme;
            if(TextUtils.isEmpty(scheme)) {
                pScheme = SipManager.PROTOCOL_SIP;
            }
            return parseSipUri(pScheme + ":" + domain);
        }
    }

    /**
     * Parse a sip contact
     * 
     * @param sipUri string sip contact
     * @return a ParsedSipContactInfos which contains uri parts. If not match
     *         return the object with blank fields
     */
    public static ParsedSipContactInfos parseSipContact(String sipUri) {
        ParsedSipContactInfos parsedInfos = new ParsedSipContactInfos();

        if (!TextUtils.isEmpty(sipUri)) {
            Matcher m = SIP_CONTACT_PATTERN.matcher(sipUri);
            if (m.matches()) {
                parsedInfos.displayName = Uri.decode(m.group(1).trim());
                parsedInfos.domain = m.group(4);
                parsedInfos.userName = Uri.decode(m.group(3));
                parsedInfos.scheme = m.group(2);
            }else {
                // Try to consider that as host
                m = SIP_HOST_PATTERN.matcher(sipUri);
                if(m.matches()) {
                    parsedInfos.displayName = Uri.decode(m.group(1).trim());
                    parsedInfos.domain = m.group(3);
                    parsedInfos.scheme = m.group(2);
                }else {
                    m = SIP_CONTACT_ADDRESS_PATTERN.matcher(sipUri);
                    if(m.matches()) {
                        parsedInfos.userName = Uri.decode(m.group(1));
                        parsedInfos.domain = m.group(2);
                    }else {
                        // Final fallback, we have only a username given
                        parsedInfos.userName = sipUri;
                    }
                }
            }
        }

        return parsedInfos;
    }

    /**
     * Return what should be displayed as caller id for this sip uri This is the
     * merged and fancy way, fallback to uri or user name if needed
     * 
     * @param uri the uri to display
     * @return the simple display
     */
    public static String getDisplayedSimpleContact(CharSequence uri) {
        // Reformat number
        if (uri != null) {
            String remoteContact = uri.toString();
            ParsedSipContactInfos parsedInfos = parseSipContact(remoteContact);

            if (!TextUtils.isEmpty(parsedInfos.displayName)) {
                // If available prefer the display name
                remoteContact = parsedInfos.displayName;
            } else if (!TextUtils.isEmpty(parsedInfos.userName)) {
                // Else, if available choose the username
                remoteContact = parsedInfos.userName;
            	remoteContact = getFormattedPhoneNumber(parsedInfos);
            }
            return remoteContact;
        }
        return "";
    }

    /**
     * Check if username is a telephone number
     * 
     * @param phone username to check
     * @return true if it looks like a phone number
     */
    public static boolean isPhoneNumber(String phone) {
        return (!TextUtils.isEmpty(phone) && Pattern.matches(DIGIT_NBR_RULE, phone));
    }
    
    /**
     * Get extract a phone number from sip uri if any available
     * 
     * @param uriInfos the parsed information of the uri obtained with {@link #parseSipContact(String)}
     * @return null if no phone number detected. The phone number otherwise.
     */
    public static String getPhoneNumber(ParsedSipContactInfos uriInfos) {
        if(uriInfos == null) {
            return null;
        }
        if(isPhoneNumber(uriInfos.userName)) {
           return uriInfos.userName; 
        }else if(isPhoneNumber(uriInfos.displayName)) {
            return uriInfos.displayName;
        }
        return null;
    }
    
    /**
     * Extract a phone number from the API call to NewCell's server
     * @param phone A string in the format "1234567890" <sip:9876543210@newcell.nunsec.com>
     * @return A phone number in the format 1234567890 from the first number in the string.
     *  Or an empty string if none found
     */
    public static String getPhoneNumberFromApiSipString(String phone) {
    	Pattern pattern = Pattern.compile(API_PHONE_NBR_RULE);
    	Matcher matcher = pattern.matcher(phone);
    	if(matcher.find()) {
    		String match = matcher.group(0);
    		return match.substring(1, match.length() - 1);
    	}
    	
    	return "";
    }
    
    /**
     * Extract a phone number from a sip uri if available and format it to 
     * (555) 555-1234 or 1 555-555-1234
     * 
     * @param uriInfos the parsed information of the uri obtained with {@link #parseSipContact(String)}
     * @return null if no phone number detected. The phone number otherwise.
     */
    public static String getFormattedPhoneNumber(ParsedSipContactInfos uriInfos) {
    	if(uriInfos == null) {
    		return null;
    	}
    	MessageFormat phoneMsgFormat10 = new MessageFormat("({0})-{1}-{2}");
    	MessageFormat phoneMsgFormat11 = new MessageFormat("{0} {1}-{2}-{3}");
    	String[] phoneNumArr = new String[4];
    	if(isPhoneNumber(uriInfos.userName)) {
    		if(uriInfos.userName.length() == 10) {
    			phoneNumArr[0] = uriInfos.userName.substring(0, 3);
    			phoneNumArr[1] = uriInfos.userName.substring(3, 6);
    			phoneNumArr[2] = uriInfos.userName.substring(6);
    			return phoneMsgFormat10.format(phoneNumArr);
    		} 
    		else if(uriInfos.userName.length() == 11) {
    			phoneNumArr[0] = uriInfos.userName.substring(0, 1);
    			phoneNumArr[1] = uriInfos.userName.substring(1, 4);
    			phoneNumArr[2] = uriInfos.userName.substring(4, 7);
    			phoneNumArr[3] = uriInfos.userName.substring(7);
    			return phoneMsgFormat11.format(phoneNumArr);
    		} 
    		else {
    			return uriInfos.userName;
    		}
    	} else if(isPhoneNumber(uriInfos.displayName)) {
    		if(uriInfos.displayName.length() == 10) {
    			phoneNumArr[0] = uriInfos.displayName.substring(0, 3);
    			phoneNumArr[1] = uriInfos.displayName.substring(3, 6);
    			phoneNumArr[2] = uriInfos.displayName.substring(6);
    			return phoneMsgFormat10.format(phoneNumArr);
    		}
    		else if(uriInfos.displayName.length() == 11) {
    			phoneNumArr[0] = uriInfos.displayName.substring(0, 1);
    			phoneNumArr[1] = uriInfos.displayName.substring(1, 4);
    			phoneNumArr[2] = uriInfos.displayName.substring(4, 7);
    			phoneNumArr[3] = uriInfos.displayName.substring(7);
    			return phoneMsgFormat11.format(phoneNumArr);
    		}
    		else {
    			return uriInfos.displayName;
    		}
    	}
    	return null;
    }
    
    /**
     * Extract a phone number from a sip uri if available and format it to 
     * (555) 555-1234 or 1 555-555-1234
     * 
     * @param originalPhone the phone number to be formatted
     * @return null if no phone number detected. The phone number otherwise.
     */
    public static String getFormattedPhoneNumber(String originalPhone) {
    	if(originalPhone == null) {
    		return null;
    	}
    	
    	MessageFormat phoneMsgFormat10 = new MessageFormat("({0})-{1}-{2}");
    	MessageFormat phoneMsgFormat11 = new MessageFormat("{0} {1}-{2}-{3}");
    	String[] phoneNumArr = new String[4];
    	if(isPhoneNumber(originalPhone)) {
    		if(originalPhone.length() == 10) {
    			phoneNumArr[0] = originalPhone.substring(0, 3);
    			phoneNumArr[1] = originalPhone.substring(3, 6);
    			phoneNumArr[2] = originalPhone.substring(6);
    			return phoneMsgFormat10.format(phoneNumArr);
    		} 
    		else if(originalPhone.length() == 11) {
    			phoneNumArr[0] = originalPhone.substring(0, 1);
    			phoneNumArr[1] = originalPhone.substring(1, 4);
    			phoneNumArr[2] = originalPhone.substring(4, 7);
    			phoneNumArr[3] = originalPhone.substring(7);
    			return phoneMsgFormat11.format(phoneNumArr);
    		} 
    		else {
    			return originalPhone;
    		}
    	}
    	return null;
    }

    /**
     * Transform sip uri into something that doesn't depend on remote display
     * name
     * For example, if you give "Display Name" <sip:user@domain.com>
     * It will return sip:user@domain.com
     * 
     * @param sipContact full sip uri
     * @return simplified sip uri
     */
    public static String getCanonicalSipContact(String sipContact) {
        return getCanonicalSipContact(sipContact, true);
    }

    /**
     * Transform sip uri into something that doesn't depend on remote display
     * name
     * 
     * @param sipContact full sip uri
     * @param includeScheme whether to include scheme in case of username
     * @return the canonical sip contact <br/>
     *         Example sip:user@domain.com <br/>
     *         or user@domain.com (if include scheme is false)
     */
    public static String getCanonicalSipContact(String sipContact, boolean includeScheme) {
        StringBuilder sb = new StringBuilder();
        if (!TextUtils.isEmpty(sipContact)) {
            Matcher m = SIP_CONTACT_PATTERN.matcher(sipContact);
            boolean hasUsername = false;
            boolean isHost = false;

            if (m.matches()) {
                hasUsername = true;
            } else {
                m = SIP_HOST_PATTERN.matcher(sipContact);
                isHost = true;
            }

            if (m.matches()) {
                if (includeScheme || isHost) {
                    sb.append(m.group(2));
                    sb.append(":");
                }
                sb.append(m.group(3));
                if (hasUsername) {
                    sb.append("@");
                    sb.append(m.group(4));
                }
            } else {
                m = SIP_CONTACT_ADDRESS_PATTERN.matcher(sipContact);
                if(m.matches()) {
                    if(includeScheme) {
                        sb.append("sip:");
                    }
                    sb.append(sipContact);
                }else {
                    sb.append(sipContact);
                }
            }
        }

        return sb.toString();
    }

    // Uri related
    /**
     * Holder for parsed sip uri information.<br/>
     * We should have something like "{@link ParsedSipUriInfos#scheme}:{@link ParsedSipUriInfos#domain}:{@link ParsedSipUriInfos#port}"
     */
    public static class ParsedSipUriInfos {
        /**
         * Domain name/ip
         */
        public String domain = "";
        /**
         * Scheme of the protocol
         */
        public String scheme = SipManager.PROTOCOL_SIP;
        /**
         * Port number
         */
        public int port = 5060;
    }

    private final static Pattern SIP_URI_PATTERN = Pattern.compile(
            "^(sip(?:s)?):(?:[^:]*(?::[^@]*)?@)?([^:@]*)(?::([0-9]*))?$", Pattern.CASE_INSENSITIVE);

    /**
     * Parse an uri
     * 
     * @param sipUri the uri to parse
     * @return parsed object
     */
    public static ParsedSipUriInfos parseSipUri(String sipUri) {
        ParsedSipUriInfos parsedInfos = new ParsedSipUriInfos();

        if (!TextUtils.isEmpty(sipUri)) {
            Matcher m = SIP_URI_PATTERN.matcher(sipUri);
            if (m.matches()) {
                parsedInfos.scheme = m.group(1);
                parsedInfos.domain = m.group(2);
                if (m.group(3) != null) {
                    try {
                        parsedInfos.port = Integer.parseInt(m.group(3));
                    } catch (NumberFormatException e) {
                        // Log.e(THIS_FILE, "Unable to parse port number");
                    }
                }
            }
        }

        return parsedInfos;
    }
    
    public static Uri forgeSipUri(String scheme, String contact) {
        return Uri.fromParts(scheme, contact, null);
    }
    
    public static String encodeUser(String user) {
        //user             =  1*( unreserved / escaped / user-unreserved )
        //user-unreserved  =  "&" / "=" / "+" / "$" / "," / ";" / "?" / "/"
        //unreserved  =  alphanum / mark
        //mark        =  "-" / "_" / "." / "!" / "~" / "*" / "'" / "(" / ")"
        return Uri.encode(user, "&=+$,;?/-_.!~*'()");
    }

}
