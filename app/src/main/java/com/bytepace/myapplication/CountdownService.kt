package com.bytepace.myapplication

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
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
        val mBuilder = NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Countdown")
                .setContent(views)
                .setAutoCancel(false)
                .setOngoing(true)
        return mBuilder.build()
    }
}