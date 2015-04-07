package com.tastes.app;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import com.tastes.R;
import com.tastes.util.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by marine1079 on 2015-03-29.
 *
 * Asynchronously handles an intent using a worker thread. Receives a ResultReceiver object and a
 * location through an intent. Tries to fetch the address for the location using a Geocoder, and
 * sends the result to the ResultReceiver.
 */
public class AddressService extends IntentService {
    private static final String TAG = "address-service";

    /**
     * The receiver where results are forwarded from this service.
     */
    protected ResultReceiver mReceiver;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public AddressService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    /**
     * Tries to get the location address using a Geocoder. If successful, sends an address to a
     * result receiver. If unsuccessful, sends an error message instead.
     * Note: We define a {@link android.os.ResultReceiver} in * MainActivity to process content
     * sent from this service.
     *
     * This service calls this method from the default worker thread with the intent that started
     * the service. When this method returns, the service automatically stops.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf(TAG, "No receiver received. There is nowhere to send the results.");
            return;
        }

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);

        //boolean isFull = intent.getBooleanExtra(Constants.ADDRESS_TYPE_EXTRA, false);// default 큰상관없지만 혹시 home에 의도치않게 표시될 경우를 대비해서 false.(locality)

        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (location == null) {
            errorMessage = getString(R.string.msg_no_location_data_provided);
            Log.wtf(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, null, errorMessage);
            return;
        }

        // Errors could still arise from using the Geocoder (for example, if there is no
        // connectivity, or if the Geocoder is given illegal location data). Or, the Geocoder may
        // simply not have an address for a location. In all these cases, we communicate with the
        // receiver using a resultCode indicating failure. If an address is found, we use a
        // resultCode indicating success.

        // The Geocoder used in this sample. The Geocoder's responses are localized for the given
        // Locale, which represents a specific geographical or linguistic region. Locales are used
        // to alter the presentation of information such as numbers or dates to suit the conventions
        // in the region they describe.
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        // Address found using the Geocoder.
        List<Address> addresses = null;

        try {
            // Using getFromLocation() returns an array of Addresses for the area immediately
            // surrounding the given latitude and longitude. The results are a best guess and are
            // not guaranteed to be accurate.
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, we get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.msg_service_not_available);
            Log.e(TAG, errorMessage, ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.msg_invalid_lat_long_used);
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " + location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.msg_no_address_found);
                Log.e(TAG, errorMessage);
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, null, errorMessage);
        } else {
            Address address = addresses.get(0);

            deliverResultToReceiver(Constants.SUCCESS_RESULT, address, null);
        }
        /*
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();
            String result = null;

            for(int i = 0; i <= address.getMaxAddressLineIndex(); i++) {// index이므로 <=까지 해줘야 한다.
                addressFragments.add(address.getAddressLine(i));
            }

            Log.i(TAG, getString(R.string.msg_address_found));
            if(isFull) {
                // 왜 blank 아니라 \n인지 모르겠지만 어차피 blank도 여러개 해도 하나만 되는 등(trim 같은 느낌) 일관성 없어서(다른 문자열들은 또 된다.) 되어 있던 separator 그대로 간다.
                result = TextUtils.join(System.getProperty("line.separator"), addressFragments);
            } else {
                List<String> stringList = new ArrayList<String>();

                //stringList.add(address.getSubThoroughfare());// null 확률 있음.(동 이하) => 번지수 될때 있음. 버림.
                stringList.add(address.getThoroughfare());// 동 정도.
                stringList.add(address.getSubLocality());// null 확률 있음.(중국으로 치면 현 정도)
                stringList.add(address.getLocality());// 시 정도.
                stringList.add(address.getAdminArea());// 도, 주 정도.
                stringList.add(address.getCountryName());

                for(String string : stringList) {
                    if(string != null) {
                        result = string;

                        break;
                    }
                }
            }

            deliverResultToReceiver(Constants.SUCCESS_RESULT, result, isFull);
            // 위처럼은 max가 인식 안되고, 나중에 한다 쳐도 세부 내용들 join하는 방식으로 간다. 현재는 바로 0 얻고 없으면 null 보낸다.
            //Log.i(TAG, getString(R.string.msg_address_found));
            //deliverResultToReceiver(Constants.SUCCESS_RESULT, isFull ? address.getAddressLine(0) : address.getThoroughfare(), isFull);// null일 수도 있을지 모르겠다.
        }
        */
    }

    /**
     * Sends a resultCode and message to the receiver.
     */
    private void deliverResultToReceiver(int resultCode, Address result, String extra) {// extra는 왠만하면 address null 주고 들어오는 err msg일 것이다.
        Bundle bundle = new Bundle();
        bundle.putParcelable(Constants.RESULT_DATA_KEY, result);
        bundle.putString(Constants.RESULT_DATA_EXTRA, extra);
        mReceiver.send(resultCode, bundle);
    }
}