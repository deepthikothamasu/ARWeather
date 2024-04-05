using UnityEngine;
using UnityEngine.Networking;
using UnityEngine.UI;
using System.Collections;
using UnityEngine.Android;

[System.Serializable]
public class WeatherData
{
    public LocationData location;
    public CurrentData current;
}

[System.Serializable]
public class LocationData
{
    public string name;
}

[System.Serializable]
public class CurrentData
{
    public float temp_c;
    public ConditionData condition;
    public float wind_kph;
    public int humidity;
    public int pressure_mb;
}

[System.Serializable]
public class ConditionData
{
    public string text;
}

public class WeatherController : MonoBehaviour
{
    public string apiKey = "0c79af6c67d147fd9ce70938240301 ";
    public Text locationText;
    public Text temperatureText;
    public Text weatherText;
    public Text windText;
    public Text humidityText;
    public Text pressureText;
    public Image weatherImage;
    public int degree;
    private LocationInfo lastLocation;
    private string apiUrl;
    public float temperatureg;
    public Sprite sunnyImage, cloudyImage, mistImage, rainyImage, snowyImage, thunderstormImage, defaultImage;

    IEnumerator Start()
    {
        // Ask for location permission
        if (!Permission.HasUserAuthorizedPermission(Permission.FineLocation))
        {
            Permission.RequestUserPermission(Permission.FineLocation);
        }

        // Start location updates
        Input.location.Start();
        while (Input.location.status == LocationServiceStatus.Initializing)
        {
            yield return new WaitForSeconds(1);
        }

        if (Input.location.status == LocationServiceStatus.Failed)
        {
            Debug.Log("Unable to determine device location");
            yield break;
        }

        lastLocation = Input.location.lastData;

        apiUrl = "http://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + lastLocation.latitude + "," + lastLocation.longitude;

        StartCoroutine(GetWeatherData());
    }
    IEnumerator GetWeatherData()
    {
        UnityWebRequest www = UnityWebRequest.Get(apiUrl);
        yield return www.SendWebRequest();

        if (www.result != UnityWebRequest.Result.Success)
        {
            Debug.Log(www.error);
            yield break;
        }

        WeatherData weatherData = JsonUtility.FromJson<WeatherData>(www.downloadHandler.text);

        string location = weatherData.location.name;
        string temperature = weatherData.current.temp_c.ToString();
        string weather = weatherData.current.condition.text;
        string wind = weatherData.current.wind_kph.ToString();
        string humidity = weatherData.current.humidity.ToString();
        string pressure = weatherData.current.pressure_mb.ToString();
        temperatureg = weatherData.current.temp_c;
        locationText.text = "CITY:" + location;
        if (degree == 0)
        {
            temperatureText.text = "TEMPERATURE:" + temperature + "°C";
        }
        else
        {
            temperatureText.text = "TEMPERATURE:" + ((float.Parse(temperature) * 1.8f) + 32) + "°C";
        }
        weatherText.text = "WEATHER:" + weather;
        windText.text = "WIND SPEED:" + wind + " kph";
        humidityText.text = "Humidity:" + humidity + "%";
        pressureText.text = "Pressure: " + pressure + " mb";

        // Show the appropriate weather image
        switch (weather.ToLower())
        {
            case "clear":
            case "sunny":
                weatherImage.sprite = sunnyImage;
                break;
            case "partly cloudy":
            case "cloudy":
                weatherImage.sprite = cloudyImage;
                break;
            case "mist":
            case "fog":
            case "haze":
            case "smoke":
                weatherImage.sprite = mistImage;
                break;
            case "rain":
            case "moderate rain":
            case "heavy rain":
                weatherImage.sprite = rainyImage;
                break;
            case "snow":
            case "light snow":
            case "heavy snow":
                weatherImage.sprite = snowyImage;
                break;
            case "thunderstorm":
                weatherImage.sprite = thunderstormImage;
                break;
            default:
                weatherImage.sprite = defaultImage;
                break;
        }

        // Stop location updates
        Input.location.Stop();
    }

    public void ConvertTemp()
    {
        if(degree==0)
        {
            degree = 1;
            temperatureText.text = "TEMPERATURE:" + temperatureg + "°C";
        }
        else
        {
            degree = 0;
            temperatureText.text = "TEMPERATURE:" + ((temperatureg * 1.8f) + 32) + "°F";
        }
    }
}
