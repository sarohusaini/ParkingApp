package com.example.parkingapp

import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.parkingapp.ui.theme.ParkingAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Add the ImageView programmatically
        val imageView = ImageView(this)
        imageView.setImageResource(R.drawable.parkingsign)
        val params = RelativeLayout.LayoutParams(
            200,  // Set the desired width
            100) // Set the desired height
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        imageView.layoutParams = params

        // Find the layout and add the ImageView
        val layout = RelativeLayout(this)
        layout.addView(imageView)

        setContentView(layout)

        // Set the content for Compose
        setContent {
            ParkingAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ParkingAppTheme {
        Greeting("Android")
    }
}
