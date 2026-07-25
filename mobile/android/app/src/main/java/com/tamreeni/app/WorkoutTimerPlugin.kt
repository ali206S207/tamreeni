package com.tamreeni.app

import android.content.Intent
import androidx.core.content.ContextCompat
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
        intent.action = WorkoutForegroundService.ACTION_STOP
        ContextCompat.startForegroundService(context, intent)
        call.resolve()
    }
}
