package com.tamreeni.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

@CapacitorPlugin(name = "WorkoutTimer")
class WorkoutTimerPlugin : Plugin() {

    @PluginMethod
    fun startWorkout(call: PluginCall) {
        val intent = Intent(context, WorkoutForegroundService::class.java)
        intent.action = WorkoutForegroundService.ACTION_START_WORKOUT
        ContextCompat.startForegroundService(context, intent)
        call.resolve()
    }

    @PluginMethod
    fun resumeWorkout(call: PluginCall) {
        val intent = Intent(context, WorkoutForegroundService::class.java)
        intent.action = WorkoutForegroundService.ACTION_RESUME_WORKOUT
        ContextCompat.startForegroundService(context, intent)
        call.resolve()
    }

    @PluginMethod
    fun startRest(call: PluginCall) {
        val seconds = call.getInt("seconds") ?: 90
        val intent = Intent(context, WorkoutForegroundService::class.java)
        intent.action = WorkoutForegroundService.ACTION_START_REST
        intent.putExtra(WorkoutForegroundService.EXTRA_SECONDS, seconds)
        ContextCompat.startForegroundService(context, intent)
        call.resolve()
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        val intent = Intent(context, WorkoutForegroundService::class.java)
        context.stopService(intent)
        call.resolve()
    }

    @PluginMethod
    fun requestIgnoreBatteryOptimizations(call: PluginCall) {
        try {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val pkg = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm.isIgnoringBatteryOptimizations(pkg)) {
                val ret = JSObject()
                ret.put("alreadyIgnoring", true)
                call.resolve(ret)
                return
            }
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            intent.data = Uri.parse("package:$pkg")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            val ret = JSObject()
            ret.put("alreadyIgnoring", false)
            call.resolve(ret)
        } catch (e: Exception) {
            call.reject("failed", e)
        }
    }
}
