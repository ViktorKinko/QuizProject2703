package com.bytepace.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

const val NOTIFICATION_ID: Int = 103
const val MILLIS_IN_SECOND: Long = 1000
const val ARG_START_TIME: String = "Start time"

class CountdownService : Service() {

    private lateinit var mBuilder: NotificationCompat.Builder

    companion object {
        fun newIntent(context: Context, duration: Long): Intent {
            val intent = Intent(context, CountdownService::class.java)
            intent.putExtra(ARG_START_TIME, duration)
            return intent
        }
    }

    private var isServiceInit = false

    override fun onBind(intent: Intent?): IBinder {
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isServiceInit) {
            intent ?: return START_NOT_STICKY
            countDown(intent.getLongExtra(ARG_START_TIME, 0))
            isServiceInit = true
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {

    }

    private fun countDown(time: Long) {
        secondsCountdownObservable(time)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribe(object : Subscriber<Long>() {
                    override fun onNext(t: Long?) {
                        startForeground(NOTIFICATION_ID, getOngoingLoadingIntent(t!!))
                    }

                    override fun onCompleted() {
                        stopForeground(true)
                    }

                    override fun onError(e: Throwable?) {}
                })

    }

    private fun secondsCountdownObservable(time: Long): Observable<Long> {
        return Observable.create<Long> {
            for (i in time downTo 1 step MILLIS_IN_SECOND) {
                it.onNext(i / MILLIS_IN_SECOND)
                SystemClock.sleep(MILLIS_IN_SECOND)
            }
            it.onCompleted()
        }
    }

    private fun getOngoingLoadingIntent(time: Long): Notification {
        val views = RemoteViews(packageName, R.layout.notification_countdown)
        views.setTextViewText(R.id.text, getString(R.string.notification_countdown, time))
        return buildNotification(views)
    }

    private fun buildNotification(views: RemoteViews): Notification {
        if (!this::mBuilder.isInitialized) {
            val channelId =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        createNotificationChannel()
                    else ""
            mBuilder = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Countdown")
                    .setContent(views)
                    .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                    .setAutoCancel(false)
                    .setOngoing(true)
        } else {
            mBuilder.setContent(views)
        }
        return mBuilder.build()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "countdown_quiz_service"
        val channelName = "Countdown foreground service"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }
}