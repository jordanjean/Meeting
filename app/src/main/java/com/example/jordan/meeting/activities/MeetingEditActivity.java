package com.example.jordan.meeting.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.jordan.meeting.R;
import com.example.jordan.meeting.components.UnrolledListView;
import com.example.jordan.meeting.database.AttendTo;
import com.example.jordan.meeting.database.Attendee;
import com.example.jordan.meeting.database.Meeting;
import com.example.jordan.meeting.repositories.AttendToRepo;
import com.example.jordan.meeting.repositories.AttendeeRepo;
import com.example.jordan.meeting.repositories.MeetingRepo;
import com.example.jordan.meeting.tasks.GoogleCalendarTask;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import static com.example.jordan.meeting.R.layout.edit_attendee_entry;
import static com.example.jordan.meeting.R.layout.spinner_attendee_entry;

public class MeetingEditActivity extends AppCompatActivity implements android.view.View.OnClickListener {

    ImageButton btnNewAttendee;

    TextView textDate;
    TextView textTime;

    EditText editTextName;
    EditText editTextAttendee;
    EditText editTextLocation;
    EditText editTextNotes;

    UnrolledListView attendeeListView;

    Spinner spnAttendees;

    CheckBox checkBoxGoogleCalendar;

    AttendeeRepo attendeeRepo;
    AttendToRepo attendToRepo;
    MeetingRepo meetingRepo;

    private int _Meeting_Id = 0;
    Meeting meeting;

    String returnKey = "";
    String newAttendee = "";
    String tag = "events";

