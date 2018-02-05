package com.example.joshgr.mpgtracker.fragments;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.example.joshgr.mpgtracker.helpers.MpgDbHelper;
import com.example.joshgr.mpgtracker.R;
import com.example.joshgr.mpgtracker.data.TripDataItem;

import java.util.Calendar;

public class TripEditFragment extends BaseFragment {

    TextView mDatePickerText;
    Calendar mCalendar;
    DatePickerDialog.OnDateSetListener mDate;
    int mId = -1;
    boolean mCreateMode = false;

    public TripEditFragment() {

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_trip_edit, container, false);

        return view;
    }

    @Override
    protected String getTitle() {
        if(mCreateMode){ return "Create Trip"; }
        else { return "Edit Trip"; }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        if(getArguments() == null){
            mCreateMode = true;
        }
        super.onViewCreated(view, savedInstanceState);

        //Set up date picker
        mCalendar = Calendar.getInstance();
        mDatePickerText = (TextView) view.findViewById(R.id.datePicker);
        this.initDatePicker();

        //Set up fields if editing existing trip
        if (!mCreateMode) {
            double cost = getArguments().getDouble("cost");
            double miles = getArguments().getDouble("miles");
            double gallons = getArguments().getDouble("gallons");
            double odometer = getArguments().getDouble("odometer");
            boolean filledTank = getArguments().getBoolean("filledTank");
            String date = getArguments().getString("date");
            mId = getArguments().getInt("id");

            ((TextView)view.findViewById(R.id.datePicker)).setText(date);
            ((EditText)view.findViewById(R.id.milesEditText)).setText(String.format(Double.toString(miles), "%.1f"));
            ((EditText)view.findViewById(R.id.gallonsEditText)).setText(String.format(Double.toString(gallons), "%.3f"));
            ((EditText)view.findViewById(R.id.costEditText)).setText(String.format(Double.toString(cost), "%.2f"));
            ((EditText)view.findViewById(R.id.odometerEditText)).setText(String.format(Double.toString(odometer), "%.1f"));
            ((CheckBox)view.findViewById(R.id.filledTankCheckBox)).setChecked(filledTank);
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

    private void saveTrip(View view){

        // Validate form
        final TripDataItem trip = validateTrip(view);
        if(trip == null){
            return;
        }

        // This was previously being run in a background thread which caused issues
        // when this fragment was popped and the TripListFragment resumed, fetching all
        // trip data points while this new one was being written to the db.
        // TODO: Need to update db handling for multithreading
        MpgDbHelper db = new MpgDbHelper(getContext());
        if(mId == -1){
            db.addTrip(trip);
        }
        else{
            db.updateTrip(trip, mId);
        }

        getActivity().getSupportFragmentManager().popBackStack();
    }

    private TripDataItem validateTrip(View view){
        EditText milesEditText = (EditText)view.findViewById(R.id.milesEditText);
        EditText costEditText = (EditText)view.findViewById(R.id.costEditText);
        EditText gallonsEditText = (EditText)view.findViewById(R.id.gallonsEditText);
        EditText odometerEditText = (EditText)view.findViewById(R.id.odometerEditText);
        CheckBox filledTankCheckBox = (CheckBox)view.findViewById(R.id.filledTankCheckBox);

        String date = ((TextView)view.findViewById(R.id.datePicker)).getText().toString();
        double miles = validateEditTextDouble(milesEditText, "Distance required.", "Invalid miles value. Must be greater than 0.");
        double cost = validateEditTextDouble(costEditText, "Cost required.", "Invalid cost value. Must be greater than 0.");
        double gallons = validateEditTextDouble(gallonsEditText, "Gallons required.", "Invalid gallons value. Must be greater than 0.");
        double odometer = validateEditTextDouble(odometerEditText, "Odometer reading required.", "Invalid odometer value. Must be greater than 0.");
        boolean filledTank = filledTankCheckBox.isChecked();

        if(miles <= 0 || cost <= 0 || gallons <= 0 || odometer <= 0){
            return null;
        }
        else{
            return new TripDataItem(0, date, gallons, miles, cost, odometer, filledTank);
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
        mDatePickerText.setText(TripDataItem.DATE_FORMAT.format(mCalendar.getTime()));
    }
}
