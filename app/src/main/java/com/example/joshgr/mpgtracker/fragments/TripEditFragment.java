package com.example.joshgr.mpgtracker.fragments;


import android.app.DatePickerDialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.example.joshgr.mpgtracker.R;
import com.example.joshgr.mpgtracker.data.Trip;
import com.example.joshgr.mpgtracker.data.TripViewModel;
import com.example.joshgr.mpgtracker.data.TripsDatabase;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

public class TripEditFragment extends BaseFragment {

    private TripViewModel mTripViewModel;
    private TextView mDatePickerText;
    private Calendar mCalendar;
    private DatePickerDialog.OnDateSetListener mDate;
    private int mId = -1;
    private boolean mCreateMode = false;

    public TripEditFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trip_edit, container, false);

        // Get ViewModel
        mTripViewModel = ViewModelProviders.of(getActivity()).get(TripViewModel.class);

        return view;
    }

    @Override
    protected String getTitle() {
        if(mCreateMode){ return getResources().getString(R.string.create_title); }
        else { return getResources().getString(R.string.edit_title); }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if(mTripViewModel.getSelectedTrip().getValue() == null){
            mCreateMode = true;
        }
        super.onViewCreated(view, savedInstanceState);

        //Set up date picker
        mCalendar = Calendar.getInstance();
        mDatePickerText = (TextView) view.findViewById(R.id.datePicker);
        this.initDatePicker();

        //Set up fields if editing existing trip
        if (!mCreateMode) {
            Trip trip = mTripViewModel.getSelectedTrip().getValue();
//            double cost = getArguments().getDouble("cost");
//            double miles = getArguments().getDouble("miles");
//            double gallons = getArguments().getDouble("gallons");
//            double odometer = getArguments().getDouble("odometer");
//            boolean filledTank = getArguments().getBoolean("filledTank");
//            String date = getArguments().getString("date");
            mId = trip.getId();

            ((TextView)view.findViewById(R.id.datePicker)).setText(trip.getFormattedDate());
            ((EditText)view.findViewById(R.id.milesEditText)).setText(String.format(Double.toString(trip.getMiles()), "%.1f"));
            ((EditText)view.findViewById(R.id.gallonsEditText)).setText(String.format(Double.toString(trip.getGallons()), "%.3f"));
            ((EditText)view.findViewById(R.id.costEditText)).setText(String.format(Double.toString(trip.getTripCost()), "%.2f"));
            ((EditText)view.findViewById(R.id.odometerEditText)).setText(String.format(Double.toString(trip.getOdometer()), "%.1f"));
            ((CheckBox)view.findViewById(R.id.filledTankCheckBox)).setChecked(trip.getFilledTank());
        }
        else {
            //Set filledTankCheckBox to preference setting
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            boolean filledTankPref = prefs.getBoolean("default_tank_filled", true);
            ((CheckBox)view.findViewById(R.id.filledTankCheckBox)).setChecked(filledTankPref);
        }

        Button saveButton = (Button) view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTrip((ViewGroup) v.getParent());
            }
        });

        Button cancelButton = (Button) view.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        mTripViewModel.clearSelectedTrip();
    }

    private void saveTrip(View view){

        // Validate form
        final Trip trip = validateTrip(view);
        if(trip == null){
            return;
        }

        // This was previously being run in a background thread which caused issues
        // when this fragment was popped and the TripListFragment resumed, fetching all
        // trip data points while this new one was being written to the db.
        // TODO: Need to update db handling for multithreading
        //TripsDatabase db = TripsDatabase.getTripsDatabase(getContext());
        if(mCreateMode){
            //db.tripDAO().insertTrip(trip);
            mTripViewModel.insert(trip);
        }
        else{
            //db.tripDAO().updateTrip(trip);
            trip.id = mId;
            mTripViewModel.update(trip);
        }

        getActivity().getSupportFragmentManager().popBackStack();
    }

    private Trip validateTrip(View view){
        EditText milesEditText = (EditText)view.findViewById(R.id.milesEditText);
        EditText costEditText = (EditText)view.findViewById(R.id.costEditText);
        EditText gallonsEditText = (EditText)view.findViewById(R.id.gallonsEditText);
        EditText odometerEditText = (EditText)view.findViewById(R.id.odometerEditText);
        CheckBox filledTankCheckBox = (CheckBox)view.findViewById(R.id.filledTankCheckBox);

        String dateString = ((TextView)view.findViewById(R.id.datePicker)).getText().toString();
        Date date = parseDate(dateString);
        double miles = validateEditTextDouble(milesEditText, getResources().getString(R.string.miles_missing), getResources().getString(R.string.miles_correct_format));
        double cost = validateEditTextDouble(costEditText, getResources().getString(R.string.cost_missing), getResources().getString(R.string.cost_correct_format));
        double gallons = validateEditTextDouble(gallonsEditText, getResources().getString(R.string.gallons_missing), getResources().getString(R.string.gallons_correct_format));
        double odometer = validateEditTextDouble(odometerEditText, getResources().getString(R.string.odometer_missing), getResources().getString(R.string.odometer_correct_format));
        boolean filledTank = filledTankCheckBox.isChecked();

        if(miles <= 0 || cost <= 0 || gallons <= 0 || odometer <= 0){
            return null;
        }
        else{
            return new Trip(date, gallons, miles, cost, odometer, filledTank);
        }
    }

    private double validateEditTextDouble(EditText editText, String missingMessage, String correctFormatMessage){
        double value = 0;
        try {
            value = Double.parseDouble(editText.getText().toString());
            if(value <= 0){
                editText.setError(correctFormatMessage);
            }
        } catch (java.lang.NumberFormatException e) {
            editText.setError(missingMessage);
        }

        return value;
    }

    private void initDatePicker(){
        if(mDatePickerText.getText() == ""){
            this.updateDateLabel();
        }
        mDate = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                mCalendar.set(Calendar.YEAR, year);
                mCalendar.set(Calendar.MONTH, month);
                mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                updateDateLabel();
            }
        };

        mDatePickerText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(TripEditFragment.this.getContext(), mDate, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });
    }

    private void updateDateLabel(){
        mDatePickerText.setText(Trip.DATE_FORMAT.format(mCalendar.getTime()));
    }

    private Date parseDate(String date) {
        try {
            return Trip.DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
