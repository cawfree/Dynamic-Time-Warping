# Dynamic-Time-Warping
This application demonstrates how the Dynamic Time Warping (DTW) algorithm can be applied to recognizing the _shape_ of accelerometer waveform data. 

# How to Use
Download and run the application. In **Training** mode, tapping and holding on the screen will start recording accelerometer data, so hold down on the screen for the period of the gesture you'd like to record. Select **Recognition** mode by tapping the `Switch` on the bottom-right of the screen. In **Recognition** mode, recorded gestures are compared to those made in **Training** mode using the DTW algorithm. 

The error calculations, here referred to as _distance_, are returned to the user via a `Toast`. Each individual axis of the accelerometer has it's own distance measurement for the period of the gesture. 

## Dependencies
[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

## Credit
Java DTW provided by [GART](http://trac.research.cc.gatech.edu/gart/).
