/*
 *  Copyright (C) 2012-2013 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo RSR.
 *
 *  Akvo RSR is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo RSR is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included with this program for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.rsr.up.xml;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.akvo.rsr.up.dao.RsrDbAdapter;
import org.akvo.rsr.up.domain.Update;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


/*
 * Class to handle XML parsing for a project update.
 * Normally requested as a list, in which case the root tags of the XML will be <response><objects type="list">
 * Example input for one object:
 * 
<object>
<update_method>W</update_method>
<photo_credit/>
<photo_caption/>
<title>Video screening improves farms productivity</title>
<photo>http://test.akvo.org/rsr/media/db/project/363/update/2505/ProjectUpdate_2505_photo_2013-02-04_10.56.30.JPG</photo>
<absolute_url>/rsr/project/363/update/2505/</absolute_url>
<project>/api/v1/project/363/</project>
<video_caption/>
<photo_location>E</photo_location>
<video_credit/>
<video/>
<user>/api/v1/user/460/</user>
<uuid>893274983-3243-23423433242342234</uuid>
<time>2013-02-04T10:54:12</time>
<time_last_updated>2013-02-04T10:56:30</time_last_updated>
<text>After training on audio Visual content development(supported by IICD) ADS-Nyanza is currently using the videos to train farmers on how they can improve their farm productivity.

http://www.iicd.org/articles/video-screenings-are-starting-point-for-better-crops-in-kenya</text>
<id type="integer">2505</id>
<resource_uri>/api/v1/project_update/2505/</resource_uri>
</object>

 */



public class UpdateListHandler extends DefaultHandler {


	// ===========================================================
	// Fields
	// ===========================================================
	
	private boolean in_update = false;
	private boolean in_id = false;
	private boolean in_title = false;
	private boolean in_project_id = false;
	private boolean in_user_id = false;
    private boolean in_photo = false;
    private boolean in_photo_credit = false;
    private boolean in_photo_caption = false;
    private boolean in_video = false;
	private boolean in_text = false;
	private boolean in_time = false;
	private boolean in_uuid = false;

	private Update currentUpd;
	private int updateCount;
	private boolean syntaxError = false;
    private boolean insert;
    private boolean extra;
	private int depth = 0;
	private SimpleDateFormat df1;
	private String buffer;
	
	//where to store results
	private RsrDbAdapter dba;
	
	/*
	 * constructor
	 */
	public UpdateListHandler(RsrDbAdapter aDba, boolean insert, boolean extra) {
		super();
		dba = aDba;
        this.insert = insert;
        this.extra = extra;
		df1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
		df1.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public boolean getError() {
		return syntaxError;
	}

	public int getCount() {
		return updateCount;
	}

	public Update getLastUpdate() {
		return currentUpd; //only valid if insert==False
	}

	// ===========================================================
	// Methods
	// ===========================================================
	@Override
	public void startDocument() throws SAXException {
		dba.open();
		updateCount = 0;
		depth = 0;
		syntaxError = false;
	}

	@Override
	public void endDocument() throws SAXException {
		dba.close();
	}

	/** Gets be called on opening tags like: 
	 * <tag> 
	 * Can provide attribute(s), when xml was like:
	 * <tag attribute="attributeValue">*/
	@Override
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		buffer = "";
		if (localName.equals("object")) {
			this.in_update = true;
			currentUpd = new Update();
		} else if (in_update) {
			if (localName.equals("id")) {
				this.in_id = true;
			} else if (localName.equals("title")) {
				this.in_title = true;
			} else if (localName.equals("text")) {
				this.in_text = true;
			} else if (localName.equals("time")) {
				this.in_time = true;
			} else if (localName.equals("project")) {
				this.in_project_id = true;
			} else if (localName.equals("user")) {
				this.in_user_id = true;
			} else if (localName.equals("uuid")) {
				this.in_uuid = true;
            } else if (localName.equals("photo")) {
                this.in_photo = true;
            } else if (localName.equals("photo_credit")) {
                this.in_photo_credit = true;
            } else if (localName.equals("photo_caption")) {
                this.in_photo_caption = true;
            } else if (localName.equals("video")) {
                this.in_video = true;
			}
		}
		
		depth++;
	}
	
	/** Gets called on closing tags like: 
	 * </tag> */
	@Override
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
		depth--;

		if (localName.equals("object")) { //we are done
            this.in_update = false;
            if (currentUpd != null && currentUpd.getId() != null) {
                updateCount++;
                if (insert) {
                    dba.saveUpdate(currentUpd, false); //preserve name of any cached image
                    currentUpd = null;
                }
            }
        } else if (localName.equals("id")) {
			this.in_id = false;
			currentUpd.setId(buffer);
		} else if (localName.equals("title")) {
			this.in_title = false;
			currentUpd.setTitle(buffer);
		} else if (localName.equals("text")) {
			this.in_text = false;
			currentUpd.setText(buffer);
		} else if (localName.equals("time")) {
			this.in_time = false;
			try {
				currentUpd.setDate(df1.parse(buffer));
			} catch (ParseException e1) {
				syntaxError = true;
			}
		} else if (localName.equals("project")) {
			this.in_project_id = false;
			currentUpd.setProjectId(idFromUrl(buffer));
		} else if (localName.equals("user")) {
			this.in_user_id = false;
			currentUpd.setUserId(idFromUrl(buffer));
		} else if (localName.equals("uuid")) {
			this.in_uuid = false;
			currentUpd.setUuid(buffer);
		} else if (localName.equals("photo")) {
			this.in_photo = false;
			currentUpd.setThumbnailUrl(buffer);
        } else if (localName.equals("photo_credit")) {
            this.in_photo_credit = false;
            currentUpd.setPhotoCredit(buffer);
        } else if (localName.equals("photo_caption")) {
            this.in_photo_caption = false;
            currentUpd.setPhotoCaption(buffer);
        } else if (localName.equals("video")) {
            this.in_video = false;
            currentUpd.setVideoUrl(buffer);
		}
	}
	
	/** Gets called on the following structure: 
	 * <tag>characters</tag> */
	// May be called multiple times for pieces of the same tag contents!
	@Override
    public void characters(char ch[], int start, int length) {
			if (this.in_id
			 || this.in_title
			 || this.in_uuid
			 || this.in_user_id
			 || this.in_project_id
             || this.in_photo
             || this.in_photo_credit
             || this.in_photo_caption
             || this.in_video
			 || this.in_text
			 || this.in_time
			 ) { //remember content
				buffer += new String(ch, start, length);
			}
	}
	
	
	// extract id from things like /api/v1/project/574/
	private String idFromUrl(String s) {
		if (s.endsWith("/")) {
			int i = s.lastIndexOf('/',s.length()-2);
			if (i>=0) {
				return s.substring(i+1, s.length()-1);
			} else syntaxError = true;
		} else syntaxError = true;
		return null;
	}

}
