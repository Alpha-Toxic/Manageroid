/**
 * 
 */
package com.manageroid.application.services;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;

import com.manageroid.application.proxy.AllTasks;
import com.manageroid.application.proxy.ManageroidTask;

/**
 * This is the main worker class of the application.
 * @author Administrator
 *
 */
public class ManageroidService extends Service {
//	private final static long UPDATE_INTERVAL = 60 * 5 * 1000; // 5 minutes
	private final static long UPDATE_INTERVAL = 10 * 1000; // seconds
	private final static long DELAY_INTERVAL = 0;
	
	private AllTasks tasks = null;

	/**
	 * @return
	 * 		{@link AllTasks} object containing all tasks in the manager
	 */
	public AllTasks getTasks() {
		return tasks;
	}

	/**
	 * @param value
	 * 		{@link AllTasks} object
	 */
	public void setTasks(AllTasks value) {
		this.tasks = value;
	}

	private Timer timer = new Timer();

	public static Date currentTime = null;
	public static Location currentLocation = null;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		super.onCreate();
		DebugLog.write("service onCreate");
		
		setTasks( AllTasks.getInstance());
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		_startService();
	}

	/*
	 * starting the service
	 */
	private void _startService() {
		LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		final ManageroidGPSLocationListener mlocListener = new ManageroidGPSLocationListener();
		mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);
        
        // read once on service start
		tasks.load();

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run()
			{
				DebugLog.write("in timer");
				boolean mustWriteTasks = false;
				currentTime = new Date();
				currentLocation = mlocListener.currentLocation;
				if (currentLocation != null) {
					double currentLat = currentLocation.getLongitude();
					double currentLong = currentLocation.getLatitude();
					DebugLog.write(currentLat + " " + currentLong);
				}
				
				if (!getTasks().isEmpty())
				{
					List<ManageroidTask> taskList = getTasks().getAllMyTasks();
					for (int i = taskList.size() - 1; i >= 0;--i)
	    			{
						ManageroidTask currentTask =  taskList.get(i);
	    				if(currentTime.after(currentTask.expirationDate))
	    				{
							if (currentTask.isActive && currentTask.isRevertable())
							{
								currentTask.revert();
							}
							tasks.remove(i);
							mustWriteTasks = true;
	    					continue;
	    				}
	
	    				if (currentTask.requirementsAreMet() && !currentTask.isActive)
	    				{
	    					currentTask.exec();
	    					currentTask.isActive = true;
	        				--currentTask.repeat;
	
	        				if(currentTask.repeat < 0 && !currentTask.isRevertable())
	    					{
	        					tasks.remove(i);
	    					}
	    					else
	    					{
	    						tasks.set(i, currentTask);
	    					}
	        				mustWriteTasks = true;
	    				}
	    				else if (!currentTask.requirementsAreMet() && currentTask.isActive)
	    				{
	    					if (currentTask.isRevertable())
	    					{
	    						currentTask.revert();
	    					}
	    					currentTask.isActive = false;

	        				if(currentTask.repeat < 0)
	    					{
	        					tasks.remove(i);
	    					}
	    					else
	    					{
	    						tasks.set(i, currentTask);
	    					}

	        				mustWriteTasks = true;
	    				}
	    			}
				}
				
		        // write if anything has changed
				if (mustWriteTasks)
				{
					DebugLog.write("archive " + tasks.size());
					tasks.archive();
				}
			}
		}, DELAY_INTERVAL, UPDATE_INTERVAL);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	    shutdownService();
	}

	/*
	 * Shutting down the service
	 */
	private void shutdownService() {
		if (timer != null)
			timer.cancel();
	}

	/** 
	 * Class Manageroid GPS Location Listener 
	 **/
	public class ManageroidGPSLocationListener implements LocationListener {
		public Location currentLocation = null;

		@Override
		public void onLocationChanged(Location newLocation) {
			currentLocation = newLocation;
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub
			
		}

	}
}
