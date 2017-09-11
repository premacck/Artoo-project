package com.prembros.artooproject;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * Created by Prem $ on 9/11/2017.
 */

class DirectionsJSONParser {

    /** Receives a JSONObject and returns a list of lists containing latitude and longitude */
    List<List<HashMap<String,String>>> parse(JSONObject jObject){

        List<List<HashMap<String, String>>> routes = new ArrayList<>() ;
        JSONArray jRoutes;
        JSONArray jLegs;
        JSONArray jSteps;

        try {

            jRoutes = jObject.getJSONArray("routes");

            /* Traversing all routes */
            for(int i = 0; i < jRoutes.length(); i++){
                jLegs = ( (JSONObject)jRoutes.get(i)).getJSONArray("legs");
                List<HashMap<String, String>> path = new ArrayList<>();

                /* Traversing all legs */
                for(int j = 0; j < jLegs.length(); j++){
                    jSteps = ( (JSONObject)jLegs.get(j)).getJSONArray("steps");

                    /* Traversing all steps */
                    for(int k = 0; k < jSteps.length(); k++){
                        String polyline;
                        polyline = (String)((JSONObject)((JSONObject)jSteps.get(k)).get("polyline")).get("points");
                        List<LatLng> list = decodePolyline(polyline);

                        /* Traversing all points */
                        for(int l = 0 ; l < list.size(); l++){
                            HashMap<String, String> hm = new HashMap<>();
                            hm.put("lat", Double.toString((list.get(l)).latitude) );
                            hm.put("lng", Double.toString((list.get(l)).longitude) );
                            path.add(hm);
                        }
                    }
                    routes.add(path);
                }
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return routes;
    }

    /**
     * Method to decode polyline points
     * Source : http://jeffreysambells.com/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java
     * */
    private List<LatLng> decodePolyline(String encodedString) {

        List<LatLng> polylineList = new ArrayList<>();
        int lat = 0;
        int lng = 0;
        int index = 0;
        int stringLength = encodedString.length();

        while (index < stringLength) {
            int b, shift = 0, result = 0;
            do {
                b = encodedString.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encodedString.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            polylineList.add(p);
        }

        return polylineList;
    }
}