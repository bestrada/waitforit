package com.bryanestrada.waitforit;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.widget.ListView;

/***
 * Listen for location changes and update the UI when we get them. 
 */
class TransitLocationListener implements LocationListener
{
	private final float _proximity;
	private final ListView _stops;

	/**
	 * Constructs a new TransitLocationListener
	 * @param proximity maximum distance (in meters) to which this listener will
	 * consider things "close" 
	 * @param stops the list that needs to be modified when the location is 
	 * updated
	 */
	TransitLocationListener(float proximity, ListView stops)
	{
		_proximity = proximity;
		_stops = stops;
	}
	

	public void onLocationChanged(Location position)
	{
		/*for (int i = 0; i < _stops.getCount(); i++)
		{
			
		}*/
	}

	public void onProviderDisabled(String provider)
	{
		
	}

	public void onProviderEnabled(String provider)
	{
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		
	}
	
	/**
	 * Compute the distance between two points where the first is a Location 
	 * object and the second is a cartesian lat-lon pair.
	 */
	private static float distanceBetween(Location loc, double lat, double lon)
	{
		// just construct the other location by copying the first one then 
		// moving its position.
		Location other = new Location(loc);
		other.setLatitude(lat);
		other.setLongitude(lon);
		
		return Math.abs(loc.distanceTo(other));
	}
}
