package io.github.cawfree.dtw;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.cawfree.dtw.alg.DTW;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    /** Converts a List of Floats into a primitive equivalent. */
    private static final float[] primitive(final List<Float> pList) {
        // Declare the Array.
        final float[] lT = new float[pList.size()];
        // Iterate the List.
        for(int i = 0; i < pList.size(); i++) {
            // Buffer the Element.
            lT[i] = pList.get(i);
        }
        // Return the Array.
        return lT;
    }

    /** Colors an Array of DataSets. */
    private static final void color(final LineDataSet[] pLineDataSets, final int[] pColor) {
        // Iterate.
        for(int i = 0; i < pLineDataSets.length; i++) {
            // Update the color of the LineDataSet.
            MainActivity.color(pLineDataSets[i], pColor[i]);
        }
    }

    /** Colors a LineDataSet. */
    private static final void color(final LineDataSet pLineDataSet, final int pColor) {
        // Update the Colors.
        pLineDataSet.setColor(pColor);
        pLineDataSet.setCircleColorHole(pColor);
        pLineDataSet.setCircleColor(pColor);
    }

    /** Abstracts graph updates. */
    private static class LineChartManager {

        /* Member Variables. */
        private final LineChart     mLineChart;
        private final LineDataSet[] mDataSets;
        private final int           mWindow;
        private final float[]       mBuffer;
        private       int           mOffset;

        /** Constructor. */
        public LineChartManager(final LineChart pLineChart, final int pWindow, final LineDataSet ... pDataSets) {
            // Initialize Member Variables.
            this.mLineChart = pLineChart;
            this.mDataSets  = pDataSets;
            this.mWindow    = pWindow;
            this.mBuffer    = new float[pDataSets.length];
            this.mOffset    = 0;
        }

        /** Updates the Chart. */
        public final void onUpdateChart(final float ... pVertical) {
            // Increment the Offset.
            this.setOffset(this.getOffset() + 1);
            // Buffer the Averages.
            for(int i = 0; i < pVertical.length; i++) {
                // Accumulate.
                this.getBuffer()[i] += pVertical[i];
            }
            // Have we reached the window length?
            if(this.getOffset() % this.getWindow() == 0) {
                // Perform an aggregated update.
                this.onAggregateUpdate(this.getBuffer());
                // Clear the Buffer.
                Arrays.fill(this.getBuffer(), 0.0f);
            }
        }

        /** Called when the number of samples displayed on the graph have satisfied the window size. */
        public void onAggregateUpdate(final float[] pAggregate) {
            // Update the chart.
            for(int i = 0; i < this.getDataSets().length; i++) {
                // Calculate the Average.
                final float       lAverage      = this.getBuffer()[i] / this.getWindow();
                // Fetch the DataSet.
                final LineDataSet lLineDataSet  = this.getDataSets()[i];
                // Write this Value to the Aggregate for subclasses.
                                  pAggregate[i] = lAverage;
                // Remove the oldest element.
                lLineDataSet.removeFirst();
                // Buffer the Average.
                lLineDataSet.addEntry(new Entry(this.getOffset(), lAverage));
            }
            // Invalidate the Graph. (Ensure it is redrawn!)
            this.getLineChart().invalidate();
        }

        /* Getters. */
        private final LineChart getLineChart() {
            return this.mLineChart;
        }

        private final LineDataSet[] getDataSets() {
            return this.mDataSets;
        }

        private final int getWindow() {
            return this.mWindow;
        }

        private final float[] getBuffer() {
            return this.mBuffer;
        }

        public final void setOffset(final int pOffset) {
            this.mOffset = pOffset;
        }

        private final int getOffset() {
            return this.mOffset;
        }

    }

    /** Mode Definition. */
    private enum EMode {
        /** Defines when the app is recording motion data. */
        TRAINING,
        /** Defines when the app is attempting to recognize motion data. */
        RECOGNITION;
    }

    /* Empty Description. */
    private static final Description DESCRIPTION_NULL = new Description() { { this.setText(""); }};

    /* Tag Declaration. */
    private static final String TAG = "AndroidGeeExample";

    /* Chart Constants. */
    private static final int    LENGTH_CHART_HISTORY  = 64;
    private static final int    AVERAGE_WINDOW_LENGTH = 1;
    private static final int    DELAY_SENSOR          = SensorManager.SENSOR_DELAY_FASTEST;

    /* Member Variables. */
    private EMode         mMode;
    private boolean       mResponsive;
    private SensorManager mSensorManager;

    /* Feedback. */
    private RelativeLayout mFeedbackLayout;
    private ImageView      mFeedbackView;
    private RelativeLayout mObscureLayout;
    private TextView       mModeTitle;
    private TextView       mModeDescription;
    private Switch         mModeSwitch;

    /* Graphs. */
    private LineChart mLineAcc;
    private LineChart mLineTrain;
    private LineChart mLineRecognition;

    /* Data. */
    private LineData  mAccData;
    private LineData  mTrainData;
    private LineData  mRecognitionData;

    /* Datasets. */
    private LineDataSet[] mAcceleration;
    private LineDataSet[] mTraining;
    private LineDataSet[] mRecognition;

    /* Chart Managers. */
    private LineChartManager mAccChartManager;
    private LineChartManager mTrainChartManager;
    private LineChartManager mRecognitionChartManager;

    /* History Lists. */
    private List<Float>[] mTrainingHistory;
    private List<Float>[] mRecognitionHistory;

    /** Handle Creation of the Activity. */
    @Override protected final void onCreate(final Bundle pSavedInstanceState) {
        // Implement the Parent Definition.
        super.onCreate(pSavedInstanceState);
        // Set the Content View.
        this.setContentView(R.layout.activity_main);
        // Initialize Graphs.
        this.mLineAcc         = $(R.id.lc_acc);
        this.mLineTrain       = $(R.id.lc_train);
        this.mLineRecognition = $(R.id.lc_recognize);
        // Initialize Feedback.
        this.mFeedbackLayout  = $(R.id.rl_feedback);
        this.mFeedbackView    = $(R.id.iv_feedback);
        this.mObscureLayout   = $(R.id.rl_obscure);
        // Initialize UI.
        this.mModeTitle       = $(R.id.tv_mode);
        this.mModeDescription = $(R.id.tv_mode_desc);
        this.mModeSwitch      = $(R.id.sw_mode);
        // Configure the ColorFilter for the FeedbackView.
        this.getFeedbackView().setColorFilter(ContextCompat.getColor(this, R.color.colorAccent));
        // Prepare data.
        this.mAccData         = new LineData();
        this.mTrainData       = new LineData();
        this.mRecognitionData = new LineData();
        // Allocate the histories.
        this.mTrainingHistory    = new List[]{ new ArrayList(), new ArrayList(), new ArrayList() };
        this.mRecognitionHistory = new List[]{ new ArrayList(), new ArrayList(), new ArrayList() };

        // Register the Line Data Sources.
                this.getLineAcc().setData(this.getAccData());
              this.getLineTrain().setData(this.getTrainData());
        this.getLineRecognition().setData(this.getRecognitionData());

        // Enable AutoScaling.
                this.getLineAcc().setAutoScaleMinMaxEnabled(true);
              this.getLineTrain().setAutoScaleMinMaxEnabled(true);
        this.getLineRecognition().setAutoScaleMinMaxEnabled(true);

        // Hide the left axis for training and recognition.
              this.getLineTrain().getAxisLeft().setDrawLabels(false);
//        this.getLineRecognition().getAxisLeft().setDrawLabels(false);
        // Hide the right axis for training and recognition.
//              this.getLineTrain().getAxisRight().setDrawLabels(false);
        this.getLineRecognition().getAxisRight().setDrawLabels(false);

                this.getLineAcc().setDescription(MainActivity.DESCRIPTION_NULL);
              this.getLineTrain().setDescription(MainActivity.DESCRIPTION_NULL);
        this.getLineRecognition().setDescription(MainActivity.DESCRIPTION_NULL);

        // Allocate the Acceleration.
        this.mAcceleration = new LineDataSet[] { new LineDataSet(null, "X"), new LineDataSet(null, "Y"), new LineDataSet(null, "Z") };
        this.mTraining     = new LineDataSet[] { new LineDataSet(null, "X"), new LineDataSet(null, "Y"), new LineDataSet(null, "Z") };
        this.mRecognition  = new LineDataSet[] { new LineDataSet(null, "X"), new LineDataSet(null, "Y"), new LineDataSet(null, "Z") };

        // Initialize chart data.
        MainActivity.this.onInitializeData(this.getAcceleration(), MainActivity.LENGTH_CHART_HISTORY);
        MainActivity.this.onInitializeData(this.getTraining(),     MainActivity.LENGTH_CHART_HISTORY);
        MainActivity.this.onInitializeData(this.getRecognition(),  MainActivity.LENGTH_CHART_HISTORY);

        // Register the LineDataSets.
        for(final LineDataSet lLineDataSet : this.getAcceleration()) {         this.getAccData().addDataSet(lLineDataSet); }
        for(final LineDataSet lLineDataSet : this.getTraining())     {       this.getTrainData().addDataSet(lLineDataSet); }
        for(final LineDataSet lLineDataSet : this.getRecognition())  { this.getRecognitionData().addDataSet(lLineDataSet); }

        // Assert that the DataSet has changed, and that we'll be using threse three sources.
                this.getAccData().notifyDataChanged();
              this.getTrainData().notifyDataChanged();
        this.getRecognitionData().notifyDataChanged();

        // Color the DataSets.
        MainActivity.color(this.getAcceleration(), new int[]{ Color.RED,  Color.GREEN,   Color.BLUE });
        MainActivity.color(this.getTraining(),     new int[]{ Color.RED,  Color.GREEN,   Color.BLUE });
        MainActivity.color(this.getRecognition(),  new int[]{ Color.CYAN, Color.MAGENTA, Color.BLACK });

        // Declare the LineChartManager.
        this.mAccChartManager         = new LineChartManager(this.getLineAcc(),         MainActivity.AVERAGE_WINDOW_LENGTH, this.getAcceleration());
        // Declare the Training and Recognition update handling.
        this.mTrainChartManager       = new LineChartManager(this.getLineTrain(),       MainActivity.AVERAGE_WINDOW_LENGTH, this.getTraining()) { @Override public final void onAggregateUpdate(final float[] pAggregate) {
            // Update the graph. (This actually manipulates the buffer to compensate for averaging.)
            super.onAggregateUpdate(pAggregate);
            // Iterate the Averages.
            for(int i = 0; i < pAggregate.length; i++) {
                // Buffer the Value.
                MainActivity.this.getTrainingHistory()[i].add(Float.valueOf(pAggregate[i]));
            }
        } };
        // Declare Recognition Handling.
        this.mRecognitionChartManager = new LineChartManager(this.getLineRecognition(), MainActivity.AVERAGE_WINDOW_LENGTH, this.getRecognition()) { @Override public final void onAggregateUpdate(final float[] pAggregate) {
            // Update the graph. (Compute the averages and store the result in the aggregate.)
            super.onAggregateUpdate(pAggregate);
            // Iterate the Averages.
            for(int i = 0; i < pAggregate.length; i++) {
                // Buffer the Value.
                MainActivity.this.getRecognitionHistory()[i].add(Float.valueOf(pAggregate[i]));
            }
        } };

        // Define the startup mode.
        this.mMode          = EMode.TRAINING;
        // Define whether we're going to start processing motion data
        this.mResponsive    = false;
        // Fetch the SensorManager.
        this.mSensorManager = (SensorManager)this.getSystemService(SENSOR_SERVICE);
        // Listen for clicks on the Mode switch.
        this.getModeSwitch().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { @Override public final void onCheckedChanged(final CompoundButton pCompoundButton, final boolean pIsChecked) {
            // Update the training state.
            MainActivity.this.setMode(pIsChecked ? EMode.RECOGNITION : EMode.TRAINING);
            // Update the title and description.
                  MainActivity.this.getModeTitle().setText(pIsChecked ? R.string.mode_recognition      : R.string.mode_training);
            MainActivity.this.getModeDescription().setText(pIsChecked ? R.string.mode_recognition_desc : R.string.mode_training_desc);
        } });

        // Handle the ObscureLayout.
        this.getObscureLayout().setOnTouchListener(new View.OnTouchListener() { @Override public final boolean onTouch(final View pView, final MotionEvent pMotionEvent) {
            // Whilst the ObscureLayout is visible, obscure all touch data.
            return MainActivity.this.getObscureLayout().getVisibility() == View.VISIBLE;
        } });

        // Listen for Touch Events on the FeedbackLayout.
        this.getFeedbackLayout().setOnTouchListener(new View.OnTouchListener() { @Override public final boolean onTouch(final View pView, final MotionEvent pMotionEvent) {
            // Handle the MotionEvent.
            switch(pMotionEvent.getActionMasked()) {
                /** When the user touches down on the graphs... */
                case MotionEvent.ACTION_DOWN : {
                    // Disable the Switch.
                    MainActivity.this.getModeSwitch().setEnabled(false);
                    // Handle the Mode.
                    switch(MainActivity.this.getMode()) {
                        case TRAINING     : {
                            // Reset the Training History.
                            for(final List<Float> lTraining : MainActivity.this.getTrainingHistory()) {
                                // Clear the Training List.
                                lTraining.clear();
                            }
                            // Reset the Training Chart.
                            MainActivity.this.getTrainChartManager().setOffset(0);
                            // Re-initialize the Training Data.
                            MainActivity.this.onInitializeData(MainActivity.this.getTraining(), MainActivity.LENGTH_CHART_HISTORY);
                            // Assert that we're recording.
                            MainActivity.this.onFeedbackRecording();
                        } break;
                        case RECOGNITION  : {
                            // Reset the Recognition History.
                            for(final List<Float> lRecognition : MainActivity.this.getRecognitionHistory()) {
                                // Clear the Recognition List.
                                lRecognition.clear();
                            }
                            // Reset the Recognition Chart.
                            MainActivity.this.getRecognitionChartManager().setOffset(0);
                            // Re-initialize the Recognition Data.
                            MainActivity.this.onInitializeData(MainActivity.this.getRecognition(), MainActivity.LENGTH_CHART_HISTORY);
                            // Assert that we're listening.
                            MainActivity.this.onFeedbackRecognition();
                        } break;
                    }
                    // Assert that we're now responsive.
                    MainActivity.this.setResponsive(true);
                } break;
                /** Once the user has stopped touching the graphs... */
                case MotionEvent.ACTION_UP   : {
                    // We're no longer responsive.
                    MainActivity.this.setResponsive(false);
                    // Hide the FeedbackLayout.
                    MainActivity.this.onHideFeedback();
                    // Handle the Mode.
                    switch(MainActivity.this.getMode()) {
                        case TRAINING     : {

                        } break;
                        case RECOGNITION  : {
                            // Ensure the ObscureLayout is visible.
                            MainActivity.this.getObscureLayout().setVisibility(View.VISIBLE);
                            // Launch an AsyncTask.
                            final AsyncTask lAsyncTask = new AsyncTask() { @Override protected final Object doInBackground(final Object ... pObjects) {
                                // Declare the Averages.
                                final double[] lAverages = new double[3];
                                // Declare the Dynamic Time Warping Algorithm.
                                final DTW      lDTW      = new DTW();
                                // Iterate the Histories.
                                for(int i = 0; i < 3; i++) {
                                    // Fetch the Primitive Histories for this Axis.
                                    final float[] lTraining    = MainActivity.primitive(MainActivity.this.getTrainingHistory()[i]);
                                    final float[] lRecognition = MainActivity.primitive(MainActivity.this.getRecognitionHistory()[i]);
                                    // Calculate the distance using Dynamic Time Warping.
                                                  lAverages[i] = lDTW.compute(lRecognition, lTraining).getDistance();
                                }
                                // Linearize execution on the UI Thread.
                                MainActivity.this.runOnUiThread(new Runnable() { @Override public final void run() {
                                    // Allow the layout to be interacted with again.
                                    MainActivity.this.getObscureLayout().setVisibility(View.GONE);
                                    // Print the Result.
                                    Toast.makeText(MainActivity.this, "D(X:" + lAverages[0] + ", Y:" + lAverages[1] + ", Z:" + lAverages[2] + ")", Toast.LENGTH_LONG).show();
                                } });
                                // Satisfy the compiler.
                                return null;
                            } };
                            // Execute the AsyncTask.
                            lAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
                        } break;
                    }
                    // Re-enable the Switch.
                    MainActivity.this.getModeSwitch().setEnabled(true);
                } break;
            }
            // Consume all touch data.
            return true;
        } });
        // Hide the Feedback Layout.
        this.onHideFeedback();
    }

    /** Resets a Chart. */
    private final void onInitializeData(final LineDataSet[] pDataSet, final int pHistoryLength) {
        // Ensure the DataSets are empty.
        for(final LineDataSet lLineDataSet : pDataSet) {
            // Clear the DataSet.
            lLineDataSet.clear();
        }
        // Initialize the Acceleration Charts.
        for(int i = 0; i < pHistoryLength; i++) {
            // Allocate a the default Entry.
            final Entry lEntry = new Entry(i, 0);
            // Iterate the DataSets.
            for(final LineDataSet lLineDataSet : pDataSet) {
                // Buffer the Entry.
                lLineDataSet.addEntry(lEntry);
            }
        }
    }

    /** Hides the Feedback View. */
    private final void onHideFeedback() {
        // Hide the Layout.
        this.getFeedbackView().setVisibility(View.GONE);
        // Make the Background Color Transparent.
        this.getFeedbackLayout().setBackgroundColor(ContextCompat.getColor(this, R.color.colorTransparent));
    }

    /** Asserts that we're recording. */
    private final void onFeedbackRecording() {
        // Show the FeedbackView.
        this.getFeedbackView().setVisibility(View.VISIBLE);
        // Set the ImageView.
        this.getFeedbackView().setImageResource(R.drawable.ic_voicemail_black_24dp);
        // Make the Background Color Visible.
        this.getFeedbackLayout().setBackgroundColor(ContextCompat.getColor(this, R.color.colorMature));
    }

    /** Asserts that we're recognizing. */
    private final void onFeedbackRecognition() {
        // Show the FeedbackView.
        this.getFeedbackView().setVisibility(View.VISIBLE);
        // Set the ImageView.
        this.getFeedbackView().setImageResource(R.drawable.ic_gesture_black_24dp);
        // Make the Background Color Visible.
        this.getFeedbackLayout().setBackgroundColor(ContextCompat.getColor(this, R.color.colorMature));
    }

    /** Simple return for a resource reference. */
    private final <T extends View> T $(final int pId) {
        // Return the type-casted View.
        return (T)this.findViewById(pId);
    }

    /** Handle a change to sensor data. */
    @Override public final void onSensorChanged(final SensorEvent pSensorEvent) {
        // Are we handling accelerometer data?
        if(pSensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Update the LineChartManager.
            this.getAccChartManager().onUpdateChart(pSensorEvent.values);
            // Are we Responsive?
            if(MainActivity.this.isResponsive()) {
                // Handle the Mode.
                switch(MainActivity.this.getMode()) {
                    /** Are we training? */
                    case TRAINING    : {
                        // Update the Training Chart.
                        this.getTrainChartManager().onUpdateChart(pSensorEvent.values);
                    } break;
                    /** Are we recognizing? */
                    case RECOGNITION : {
                        // Update the Training Chart.
                        this.getRecognitionChartManager().onUpdateChart(pSensorEvent.values);
                    } break;
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        /** TODO: */
    }

    /** When the Activity is resumed. */
    @Override protected final void onResume() {
        // Implement the Parent Definition.
        super.onResume();
        // Register for updates on the SensorManager. (We want to listen to accelerometer data.)
        this.getSensorManager().registerListener(this, this.getSensorManager().getDefaultSensor(Sensor.TYPE_ACCELEROMETER), MainActivity.DELAY_SENSOR);
    }

    /** When the Activity is paused. */
    @Override protected final void onPause() {
        // Implement the Parent Definition.
        super.onPause();
        // Stop listening for accelerometer data.
        this.getSensorManager().unregisterListener(this);
    }

    /* Getters. */
    private final void setMode(final EMode pMode) {
        this.mMode = pMode;
    }

    private final EMode getMode() {
        return this.mMode;
    }

    private final void setResponsive(final boolean pIsResponsive) {
        this.mResponsive = pIsResponsive;
    }

    private final boolean isResponsive() {
        return this.mResponsive;
    }

    private final SensorManager getSensorManager() {
        return this.mSensorManager;
    }

    private final LineChart getLineAcc() {
        return this.mLineAcc;
    }

    private final LineChart getLineTrain() {
        return this.mLineTrain;
    }

    private final LineChart getLineRecognition() {
        return this.mLineRecognition;
    }

    private final RelativeLayout getFeedbackLayout() {
        return this.mFeedbackLayout;
    }

    private final ImageView getFeedbackView() {
        return this.mFeedbackView;
    }

    private final RelativeLayout getObscureLayout() {
        return this.mObscureLayout;
    }

    private final TextView getModeTitle() {
        return this.mModeTitle;
    }

    private final TextView getModeDescription() {
        return this.mModeDescription;
    }

    private final Switch getModeSwitch() {
        return this.mModeSwitch;
    }

    private final LineData getAccData() {
        return this.mAccData;
    }

    private final LineData getTrainData() {
        return this.mTrainData;
    }

    private final LineData getRecognitionData() {
        return this.mRecognitionData;
    }

    private final LineDataSet[] getAcceleration() {
        return this.mAcceleration;
    }

    private final LineDataSet[] getTraining() {
        return this.mTraining;
    }

    private final LineDataSet[] getRecognition() {
        return this.mRecognition;
    }

    private final LineChartManager getAccChartManager() {
        return this.mAccChartManager;
    }

    private final LineChartManager getTrainChartManager() {
        return this.mTrainChartManager;
    }

    private final LineChartManager getRecognitionChartManager() {
        return this.mRecognitionChartManager;
    }

    private final List<Float>[] getTrainingHistory() {
        return this.mTrainingHistory;
    }

    private final List<Float>[] getRecognitionHistory() {
        return this.mRecognitionHistory;
    }

}
