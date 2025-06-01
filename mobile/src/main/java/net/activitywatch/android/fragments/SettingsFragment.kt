package net.activitywatch.android.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import net.activitywatch.android.AWPreferences
import net.activitywatch.android.R
import net.activitywatch.android.RustInterface
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
    }
    
    private fun updateServerStatus(statusText: TextView, isRunning: Boolean) {
        statusText.text = if (isRunning) {
            "Built-in server is running"
        } else {
            "Built-in server is stopped - use SSH tunnel or external server"
        }
    }
}