package com.example.jordan.meeting.tasks;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.example.jordan.meeting.R;
import com.example.jordan.meeting.database.Attendee;
import com.example.jordan.meeting.database.Meeting;
import com.example.jordan.meeting.repositories.AttendToRepo;
import com.example.jordan.meeting.repositories.AttendeeRepo;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

public class GoogleCalendarTask extends AsyncTask<Void, Void, String> {

    /* request codes related to the google calendar feature */
    public static final int ACCOUNT_REQUEST_CODE = 6;
    public static final int PERMISSION_REQUEST_CODE = 7;

    private GoogleAccountCredential credential;
    private final HttpTransport transport = AndroidHttp.newCompatibleTransport();
    private final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
    private String tag = "events";
    private Meeting meeting;

    /* This AsyncTask object is bound to be destroyed by the garbage collector after the next Google
     * Calendar event adding request.
     * */
    @SuppressLint("StaticFieldLeak")
    private AppCompatActivity activity;
    public boolean retry = false;
    private AttendToRepo attendToRepo;
    private AttendeeRepo attendeeRepo;
    private boolean feedback;

    public GoogleCalendarTask(final Meeting meeting, final AppCompatActivity activity,
                              AttendToRepo attendToRepo, AttendeeRepo attendeeRepo, boolean feedback){
        this.meeting = meeting;
        this.activity = activity;
        this.attendToRepo = attendToRepo;
        this.attendeeRepo = attendeeRepo;
        this.feedback = feedback;
    }

    public void getAccountName(Intent data) {

        /* Get account name */
        String accountName =
                Objects.requireNonNull(data.getExtras()).getString(AccountManager.KEY_ACCOUNT_NAME);
        if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(activity.getString(R.string.pref_key_google_account_name), accountName);
            editor.apply();
        }
    }

    @Override
    protected String doInBackground(Void... params) {

        /* Getting Google account credential */
        credential = GoogleAccountCredential.usingOAuth2(activity,
                Collections.singleton(CalendarScopes.CALENDAR));
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        Log.d(tag, "Google account name: " + sharedPref.getString(activity.getString(R.string.pref_key_google_account_name),
                null));
        credential.setSelectedAccountName(sharedPref.getString(activity.getString(R.string.pref_key_google_account_name),
                null));

        /* Getting Google Calendar client */
        com.google.api.services.calendar.Calendar client =
                new com.google.api.services.calendar.Calendar.Builder(transport, jsonFactory,
                        credential).setApplicationName(activity.getString(R.string.app_name)).build();

        /* Asking user for choosing Google account */
        if (credential.getSelectedAccountName() == null) {
            activity.startActivityForResult(credential.newChooseAccountIntent(),
                    ACCOUNT_REQUEST_CODE);
            retry = true;
            return null;
        }

        /* Asking user for permission if needed */
        try {
            Log.d(tag, "Token: " + GoogleAuthUtil.getToken(activity.getApplicationContext(),
                    credential.getSelectedAccount(), credential.getScope()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } catch (UserRecoverableAuthException e) {
            Log.d(tag, "Need permission");
            activity.startActivityForResult(e.getIntent(),PERMISSION_REQUEST_CODE);
            retry = true;
            return null;
        } catch (GoogleAuthException e) {
            e.printStackTrace();
            return null;
        }
        Log.d(tag, "Permission OK");

        Event event = new Event()
                .setSummary(meeting.name)
                .setLocation(meeting.location)
                .setDescription(meeting.notes);

        /* Parsing the meeting date */
        SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy-HH:mm", Locale.ENGLISH);
        DateTime startDateTime;
        try {
            startDateTime = new DateTime(dateFormatter.parse(meeting.date
                    + "-" + meeting.time).getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        Log.d(tag, "Parsing meeting date OK");

        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime);
        event.setStart(start);

        EventDateTime end = new EventDateTime()
                .setDateTime(startDateTime);
        event.setEnd(end);

        /* Setting attendees list */
        ArrayList<Integer> ids = attendToRepo.getAttendeeIDs(meeting.meeting_ID);
        ArrayList<EventAttendee> attendees = new ArrayList<>();
        for (int id : ids){
            Attendee attendee = attendeeRepo.getAttendeeById(id);
            EventAttendee eventAttendee = new EventAttendee();
            eventAttendee.setDisplayName(attendee.name);

            /* Email is mandatory for the API request */
            eventAttendee.setEmail(attendee.name + activity.getString(R.string.google_calendar_email_example));
            attendees.add(eventAttendee);
            Log.d(tag, "GoogleCalendarTask " + attendee.name + " added to attendees");
        }
        event.setAttendees(attendees);

        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(10),
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        String calendarId = "primary";
        try {
            event = client.events().insert(calendarId, event).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(tag, "Event created link: " + event.getHtmlLink());

        return event.getHtmlLink();
    }

    @Override
    protected void onPostExecute(String event) {
        Log.d(tag, "GoogleCalendarTask onPostExecute");

        if(!feedback && !retry && event!= null){

            /* Finishing editing activity */
            activity.finish();
            return;
        }

        if (event == null && !retry)
            Toast.makeText(activity, R.string.toast_google_calendar_sync_fail, Toast.LENGTH_SHORT).show();
        else if(event != null)
            Toast.makeText(activity, R.string.toast_google_calendar_sync_success,
                    Toast.LENGTH_SHORT).show();
    }
}
