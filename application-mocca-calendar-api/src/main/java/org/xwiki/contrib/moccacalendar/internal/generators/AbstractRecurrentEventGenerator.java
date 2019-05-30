/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.moccacalendar.internal.generators;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.joda.time.DateTime;
import org.xwiki.contrib.moccacalendar.EventInstance;
import org.xwiki.contrib.moccacalendar.RecurrentEventGenerator;
import org.xwiki.contrib.moccacalendar.internal.EventConstants;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class to create recurrent events if the frequency is sufficient regular allow this.
 * Concrete subclasses must implement the {@link #incrementCalendarByOnePeriod(Calendar)} method.
 */
public abstract class AbstractRecurrentEventGenerator implements RecurrentEventGenerator
{

    /**
     * increment the calendar by the period of the event.
     * E.g. in case of weekly events increment it by one week.
     * @param cal the calendar to increment; should never be null
     */
    protected abstract void incrementCalendarByOnePeriod(Calendar cal);
    
    /**
     * generate a list of event instances for the given date range
     * by incrementing the calendar from the start date until it has covered
     * the complete date range.
     * @see {@link RecurrentEventGenerator#generate(XWikiDocument, Date, Date)}
     */
    @Override
    public List<EventInstance> generate(XWikiDocument event, Date dateFrom, Date dateTo)
    {
        BaseObject eventData = event
            .getXObject(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_CLASS_NAME));
        BaseObject eventRecData = event
            .getXObject(event.resolveClassReference(EventConstants.MOCCA_CALENDAR_EVENT_RECURRENCY_CLASS_NAME));
    
        Date startDate = eventData.getDateValue(EventConstants.PROPERTY_STARTDATE_NAME);
        Date endDate = eventData.getDateValue(EventConstants.PROPERTY_ENDDATE_NAME);
        final boolean allDay = eventData.getIntValue(EventConstants.PROPERTY_ALLDAY_NAME) == 1;
        if (allDay) {
            // we need to cut out the start time:
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            startDate = cal.getTime();
        }
        if (endDate == null) {
            // FIXME: hardwired default event duration of 30 minutes, another time. BAD!
            if (allDay) {
                endDate = new Date(startDate.getTime() /* + 24 * 60 * 60 * 1000L - 30 * 60 * 1000L*/);
            } else {
                endDate = new Date(startDate.getTime() + 30 * 60 * 1000L);
            }
        }
        final long duration = endDate.getTime() - startDate.getTime();
    
        Date firstInstance = eventRecData.getDateValue(EventConstants.PROPERTY_FIRSTINSTANCE_NAME);
        Date lastInstance = eventRecData.getDateValue(EventConstants.PROPERTY_LASTINSTANCE_NAME);
    
        if (firstInstance != null && firstInstance.after(dateFrom)) {
            dateFrom = firstInstance;
        }
        // FIXME: this is likely not to be correct
        if (lastInstance != null && lastInstance.before(dateTo)) {
            dateTo = lastInstance;
        }
    
        if (dateTo.before(dateFrom)) {
            return Collections.emptyList();
        }
    
        Calendar cal = Calendar.getInstance();
    
        // FIXME: it is endDate that should be after dateFrom instead !
        cal.setTime(startDate);
    
        // stupid, inefficient, but should work
        while (cal.getTimeInMillis() + duration < dateFrom.getTime()) {
            incrementCalendarByOnePeriod(cal);
        }
    
        List<EventInstance> eventInstances = new ArrayList<>();
        while (cal.getTime().compareTo(dateTo) <= 0) {
            EventInstance instance = new EventInstance();
            instance.setStartDate(new DateTime(cal.getTimeInMillis()));
            instance.setEndDate(new DateTime(cal.getTimeInMillis() + duration));
    
            eventInstances.add(instance);
    
            incrementCalendarByOnePeriod(cal);
        }
    
        return eventInstances;
    }

}
