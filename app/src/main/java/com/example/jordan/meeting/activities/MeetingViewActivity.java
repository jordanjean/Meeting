package com.example.jordan.meeting.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jordan.meeting.R;
import com.example.jordan.meeting.components.UnrolledListView;
import com.example.jordan.meeting.database.Meeting;
import com.example.jordan.meeting.repositories.AttendToRepo;
import com.example.jordan.meeting.repositories.AttendeeRepo;
import com.example.jordan.meeting.repositories.MeetingRepo;
import com.example.jordan.meeting.tasks.GoogleCalendarTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static com.example.jordan.meeting.R.layout.view_attendee_entry;

public class MeetingViewActivity extends AppCompatActivity implements android.view.View.OnClickListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener{

    private static final int MAPS_REQUEST_CODE = 3;
    private static final int SETTINGS_REQUEST_CODE = 4;
    public static final int MEETING_EDIT_REQUEST_CODE = 5;

    private static final int SWIPE_THRESHOLD = 150;
    private static final int SWIPE_VELOCITY_THRESHOLD = 150;

    private String tag = "events";
    private int _Meeting_Id;
    private Meeting meeting;

    public MeetingRepo meetingRepo;
    public AttendToRepo attendToRepo;
    public AttendeeRepo attendeeRepo;

    UnrolledListView attendeeListView;

    TextView textName;
    TextView textDate;
    TextView textTime;
    TextView textLocation;
    TextView textNotes;
    private SharedPreferences sharedPref;
    private float fontSize;
    private int fontColor;

    GoogleCalendarTask googleCalendarTask;

