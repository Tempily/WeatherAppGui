import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class WeatherApp {
    public static JSONObject getWeatherData(String locationName){
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            System.out.println("No location data found.");
            return null;
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,weather_code,wind_speed_10m,relative_humidity_2m&timezone=America%2FLos_Angeles";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);
            if(conn == null || conn.getResponseCode() != 200){
                System.out.println("Error fetching weather data.");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());

            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray weatherCodeData = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weatherCodeData.get(index));

            JSONArray relativeHumidityData = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidityData.get(index);

            JSONArray windSpeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windSpeed = (double) windSpeedData.get(index);

            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("wind_speed", windSpeed);

            return weatherData;

        } catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    public static JSONArray getLocationData(String locationName){
        locationName = locationName.replaceAll(" ", "+");
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);
            if(conn == null || conn.getResponseCode() != 200){
                System.out.println("Error getting location data.");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }
            scanner.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultsJsonObj = (JSONObject) parser.parse(resultJson.toString());

            return (JSONArray) resultsJsonObj.get("results");

        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();
            return conn;
        } catch (IOException e){
            e.printStackTrace();
            return null;
        }
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime)){
                return i;
            }
        }
        return 0; // Если не найдено точное совпадение, вернуть первый элемент
    }

    public static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        return currentDateTime.format(formatter);
    }

    private static String convertWeatherCode(long weatherCode){
        if(weatherCode == 0L){
            return "Clear";
        } else if(weatherCode >= 1L && weatherCode <= 3L){
            return "Cloudy";
        } else if((weatherCode >= 51L && weatherCode <= 67L) || (weatherCode >= 80L && weatherCode <= 99L)){
            return "Rain";
        } else if(weatherCode >= 71L && weatherCode <= 77L){
            return "Snow";
        }
        return "Unknown";
    }

    public static void main(String[] args) {
        JSONObject weatherData = getWeatherData("Berlin");
        if (weatherData != null) {
            System.out.println(weatherData.toJSONString());
        }
    }
}