    GoogleCalendarTask googleCalendarTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(tag, "MeetingEditActivity onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_edit);

        /* Setting toolbar_meeting_view */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();

        /* Enabling the Up button */
        Objects.requireNonNull(ab).setDisplayHomeAsUpEnabled(true);

        btnNewAttendee = findViewById(R.id.btnNewAttendee);

        editTextName = findViewById(R.id.editTextName);
        textDate = findViewById(R.id.textDate);
        textTime = findViewById(R.id.textTime);
        editTextAttendee = findViewById(R.id.editTextAttendee);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextNotes = findViewById(R.id.editTextNotes);
        attendeeListView = findViewById(R.id.attendeeList);
        spnAttendees = findViewById(R.id.spnAttendee);
        checkBoxGoogleCalendar = findViewById(R.id.checkboxGoogleCalendar);

        btnNewAttendee.setOnClickListener(this);
        textDate.setOnClickListener(this);
        textTime.setOnClickListener(this);

        attendeeRepo = new AttendeeRepo(this);
        attendToRepo = new AttendToRepo(this);
        meetingRepo = new MeetingRepo(this);

        /* Getting selected meeting */
        Intent intent = getIntent();
        _Meeting_Id = intent.getIntExtra("meeting_Id", 0);
        meeting = meetingRepo.getMeetingById(_Meeting_Id);

        /* Setting meeting info */
        editTextName.setText(meeting.name);
        editTextName.setHint(getString(R.string.hint_meeting_name));

        /* Pre-selecting current date for meeting date */
        if (_Meeting_Id != 0)
            textDate.setText(meeting.date);
        else {
            final Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormatter = new SimpleDateFormat(
                    "MM/dd/yyyy", Locale.ENGLISH);
            textDate.setText(dateFormatter.format(calendar.getTime()));
        }
        textDate.setHint(R.string.hint_date);
        textDate.setTextColor(getColor(R.color.colorBlack));

        /* Pre-selecting current time for meeting time */
        if (_Meeting_Id != 0)
            textTime.setText(meeting.time);
        else {
            final Calendar calendar = Calendar.getInstance();
            SimpleDateFormat timeFormatter = new SimpleDateFormat(
                    "HH:mm", Locale.ENGLISH);
            textTime.setText(timeFormatter.format(calendar.getTime()));
        }
        textTime.setHint(R.string.hint_time);
        textTime.setTextColor(getColor(R.color.colorBlack));
        editTextLocation.setText(meeting.location);
        editTextLocation.setHint(R.string.hint_location);
        editTextNotes.setText(meeting.notes);
        editTextNotes.setHint(R.string.hint_note);
        editTextAttendee.setHint(R.string.hint_new_attendee);

        /* Setting  list of all meetings attendee */
        ArrayList<HashMap<String, String>> allAttendeeList = attendeeRepo.getAttendeeList();
        HashMap<String, String> p0 = new HashMap<>();
        p0.put("name", getString(R.string.prompt_existing_attendee));
        allAttendeeList.add(0, p0);

        /* Setting Spinner adapter */
        SpinnerAdapter spinnerAdapter = new SimpleAdapter(this, allAttendeeList, spinner_attendee_entry,
                new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
        spnAttendees.setAdapter(spinnerAdapter);
        spnAttendees.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(tag, "Spinner position=" + position + " id=" + id);
                if (position != 0) {

                    /* Adding attendee to the meeting */
                    TextView tv = view.findViewById(R.id.attendee_Id);
                    int attendeeId = Integer.valueOf(tv.getText().toString());
                    addAttendee(attendeeId);

                    /* Printing hint */
                    spnAttendees.setSelection(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(tag, "Spinner nothing selected");
            }
        });

        /* Setting meeting attendees list */
        attendeeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                /* Remove attendee from the meeting */
                deleteAttendee(parent, view);
            }
        });
        ArrayList<Integer> idList = attendToRepo.getAttendeeIDs(_Meeting_Id);
        ArrayList<HashMap<String, String>> attendeeList = new ArrayList<>();
        for(int id : idList){
            HashMap<String, String> attendeeMap = new HashMap<>();
            attendeeMap.put("id", String.valueOf(attendeeRepo.getAttendeeById(id).attendee_ID));
            attendeeMap.put("name", attendeeRepo.getAttendeeById(id).name);
            attendeeList.add(attendeeMap);
        }

        /* Setting ListView adapter */
        ListAdapter listAdapter = new SimpleAdapter(this, attendeeList, edit_attendee_entry,
                new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
        attendeeListView.setAdapter(listAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Inflating the menu */
        getMenuInflater().inflate(R.menu.toolbar_meeting_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_save:
                Log.d(tag, "Action save");
                meeting = new Meeting();
                if (editTextName.getText().toString().equals(""))
                    meeting.name = "Meeting Name";
                else
                    meeting.name = editTextName.getText().toString();
                meeting.time = textTime.getText().toString();
                meeting.date = textDate.getText().toString();
                meeting.location = editTextLocation.getText().toString();
                meeting.notes = editTextNotes.getText().toString();
                meeting.meeting_ID = _Meeting_Id;

                if (_Meeting_Id == 0) {

                    /* Create new meeting */
                    _Meeting_Id = meetingRepo.insert(meeting);
                    meeting.meeting_ID = _Meeting_Id;

                    /* Moving attendees from temporary meeting to the new meeting */
                    ArrayList<Integer> attendeeIds = attendToRepo.getAttendeeIDs(0);
                    for (int id : attendeeIds){
                        AttendTo attendTo = new AttendTo();
                        attendTo.attendee_ID = id;
                        attendTo.meeting_ID = _Meeting_Id;
                        Log.d(tag, "Moving attendee " + id + " to meeting " + _Meeting_Id + " from meeting temp");
                        attendToRepo.insert(attendTo);
                        attendToRepo.delete(id, 0);
                    }
                    returnKey = getString(R.string.return_key_insert);
                } else {

                    /* Updating meeting */
                    meetingRepo.update(meeting);
                    returnKey = getString(R.string.return_key_update);
                }

                /* Adding event to Google Calendar */
                if (checkBoxGoogleCalendar.isChecked()){
                    googleCalendarTask = new GoogleCalendarTask(meeting,
                            this, attendToRepo, attendeeRepo, false);
                    googleCalendarTask.execute();
                } else {
                    finish();
                }

                return true;

            case R.id.action_delete:
                deleteMeeting(_Meeting_Id);
                returnKey = getString(R.string.return_key_delete);
                finish();
                return  true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onClick(View view) {
        Log.d(tag, "MeetingEditActivity onClick");
        final Calendar calendar = Calendar.getInstance();
        switch (view.getId()) {
            case R.id.btnNewAttendee:
                newAttendee = editTextAttendee.getText().toString();

                /* Create new attendee if this one does not exist yet */
                int attendeeID;
                int temp = attendeeRepo.getAttendeeByName(newAttendee).attendee_ID;
                Log.d(tag, "Search result: " + temp);
                if (temp == -1) {

                    /* The attendee does not exist yet */
                    Attendee attendee = new Attendee();
                    attendee.name = newAttendee;
                    attendeeID = attendeeRepo.insert(attendee);
                    attendee.attendee_ID = attendeeID;

                    /* Refreshing spinner */
                    ArrayList<HashMap<String, String>> allAttendeeList = attendeeRepo.getAttendeeList();
                    HashMap<String, String> p0 = new HashMap<>();
                    p0.put("name", getString(R.string.prompt_existing_attendee));
                    allAttendeeList.add(0, p0);
                    SpinnerAdapter spinnerAdapter = new SimpleAdapter(this, allAttendeeList, spinner_attendee_entry,
                            new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
                    spnAttendees.setAdapter(spinnerAdapter);
                }else{

                    /* The attendee already exists in the Attendee table */
                    attendeeID = temp;
                }

                /* Add the new attendee to this meeting */
                addAttendee(attendeeID);
                editTextAttendee.setText("");
                break;

            case R.id.textDate:

                /* Printing user feedback */
                textDate.setTextColor(getColor(R.color.colorAccent));

                /* Show date picker dialog */
                DatePickerDialog dateDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Log.d(tag, "Date set: " + monthOfYear + "/" + dayOfMonth + "/" + year);
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(year, monthOfYear, dayOfMonth);
                        SimpleDateFormat dateFormatter = new SimpleDateFormat(
                                "MM/dd/yyyy", Locale.ENGLISH);
                        textDate.setText(dateFormatter.format(newDate.getTime()));
                        meeting.date = dateFormatter.format(newDate.getTime());
                        textDate.setTextColor(getColor(R.color.colorBlack));
                    }

                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                dateDialog.setOnCancelListener(new DatePickerDialog.OnCancelListener(){

                    @Override
                    public void onCancel(DialogInterface dialog) {

                        /* Resetting user feedback */
                        textDate.setTextColor(getColor(R.color.colorBlack));
                    }
                });

                dateDialog.show();
                break;

            case R.id.textTime:

                /* Printing user feedback */
                textTime.setTextColor(getColor(R.color.colorAccent));

                /* Show time picker dialog */
                TimePickerDialog timeDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {

                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Log.d(tag, "Time set: " + hourOfDay + ":" + minute);
                        Calendar newDate = Calendar.getInstance();
                        newDate.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH), hourOfDay, minute, 0);
                        SimpleDateFormat timeFormatter = new SimpleDateFormat(
                                "HH:mm", Locale.ENGLISH);
                        textTime.setText(timeFormatter.format(newDate.getTime()));
                        meeting.time = timeFormatter.format(newDate.getTime());
                        textTime.setTextColor(getColor(R.color.colorBlack));
                    }

                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);

                timeDialog.setOnCancelListener(new TimePickerDialog.OnCancelListener() {

                    @Override
                    public void onCancel(DialogInterface dialog) {

                        /* Resetting user feedback */
                        textTime.setTextColor(getColor(R.color.colorBlack));

                    }
                });

                timeDialog.show();
                break;
        }
    }

    private void deleteMeeting(int meetingId) {

        /* Updating AttendTo database */
        ArrayList<Integer> attendeeIds = attendToRepo.getAttendeeIDs(meetingId);
        for (int id : attendeeIds){
            Log.d(tag, "Removing attendee " + id + " from meeting " + meetingId);
            attendToRepo.delete(id, meetingId);

            /* Deleting the attendee if this one does not attend any meeting any more */
            if (attendToRepo.getMeetingIDs(id).size() == 0)
                attendeeRepo.delete(id);
        }

        /* Removing the meeting */
        meetingRepo.delete(meetingId);
    }

    private void addAttendee(int attendeeId) {

        /* Updating AttendTo database table */
        if (!attendToRepo.getMeetingIDs(attendeeId).contains(_Meeting_Id)) {
            Log.d(tag, "Insert attendee " + attendeeId + " to meeting " + _Meeting_Id);
            AttendTo attendTo = new AttendTo();
            attendTo.meeting_ID = _Meeting_Id;
            attendTo.attendee_ID = attendeeId;
            attendToRepo.insert(attendTo);
        }

        /* Updating attendees list */
        ArrayList<Integer> idList = attendToRepo.getAttendeeIDs(_Meeting_Id);
        ArrayList<HashMap<String, String>> attendeeList = new ArrayList<>();
        for(int id : idList){
            HashMap<String, String> attendeeMap = new HashMap<>();
            attendeeMap.put("id", String.valueOf(attendeeRepo.getAttendeeById(id).attendee_ID));
            attendeeMap.put("name", attendeeRepo.getAttendeeById(id).name);
            attendeeList.add(attendeeMap);
        }

        /* Refreshing ListView */
        ListAdapter adapter = new SimpleAdapter(this, attendeeList, edit_attendee_entry,
                new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
        attendeeListView.setAdapter(adapter);
    }

    private void deleteAttendee(AdapterView<?> parent, View view) {
        Log.d(tag, "MeetingEditActivity deleteAttendee");
        String attendeeName;

        /* Updating AttendTo table */
        TextView textAttendeeId = view.findViewById(R.id.attendee_Id);
        int attendeeId = Integer.valueOf(textAttendeeId.getText().toString());
        attendeeName = attendeeRepo.getAttendeeById(attendeeId).name;
        attendToRepo.delete(attendeeId, _Meeting_Id);

        /* Delete the attendee if this one does not attend any meeting */
        if (attendToRepo.getMeetingIDs(attendeeId).size() == 0) {
            attendeeRepo.delete(attendeeId);

            /* Updating attendee list */
            ArrayList<HashMap<String, String>> allAttendeeList = attendeeRepo.getAttendeeList();
            HashMap<String, String> p0 = new HashMap<>();
            p0.put("name", getString(R.string.prompt_existing_attendee));
            allAttendeeList.add(0, p0);
            SpinnerAdapter spinnerAdapter = new SimpleAdapter(this, allAttendeeList, spinner_attendee_entry,
                    new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
            spnAttendees.setAdapter(spinnerAdapter);
        }

        /* Updating attendee list */
        ArrayList<Integer> idList = attendToRepo.getAttendeeIDs(_Meeting_Id);
        ArrayList<HashMap<String, String>> attendeeList = new ArrayList<>();
        for(int i : idList){
            HashMap<String, String> attendeeMap = new HashMap<>();
            attendeeMap.put("id", String.valueOf(attendeeRepo.getAttendeeById(i).attendee_ID));
            attendeeMap.put("name", attendeeRepo.getAttendeeById(i).name);
            attendeeList.add(attendeeMap);
        }

        /* Updating meeting attendees list */
        ListAdapter adapter = new SimpleAdapter(parent.getContext(), attendeeList, edit_attendee_entry,
                new String[]{"id", "name"}, new int[]{R.id.attendee_Id, R.id.attendee_name});
        attendeeListView.setAdapter(adapter);

        /* User feedback */
        Toast.makeText(parent.getContext(), attendeeName + getString(R.string.feedback_attendee_removed),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d(tag, "MeetingEditActivity onActivityResult");
        if (resultCode == RESULT_OK){
            switch (requestCode){

                case GoogleCalendarTask.ACCOUNT_REQUEST_CODE:
                    Log.d(tag, "User requested for account");

                    /* Getting Google account name */
                    googleCalendarTask.getAccountName(data);

                    /* User has been asked for account, let's try again */
                    if (googleCalendarTask.retry) {
                        googleCalendarTask = new GoogleCalendarTask(meeting, this,
                                attendToRepo, attendeeRepo, false);
                        googleCalendarTask.execute();
                    }
                    break;

                case GoogleCalendarTask.PERMISSION_REQUEST_CODE:
                    Log.d(tag, "User requested for permission");

                    /* User has been asked for permission, let's try again */
                    if (googleCalendarTask.retry) {
                        googleCalendarTask = new GoogleCalendarTask(meeting, this,
                                attendToRepo, attendeeRepo, false);
                        googleCalendarTask.execute();
                    }
                    break;
            }
        }
    }

    @Override
    public void finish(){
        Log.d(tag, "MeetingEditActivity finish");

        Intent data = new Intent();

        if (!returnKey.isEmpty()) {
            data.putExtra("returnKey", returnKey);
        }

        setResult(RESULT_OK, data);

        /* Deleting temporary meeting data */
        if (_Meeting_Id == 0)
            deleteMeeting(_Meeting_Id);

        super.finish();
    }
}
