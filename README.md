# Dynamic-Time-Warping
This Android application demonstrates how the [Dynamic Time Warping (DTW)](https://pdfs.semanticscholar.org/05a2/0cde15e172fc82f32774dd0cf4fe5827cad2.pdf) algorithm can be applied to recognizing the _shape_ of waveform data. This is a very useful ability to have for applications which need to interpret time-domain signals, such as physical gestures from an accelerometer.

![Dynamic Time Warping Example](http://i.imgur.com/iVxVr0B.png)

## How to Use
Download and run the application. In **Training** mode, tapping and holding on the screen will start recording accelerometer data, so hold down on the screen for the period of the gesture you'd like to record. Select **Recognition** mode by tapping the `Switch` on the bottom-right of the screen. In **Recognition** mode, recorded gestures are compared to those made in **Training** mode using the DTW algorithm. 

The error calculations, here referred to as _distance_, are returned to the user via a `Toast`. Each individual axis of the accelerometer has it's own distance measurement for the period of the gesture. 

## Dependencies
[MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

## Credit
Java DTW algorithm benevolently provided by [GART](http://trac.research.cc.gatech.edu/gart/). 

Show them some love! ♥

## [@cawfree](https://twitter.com/cawfree)

Open source takes a lot of work! If this project has helped you, please consider [buying me a coffee](https://www.buymeacoffee.com/cawfree). ☕ 

<p align="center">
  <a href="https://www.buymeacoffee.com/cawfree">
    <img src="https://cdn.buymeacoffee.com/buttons/default-orange.png" alt="Buy @cawfree a coffee" width="232" height="50" />
  </a>
</p>
