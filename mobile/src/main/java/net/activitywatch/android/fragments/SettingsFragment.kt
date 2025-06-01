package net.activitywatch.android.fragments

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import net.activitywatch.android.AWPreferences
import net.activitywatch.android.R
import net.activitywatch.android.RustInterface
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class SettingsFragment : Fragment() {

    private lateinit var prefs: AWPreferences
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        prefs = AWPreferences(requireContext())
        
        val serverSwitch = view.findViewById<Switch>(R.id.switch_local_server)
        val serverUrlText = view.findViewById<TextView>(R.id.text_server_url)
        val serverStatusText = view.findViewById<TextView>(R.id.text_server_status)
        val healthCheckButton = view.findViewById<Button>(R.id.button_health_check)
        val healthStatusText = view.findViewById<TextView>(R.id.text_health_status)
        
        // Display the server URL
        serverUrlText.text = AWPreferences.SERVER_URL
        
        // Set initial switch state based on actual server status
        val rustInterface = RustInterface.getInstance(requireContext())
        serverSwitch.isChecked = RustInterface.serverStarted
        updateServerStatus(serverStatusText, RustInterface.serverStarted)
        
        // Handle switch changes
        serverSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setLocalServerEnabled(isChecked)
            serverSwitch.isEnabled = false // Disable during operation
            serverStatusText.text = if (isChecked) "Starting server..." else "Stopping server..."
            
            // Perform server start/stop in background
            Executors.newSingleThreadExecutor().execute {
                if (isChecked) {
                    rustInterface.startServerTask(requireContext())
                    // Wait a bit for server to start
                    Thread.sleep(2000)
                } else {
                    rustInterface.stopServerTask()
                    // Wait a bit for server to stop
                    Thread.sleep(1000)
                }
                
                // Update UI on main thread
                handler.post {
                    serverSwitch.isEnabled = true
                    val actualState = RustInterface.serverStarted
                    serverSwitch.isChecked = actualState
                    updateServerStatus(serverStatusText, actualState)
                    
                    if (isChecked != actualState) {
                        Toast.makeText(
                            requireContext(),
                            if (isChecked) "Failed to start server" else "Failed to stop server",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        
        // Handle health check button click
        healthCheckButton.setOnClickListener {
            healthCheckButton.isEnabled = false
            healthStatusText.text = "Checking..."
            healthStatusText.setTextColor(Color.GRAY)
            
            performHealthCheck { status, latency, info ->
                handler.post {
                    healthCheckButton.isEnabled = true
                    when (status) {
                        HealthCheckStatus.SUCCESS -> {
                            val version = info?.optString("version", "unknown") ?: "unknown"
                            val hostname = info?.optString("hostname", "unknown") ?: "unknown"
                            healthStatusText.text = "✓ OK (${latency}ms)\n$version • $hostname"
                            healthStatusText.setTextColor(Color.parseColor("#4CAF50"))
                        }
                        HealthCheckStatus.ERROR -> {
                            healthStatusText.text = "✗ Error"
                            healthStatusText.setTextColor(Color.parseColor("#F44336"))
                        }
                        HealthCheckStatus.TIMEOUT -> {
                            healthStatusText.text = "✗ Timeout"
                            healthStatusText.setTextColor(Color.parseColor("#FF9800"))
                        }
                    }
                }
            }
        }
    }
    
    private fun updateServerStatus(statusText: TextView, isRunning: Boolean) {
        statusText.text = if (isRunning) {
            "Built-in server is running"
        } else {
            "Built-in server is stopped - use SSH tunnel or external server"
        }
    }
    
    private enum class HealthCheckStatus {
        SUCCESS, ERROR, TIMEOUT
    }
    
    private fun performHealthCheck(callback: (HealthCheckStatus, Long, JSONObject?) -> Unit) {
        Executors.newSingleThreadExecutor().execute {
            val startTime = System.currentTimeMillis()
            try {
                val url = URL("${AWPreferences.SERVER_URL}/api/0/info")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                
                val responseCode = connection.responseCode
                val latency = System.currentTimeMillis() - startTime
                
                if (responseCode == 200) {
                    // Read the response
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    
                    try {
                        val jsonResponse = JSONObject(response)
                        callback(HealthCheckStatus.SUCCESS, latency, jsonResponse)
                    } catch (e: Exception) {
                        // Response parsing failed, but connection was successful
                        callback(HealthCheckStatus.SUCCESS, latency, null)
                    }
                } else {
                    callback(HealthCheckStatus.ERROR, latency, null)
                }
                
                connection.disconnect()
            } catch (e: java.net.SocketTimeoutException) {
                val latency = System.currentTimeMillis() - startTime
                callback(HealthCheckStatus.TIMEOUT, latency, null)
            } catch (e: Exception) {
                val latency = System.currentTimeMillis() - startTime
                callback(HealthCheckStatus.ERROR, latency, null)
            }
        }
    }
}