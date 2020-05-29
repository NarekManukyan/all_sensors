package com.cindyu.all_sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import static android.content.Context.POWER_SERVICE;

/** AllSensorsPlugin */
public class AllSensorsPlugin implements EventChannel.StreamHandler {
  /** Plugin registration. */
  private static final String ACCELEROMETER_CHANNEL_NAME =
          "cindyu.com/all_sensors/accelerometer";
  private static final String GYROSCOPE_CHANNEL_NAME = "cindyu.com/all_sensors/gyroscope";
  private static final String USER_ACCELEROMETER_CHANNEL_NAME =
          "cindyu.com/all_sensors/user_accel";
  private static final String PROXIMITY_CHANNELNAME =
          "cindyu.com/all_sensors/proximity";

  private static PowerManager powerManager;
  private static PowerManager.WakeLock wakeLock;
  private static int field = 0x00000020;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    try {
      field = PowerManager.class.getField("PROXIMITY_SCREEN_OFF_WAKE_LOCK").getInt(null);
    } catch (Throwable ignored) {
    }

    powerManager = (PowerManager) registrar.context().getSystemService(POWER_SERVICE);
    wakeLock = powerManager.newWakeLock(field, "AllSensors::Wakelock");


    final EventChannel accelerometerChannel =
            new EventChannel(registrar.messenger(), ACCELEROMETER_CHANNEL_NAME);
    accelerometerChannel.setStreamHandler(
            new AllSensorsPlugin(registrar.context(), Sensor.TYPE_ACCELEROMETER));

    final EventChannel userAccelChannel =
            new EventChannel(registrar.messenger(), USER_ACCELEROMETER_CHANNEL_NAME);
    userAccelChannel.setStreamHandler(
            new AllSensorsPlugin(registrar.context(), Sensor.TYPE_LINEAR_ACCELERATION));

    final EventChannel gyroscopeChannel =
            new EventChannel(registrar.messenger(), GYROSCOPE_CHANNEL_NAME);
    gyroscopeChannel.setStreamHandler(
            new AllSensorsPlugin(registrar.context(), Sensor.TYPE_GYROSCOPE));

    final EventChannel proximityChannel =
            new EventChannel(registrar.messenger(), PROXIMITY_CHANNELNAME);
    proximityChannel.setStreamHandler(
            new AllSensorsPlugin(registrar.context(), Sensor.TYPE_PROXIMITY));
  }

  private SensorEventListener sensorEventListener;
  private final SensorManager sensorManager;
  private final Sensor sensor;

  private AllSensorsPlugin(Context context, int sensorType) {
    sensorManager = (SensorManager) context.getSystemService(context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(sensorType);
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    sensorEventListener = createSensorEventListener(events);
    sensorManager.registerListener(sensorEventListener, sensor, sensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  public void onCancel(Object arguments) {
    sensorManager.unregisterListener(sensorEventListener);
  }

  SensorEventListener createSensorEventListener(final EventChannel.EventSink events) {
    return new SensorEventListener() {
      @Override
      public void onAccuracyChanged(Sensor sensor, int accuracy) {}

      @Override
      public void onSensorChanged(SensorEvent event) {
        double[] sensorValues = new double[event.values.length];
        for (int i = 0; i < event.values.length; i++) {
          sensorValues[i] = event.values[i];
        }
        if(event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
          setWakeLock(sensorValues[0]);
        }
        events.success(sensorValues);
      }
    };
  }

  private void setWakeLock (double value) {
    if(value == 0) wakeLock.acquire();
    else wakeLock.release();
  }
}
