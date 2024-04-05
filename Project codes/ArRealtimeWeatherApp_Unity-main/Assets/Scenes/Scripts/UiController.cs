using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class UiController : MonoBehaviour
{
   public void ToggleCel()
    {
       FindObjectOfType<WeatherController>().ConvertTemp();
    }
}