    private GestureDetectorCompat gestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(tag, "MeetingViewActivity onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_view);

        /* Setting toolbar_meeting_view */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        /* Enabling the Up button */
        Objects.requireNonNull(ab).setDisplayHomeAsUpEnabled(true);

        /* Get preferences */
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        /* Initializing repositories */
        meetingRepo = new MeetingRepo(this);
        attendToRepo = new AttendToRepo(this);
        attendeeRepo = new AttendeeRepo(this);

        /* Getting meeting */
        _Meeting_Id = 0;
        Intent intent = getIntent();
        _Meeting_Id = intent.getIntExtra("meeting_Id", 0);
        meeting = meetingRepo.getMeetingById(_Meeting_Id);

        /* Retrieving fields */
        attendeeListView = findViewById(R.id.attendeeList);
        textName = findViewById(R.id.textName);
        textDate = findViewById(R.id.textDate);
        textTime = findViewById(R.id.textTime);
        textLocation = findViewById(R.id.textLocation);
        textNotes = findViewById(R.id.textNotes);

        /* Making ListView items non clickable */
        attendeeListView.setEnabled(false);
        attendeeListView.setOnItemClickListener(null);

        /* Click listener */
        textLocation.setOnClickListener(this);

        /* Setting fields */
        refresh();

        /* Setting gesture detector */
        gestureDetector = new GestureDetectorCompat(this,this);
        gestureDetector.setOnDoubleTapListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event){
        super.dispatchTouchEvent(event);

        /* Catching touch events */
        return this.onTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if (this.gestureDetector.onTouchEvent(event)) {
            return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        float deltaY = event2.getY() - event1.getY();
        float deltaX = event2.getX() - event1.getX();

        /* Checking distance and velocity thresholds */
        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            if (Math.abs(deltaX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (deltaX > 0) {

                    /* Finishing if swiping left */
                    finish();
                } else {
                    Log.d(tag, "Swiping right");
                }
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflating the menu */
        getMenuInflater().inflate(R.menu.toolbar_meeting_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Log.d(tag, "Action settings");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, SETTINGS_REQUEST_CODE);
                return true;

            case R.id.action_edit:
                Log.d(tag, "Action edit");
                Intent indent = new Intent(getApplicationContext(), MeetingEditActivity.class);
                indent.putExtra("meeting_Id", _Meeting_Id);
                startActivityForResult(indent, MEETING_EDIT_REQUEST_CODE);
                return true;

            case R.id.action_google_calendar:
                Log.d(tag, "Action sync calendar");

                /* Initializing connection to Google API */
                googleCalendarTask = new GoogleCalendarTask(meeting, this,
                        attendToRepo, attendeeRepo, true);

                /* Adding event to Google calendar */
                googleCalendarTask.execute();

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(tag, "MeetingViewActivity onActivityResult");
        if (resultCode == RESULT_OK){
            switch (requestCode){

                case MEETING_EDIT_REQUEST_CODE:
                    if (data.hasExtra("returnKey")) {
                        String returnKey = Objects.requireNonNull(data.getExtras()).getString("returnKey");

                        /* Finishing if the meeting has been deleted */
                        if (Objects.requireNonNull(returnKey).equals(getString(R.string.return_key_delete))) {

                            /* User feedback */
                            Toast.makeText(this, returnKey, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    /* Refreshing meeting view */
                    refresh();
                    break;

                case MAPS_REQUEST_CODE:
                    Log.d(tag, "Coming back from MapActivity");

                    /* Resetting feedback */
                    refresh();
                    break;

                case GoogleCalendarTask.ACCOUNT_REQUEST_CODE:
                    Log.d(tag, "User requested for account");

                    /* Getting Google account name */
                    googleCalendarTask.getAccountName(data);

                    /* User has been asked for account, let's try again */
                    if (googleCalendarTask.retry) {
                        googleCalendarTask = new GoogleCalendarTask(meeting, this,
                                attendToRepo, attendeeRepo, true);
                        googleCalendarTask.execute();
                    }
                    break;

                case GoogleCalendarTask.PERMISSION_REQUEST_CODE:
                    Log.d(tag, "User requested for permission");

                    /* User has been asked for permission, let's try again */
                    if (googleCalendarTask.retry) {
                        googleCalendarTask = new GoogleCalendarTask(meeting, this,
                                attendToRepo, attendeeRepo, true);
                        googleCalendarTask.execute();
                    }
                    break;

                case SETTINGS_REQUEST_CODE:
                    Log.d(tag, "Outing settings");

                    /* Refreshing */
                    refresh();
                    break;
            }
        } else {
            Log.d(tag, "result code not OK");

            /* Refreshing */
            refresh();
        }
    }

    private void refresh() {
        Log.d(tag, "MeetingViewActivity refresh");

        /* Updating attendees list */
        ArrayList<Integer> idList = attendToRepo.getAttendeeIDs(_Meeting_Id);
        ArrayList<HashMap<String, String>> attendeeList = new ArrayList<>();
        for(int id : idList){
            HashMap<String, String> attendeeMap = new HashMap<>();
            attendeeMap.put("id", String.valueOf(attendeeRepo.getAttendeeById(id).attendee_ID));
            attendeeMap.put("name", attendeeRepo.getAttendeeById(id).name);
            attendeeList.add(attendeeMap);
        }

        /* Getting preferences */
        String prefFontSize = sharedPref.getString("fontSize", "-1");
        final String prefFontColor = sharedPref.getString("fontColor", "-1");
        Log.d(tag, "updateFontStyle size: " + prefFontSize + " color " + prefFontColor);
        fontSize = Float.valueOf(prefFontSize);

        switch (Integer.valueOf(prefFontColor)){
            case 1:
                fontColor = getResources().getColor(R.color.colorBlack, null);
                break;

            case 2:
                fontColor = getResources().getColor(R.color.colorBlue, null);
                break;

            case 3:
                fontColor = getResources().getColor(R.color.colorGrey, null);
                break;

            default:
                fontColor = Color.BLACK;
        }

        /* Refreshing ListView */
        ListAdapter adapter = new SimpleAdapter(this, attendeeList, view_attendee_entry,
                new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name}){
            @Override
            public View getView(int position, View convertView, ViewGroup parent){
                View view = super.getView(position, convertView, parent);

                TextView tv_name = view.findViewById(R.id.attendee_name);

                /* Updating font size */
                tv_name.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);

                /* Updating font color */
                tv_name.setTextColor(fontColor);

                return view;
            }
        };
        attendeeListView.setAdapter(adapter);

        /* Refreshing fields */
        meeting = meetingRepo.getMeetingById(_Meeting_Id);
        textName.setText(meeting.name);
        textName.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);
        textName.setTextColor(fontColor);
        textDate.setText(meeting.date);
        textDate.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);
        textDate.setTextColor(fontColor);
        textTime.setText(meeting.time);
        textTime.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);
        textTime.setTextColor(fontColor);
        textLocation.setText(meeting.location);
        textLocation.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);
        textLocation.setTextColor(fontColor);
        textLocation.getPaint().setUnderlineText(true);
        textNotes.setTextSize(TypedValue.COMPLEX_UNIT_PT,fontSize);
        textNotes.setTextColor(fontColor);
        textNotes.setText(meeting.notes);
    }

    @Override
    public void onClick(View v) {
        Log.d(tag, "MeetingViewActivity onClick");
        switch (v.getId()) {
            case R.id.textLocation:

                /* Triggering feedback */
                textLocation.setTextColor(getColor(R.color.colorAccent));

                Intent indent = new Intent(getApplicationContext(), MapActivity.class);
                indent.putExtra("meeting_Id", _Meeting_Id);
                startActivityForResult(indent, MAPS_REQUEST_CODE);
        }
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public void finish(){
        Log.d(tag, "MeetingViewActivity finish");
        setResult(RESULT_OK, new Intent());
        super.finish();
    }
}
